package com.buxfer.mcp

import com.buxfer.mcp.api.BuxferClient
import com.buxfer.mcp.tools.AccountTools
import com.buxfer.mcp.tools.BudgetTools
import com.buxfer.mcp.tools.ContactTools
import com.buxfer.mcp.tools.GroupTools
import com.buxfer.mcp.tools.LoanTools
import com.buxfer.mcp.tools.ReminderTools
import com.buxfer.mcp.tools.TagTools
import com.buxfer.mcp.tools.TransactionTools
import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.server.ServerOptions
import io.modelcontextprotocol.kotlin.sdk.server.StdioServerTransport
import io.modelcontextprotocol.kotlin.sdk.types.Implementation
import io.modelcontextprotocol.kotlin.sdk.types.ServerCapabilities
import kotlinx.io.asSink
import kotlinx.io.asSource
import kotlinx.io.buffered

class BuxferMcpServer(client: BuxferClient) {

    private val transactionTools = TransactionTools(client)
    private val accountTools = AccountTools(client)
    private val tagTools = TagTools(client)
    private val budgetTools = BudgetTools(client)
    private val reminderTools = ReminderTools(client)
    private val groupTools = GroupTools(client)
    private val contactTools = ContactTools(client)
    private val loanTools = LoanTools(client)

    private val server = Server(
        serverInfo = Implementation(name = "buxfer", version = "1.0.0"),
        options = ServerOptions(capabilities = ServerCapabilities())
    ).apply {
        addTool(
            name = "buxfer_list_accounts",
            description = "List all Buxfer accounts with balances."
        ) { _ -> accountTools.listAccounts() }

        addTool(
            name = "buxfer_list_transactions",
            description = "List Buxfer transactions, optionally filtered by accountId, accountName, tagId, tagName, startDate, endDate, budgetId, budgetName, contactId, contactName, groupId, groupName, status, page."
        ) { request -> transactionTools.listTransactions(request.arguments) }

        addTool(
            name = "buxfer_add_transaction",
            description = "Add a Buxfer transaction. Required: description, amount, accountId, date. Optional: tags, type, status."
        ) { request -> transactionTools.addTransaction(request.arguments) }

        addTool(
            name = "buxfer_edit_transaction",
            description = "Edit a Buxfer transaction by id. Required: id, description, amount, accountId, date. Optional: tags, type, status."
        ) { request -> transactionTools.editTransaction(request.arguments) }

        addTool(
            name = "buxfer_delete_transaction",
            description = "Delete a Buxfer transaction by id."
        ) { request -> transactionTools.deleteTransaction(request.arguments) }

        addTool(
            name = "buxfer_upload_statement",
            description = "Upload a bank statement to a Buxfer account. Required: accountId, statement. Optional: dateFormat."
        ) { request -> transactionTools.uploadStatement(request.arguments) }

        addTool(
            name = "buxfer_list_tags",
            description = "List all Buxfer tags."
        ) { _ -> tagTools.listTags() }

        addTool(
            name = "buxfer_list_budgets",
            description = "List all Buxfer budgets."
        ) { _ -> budgetTools.listBudgets() }

        addTool(
            name = "buxfer_list_reminders",
            description = "List all Buxfer reminders."
        ) { _ -> reminderTools.listReminders() }

        addTool(
            name = "buxfer_list_groups",
            description = "List all Buxfer groups."
        ) { _ -> groupTools.listGroups() }

        addTool(
            name = "buxfer_list_contacts",
            description = "List all Buxfer contacts."
        ) { _ -> contactTools.listContacts() }

        addTool(
            name = "buxfer_list_loans",
            description = "List all Buxfer loans."
        ) { _ -> loanTools.listLoans() }
    }

    suspend fun start() {
        val transport = StdioServerTransport(
            inputStream = System.`in`.asSource().buffered(),
            outputStream = System.out.asSink().buffered()
        )
        server.createSession(transport)
    }
}
