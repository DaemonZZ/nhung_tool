package com.nhungtool.reconcore.ui

import java.io.Reader
import java.io.Writer
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import java.util.Properties

data class OpenAiSettings(
    val enabled: Boolean = false,
    val apiKey: String = "",
    val model: String = "gpt-5.4-mini",
    val baseUrl: String = "https://api.openai.com/v1",
    val timeoutSeconds: Int = 8,
) {
    val configured: Boolean
        get() = enabled && apiKey.isNotBlank()

    fun signature(): String {
        val raw = listOf(
            enabled.toString(),
            model.trim(),
            baseUrl.trim().trimEnd('/'),
            timeoutSeconds.toString(),
            sha256(apiKey.trim()),
        ).joinToString("|")
        return sha256(raw)
    }

    companion object {
        private fun sha256(text: String): String {
            val digest = MessageDigest.getInstance("SHA-256").digest(text.toByteArray(StandardCharsets.UTF_8))
            return digest.joinToString("") { "%02x".format(it) }
        }
    }
}

object OpenAiSettingsService {
    private val storagePath: Path = Path.of(System.getProperty("user.dir"), ".reconcore", "openai-settings.properties")
    private val supportedModels = listOf(
        "gpt-5.4-mini",
        "gpt-5.4-nano",
        "gpt-5.5",
    )

    fun load(): OpenAiSettings {
        if (!Files.exists(storagePath)) return OpenAiSettings()
        val properties = Properties()
        Files.newBufferedReader(storagePath).use { reader: Reader ->
            properties.load(reader)
        }
        return OpenAiSettings(
            enabled = properties.getProperty("enabled", "false").toBooleanStrictOrNull() ?: false,
            apiKey = properties.getProperty("apiKey", "").trim(),
            model = properties.getProperty("model", OpenAiSettings().model).trim().ifBlank { OpenAiSettings().model },
            baseUrl = properties.getProperty("baseUrl", OpenAiSettings().baseUrl).trim().ifBlank { OpenAiSettings().baseUrl },
            timeoutSeconds = properties.getProperty("timeoutSeconds", OpenAiSettings().timeoutSeconds.toString()).toIntOrNull()
                ?.coerceIn(3, 120) ?: OpenAiSettings().timeoutSeconds,
        )
    }

    fun save(settings: OpenAiSettings) {
        Files.createDirectories(storagePath.parent)
        val properties = Properties()
        properties["enabled"] = settings.enabled.toString()
        properties["apiKey"] = settings.apiKey.trim()
        properties["model"] = settings.model.trim()
        properties["baseUrl"] = settings.baseUrl.trim().trimEnd('/')
        properties["timeoutSeconds"] = settings.timeoutSeconds.coerceIn(3, 120).toString()
        Files.newBufferedWriter(storagePath).use { writer: Writer ->
            properties.store(writer, "ReconCore OpenAI online settings")
        }
    }

    fun supportedModels(): List<String> = supportedModels

    fun storagePath(): Path = storagePath
}
