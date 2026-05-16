package com.buxfer.mcp.tools

import com.buxfer.mcp.api.models.PayerShare
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

class JsonObjectExtensionsTest {

    @Test
    fun `requireInt returns the value when key holds an int`() {
        val args = buildJsonObject { put("id", 42) }

        assertThat(args.requireInt("id")).isEqualTo(42)
    }

    @Test
    fun `requireInt throws when key is missing`() {
        val args = buildJsonObject { }

        assertThatThrownBy { args.requireInt("id") }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessage("Missing or malformed required argument 'id'")
    }

    @Test
    fun `requireInt throws when value is a non-numeric string`() {
        val args = buildJsonObject { put("id", "abc") }

        assertThatThrownBy { args.requireInt("id") }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessage("Missing or malformed required argument 'id'")
    }

    @Test
    fun `requireInt throws when receiver is null`() {
        val args: JsonObject? = null

        assertThatThrownBy { args.requireInt("id") }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessage("Missing or malformed required argument 'id'")
    }

    @Test
    fun `requireDouble returns the value when key holds a number`() {
        val args = buildJsonObject { put("amount", 12.34) }

        assertThat(args.requireDouble("amount")).isEqualTo(12.34)
    }

    @Test
    fun `requireDouble throws when key is missing`() {
        val args = buildJsonObject { }

        assertThatThrownBy { args.requireDouble("amount") }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessage("Missing or malformed required argument 'amount'")
    }

    @Test
    fun `requireDouble throws when value is a non-numeric string`() {
        val args = buildJsonObject { put("amount", "abc") }

        assertThatThrownBy { args.requireDouble("amount") }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessage("Missing or malformed required argument 'amount'")
    }

    @Test
    fun `requireDouble throws when receiver is null`() {
        val args: JsonObject? = null

        assertThatThrownBy { args.requireDouble("amount") }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessage("Missing or malformed required argument 'amount'")
    }

    @Test
    fun `requireString returns the value when key holds a string`() {
        val args = buildJsonObject { put("description", "hello") }

        assertThat(args.requireString("description")).isEqualTo("hello")
    }

    @Test
    fun `requireString throws when key is missing`() {
        val args = buildJsonObject { }

        assertThatThrownBy { args.requireString("description") }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessage("Missing or malformed required argument 'description'")
    }

    @Test
    fun `requireString throws when value is JSON null`() {
        val args = buildJsonObject { put("description", JsonNull) }

        assertThatThrownBy { args.requireString("description") }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessage("Missing or malformed required argument 'description'")
    }

    @Test
    fun `requireString throws when receiver is null`() {
        val args: JsonObject? = null

        assertThatThrownBy { args.requireString("description") }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessage("Missing or malformed required argument 'description'")
    }

    @Test
    fun `optString returns the value when key holds a string`() {
        val args = buildJsonObject { put("dateFormat", "MM/DD/YYYY") }

        assertThat(args.optString("dateFormat")).isEqualTo("MM/DD/YYYY")
    }

    @Test
    fun `optString returns null when key is missing`() {
        val args = buildJsonObject { }

        assertThat(args.optString("dateFormat")).isNull()
    }

    @Test
    fun `optString returns null when receiver is null`() {
        val args: JsonObject? = null

        assertThat(args.optString("dateFormat")).isNull()
    }

    @Test
    fun `optInt returns the value when key holds an int`() {
        val args = buildJsonObject { put("toAccountId", 10351) }

        assertThat(args.optInt("toAccountId")).isEqualTo(10351)
    }

    @Test
    fun `optInt returns null when key is missing`() {
        val args = buildJsonObject { }

        assertThat(args.optInt("toAccountId")).isNull()
    }

    @Test
    fun `optInt returns null when value is a non-numeric string`() {
        val args = buildJsonObject { put("toAccountId", "abc") }

        assertThat(args.optInt("toAccountId")).isNull()
    }

    @Test
    fun `optBoolean returns the value when key holds a boolean`() {
        val args = buildJsonObject { put("isEvenSplit", true) }

        assertThat(args.optBoolean("isEvenSplit")).isTrue()
    }

    @Test
    fun `optBoolean returns null when key is missing`() {
        val args = buildJsonObject { }

        assertThat(args.optBoolean("isEvenSplit")).isNull()
    }

    @Test
    fun `optBoolean returns null when receiver is null`() {
        val args: JsonObject? = null

        assertThat(args.optBoolean("isEvenSplit")).isNull()
    }

    @Test
    fun `optPayerShareList parses an array of email-amount objects`() {
        val args = buildJsonObject {
            putJsonArray("sharers") {
                addJsonObject { put("email", "a@example.com"); put("amount", 60.0) }
                addJsonObject { put("email", "b@example.com"); put("amount", 40.0) }
            }
        }

        val parsed = args.optPayerShareList("sharers")

        assertThat(parsed).containsExactly(
            PayerShare(email = "a@example.com", amount = 60.0),
            PayerShare(email = "b@example.com", amount = 40.0),
        )
    }

    @Test
    fun `optPayerShareList accepts entries without amount (even-split case)`() {
        val args = buildJsonObject {
            putJsonArray("sharers") {
                addJsonObject { put("email", "a@example.com") }
                addJsonObject { put("email", "b@example.com") }
            }
        }

        val parsed = args.optPayerShareList("sharers")

        assertThat(parsed).containsExactly(
            PayerShare(email = "a@example.com", amount = null),
            PayerShare(email = "b@example.com", amount = null),
        )
    }

    @Test
    fun `optPayerShareList returns null when key is missing`() {
        val args = buildJsonObject { }

        assertThat(args.optPayerShareList("sharers")).isNull()
    }

    @Test
    fun `optPayerShareList throws when an entry is missing email`() {
        val args = buildJsonObject {
            putJsonArray("sharers") {
                addJsonObject { put("amount", 50.0) }
            }
        }

        assertThatThrownBy { args.optPayerShareList("sharers") }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("'sharers'")
            .hasMessageContaining("email")
    }
}
