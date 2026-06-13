package com.nhungtool.reconcore.ui.controller

import com.nhungtool.reconcore.ui.AppScreen
import com.nhungtool.reconcore.ui.AppUiCoordinator
import com.nhungtool.reconcore.ui.DashboardDataService
import com.nhungtool.reconcore.ui.MetricSummary
import com.nhungtool.reconcore.ui.SourceSummary
import javafx.fxml.FXML
import javafx.scene.control.Label
import javafx.scene.layout.FlowPane
import javafx.scene.layout.VBox

class DashboardController {
    @FXML private lateinit var currentPeriodLabel: Label
    @FXML private lateinit var workspaceStatusLabel: Label
    @FXML private lateinit var workspaceSummaryLabel: Label
    @FXML private lateinit var xntStatusLabel: Label
    @FXML private lateinit var xntFileLabel: Label
    @FXML private lateinit var xntMetaLabel: Label
    @FXML private lateinit var invoiceStatusLabel: Label
    @FXML private lateinit var invoiceFileLabel: Label
    @FXML private lateinit var invoiceMetaLabel: Label
    @FXML private lateinit var xntSheetsPane: FlowPane
    @FXML private lateinit var invoiceSheetsPane: FlowPane
    @FXML private lateinit var validationCard: VBox
    @FXML private lateinit var mappingCard: VBox
    @FXML private lateinit var unitCard: VBox
    @FXML private lateinit var negativeCard: VBox
    @FXML private lateinit var validationTitleLabel: Label
    @FXML private lateinit var validationValueLabel: Label
    @FXML private lateinit var validationDetailLabel: Label
    @FXML private lateinit var mappingTitleLabel: Label
    @FXML private lateinit var mappingValueLabel: Label
    @FXML private lateinit var mappingDetailLabel: Label
    @FXML private lateinit var unitTitleLabel: Label
    @FXML private lateinit var unitValueLabel: Label
    @FXML private lateinit var unitDetailLabel: Label
    @FXML private lateinit var negativeTitleLabel: Label
    @FXML private lateinit var negativeValueLabel: Label
    @FXML private lateinit var negativeDetailLabel: Label

    @FXML
    fun initialize() {
        val summary = DashboardDataService.loadSummary()
        currentPeriodLabel.text = "Kỳ dữ liệu hiện tại: ${summary.periodLabel}"
        workspaceStatusLabel.text = if (summary.validationMetric.value == "Sẵn sàng") "Sẵn sàng" else "Cần rà soát"
        workspaceStatusLabel.styleClass.setAll("pill-" + if (summary.validationMetric.value == "Sẵn sàng") "success" else "warning")
        workspaceSummaryLabel.text =
            "Đã nạp ${summary.xntSource.fileName} + ${summary.invoiceSource.fileName}. " +
                "${summary.validationMetric.value} • ${summary.mappingMetric.value} • ${summary.unitMetric.value} • ${summary.negativeMetric.value}."

        renderSource(summary.xntSource, xntStatusLabel, xntFileLabel, xntMetaLabel, xntSheetsPane)
        renderSource(summary.invoiceSource, invoiceStatusLabel, invoiceFileLabel, invoiceMetaLabel, invoiceSheetsPane)

        renderMetric(summary.validationMetric, validationCard, validationTitleLabel, validationValueLabel, validationDetailLabel)
        renderMetric(summary.mappingMetric, mappingCard, mappingTitleLabel, mappingValueLabel, mappingDetailLabel)
        renderMetric(summary.unitMetric, unitCard, unitTitleLabel, unitValueLabel, unitDetailLabel)
        renderMetric(summary.negativeMetric, negativeCard, negativeTitleLabel, negativeValueLabel, negativeDetailLabel)
    }

    @FXML
    private fun handleOpenInputValidation() {
        AppUiCoordinator.requestRefresh(force = false, screen = AppScreen.INPUT_VALIDATION)
    }

    @FXML
    private fun handleOpenMappingReview() {
        AppUiCoordinator.requestRefresh(force = false, screen = AppScreen.MAPPING_REVIEW)
    }

    @FXML
    private fun handleOpenUnitReview() {
        AppUiCoordinator.requestRefresh(force = false, screen = AppScreen.UNIT_REVIEW)
    }

    @FXML
    private fun handleOpenDetailedResults() {
        AppUiCoordinator.requestRefresh(force = false, screen = AppScreen.DETAILED_RESULTS)
    }

    @FXML
    private fun handleOpenNegativeInventory() {
        AppUiCoordinator.requestRefresh(force = false, screen = AppScreen.NEGATIVE_INVENTORY)
    }

    private fun renderSource(summary: SourceSummary, statusLabel: Label, fileLabel: Label, metaLabel: Label, sheetsPane: FlowPane) {
        statusLabel.text = summary.status
        statusLabel.styleClass.setAll("pill-" + if (summary.status.equals("Đã nạp", ignoreCase = true)) "success" else "warning")
        fileLabel.text = summary.fileName
        metaLabel.text = summary.meta
        sheetsPane.children.clear()

        val sheetLabels = if (summary.sheets.isEmpty()) {
            listOf("Không có tab khả dụng")
        } else {
            summary.sheets
        }

        sheetLabels.forEach { sheetText ->
            val label = Label(sheetText)
            label.styleClass.add("pill-neutral")
            sheetsPane.children += label
        }
    }

    private fun renderMetric(metric: MetricSummary, card: VBox, titleLabel: Label, valueLabel: Label, detailLabel: Label) {
        titleLabel.text = metric.title
        valueLabel.text = metric.value
        detailLabel.text = metric.detail
        card.styleClass.removeAll("metric-danger", "metric-warning", "metric-success")
        card.styleClass += metricTone(metric)
    }

    private fun metricTone(metric: MetricSummary): String {
        val value = metric.value.lowercase()
        return when (metric.title) {
            "Kiểm tra dữ liệu" -> when {
                "lỗi" in value -> "metric-danger"
                "cảnh báo" in value -> "metric-warning"
                else -> "metric-success"
            }

            "Rà soát ánh xạ" -> if (value.startsWith("0 ")) "metric-success" else "metric-warning"
            "Hàng đợi lệch đơn vị" -> if (value.startsWith("0 ")) "metric-success" else "metric-warning"
            "Âm kho" -> if (value.startsWith("0 ")) "metric-success" else "metric-danger"
            else -> "metric-warning"
        }
    }
}
