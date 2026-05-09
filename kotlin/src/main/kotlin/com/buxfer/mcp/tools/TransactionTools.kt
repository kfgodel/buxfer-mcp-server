package com.buxfer.mcp.tools

import com.buxfer.mcp.api.BuxferClient
import com.buxfer.mcp.api.buxferJson
import com.buxfer.mcp.api.models.AddTransactionParams
import com.buxfer.mcp.api.models.TransactionFilters
import io.modelcontextprotocol.kotlin.sdk.types.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import io.modelcontextprotocol.kotlin.sdk.types.ToolSchema
import kotlinx.serialization.json.JsonObject
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

        private val statusEnum = listOf("pending", "cleared", "reconciled")

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
                    put("description", "Transaction status")
                    putJsonArray("enum") { statusEnum.forEach { add(it) } }
                })
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
                    put("description", "Transaction status")
                    putJsonArray("enum") { statusEnum.forEach { add(it) } }
                })
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
        val params = AddTransactionParams(
            description = args.requireString("description"),
            amount = args.requireDouble("amount"),
            accountId = args.requireInt("accountId"),
            date = args.requireString("date"),
            type = args.requireString("type"),
            tags = args.optString("tags"),
            status = args.optString("status"),
        )
        val tx = client.addTransaction(params)
        CallToolResult(content = listOf(TextContent(buxferJson.encodeToString(tx))))
    }.getOrElse { e ->
        log.error("tool=buxfer_add_transaction failed", e)
        CallToolResult(content = listOf(TextContent("Error: ${e.message}")), isError = true)
    }

    suspend fun editTransaction(args: JsonObject?): CallToolResult = runCatching {
        logToolEntry("buxfer_edit_transaction", args)
        val id = args.requireInt("id")
        val params = AddTransactionParams(
            description = args.requireString("description"),
            amount = args.requireDouble("amount"),
            accountId = args.requireInt("accountId"),
            date = args.requireString("date"),
            type = args.requireString("type"),
            tags = args.optString("tags"),
            status = args.optString("status"),
        )
        val tx = client.editTransaction(id, params)
        CallToolResult(content = listOf(TextContent(buxferJson.encodeToString(tx))))
    }.getOrElse { e ->
        log.error("tool=buxfer_edit_transaction failed", e)
        CallToolResult(content = listOf(TextContent("Error: ${e.message}")), isError = true)
    }

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
