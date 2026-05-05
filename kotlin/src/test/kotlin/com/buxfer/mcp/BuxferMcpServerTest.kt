package com.buxfer.mcp

import com.buxfer.mcp.api.BuxferClient
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class BuxferMcpServerTest {

    private val mockClient = mockk<BuxferClient>()

    @Test
    fun `registers the expected 12 tools`() {
        val subject = BuxferMcpServer(mockClient)

        assertThat(subject.toolDescriptors.keys).containsExactlyInAnyOrder(
            "buxfer_list_accounts",
            "buxfer_list_transactions",
            "buxfer_add_transaction",
            "buxfer_edit_transaction",
            "buxfer_delete_transaction",
            "buxfer_upload_statement",
            "buxfer_list_tags",
            "buxfer_list_budgets",
            "buxfer_list_reminders",
            "buxfer_list_groups",
            "buxfer_list_contacts",
            "buxfer_list_loans"
        )
    }

    @Test
    fun `every registered tool has a non-blank description`() {
        val subject = BuxferMcpServer(mockClient)

        assertThat(subject.toolDescriptors.values).allSatisfy { description ->
            assertThat(description).isNotBlank()
        }
    }
}
