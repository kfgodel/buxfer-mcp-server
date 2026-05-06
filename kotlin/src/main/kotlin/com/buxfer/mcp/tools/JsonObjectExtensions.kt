package com.buxfer.mcp.tools

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
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
