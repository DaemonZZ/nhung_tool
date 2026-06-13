package com.nhungtool.reconcore.ui

data class SourceSummary(
    val fileName: String,
    val status: String,
    val meta: String,
    val sheets: List<String>,
)

data class MetricSummary(
    val title: String,
    val value: String,
    val detail: String,
)

data class DashboardSummary(
    val periodLabel: String,
    val xntSource: SourceSummary,
    val invoiceSource: SourceSummary,
    val validationMetric: MetricSummary,
    val mappingMetric: MetricSummary,
    val unitMetric: MetricSummary,
    val negativeMetric: MetricSummary,
)

object DashboardDataService {
    fun loadSummary(): DashboardSummary = WorkspaceAnalysisService.load().dashboardSummary
}
