package com.nhungtool.reconcore.ui.controller

import com.nhungtool.reconcore.ui.ExportPreviewView
import com.nhungtool.reconcore.ui.ExportSheetPreview
import com.nhungtool.reconcore.ui.ExportWorkbookService
import com.nhungtool.reconcore.ui.WorkspaceAnalysisService
import javafx.fxml.FXML
import javafx.scene.control.Alert
import javafx.scene.control.Button
import javafx.scene.control.ButtonType
import javafx.scene.control.CheckBox
import javafx.scene.control.Label
import javafx.scene.control.RadioButton
import javafx.scene.control.TextField
import javafx.scene.layout.VBox
import javafx.stage.DirectoryChooser
import java.nio.file.Files
import java.nio.file.Path

class ExportController {
    @FXML private lateinit var outputDirectoryField: TextField
    @FXML private lateinit var outputFileNameField: TextField
    @FXML private lateinit var browseDirectoryButton: Button
    @FXML private lateinit var exportAllRadio: RadioButton
    @FXML private lateinit var reviewedOnlyRadio: RadioButton
    @FXML private lateinit var includeWarningsCheckBox: CheckBox
    @FXML private lateinit var includeRunLogCheckBox: CheckBox
    @FXML private lateinit var resetFormButton: Button
    @FXML private lateinit var exportWorkbookButton: Button
    @FXML private lateinit var previewBox: VBox
    @FXML private lateinit var totalSizeLabel: Label

    private lateinit var defaultPreview: ExportPreviewView

    @FXML
    fun initialize() {
        defaultPreview = WorkspaceAnalysisService.load().exportPreviewView
        outputDirectoryField.text = defaultPreview.outputDirectory
        outputFileNameField.text = defaultPreview.fileName

        outputDirectoryField.textProperty().addListener { _, _, _ -> refreshPreview() }
        outputFileNameField.textProperty().addListener { _, _, _ -> refreshPreview() }
        exportAllRadio.selectedProperty().addListener { _, _, _ -> refreshPreview() }
        reviewedOnlyRadio.selectedProperty().addListener { _, _, _ -> refreshPreview() }
        includeWarningsCheckBox.selectedProperty().addListener { _, _, _ -> refreshPreview() }
        includeRunLogCheckBox.selectedProperty().addListener { _, _, _ -> refreshPreview() }

        refreshPreview()
    }

    @FXML
    private fun handleBrowseDirectory() {
        val chooser = DirectoryChooser().apply {
            title = "Chọn thư mục xuất tệp Excel"
            val current = outputDirectoryField.text.trim().takeIf { it.isNotBlank() }?.let { Path.of(it) }
            current?.toFile()?.takeIf { it.exists() }?.let { initialDirectory = it }
        }
        val window = outputDirectoryField.scene?.window ?: return
        chooser.showDialog(window)?.toPath()?.let {
            outputDirectoryField.text = it.toString()
        }
    }

    @FXML
    private fun handleResetForm() {
        outputDirectoryField.text = defaultPreview.outputDirectory
        outputFileNameField.text = defaultPreview.fileName
        exportAllRadio.isSelected = true
        includeWarningsCheckBox.isSelected = true
        includeRunLogCheckBox.isSelected = true
        refreshPreview()
    }

    @FXML
    private fun handleExportWorkbook() {
        val outputDirectory = outputDirectoryField.text.trim().takeIf { it.isNotBlank() }?.let { Path.of(it) }
        if (outputDirectory == null) {
            showAlert(Alert.AlertType.ERROR, "Thiếu thư mục xuất", "Hãy nhập hoặc chọn thư mục xuất tệp Excel.")
            return
        }

        val fileName = outputFileNameField.text.trim().ifBlank { defaultPreview.fileName }
        val options = ExportWorkbookService.ExportOptions(
            outputDirectory = outputDirectory,
            fileName = fileName,
            reviewedOnly = reviewedOnlyRadio.isSelected,
            includeWarningSheets = includeWarningsCheckBox.isSelected,
            includeRunLog = includeRunLogCheckBox.isSelected,
        )

        val targetPath = options.outputDirectory.resolve(
            if (fileName.lowercase().endsWith(".xlsx")) fileName else "$fileName.xlsx",
        )
        if (Files.exists(targetPath)) {
            val confirm = Alert(Alert.AlertType.CONFIRMATION).apply {
                title = "ReconCore"
                headerText = "File đã tồn tại"
                contentText = "File ${targetPath.fileName} đã tồn tại. Ghi đè file này?"
                buttonTypes.setAll(ButtonType.OK, ButtonType.CANCEL)
            }
            if (confirm.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) {
                return
            }
        }

        val result = runCatching { ExportWorkbookService.export(options) }.getOrElse { error ->
            showAlert(Alert.AlertType.ERROR, "Xuất tệp Excel thất bại", error.message ?: "Không xác định được lỗi khi ghi file.")
            return
        }

        outputFileNameField.text = result.outputPath.fileName.toString()
        showAlert(
            Alert.AlertType.INFORMATION,
            "Đã xuất tệp Excel",
            buildString {
                appendLine("Đường dẫn: ${result.outputPath}")
                appendLine("Các tab: ${result.sheetNames.joinToString(", ")}")
                append("Số dòng: KetQua_ChiTiet=${result.exportedDetailedRows}, KetQua_AmKho=${result.exportedNegativeRows}")
            },
        )
        refreshPreview()
    }

    private fun refreshPreview() {
        val preview = ExportWorkbookService.buildPreview(
            analysis = WorkspaceAnalysisService.load(),
            outputDirectory = outputDirectoryField.text.trim().ifBlank { defaultPreview.outputDirectory },
            fileName = outputFileNameField.text.trim().ifBlank { defaultPreview.fileName },
            reviewedOnly = reviewedOnlyRadio.isSelected,
            includeWarningSheets = includeWarningsCheckBox.isSelected,
            includeRunLog = includeRunLogCheckBox.isSelected,
        )
        previewBox.children.setAll(preview.sheets.map { sheetLabel(it) })
        totalSizeLabel.text = preview.totalSizeLabel
    }

    private fun sheetLabel(sheet: ExportSheetPreview): Label {
        return Label("${sheet.label} • ${sheet.rowCount} dòng").apply {
            styleClass.add(
                when (sheet.tone) {
                    "warning" -> "pill-warning"
                    "danger" -> "pill-danger"
                    "info" -> "pill-info"
                    else -> "pill-neutral"
                },
            )
        }
    }

    private fun showAlert(type: Alert.AlertType, header: String, content: String) {
        Alert(type).apply {
            title = "ReconCore"
            headerText = header
            contentText = content
            dialogPane.minHeight = 260.0
        }.showAndWait()
    }
}
