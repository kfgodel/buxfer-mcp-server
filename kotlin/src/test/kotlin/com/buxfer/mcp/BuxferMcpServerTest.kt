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

    @Test
    fun `buxfer_list_transactions advertises its filter parameters in the input schema`() {
        val subject = BuxferMcpServer(mockClient)

        val schema = subject.inputSchemaFor("buxfer_list_transactions")

        assertThat(schema.properties?.keys).contains(
            "accountId", "accountName", "tagId", "tagName",
            "startDate", "endDate", "budgetId", "budgetName",
            "contactId", "contactName", "groupId", "groupName",
            "status", "page",
        )
        assertThat(schema.required).isNullOrEmpty()
    }

    @Test
    fun `buxfer_add_transaction declares its required and optional fields`() {
        val subject = BuxferMcpServer(mockClient)

        val schema = subject.inputSchemaFor("buxfer_add_transaction")

        assertThat(schema.properties?.keys).containsExactlyInAnyOrder(
            "description", "amount", "accountId", "date", "type", "tags", "status",
        )
        assertThat(schema.required).containsExactlyInAnyOrder(
            "description", "amount", "accountId", "date", "type",
        )
    }

    @Test
    fun `buxfer_edit_transaction declares id plus add fields as required`() {
        val subject = BuxferMcpServer(mockClient)

        val schema = subject.inputSchemaFor("buxfer_edit_transaction")

        assertThat(schema.properties?.keys).containsExactlyInAnyOrder(
            "id", "description", "amount", "accountId", "date", "type", "tags", "status",
        )
        assertThat(schema.required).containsExactlyInAnyOrder(
            "id", "description", "amount", "accountId", "date", "type",
        )
    }

    @Test
    fun `buxfer_delete_transaction requires only id`() {
        val subject = BuxferMcpServer(mockClient)

        val schema = subject.inputSchemaFor("buxfer_delete_transaction")

        assertThat(schema.properties?.keys).containsExactly("id")
        assertThat(schema.required).containsExactly("id")
    }

    @Test
    fun `buxfer_upload_statement requires accountId and statement, dateFormat optional`() {
        val subject = BuxferMcpServer(mockClient)

        val schema = subject.inputSchemaFor("buxfer_upload_statement")

        assertThat(schema.properties?.keys).containsExactlyInAnyOrder(
            "accountId", "statement", "dateFormat",
        )
        assertThat(schema.required).containsExactlyInAnyOrder("accountId", "statement")
    }

    @Test
    fun `every arg-taking tool declares a non-empty input schema`() {
        val subject = BuxferMcpServer(mockClient)

        val argTakingTools = listOf(
            "buxfer_list_transactions",
            "buxfer_add_transaction",
            "buxfer_edit_transaction",
            "buxfer_delete_transaction",
            "buxfer_upload_statement",
        )

        argTakingTools.forEach { toolName ->
            val schema = subject.inputSchemaFor(toolName)
            assertThat(schema.properties)
                .`as`("$toolName must declare its input schema (properties non-null)")
                .isNotNull()
            assertThat(schema.properties?.keys)
                .`as`("$toolName must declare at least one property")
                .isNotEmpty()
        }
    }
}
