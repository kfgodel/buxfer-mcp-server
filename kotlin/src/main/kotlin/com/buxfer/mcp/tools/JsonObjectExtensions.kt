package com.buxfer.mcp.tools

import com.buxfer.mcp.api.models.PayerShare
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Extension helpers for extracting MCP tool arguments from a nullable `JsonObject`.
 *
 * Tool handlers receive `request.arguments: JsonObject?` from the MCP SDK. These
 * helpers turn that into typed values, throwing [IllegalArgumentException] when a
 * required argument is missing or wrong-typed — caught by the tool's `runCatching`
 * wrapper and surfaced to the caller as `isError = true`. Avoids the silent-zero
 * antipattern (`?: 0`, `?: ""`) that could otherwise let a missing `id` reach the
 * Buxfer API as `id = 0`.
 */

/** Pluck an Int argument by name. Throws if missing or not a parseable integer. */
internal fun JsonObject?.requireInt(name: String): Int =
    this?.get(name)?.jsonPrimitive?.intOrNull
        ?: throw IllegalArgumentException("Missing or malformed required argument '$name'")

/** Pluck a Double argument by name. Throws if missing or not a parseable number. */
internal fun JsonObject?.requireDouble(name: String): Double =
    this?.get(name)?.jsonPrimitive?.doubleOrNull
        ?: throw IllegalArgumentException("Missing or malformed required argument '$name'")

/** Pluck a String argument by name. Throws if missing or not a JSON string. */
internal fun JsonObject?.requireString(name: String): String =
    this?.get(name)?.jsonPrimitive?.contentOrNull
        ?: throw IllegalArgumentException("Missing or malformed required argument '$name'")

/** Pluck an optional String argument by name. Returns null when the key is absent. */
internal fun JsonObject?.optString(name: String): String? =
    this?.get(name)?.jsonPrimitive?.contentOrNull

/** Pluck an optional Int argument by name. Returns null when the key is absent or not a parseable integer. */
internal fun JsonObject?.optInt(name: String): Int? =
    this?.get(name)?.jsonPrimitive?.intOrNull

/** Pluck an optional Boolean argument by name. Returns null when the key is absent or not a boolean. */
internal fun JsonObject?.optBoolean(name: String): Boolean? =
    this?.get(name)?.jsonPrimitive?.booleanOrNull

/**
 * Decode an optional JSON-array argument into `List<PayerShare>`. Returns `null` when
 * the key is absent (caller may want to omit the field), but throws on a present but
 * malformed value — a `payers`/`sharers` array with a missing `email` is a programming
 * error from the caller, not something to silently drop on the floor. Each element
 * must be an object with at minimum an `email` string; `amount` is optional (omitted
 * when `isEvenSplit = true`).
 */
internal fun JsonObject?.optPayerShareList(name: String): List<PayerShare>? {
    val arr = this?.get(name) as? JsonArray ?: return null
    return arr.map { element ->
        val obj = element.jsonObject
        val email = obj["email"]?.jsonPrimitive?.contentOrNull
            ?: throw IllegalArgumentException(
                "Malformed entry in '$name': each element must include 'email'",
            )
        PayerShare(
            email = email,
            amount = obj["amount"]?.jsonPrimitive?.doubleOrNull,
        )
    }
}
