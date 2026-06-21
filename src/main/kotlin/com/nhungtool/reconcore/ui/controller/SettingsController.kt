package com.nhungtool.reconcore.ui.controller

import com.nhungtool.reconcore.ui.OpenAiNameMatcher
import com.nhungtool.reconcore.ui.OpenAiSettings
import com.nhungtool.reconcore.ui.OpenAiSettingsService
import javafx.concurrent.Task
import javafx.fxml.FXML
import javafx.scene.control.Alert
import javafx.scene.control.Button
import javafx.scene.control.CheckBox
import javafx.scene.control.ComboBox
import javafx.scene.control.Label
import javafx.scene.control.PasswordField
import javafx.scene.control.TextField

class SettingsController {
    @FXML private lateinit var enableOpenAiCheckBox: CheckBox
    @FXML private lateinit var apiKeyField: PasswordField
    @FXML private lateinit var modelComboBox: ComboBox<String>
    @FXML private lateinit var baseUrlField: TextField
    @FXML private lateinit var timeoutField: TextField
    @FXML private lateinit var storagePathLabel: Label
    @FXML private lateinit var statusLabel: Label
    @FXML private lateinit var saveSettingsButton: Button
    @FXML private lateinit var testConnectionButton: Button
    @FXML private lateinit var clearCacheButton: Button

    @FXML
    fun initialize() {
        modelComboBox.items.addAll(OpenAiSettingsService.supportedModels())
        storagePathLabel.text = "Lưu cục bộ tại: ${OpenAiSettingsService.storagePath()}"
        loadIntoForm()
    }

    @FXML
    private fun handleSaveSettings() {
        val settings = formSettings() ?: return
        OpenAiSettingsService.save(settings)
        statusLabel.text = "Đã lưu cấu hình GPT online. Bấm 'Chạy đối chiếu' để áp dụng."
    }

    @FXML
    private fun handleTestConnection() {
        val settings = formSettings() ?: return
        setBusy(true)
        statusLabel.text = "Đang kiểm tra kết nối OpenAI..."
        val task = object : Task<OpenAiNameMatcher.ConnectionTestResult>() {
            override fun call(): OpenAiNameMatcher.ConnectionTestResult {
                return OpenAiNameMatcher.testConnection(settings)
            }
        }
        task.setOnSucceeded {
            setBusy(false)
            val result = task.value
            statusLabel.text = result.message
        }
        task.setOnFailed {
            setBusy(false)
            statusLabel.text = "Kiểm tra kết nối thất bại: ${task.exception?.message ?: "không xác định"}"
        }
        Thread(task, "openai-connection-test").apply {
            isDaemon = true
            start()
        }
    }

    @FXML
    private fun handleClearCache() {
        val removed = OpenAiNameMatcher.clearCache()
        statusLabel.text = "Đã xóa $removed bản ghi cache GPT online."
    }

    private fun loadIntoForm() {
        val settings = OpenAiSettingsService.load()
        enableOpenAiCheckBox.isSelected = settings.enabled
        apiKeyField.text = settings.apiKey
        modelComboBox.value = settings.model
        baseUrlField.text = settings.baseUrl
        timeoutField.text = settings.timeoutSeconds.toString()
        statusLabel.text = if (settings.configured) {
            "GPT online đã cấu hình. Model hiện tại: ${settings.model}"
        } else {
            "GPT online đang tắt hoặc chưa có API key."
        }
    }

    private fun formSettings(): OpenAiSettings? {
        val apiKey = apiKeyField.text.trim()
        val model = modelComboBox.value?.trim().orEmpty().ifBlank { OpenAiSettings().model }
        val baseUrl = baseUrlField.text.trim().ifBlank { OpenAiSettings().baseUrl }
        val timeout = timeoutField.text.trim().toIntOrNull()
        if (enableOpenAiCheckBox.isSelected && apiKey.isBlank()) {
            showAlert("Thiếu API key", "Hãy nhập API key OpenAI trước khi bật GPT online.")
            return null
        }
        if (!baseUrl.startsWith("http://") && !baseUrl.startsWith("https://")) {
            showAlert("Base URL không hợp lệ", "Base URL phải bắt đầu bằng http:// hoặc https://")
            return null
        }
        if (timeout == null || timeout !in 5..120) {
            showAlert("Timeout không hợp lệ", "Timeout phải nằm trong khoảng 5 đến 120 giây.")
            return null
        }
        return OpenAiSettings(
            enabled = enableOpenAiCheckBox.isSelected,
            apiKey = apiKey,
            model = model,
            baseUrl = baseUrl.trimEnd('/'),
            timeoutSeconds = timeout,
        )
    }

    private fun setBusy(busy: Boolean) {
        saveSettingsButton.isDisable = busy
        testConnectionButton.isDisable = busy
        clearCacheButton.isDisable = busy
    }

    private fun showAlert(header: String, content: String) {
        Alert(Alert.AlertType.WARNING).apply {
            headerText = header
            contentText = content
        }.showAndWait()
    }
}
