package com.buxfer.mcp.tools

import com.buxfer.mcp.api.BuxferClient

// TODO: Implement MCP tool handlers for transaction-related operations.
//
// Each method receives a parsed JSON input (from the MCP tool request) and returns
// a JSON string to be sent back as the tool result content.
//
// Tools to implement:
//
//   buxfer_list_transactions
//     Input: optional filters (accountId/Name, tagId/Name, startDate, endDate,
//            budgetId/Name, contactId/Name, groupId/Name, status, page)
//     Action: GET /api/transactions with the provided filters
//     Output: JSON array of Transaction objects + numTransactions
//
//   buxfer_add_transaction
//     Input: description, amount, accountId, date (required);
//            tags, type, status (optional);
//            sharedBill/loan/paidForFriend extra fields (conditional)
//     Action: POST /api/transaction_add
//     Output: JSON of created Transaction
//
//   buxfer_edit_transaction
//     Input: id (required) + same optional fields as add
//     Action: POST /api/transaction_edit
//     Output: JSON of updated Transaction
//
//   buxfer_delete_transaction
//     Input: id (required)
//     Action: POST /api/transaction_delete
//     Output: confirmation message
//
//   buxfer_upload_statement
//     Input: accountId, statement, dateFormat (optional)
//     Action: POST /api/upload_statement
//     Output: JSON with uploaded count and balance
//
// See ../../../../../../shared/api-spec/buxfer-api.md for full parameter contracts.

class TransactionTools(private val client: BuxferClient) {
    // TODO: Implement tool handler methods
}
