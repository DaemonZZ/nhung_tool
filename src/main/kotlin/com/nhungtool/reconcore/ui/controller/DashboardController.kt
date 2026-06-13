package com.nhungtool.reconcore.ui.controller

import com.nhungtool.reconcore.ui.DashboardDataService
import com.nhungtool.reconcore.ui.MetricSummary
import com.nhungtool.reconcore.ui.SourceSummary
import javafx.fxml.FXML
import javafx.scene.control.Label
import javafx.scene.layout.VBox

class DashboardController {
    @FXML private lateinit var currentPeriodLabel: Label
    @FXML private lateinit var xntFileLabel: Label
    @FXML private lateinit var xntMetaLabel: Label
    @FXML private lateinit var invoiceFileLabel: Label
    @FXML private lateinit var invoiceMetaLabel: Label
    @FXML private lateinit var xntSheetsBox: VBox
    @FXML private lateinit var invoiceSheetsBox: VBox
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

        currentPeriodLabel.text = "Current period: ${summary.periodLabel}"
        renderSource(summary.xntSource, xntFileLabel, xntMetaLabel, xntSheetsBox)
        renderSource(summary.invoiceSource, invoiceFileLabel, invoiceMetaLabel, invoiceSheetsBox)
        renderMetric(summary.validationMetric, validationTitleLabel, validationValueLabel, validationDetailLabel)
        renderMetric(summary.mappingMetric, mappingTitleLabel, mappingValueLabel, mappingDetailLabel)
        renderMetric(summary.unitMetric, unitTitleLabel, unitValueLabel, unitDetailLabel)
        renderMetric(summary.negativeMetric, negativeTitleLabel, negativeValueLabel, negativeDetailLabel)
    }

    private fun renderSource(summary: SourceSummary, fileLabel: Label, metaLabel: Label, sheetsBox: VBox) {
        fileLabel.text = "${summary.fileName} • ${summary.status}"
        metaLabel.text = summary.meta
        sheetsBox.children.clear()

        val sheetLabels = if (summary.sheets.isEmpty()) {
            listOf("Không có sheet khả dụng")
        } else {
            summary.sheets
        }

        sheetLabels.forEach { sheetText ->
            val label = Label(sheetText)
            label.styleClass.add("pill-neutral")
            sheetsBox.children += label
        }
    }

    private fun renderMetric(metric: MetricSummary, titleLabel: Label, valueLabel: Label, detailLabel: Label) {
        titleLabel.text = metric.title
        valueLabel.text = metric.value
        detailLabel.text = metric.detail
    }
}
