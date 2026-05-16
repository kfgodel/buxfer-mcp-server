package com.buxfer.mcp.tools

import com.buxfer.mcp.api.BuxferClient
import com.buxfer.mcp.api.buxferJson
import com.buxfer.mcp.api.models.AddTransactionParams
import com.buxfer.mcp.api.models.TransactionFilters
import io.modelcontextprotocol.kotlin.sdk.types.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import io.modelcontextprotocol.kotlin.sdk.types.ToolSchema
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import org.slf4j.LoggerFactory

class TransactionTools(private val client: BuxferClient) {

    companion object {
        private val log = LoggerFactory.getLogger(TransactionTools::class.java)

        /**
         * Always-redacted arg keys (mirrored in logback.xml header).
         *  - statement: raw bank-statement text (often many KB; would leak transaction history wholesale).
         *  - password : credential, not currently a tool arg but listed defensively in case it is added later.
         */
        private val redactedArgs = setOf("statement", "password")

        // Enum values per shared/api-spec/buxfer-api.md "Transaction Types" section.
        private val transactionTypeEnum = listOf(
            "expense", "income", "refund", "payment", "transfer",
            "investment_buy", "investment_sell", "investment_dividend",
            "capital_gain", "capital_loss",
            "sharedBill", "paidForFriend", "settlement", "loan",
        )

        // Used by the /transactions filter, which accepts all three lifecycle states as
        // query values per the upstream docs.
        private val statusEnum = listOf("pending", "cleared", "reconciled")

        // Used by transaction_add / transaction_edit input schemas. `reconciled` is a state
        // a transaction reaches via reconciliation actions on Buxfer's side, not via direct
        // create/edit write — per the upstream docs only `pending` and `cleared` are valid
        // here. See `shared/api-spec/buxfer-api.md`.
        private val writableStatusEnum = listOf("pending", "cleared")

        val LIST_TRANSACTIONS_INPUT_SCHEMA = ToolSchema(
            properties = buildJsonObject {
                put("accountId",   buildJsonObject { put("type", "integer"); put("description", "Filter by account ID") })
                put("accountName", buildJsonObject { put("type", "string");  put("description", "Filter by account name (alternative to accountId)") })
                put("tagId",       buildJsonObject { put("type", "integer"); put("description", "Filter by tag ID") })
                put("tagName",     buildJsonObject { put("type", "string");  put("description", "Filter by tag name") })
                put("startDate",   buildJsonObject { put("type", "string");  put("description", "Start date in YYYY-MM-DD format") })
                put("endDate",     buildJsonObject { put("type", "string");  put("description", "End date in YYYY-MM-DD format") })
                put("budgetId",    buildJsonObject { put("type", "integer"); put("description", "Filter by budget ID") })
                put("budgetName",  buildJsonObject { put("type", "string");  put("description", "Filter by budget name") })
                put("contactId",   buildJsonObject { put("type", "integer"); put("description", "Filter by contact ID") })
                put("contactName", buildJsonObject { put("type", "string");  put("description", "Filter by contact name") })
                put("groupId",     buildJsonObject { put("type", "integer"); put("description", "Filter by group ID") })
                put("groupName",   buildJsonObject { put("type", "string");  put("description", "Filter by group name") })
                put("status",      buildJsonObject {
                    put("type", "string")
                    put("description", "Transaction status filter")
                    putJsonArray("enum") { statusEnum.forEach { add(it) } }
                })
                put("page",        buildJsonObject { put("type", "integer"); put("description", "Page number (1-based, default 1)") })
            },
        )

        // Extra schema fragments for the per-type fields documented in
        // `shared/api-spec/buxfer-api.md`. Defined once and added to both ADD and EDIT
        // schemas — they share the same body shape on the wire.
        private fun JsonObjectBuilder.addPerTypeTransactionProperties() {
            put("fromAccountId", buildJsonObject {
                put("type", "integer")
                put("description", "For type=transfer: source account. Pair with toAccountId.")
            })
            put("toAccountId", buildJsonObject {
                put("type", "integer")
                put("description", "For type=transfer: destination account. Pair with fromAccountId (or accountId, which Buxfer treats as the source side).")
            })
            put("payers", buildJsonObject {
                put("type", "array")
                put("description", "Required for type=sharedBill. Who paid the bill and how much. Each entry is {email, amount}.")
                put("items", buildJsonObject {
                    put("type", "object")
                    put("properties", buildJsonObject {
                        put("email",  buildJsonObject { put("type", "string"); put("description", "Payer email (must be a Buxfer contact)") })
                        put("amount", buildJsonObject { put("type", "number"); put("description", "Amount this payer paid") })
                    })
                    putJsonArray("required") { add("email"); add("amount") }
                })
            })
            put("sharers", buildJsonObject {
                put("type", "array")
                put("description", "Required for type=sharedBill. Who shares the cost and (when isEvenSplit=false) how much each owes. Each entry is {email, amount?}.")
                put("items", buildJsonObject {
                    put("type", "object")
                    put("properties", buildJsonObject {
                        put("email",  buildJsonObject { put("type", "string"); put("description", "Sharer email (must be a Buxfer contact)") })
                        put("amount", buildJsonObject { put("type", "number"); put("description", "Amount this sharer owes; omit when isEvenSplit=true") })
                    })
                    putJsonArray("required") { add("email") }
                })
            })
            put("isEvenSplit", buildJsonObject {
                put("type", "boolean")
                put("description", "For type=sharedBill. When true, Buxfer divides the total evenly across sharers (omit per-sharer amounts).")
            })
            put("loanedBy", buildJsonObject {
                put("type", "string")
                put("description", "Required for type=loan. UID or email of the lender.")
            })
            put("borrowedBy", buildJsonObject {
                put("type", "string")
                put("description", "Required for type=loan. UID or email of the borrower.")
            })
            put("paidBy", buildJsonObject {
                put("type", "string")
                put("description", "Required for type=paidForFriend. UID or email of who paid.")
            })
            put("paidFor", buildJsonObject {
                put("type", "string")
                put("description", "Required for type=paidForFriend. UID or email of the beneficiary.")
            })
        }

        val ADD_TRANSACTION_INPUT_SCHEMA = ToolSchema(
            properties = buildJsonObject {
                put("description", buildJsonObject { put("type", "string");  put("description", "Transaction description") })
                put("amount",      buildJsonObject { put("type", "number");  put("description", "Transaction amount") })
                put("accountId",   buildJsonObject { put("type", "integer"); put("description", "Account to post to") })
                put("date",        buildJsonObject { put("type", "string");  put("description", "Date in YYYY-MM-DD format") })
                put("type",        buildJsonObject {
                    put("type", "string")
                    put("description", "Transaction type")
                    putJsonArray("enum") { transactionTypeEnum.forEach { add(it) } }
                })
                put("tags",        buildJsonObject { put("type", "string");  put("description", "Comma-separated tag names") })
                put("status",      buildJsonObject {
                    put("type", "string")
                    put("description", "Transaction status at create time (reconciled is reached via reconciliation, not direct write)")
                    putJsonArray("enum") { writableStatusEnum.forEach { add(it) } }
                })
                addPerTypeTransactionProperties()
            },
            required = listOf("description", "amount", "accountId", "date", "type"),
        )

        val EDIT_TRANSACTION_INPUT_SCHEMA = ToolSchema(
            properties = buildJsonObject {
                put("id",          buildJsonObject { put("type", "integer"); put("description", "Transaction ID to edit") })
                put("description", buildJsonObject { put("type", "string");  put("description", "Transaction description") })
                put("amount",      buildJsonObject { put("type", "number");  put("description", "Transaction amount") })
                put("accountId",   buildJsonObject { put("type", "integer"); put("description", "Account the transaction belongs to") })
                put("date",        buildJsonObject { put("type", "string");  put("description", "Date in YYYY-MM-DD format") })
                put("type",        buildJsonObject {
                    put("type", "string")
                    put("description", "Transaction type")
                    putJsonArray("enum") { transactionTypeEnum.forEach { add(it) } }
                })
                put("tags",        buildJsonObject { put("type", "string");  put("description", "Comma-separated tag names") })
                put("status",      buildJsonObject {
                    put("type", "string")
                    put("description", "Transaction status (reconciled is reached via reconciliation, not direct write)")
                    putJsonArray("enum") { writableStatusEnum.forEach { add(it) } }
                })
                addPerTypeTransactionProperties()
            },
            required = listOf("id", "description", "amount", "accountId", "date", "type"),
        )

        val DELETE_TRANSACTION_INPUT_SCHEMA = ToolSchema(
            properties = buildJsonObject {
                put("id", buildJsonObject { put("type", "integer"); put("description", "Transaction ID to delete") })
            },
            required = listOf("id"),
        )

        val UPLOAD_STATEMENT_INPUT_SCHEMA = ToolSchema(
            properties = buildJsonObject {
                put("accountId",  buildJsonObject { put("type", "integer"); put("description", "Account to import statement into") })
                put("statement",  buildJsonObject { put("type", "string");  put("description", "Raw bank statement text") })
                put("dateFormat", buildJsonObject { put("type", "string");  put("description", "Optional date format hint for parsing") })
            },
            required = listOf("accountId", "statement"),
        )
    }

    private fun logToolEntry(name: String, args: JsonObject?) {
        // INFO: keys only (always safe to log even when sensitive values are present).
        if (args == null || args.isEmpty()) {
            log.info("tool={}", name)
        } else {
            log.info("tool={} args={}", name, args.keys.toList())
        }
        // DEBUG: full key=value rendering, with always-redacted values replaced.
        // log.isDebugEnabled gate keeps the formatting cost off the hot path at INFO.
        if (log.isDebugEnabled && args != null && args.isNotEmpty()) {
            val pairs = args.entries.joinToString(", ") { (k, v) ->
                val rendered = if (k in redactedArgs) {
                    val len = (v as? JsonPrimitive)?.contentOrNull?.length ?: 0
                    "<<redacted $len chars>>"
                } else {
                    v.toString()
                }
                "$k=$rendered"
            }
            log.debug("tool={} args={{{}}}", name, pairs)
        }
    }

    suspend fun listTransactions(args: JsonObject?): CallToolResult = runCatching {
        logToolEntry("buxfer_list_transactions", args)
        val filters = TransactionFilters(
            accountId = args?.get("accountId")?.jsonPrimitive?.intOrNull,
            accountName = args?.get("accountName")?.jsonPrimitive?.contentOrNull,
            tagId = args?.get("tagId")?.jsonPrimitive?.intOrNull,
            tagName = args?.get("tagName")?.jsonPrimitive?.contentOrNull,
            startDate = args?.get("startDate")?.jsonPrimitive?.contentOrNull,
            endDate = args?.get("endDate")?.jsonPrimitive?.contentOrNull,
            budgetId = args?.get("budgetId")?.jsonPrimitive?.intOrNull,
            budgetName = args?.get("budgetName")?.jsonPrimitive?.contentOrNull,
            contactId = args?.get("contactId")?.jsonPrimitive?.intOrNull,
            contactName = args?.get("contactName")?.jsonPrimitive?.contentOrNull,
            groupId = args?.get("groupId")?.jsonPrimitive?.intOrNull,
            groupName = args?.get("groupName")?.jsonPrimitive?.contentOrNull,
            status = args?.get("status")?.jsonPrimitive?.contentOrNull,
            page = args?.get("page")?.jsonPrimitive?.intOrNull
        )
        val result = client.getTransactions(filters)
        CallToolResult(content = listOf(TextContent(buxferJson.encodeToString(result))))
    }.getOrElse { e ->
        log.error("tool=buxfer_list_transactions failed", e)
        CallToolResult(content = listOf(TextContent("Error: ${e.message}")), isError = true)
    }

    suspend fun addTransaction(args: JsonObject?): CallToolResult = runCatching {
        logToolEntry("buxfer_add_transaction", args)
        val params = parseAddTransactionParams(args)
        val tx = client.addTransaction(params)
        CallToolResult(content = listOf(TextContent(buxferJson.encodeToString(tx))))
    }.getOrElse { e ->
        log.error("tool=buxfer_add_transaction failed", e)
        CallToolResult(content = listOf(TextContent("Error: ${e.message}")), isError = true)
    }

    suspend fun editTransaction(args: JsonObject?): CallToolResult = runCatching {
        logToolEntry("buxfer_edit_transaction", args)
        val id = args.requireInt("id")
        val params = parseAddTransactionParams(args)
        val tx = client.editTransaction(id, params)
        CallToolResult(content = listOf(TextContent(buxferJson.encodeToString(tx))))
    }.getOrElse { e ->
        log.error("tool=buxfer_edit_transaction failed", e)
        CallToolResult(content = listOf(TextContent("Error: ${e.message}")), isError = true)
    }

    /**
     * Shared arg → params parser for add and edit. Keeping the field list in one place
     * matches [com.buxfer.mcp.api.BuxferClient.appendTransactionFields] on the wire side
     * and prevents create/update drift as new fields are added.
     */
    private fun parseAddTransactionParams(args: JsonObject?): AddTransactionParams =
        AddTransactionParams(
            description = args.requireString("description"),
            amount = args.requireDouble("amount"),
            accountId = args.requireInt("accountId"),
            date = args.requireString("date"),
            type = args.requireString("type"),
            tags = args.optString("tags"),
            status = args.optString("status"),
            fromAccountId = args.optInt("fromAccountId"),
            toAccountId = args.optInt("toAccountId"),
            payers = args.optPayerShareList("payers"),
            sharers = args.optPayerShareList("sharers"),
            isEvenSplit = args.optBoolean("isEvenSplit"),
            loanedBy = args.optString("loanedBy"),
            borrowedBy = args.optString("borrowedBy"),
            paidBy = args.optString("paidBy"),
            paidFor = args.optString("paidFor"),
        )

    suspend fun deleteTransaction(args: JsonObject?): CallToolResult = runCatching {
        logToolEntry("buxfer_delete_transaction", args)
        val id = args.requireInt("id")
        // Forward Buxfer's actual response body (typically `{"status":"OK"}`). No
        // synthesis: the previous version hardcoded `{"deleted":true,"id":<id>}`,
        // which lied about what the API returned. Non-OK status is already turned
        // into a thrown BuxferApiException by client.deleteTransaction and lands
        // here as isError=true via the `runCatching` catch block.
        val body = client.deleteTransaction(id)
        CallToolResult(content = listOf(TextContent(buxferJson.encodeToString(body))))
    }.getOrElse { e ->
        log.error("tool=buxfer_delete_transaction failed", e)
        CallToolResult(content = listOf(TextContent("Error: ${e.message}")), isError = true)
    }

    suspend fun uploadStatement(args: JsonObject?): CallToolResult = runCatching {
        // The 'statement' arg is stripped from DEBUG output by logToolEntry's redactedArgs set.
        // See logback.xml header for the full redaction policy.
        logToolEntry("buxfer_upload_statement", args)
        val accountId = args.requireInt("accountId")
        val statement = args.requireString("statement")
        val dateFormat = args.optString("dateFormat")
        val result = client.uploadStatement(accountId, statement, dateFormat)
        CallToolResult(content = listOf(TextContent(buxferJson.encodeToString(result))))
    }.getOrElse { e ->
        log.error("tool=buxfer_upload_statement failed", e)
        CallToolResult(content = listOf(TextContent("Error: ${e.message}")), isError = true)
    }
}
