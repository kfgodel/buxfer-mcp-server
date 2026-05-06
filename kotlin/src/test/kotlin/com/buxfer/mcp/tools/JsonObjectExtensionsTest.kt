package com.buxfer.mcp.tools

import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
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
}
