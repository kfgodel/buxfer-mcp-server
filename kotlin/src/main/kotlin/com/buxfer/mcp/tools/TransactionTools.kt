package com.buxfer.mcp.tools

import com.buxfer.mcp.api.BuxferClient
import com.buxfer.mcp.api.buxferJson
import com.buxfer.mcp.api.models.AddTransactionParams
import com.buxfer.mcp.api.models.TransactionFilters
import io.modelcontextprotocol.kotlin.sdk.types.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive
import org.slf4j.LoggerFactory

class TransactionTools(private val client: BuxferClient) {

    private val log = LoggerFactory.getLogger(TransactionTools::class.java)

    /**
     * Always-redacted arg keys (mirrored in logback.xml header).
     *  - statement: raw bank-statement text (often many KB; would leak transaction history wholesale).
     *  - password : credential, not currently a tool arg but listed defensively in case it is added later.
     */
    private val redactedArgs = setOf("statement", "password")

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
            tags = args.optString("tags"),
            type = args.optString("type"),
            status = args.optString("status")
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
            tags = args.optString("tags"),
            type = args.optString("type"),
            status = args.optString("status")
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
        client.deleteTransaction(id)
        CallToolResult(content = listOf(TextContent("{\"deleted\":true,\"id\":$id}")))
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
