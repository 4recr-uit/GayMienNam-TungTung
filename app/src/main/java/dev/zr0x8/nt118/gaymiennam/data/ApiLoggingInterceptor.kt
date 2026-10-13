package dev.zr0x8.nt118.gaymiennam.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException
import java.time.LocalTime
import java.time.format.DateTimeFormatter

object ApiLogStore {
    private const val MAX_ENTRIES = 200
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss.SSS")
    private val mutableEntries = MutableStateFlow<List<String>>(emptyList())

    val entries = mutableEntries.asStateFlow()

    internal fun add(message: String) {
        val entry = "${LocalTime.now().format(timeFormatter)}  $message"
        mutableEntries.update { (it + entry).takeLast(MAX_ENTRIES) }
    }

    fun clear() {
        mutableEntries.value = emptyList()
    }

    suspend fun <T> track(
        endpoint: String,
        query: String,
        request: suspend () -> Response<T>
    ): T {
        val startedAt = System.nanoTime()
        val response = try {
            request()
        } catch (e: Exception) {
            val elapsedMs = (System.nanoTime() - startedAt) / 1_000_000
            val result = if (e is HttpException) {
                "HTTP ${e.code()}"
            } else if (e is IOException) {
                "NETWORK ERROR (${e::class.java.simpleName})"
            } else {
                "FAILED (${e::class.java.simpleName}: ${e.message.orEmpty()})"
            }
            add("GET $endpoint?q=${query.logSafe()} -> $result (${elapsedMs}ms)")
            throw e
        }

        val elapsedMs = (System.nanoTime() - startedAt) / 1_000_000
        val statusMessage = response.message().ifBlank {
            response.code().reasonPhrase()
        }
        val status = "${response.code()}: $statusMessage"
        add("GET $endpoint?q=${query.logSafe()} -> $status (${elapsedMs}ms)")
        if (!response.isSuccessful) throw HttpException(response)
        return response.body() ?: throw IOException("Successful response from $endpoint had no body")
    }

    private fun String.logSafe(): String =
        replace("\n", " ").replace("\r", " ").take(120)

    private fun Int.reasonPhrase(): String = when (this) {
        200 -> "OK"
        201 -> "Created"
        202 -> "Accepted"
        204 -> "No Content"
        301 -> "Moved Permanently"
        302 -> "Found"
        304 -> "Not Modified"
        400 -> "Bad Request"
        401 -> "Unauthorized"
        403 -> "Forbidden"
        404 -> "Not Found"
        405 -> "Method Not Allowed"
        408 -> "Request Timeout"
        409 -> "Conflict"
        410 -> "Gone"
        418 -> "I'm a Teapot"
        422 -> "Unprocessable Entity"
        429 -> "Too Many Requests"
        500 -> "Internal Server Error"
        501 -> "Not Implemented"
        502 -> "Bad Gateway"
        503 -> "Service Unavailable"
        504 -> "Gateway Timeout"
        else -> "HTTP Status Not Covered"
    }
}
