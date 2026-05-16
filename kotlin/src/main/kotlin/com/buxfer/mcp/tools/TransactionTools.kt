package com.buxfer.mcp.tools

import com.buxfer.mcp.api.BuxferClient
import com.buxfer.mcp.api.buxferJson
import com.buxfer.mcp.api.models.AddTransactionParams
import com.buxfer.mcp.api.models.PayerShare
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
                put("description", "Required for type=sharedBill. Who paid the bill and how much. Each entry is {email, amount}. The current user MUST be one of the payers — use {\"email\": \"me\", ...} for their entry (the server resolves \"me\" to the credentials' email, so the MCP client never has to know it).")
                put("items", buildJsonObject {
                    put("type", "object")
                    put("properties", buildJsonObject {
                        put("email",  buildJsonObject { put("type", "string"); put("description", "Payer email, or the literal \"me\" for the current user") })
                        put("amount", buildJsonObject { put("type", "number"); put("description", "Amount this payer paid") })
                    })
                    putJsonArray("required") { add("email"); add("amount") }
                })
            })
            put("sharers", buildJsonObject {
                put("type", "array")
                put("description", "Required for type=sharedBill. Who shares the cost and (when isEvenSplit=false) how much each owes. Each entry is {email, amount?}. The current user MUST be one of the sharers — use {\"email\": \"me\", ...} for their entry.")
                put("items", buildJsonObject {
                    put("type", "object")
                    put("properties", buildJsonObject {
                        put("email",  buildJsonObject { put("type", "string"); put("description", "Sharer email, or the literal \"me\" for the current user") })
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
                put("description", "Required for type=loan. UID or email of the lender. Exactly one of loanedBy / borrowedBy MUST be the current user — pass \"me\" for whichever side they're on (loanedBy=\"me\" if the user lent, borrowedBy=\"me\" if the user borrowed).")
            })
            put("borrowedBy", buildJsonObject {
                put("type", "string")
                put("description", "Required for type=loan. UID or email of the borrower. See loanedBy for the \"me\" placement rule.")
            })
            put("paidBy", buildJsonObject {
                put("type", "string")
                put("description", "Required for type=paidForFriend. UID or email of who paid. Exactly one of paidBy / paidFor MUST be the current user — pass \"me\" for whichever side they're on (paidBy=\"me\" if the user paid for a friend, paidFor=\"me\" if a friend paid for the user).")
            })
            put("paidFor", buildJsonObject {
                put("type", "string")
                put("description", "Required for type=paidForFriend. UID or email of the beneficiary. See paidBy for the \"me\" placement rule.")
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
        validateParticipantInvariants(params)
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
        validateParticipantInvariants(params)
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
            payers = args.optPayerShareList("payers")?.map { it.resolveMe() },
            sharers = args.optPayerShareList("sharers")?.map { it.resolveMe() },
            isEvenSplit = args.optBoolean("isEvenSplit"),
            loanedBy = args.optString("loanedBy")?.let { resolveMe(it) },
            borrowedBy = args.optString("borrowedBy")?.let { resolveMe(it) },
            paidBy = args.optString("paidBy")?.let { resolveMe(it) },
            paidFor = args.optString("paidFor")?.let { resolveMe(it) },
        )

    /**
     * Resolve the magic value `"me"` (case-insensitive) into the current session's
     * user email. The MCP client doesn't know — and shouldn't have to know — the
     * Buxfer credentials' email, so any contraparte field (`paidBy`, `paidFor`,
     * `loanedBy`, `borrowedBy`, and each `payers`/`sharers` entry) accepts the
     * literal string `"me"` as shorthand. Any other string is returned unchanged
     * (treated as a literal email or UID).
     *
     * Throws [IllegalStateException] if `"me"` is referenced before the client has
     * successfully logged in. The tool handler's `runCatching` surfaces it as an
     * `isError` result so Claude sees a clear message instead of a silent fail.
     */
    private fun resolveMe(value: String): String =
        if (value.equals("me", ignoreCase = true)) {
            client.currentUserEmail
                ?: throw IllegalStateException(
                    "Cannot resolve 'me': no active Buxfer session. The server logs in on startup using BUXFER_EMAIL/BUXFER_PASSWORD.",
                )
        } else {
            value
        }

    /** [resolveMe] applied to a [PayerShare]'s email field. */
    private fun PayerShare.resolveMe(): PayerShare = copy(email = resolveMe(email))

    /**
     * Enforce the "current user must be a participant" invariant for the multi-party
     * transaction types. Buxfer transactions live in the current user's books, so a
     * sharedBill / loan / paidForFriend that doesn't involve the user is nonsensical
     * (and the API rejects it, often opaquely). Validating up-front gives the LLM a
     * clear, actionable error instead of a 400 from Buxfer.
     *
     * Rules enforced (only when the relevant field is provided):
     *  - `sharedBill`: the current user must appear in `sharers` and in `payers`.
     *    Pass `{"email": "me", ...}` in each — `resolveMe` already substituted, so
     *    this check looks for [BuxferClient.currentUserEmail] verbatim.
     *  - `loan`: exactly one of `loanedBy` / `borrowedBy` is the current user — not
     *    both (self-loan is meaningless), not neither.
     *  - `paidForFriend`: exactly one of `paidBy` / `paidFor` is the current user.
     *
     * Throws [IllegalArgumentException] with a message that names the field and
     * tells the caller how to fix it (use the `"me"` marker).
     */
    private fun validateParticipantInvariants(params: AddTransactionParams) {
        // resolveMe has already run, so the current user's email is what we look for.
        // If currentUserEmail is null at this point AND no field referenced "me", we
        // can't enforce the rule — let it pass; Buxfer will validate. (If "me" was
        // referenced without a session, resolveMe already threw.)
        val me = client.currentUserEmail ?: return
        when (params.type) {
            "sharedBill" -> {
                params.sharers?.let { sharers ->
                    require(sharers.any { it.email == me }) {
                        "Invariant for type=sharedBill: one of `sharers` must be the current user. Pass {\"email\": \"me\", ...} in the sharers array — the server resolves \"me\" to your Buxfer email."
                    }
                }
                params.payers?.let { payers ->
                    require(payers.any { it.email == me }) {
                        "Invariant for type=sharedBill: one of `payers` must be the current user. If you paid the bill, pass [{\"email\": \"me\", \"amount\": <total>}] in payers; if a friend paid, include the current user as one of the payers anyway (Buxfer records the transaction in your books)."
                    }
                }
            }
            "loan" -> {
                val loanedByIsMe = params.loanedBy == me
                val borrowedByIsMe = params.borrowedBy == me
                // Only validate if at least one side was provided — missing both is a
                // Buxfer-level error we leave for the API to report.
                if (params.loanedBy != null || params.borrowedBy != null) {
                    require(loanedByIsMe xor borrowedByIsMe) {
                        if (loanedByIsMe && borrowedByIsMe) {
                            "Invariant for type=loan: `loanedBy` and `borrowedBy` cannot both be the current user (you can't loan to yourself)."
                        } else {
                            "Invariant for type=loan: exactly one of `loanedBy` / `borrowedBy` must be the current user. Pass \"me\" for the side you're on (loanedBy=\"me\" if you lent, borrowedBy=\"me\" if you borrowed)."
                        }
                    }
                }
            }
            "paidForFriend" -> {
                val paidByIsMe = params.paidBy == me
                val paidForIsMe = params.paidFor == me
                if (params.paidBy != null || params.paidFor != null) {
                    require(paidByIsMe xor paidForIsMe) {
                        if (paidByIsMe && paidForIsMe) {
                            "Invariant for type=paidForFriend: `paidBy` and `paidFor` cannot both be the current user (that's just a regular expense)."
                        } else {
                            "Invariant for type=paidForFriend: exactly one of `paidBy` / `paidFor` must be the current user. Pass \"me\" for the side you're on (paidBy=\"me\" if you paid for a friend, paidFor=\"me\" if a friend paid for you)."
                        }
                    }
                }
            }
        }
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
