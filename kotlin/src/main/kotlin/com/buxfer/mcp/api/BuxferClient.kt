package com.buxfer.mcp.api

import com.buxfer.mcp.api.models.Account
import com.buxfer.mcp.api.models.AddTransactionParams
import com.buxfer.mcp.api.models.Budget
import com.buxfer.mcp.api.models.Contact
import com.buxfer.mcp.api.models.Group
import com.buxfer.mcp.api.models.Loan
import com.buxfer.mcp.api.models.Reminder
import com.buxfer.mcp.api.models.Tag
import com.buxfer.mcp.api.models.Transaction
import com.buxfer.mcp.api.models.TransactionFilters
import com.buxfer.mcp.api.models.TransactionsResult
import com.buxfer.mcp.api.models.UploadStatementResult
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.request.forms.FormDataContent
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.Parameters
import io.ktor.http.isSuccess
import java.io.IOException
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.slf4j.LoggerFactory

internal val buxferJson = Json {
    // Tolerate API drift inbound: a new Buxfer field doesn't crash deserialization.
    // Required fields (no default) still fail loudly when missing — that's how we
    // detect real drift on identity fields.
    ignoreUnknownKeys = true
    // Drop fields equal to their declared default on the way out (e.g. nullable
    // fields that are null, list fields that are empty). Keeps the JSON we surface
    // to Claude compact and free of "field": null / "field": [] noise. Set
    // explicitly even though it matches the library default — the choice is part
    // of the contract, not an accident of inheritance.
    encodeDefaults = false
}

/**
 * Strict Json instance used only by [BuxferClient.validateSchema] for drift-detection
 * decode attempts. With `ignoreUnknownKeys = false` an unexpected field in the response
 * surfaces as a SerializationException; together with the schema's non-nullable fields,
 * any change in Buxfer's response shape becomes a precise warning. Failures are logged
 * and discarded — the data path uses [buxferJson] (permissive) and is never affected.
 */
internal val validatorJson = Json {
    ignoreUnknownKeys = false
    encodeDefaults = false
}

class BuxferClient(private val config: BuxferClientConfig = BuxferClientConfig()) : AutoCloseable {

    companion object {
        private val log = LoggerFactory.getLogger(BuxferClient::class.java)
    }

    private val httpClient = HttpClient(config.engine) {
        install(HttpTimeout) {
            connectTimeoutMillis = config.connectTimeoutMillis
            requestTimeoutMillis = config.requestTimeoutMillis
            socketTimeoutMillis = config.socketTimeoutMillis
        }
    }

    @Volatile private var token: String? = null

    private fun requireToken() = token ?: throw BuxferApiException("Not logged in — call login() first")

    /** Releases the underlying Ktor HTTP client and its engine. After close(), the
     *  client cannot be used. Tests should `.use { ... }`; the long-running server
     *  closes via the JVM shutdown hook in [Main][com.buxfer.mcp.Main].
     */
    override fun close() {
        httpClient.close()
    }

    /**
     * Run an HTTP call with redacted DEBUG-level request/response logging.
     * `path` is the API endpoint suffix (e.g. "/accounts") — no query params, no token.
     */
    private suspend fun <T> traced(method: String, path: String, block: suspend () -> T): T {
        log.debug("-> {} {}", method, path)
        return try {
            val result = block()
            log.debug("<- OK {}", path)
            result
        } catch (e: BuxferApiException) {
            log.error("Buxfer API error on {} {}: {}", method, path, e.message)
            throw e
        } catch (e: SerializationException) {
            // Surfaces as a precise BuxferApiException so callers see the endpoint
            // alongside kotlinx.serialization's field-and-path detail (e.g. "Field
            // 'id' is required ... at path: $[0]"). Real API drift becomes
            // immediately diagnosable instead of crashing deep in tool code.
            log.error("Buxfer API parse error on {} {}: {}", method, path, e.message)
            throw BuxferApiException("Failed to parse $method $path response: ${e.message}", e)
        } catch (e: HttpRequestTimeoutException) {
            // HttpRequestTimeoutException extends kotlinx.io.IOException, NOT java.io.IOException
            // — needs its own catch before the IOException branch.
            log.error("Buxfer API timeout on {} {}: {}", method, path, e.message)
            throw BuxferApiException(
                "Request timed out after ${config.requestTimeoutMillis}ms: $method $path", e,
            )
        } catch (e: IOException) {
            // Covers ConnectException (server down), UnknownHostException (DNS),
            // SocketTimeoutException, SSLException, SocketException — every standard
            // network failure surfaces here.
            log.error("Buxfer API network error on {} {}: {}", method, path, e.message)
            throw BuxferApiException(
                "Network error contacting Buxfer ($method $path): ${e.message}", e,
            )
        } catch (e: CancellationException) {
            // Never wrap structured-concurrency cancellation — it must propagate so the
            // suspending caller (and kotlinx.coroutines) can unwind cleanly.
            throw e
        } catch (e: Exception) {
            // Last resort: Ktor's FailToConnectException (extends plain Exception, not
            // IOException) and any other engine-level failure. Wrap so the tool layer's
            // contract — every error from BuxferClient is BuxferApiException — stays
            // clean. Catches Exception (not Throwable), so OOM / StackOverflowError
            // still propagate as-is.
            log.error("Buxfer API unexpected error on {} {}: {}", method, path, e.message)
            throw BuxferApiException("Unexpected error on $method $path: ${e.message}", e)
        }
    }

    private suspend fun text(response: HttpResponse): String {
        val body = response.bodyAsText()
        if (!response.status.isSuccess()) {
            // Include a body excerpt — Buxfer's 5xx pages and the JSON error envelope
            // both carry useful context that "HTTP 500" alone hides.
            val excerpt = body.take(500)
            throw BuxferApiException("HTTP ${response.status.value}: $excerpt")
        }
        return body
    }

    private fun responseBody(bodyText: String): JsonObject {
        val envelope = buxferJson.parseToJsonElement(bodyText).jsonObject
        val response = envelope["response"]?.jsonObject
            ?: throw BuxferApiException("Missing 'response' field in API reply")
        val status = response["status"]?.jsonPrimitive?.contentOrNull
        if (status == "error") {
            val error = response["error"]?.jsonPrimitive?.contentOrNull ?: status
            throw BuxferApiException(error)
        }
        return response
    }

    suspend fun login(email: String, password: String): Unit = traced("POST", "/login") {
        // Redaction policy: never log email (PII) or password (credential).
        val response = httpClient.post("${config.baseUrl}/login") {
            setBody(FormDataContent(Parameters.build {
                append("email", email)
                append("password", password)
            }))
        }
        val body = responseBody(text(response))
        token = body["token"]?.jsonPrimitive?.contentOrNull
            ?: throw BuxferApiException("Login succeeded but no token in response")
        log.debug("login: token acquired")
    }

    /**
     * Authenticated GET that returns a list deserialized from the named JSON key
     * inside the `response` envelope. Used by every resource list endpoint
     * (`/accounts`, `/tags`, `/budgets`, …) which all share this exact shape.
     *
     * `inline reified` is required so each call site gets a concrete `T` for
     * `decodeFromJsonElement`; the helper itself is `private` so inlining can
     * still access `httpClient`, `config`, and `requireToken()`.
     */
    private suspend inline fun <reified T> getList(path: String, jsonKey: String): List<T> =
        traced("GET", path) {
            val response = httpClient.get("${config.baseUrl}$path") {
                parameter("token", requireToken())
            }
            val body = responseBody(text(response))
            buxferJson.decodeFromJsonElement(body[jsonKey] ?: JsonArray(emptyList()))
        }

    suspend fun getAccounts(): JsonArray = getValidatedList<List<Account>>("/accounts")

    /**
     * Authenticated GET that returns the inner `JsonArray` named after the path's last
     * segment, validating it against the schema [Schema] as a side effect (warning-only,
     * see [validateSchema]). Used by every migrated list endpoint — `getAccounts`,
     * `getTags`, `getContacts`, etc. Each call site picks the schema type; the helper
     * handles auth, envelope inspection, and validation.
     *
     * Counterpart to the older [getList], which still serves the unmigrated endpoints
     * (typed data-carrier flow). Once all list endpoints migrate, [getList] goes away.
     *
     * `inline reified` is required so the call site keeps a concrete `Schema` for
     * [validateSchema]'s `decodeFromJsonElement<Schema>`; a non-reified helper would
     * fall back to `Any`'s serializer and fail at runtime.
     */
    private suspend inline fun <reified Schema> getValidatedList(path: String): JsonArray =
        traced("GET", path) {
            val response = httpClient.get("${config.baseUrl}$path") {
                parameter("token", requireToken())
            }
            val body = responseBody(text(response))
            val list = body[path.removePrefix("/")]?.jsonArray ?: JsonArray(emptyList())
            validateSchema<Schema>(list, path)
            list
        }

    /**
     * Attempts to decode [json] against the schema [T] using the strict [validatorJson].
     * Strictly side-effect: failures are logged and discarded so the data path (which
     * already holds the raw [JsonElement]) is never affected. SerializationException is
     * the expected drift signal (missing field, wrong type, unexpected key) and logs at
     * WARN; anything else is a coding-side surprise — logged at ERROR so it gets noticed
     * but still doesn't propagate.
     */
    private inline fun <reified T> validateSchema(json: JsonElement, path: String) {
        runCatching { validatorJson.decodeFromJsonElement<T>(json) }
            .onFailure { e ->
                if (e is SerializationException) {
                    log.warn("Schema drift on {}: {}", path, e.message)
                } else {
                    log.error("Schema validation failed unexpectedly on {}: {}", path, e.message, e)
                }
            }
    }

    suspend fun getTransactions(filters: TransactionFilters = TransactionFilters()): TransactionsResult =
        traced("GET", "/transactions") {
            val response = httpClient.get("${config.baseUrl}/transactions") {
                parameter("token", requireToken())
                filters.accountId?.let { parameter("accountId", it) }
                filters.accountName?.let { parameter("accountName", it) }
                filters.tagId?.let { parameter("tagId", it) }
                filters.tagName?.let { parameter("tagName", it) }
                filters.startDate?.let { parameter("startDate", it) }
                filters.endDate?.let { parameter("endDate", it) }
                filters.budgetId?.let { parameter("budgetId", it) }
                filters.budgetName?.let { parameter("budgetName", it) }
                filters.contactId?.let { parameter("contactId", it) }
                filters.contactName?.let { parameter("contactName", it) }
                filters.groupId?.let { parameter("groupId", it) }
                filters.groupName?.let { parameter("groupName", it) }
                filters.status?.let { parameter("status", it) }
                filters.page?.let { parameter("page", it) }
            }
            val body = responseBody(text(response))
            val transactions: List<Transaction> = buxferJson.decodeFromJsonElement(
                body["transactions"] ?: JsonArray(emptyList())
            )
            // Buxfer returns numTransactions as a string — decode manually rather than
            // bend the model. Throw on missing/malformed so a real API change surfaces
            // (instead of silently turning into "0 transactions").
            val numTransactions = body["numTransactions"]?.jsonPrimitive?.content?.toIntOrNull()
                ?: throw BuxferApiException("Missing or malformed 'numTransactions' in /transactions response")
            TransactionsResult(transactions, numTransactions)
        }

    suspend fun addTransaction(params: AddTransactionParams): Transaction = traced("POST", "/transaction_add") {
        val response = httpClient.post("${config.baseUrl}/transaction_add") {
            setBody(FormDataContent(Parameters.build {
                append("token", requireToken())
                append("description", params.description)
                append("amount", params.amount.toString())
                append("accountId", params.accountId.toString())
                append("date", params.date)
                params.tags?.let { append("tags", it) }
                params.type?.let { append("type", it) }
                params.status?.let { append("status", it) }
            }))
        }
        val body = responseBody(text(response))
        buxferJson.decodeFromJsonElement(body)
    }

    suspend fun editTransaction(id: Int, params: AddTransactionParams): Transaction =
        traced("POST", "/transaction_edit") {
            val response = httpClient.post("${config.baseUrl}/transaction_edit") {
                setBody(FormDataContent(Parameters.build {
                    append("token", requireToken())
                    append("id", id.toString())
                    append("description", params.description)
                    append("amount", params.amount.toString())
                    append("accountId", params.accountId.toString())
                    append("date", params.date)
                    params.tags?.let { append("tags", it) }
                    params.type?.let { append("type", it) }
                    params.status?.let { append("status", it) }
                }))
            }
            val body = responseBody(text(response))
            buxferJson.decodeFromJsonElement(body)
        }

    suspend fun deleteTransaction(id: Int): Unit = traced("POST", "/transaction_delete") {
        val response = httpClient.post("${config.baseUrl}/transaction_delete") {
            setBody(FormDataContent(Parameters.build {
                append("token", requireToken())
                append("id", id.toString())
            }))
        }
        responseBody(text(response))
    }

    suspend fun uploadStatement(accountId: Int, statement: String, dateFormat: String? = null): UploadStatementResult =
        traced("POST", "/upload_statement") {
            // Redaction policy: never log the statement body — raw bank-statement text, often many KB.
            val response = httpClient.post("${config.baseUrl}/upload_statement") {
                setBody(FormDataContent(Parameters.build {
                    append("token", requireToken())
                    append("accountId", accountId.toString())
                    append("statement", statement)
                    dateFormat?.let { append("dateFormat", it) }
                }))
            }
            val body = responseBody(text(response))
            buxferJson.decodeFromJsonElement(body)
        }

    suspend fun getTags(): JsonArray = getValidatedList<List<Tag>>("/tags")

    suspend fun getBudgets(): JsonArray = getValidatedList<List<Budget>>("/budgets")

    suspend fun getReminders(): JsonArray = getValidatedList<List<Reminder>>("/reminders")

    suspend fun getGroups(): List<Group> = getList("/groups", "groups")

    suspend fun getContacts(): JsonArray = getValidatedList<List<Contact>>("/contacts")

    suspend fun getLoans(): JsonArray = getValidatedList<List<Loan>>("/loans")
}
