package com.nhungtool.reconcore.ui

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import java.io.Reader
import java.io.Writer
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import java.time.Duration
import java.util.Base64
import java.util.Properties

internal object OpenAiNameMatcher {
    private const val SHORTLIST_LIMIT = 4
    private const val MATCH_TIMEOUT_CAP_SECONDS = 8
    private const val FAILURE_COOLDOWN_MILLIS = 5 * 60 * 1000L

    data class Candidate(
        val code: String,
        val name: String,
        val unit: String,
        val heuristicScore: Double,
    )

    data class Suggestion(
        val matchedCode: String?,
        val confidence: Double,
        val reason: String,
        val needsReview: Boolean,
        val noMatch: Boolean,
    )

    data class ConnectionTestResult(
        val success: Boolean,
        val message: String,
    )

    private enum class Decision { MATCH, REVIEW, NO_MATCH }

    private data class CachedDecision(
        val decision: Decision,
        val matchedCode: String?,
        val confidence: Double,
        val reason: String,
    )

    private val mapper: ObjectMapper = jacksonObjectMapper()
    private val httpClient: HttpClient = HttpClient.newBuilder().build()
    private val cachePath: Path = Path.of(System.getProperty("user.dir"), ".reconcore", "openai-match-cache.properties")
    private val cacheLock = Any()
    private val recentFailures = linkedMapOf<String, Long>()

    fun suggest(
        invoiceName: String,
        invoiceUnit: String,
        invoiceCode: String,
        candidates: List<Candidate>,
    ): Suggestion? {
        val settings = OpenAiSettingsService.load()
        if (!settings.configured || candidates.isEmpty()) return null

        val shortlist = candidates.take(SHORTLIST_LIMIT)
        val cacheKey = buildCacheKey(settings, invoiceName, invoiceUnit, invoiceCode, shortlist)
        loadCache(cacheKey)?.let { cached ->
            return toSuggestion(cached, shortlist)
        }
        if (inFailureCooldown(cacheKey)) {
            return null
        }

        val response = runCatching {
            requestDecision(settings, invoiceName, invoiceUnit, invoiceCode, shortlist)
        }.onFailure {
            rememberFailure(cacheKey)
        }.getOrNull() ?: return null

        clearFailure(cacheKey)
        saveCache(cacheKey, response)
        return toSuggestion(response, shortlist)
    }

    fun testConnection(settings: OpenAiSettings = OpenAiSettingsService.load()): ConnectionTestResult {
        if (settings.apiKey.isBlank()) {
            return ConnectionTestResult(false, "Chưa có API key OpenAI")
        }
        val requestBody = mapOf(
            "model" to settings.model,
            "instructions" to "Reply with the exact text OK.",
            "input" to "ping",
            "store" to false,
            "temperature" to 0.0,
        )
        return runCatching {
            val responseNode = post(settings, requestBody)
            val output = extractOutputText(responseNode)
            if (output.equals("OK", ignoreCase = true)) {
                ConnectionTestResult(true, "Kết nối OpenAI thành công với model ${settings.model}")
            } else {
                ConnectionTestResult(true, "Kết nối thành công. Phản hồi mẫu: ${output ?: "trống"}")
            }
        }.getOrElse { ex ->
            ConnectionTestResult(false, "Kết nối thất bại: ${ex.message ?: "không xác định"}")
        }
    }

    fun clearCache(): Int {
        synchronized(cacheLock) {
            val existing = if (!Files.exists(cachePath)) 0 else loadProperties().size
            recentFailures.clear()
            Files.deleteIfExists(cachePath)
            return existing
        }
    }

    private fun requestDecision(
        settings: OpenAiSettings,
        invoiceName: String,
        invoiceUnit: String,
        invoiceCode: String,
        candidates: List<Candidate>,
    ): CachedDecision {
        val requestBody = mapOf(
            "model" to settings.model,
            "instructions" to systemPrompt(),
            "input" to buildUserPrompt(invoiceName, invoiceUnit, invoiceCode, candidates),
            "store" to false,
            "temperature" to 0.0,
            "max_output_tokens" to 140,
            "text" to mapOf(
                "format" to mapOf(
                    "type" to "json_schema",
                    "name" to "product_match_decision",
                    "strict" to true,
                    "schema" to decisionSchema(),
                ),
            ),
        )
        val responseNode = post(settings, requestBody, settings.timeoutSeconds.coerceAtMost(MATCH_TIMEOUT_CAP_SECONDS))
        val outputText = extractOutputText(responseNode)
            ?: throw IllegalStateException("OpenAI không trả về output_text")
        val decisionNode = mapper.readTree(outputText)
        val decision = when (decisionNode.path("decision").asText("").lowercase()) {
            "match" -> Decision.MATCH
            "review" -> Decision.REVIEW
            "no_match" -> Decision.NO_MATCH
            else -> throw IllegalStateException("OpenAI trả về decision không hợp lệ")
        }
        val matchedCode = decisionNode.path("matched_code").asText("").trim().ifBlank { null }
        val confidence = (decisionNode.path("confidence").asDouble(0.0) / 100.0).coerceIn(0.0, 1.0)
        val reason = decisionNode.path("reason").asText("").trim().ifBlank { "Không có diễn giải" }
        return CachedDecision(decision, matchedCode, confidence, reason)
    }

    private fun toSuggestion(decision: CachedDecision, candidates: List<Candidate>): Suggestion? {
        val validCode = decision.matchedCode?.takeIf { code -> candidates.any { it.code.equals(code, ignoreCase = true) } }
        return when (decision.decision) {
            Decision.MATCH -> validCode?.let { Suggestion(it, decision.confidence, decision.reason, needsReview = true, noMatch = false) }
            Decision.REVIEW -> validCode?.let { Suggestion(it, decision.confidence, decision.reason, needsReview = true, noMatch = false) }
            Decision.NO_MATCH -> Suggestion(null, decision.confidence, decision.reason, needsReview = true, noMatch = true)
        }
    }

    private fun post(settings: OpenAiSettings, body: Map<String, Any?>, timeoutSeconds: Int = settings.timeoutSeconds): JsonNode {
        val request = HttpRequest.newBuilder()
            .uri(URI.create(settings.baseUrl.trim().trimEnd('/') + "/responses"))
            .timeout(Duration.ofSeconds(timeoutSeconds.toLong()))
            .header("Authorization", "Bearer ${settings.apiKey.trim()}")
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body)))
            .build()

        val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
        if (response.statusCode() !in 200..299) {
            val message = runCatching { mapper.readTree(response.body()).path("error").path("message").asText("") }.getOrDefault("")
            throw IllegalStateException("OpenAI HTTP ${response.statusCode()}: ${message.ifBlank { response.body().take(300) }}")
        }
        return mapper.readTree(response.body())
    }

    private fun extractOutputText(root: JsonNode): String? {
        val outputs = root.path("output")
        if (!outputs.isArray) return null
        outputs.forEach { outputItem ->
            val contents = outputItem.path("content")
            if (!contents.isArray) return@forEach
            contents.forEach { content ->
                if (content.path("type").asText() == "output_text") {
                    return content.path("text").asText(null)
                }
            }
        }
        return null
    }

    private fun buildUserPrompt(
        invoiceName: String,
        invoiceUnit: String,
        invoiceCode: String,
        candidates: List<Candidate>,
    ): String {
        val payload = mapOf(
            "invoice_item" to mapOf(
                "name" to invoiceName,
                "unit" to invoiceUnit,
                "code" to invoiceCode,
            ),
            "candidates" to candidates.map { candidate ->
                mapOf(
                    "code" to candidate.code,
                    "name" to candidate.name,
                    "unit" to candidate.unit,
                    "heuristic_score" to ((candidate.heuristicScore * 1000).toInt() / 10.0),
                )
            },
        )
        return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(payload)
    }

    private fun systemPrompt(): String {
        return """
            You match Vietnamese product names between invoice data and an internal XNT inventory shortlist.
            Rules:
            - Only choose from the provided candidates.
            - Numeric, model, size, diameter, thickness, and specification tokens are strong identifiers and must not be ignored.
            - Unit mismatches are allowed but should reduce confidence.
            - If exactly one candidate is clearly the same product, return decision=match.
            - If one candidate looks best but still needs human review, return decision=review with that candidate code.
            - If none of the candidates are reliable, return decision=no_match and leave matched_code empty.
            - Never invent a code outside the shortlist.
            """.trimIndent()
    }

    private fun decisionSchema(): Map<String, Any> {
        return mapOf(
            "type" to "object",
            "additionalProperties" to false,
            "properties" to mapOf(
                "decision" to mapOf(
                    "type" to "string",
                    "enum" to listOf("match", "review", "no_match"),
                ),
                "matched_code" to mapOf(
                    "type" to "string",
                    "description" to "Candidate code from the shortlist. Empty string when decision=no_match.",
                ),
                "confidence" to mapOf(
                    "type" to "integer",
                    "minimum" to 0,
                    "maximum" to 100,
                ),
                "reason" to mapOf(
                    "type" to "string",
                ),
            ),
            "required" to listOf("decision", "matched_code", "confidence", "reason"),
        )
    }

    private fun buildCacheKey(
        settings: OpenAiSettings,
        invoiceName: String,
        invoiceUnit: String,
        invoiceCode: String,
        candidates: List<Candidate>,
    ): String {
        val raw = buildString {
            append(settings.model.trim())
            append('\n')
            append(invoiceName.trim())
            append('\n')
            append(invoiceUnit.trim())
            append('\n')
            append(invoiceCode.trim())
            append('\n')
            candidates.forEach { candidate ->
                append(candidate.code.trim())
                append('|')
                append(candidate.name.trim())
                append('|')
                append(candidate.unit.trim())
                append('|')
                append(candidate.heuristicScore)
                append('\n')
            }
        }
        return sha256(raw)
    }

    private fun saveCache(cacheKey: String, decision: CachedDecision) {
        synchronized(cacheLock) {
            val properties = loadProperties()
            properties[cacheKey] = encode(decision)
            Files.createDirectories(cachePath.parent)
            Files.newBufferedWriter(cachePath).use { writer: Writer ->
                properties.store(writer, "ReconCore OpenAI match cache")
            }
        }
    }

    private fun loadCache(cacheKey: String): CachedDecision? {
        synchronized(cacheLock) {
            if (!Files.exists(cachePath)) return null
            val raw = loadProperties().getProperty(cacheKey) ?: return null
            return decode(raw)
        }
    }

    private fun rememberFailure(cacheKey: String) {
        synchronized(cacheLock) {
            recentFailures[cacheKey] = System.currentTimeMillis() + FAILURE_COOLDOWN_MILLIS
        }
    }

    private fun clearFailure(cacheKey: String) {
        synchronized(cacheLock) {
            recentFailures.remove(cacheKey)
        }
    }

    private fun inFailureCooldown(cacheKey: String): Boolean {
        synchronized(cacheLock) {
            val expiresAt = recentFailures[cacheKey] ?: return false
            if (expiresAt <= System.currentTimeMillis()) {
                recentFailures.remove(cacheKey)
                return false
            }
            return true
        }
    }

    private fun loadProperties(): Properties {
        val properties = Properties()
        if (!Files.exists(cachePath)) return properties
        Files.newBufferedReader(cachePath).use { reader: Reader ->
            properties.load(reader)
        }
        return properties
    }

    private fun encode(decision: CachedDecision): String {
        return listOf(
            decision.decision.name,
            decision.matchedCode.orEmpty(),
            decision.confidence.toString(),
            Base64.getUrlEncoder().withoutPadding().encodeToString(decision.reason.toByteArray(StandardCharsets.UTF_8)),
        ).joinToString("\t")
    }

    private fun decode(raw: String): CachedDecision? {
        val parts = raw.split('\t')
        val decision = runCatching { Decision.valueOf(parts.getOrElse(0) { "NO_MATCH" }) }.getOrNull() ?: return null
        val matchedCode = parts.getOrElse(1) { "" }.ifBlank { null }
        val confidence = parts.getOrElse(2) { "0" }.toDoubleOrNull()?.coerceIn(0.0, 1.0) ?: 0.0
        val reason = runCatching {
            String(Base64.getUrlDecoder().decode(parts.getOrElse(3) { "" }), StandardCharsets.UTF_8)
        }.getOrDefault("Không có diễn giải")
        return CachedDecision(decision, matchedCode, confidence, reason)
    }

    private fun sha256(text: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(text.toByteArray(StandardCharsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }
}
