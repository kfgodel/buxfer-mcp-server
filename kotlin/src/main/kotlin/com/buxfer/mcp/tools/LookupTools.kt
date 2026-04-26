package com.buxfer.mcp.tools

import com.buxfer.mcp.api.BuxferClient

// TODO: Implement MCP tool handlers for lookup/reference data.
//
// Tools to implement:
//
//   buxfer_list_tags
//     Input: none
//     Action: GET /api/tags
//     Output: JSON array of Tag objects (id, name, parentId)
//
//   buxfer_list_budgets
//     Input: none
//     Action: GET /api/budgets
//     Output: JSON array of Budget objects
//
//   buxfer_list_reminders
//     Input: none
//     Action: GET /api/reminders
//     Output: JSON array of Reminder objects
//
//   buxfer_list_groups
//     Input: none
//     Action: GET /api/groups
//     Output: JSON array of Group objects (with nested members)
//
//   buxfer_list_contacts
//     Input: none
//     Action: GET /api/contacts
//     Output: JSON array of Contact objects (id, name, email, balance)
//
//   buxfer_list_loans
//     Input: none
//     Action: GET /api/loans
//     Output: JSON array of Loan objects (entity, type, balance, description)
//
// See ../../../../../../shared/api-spec/buxfer-api.md for full response shapes.

class LookupTools(private val client: BuxferClient) {
    // TODO: Implement tool handler methods
}
