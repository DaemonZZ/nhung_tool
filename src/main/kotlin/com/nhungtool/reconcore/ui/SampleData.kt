package com.nhungtool.reconcore.ui

data class MappingRow(
    val mappingKey: String,
    val invoiceName: String,
    val xntCode: String,
    val xntName: String,
    val matchType: String,
    val confidence: String,
    val invoiceUnit: String,
    val xntUnit: String,
    val warnings: String,
    val decisionState: String,
    val matchReason: String,
    val warningIgnored: Boolean,
    val pendingReview: Boolean,
)

data class UnitMismatchRow(
    val mappingKey: String,
    val xntCode: String,
    val invoiceItem: String,
    val matchedItem: String,
    val invoiceUnit: String,
    val xntUnit: String,
    val matchStatus: String,
    val severity: String,
    val decisionState: String,
    val reason: String,
    val action: String,
    val pendingReview: Boolean,
    val warningAppearsInOutput: Boolean,
)

data class DetailedResultRow(
    val standardizedName: String,
    val xntName: String,
    val invoiceName: String,
    val xntUnit: String,
    val invoiceUnit: String,
    val matchStatus: String,
    val warnings: String,
    val openingQty: String,
    val openingAmt: String,
    val purchaseQty: String,
    val purchaseAmt: String,
    val inboundQty: String,
    val inboundAmt: String,
    val purchaseInboundDiffQty: String,
    val purchaseInboundDiffAmt: String,
    val salesQty: String,
    val salesAmt: String,
    val outboundQty: String,
    val outboundAmt: String,
    val salesOutboundDiffQty: String,
    val salesOutboundDiffAmt: String,
    val calcEndingQty: String,
    val calcEndingAmt: String,
    val xntEndingQty: String,
    val xntEndingAmt: String,
    val endingDiffQty: String,
    val endingDiffAmt: String,
    val note: String,
    val reviewedReady: Boolean,
)

data class NegativeInventoryRow(
    val productName: String,
    val xntName: String,
    val invoiceName: String,
    val xntUnit: String,
    val invoiceUnit: String,
    val date: String,
    val openingQty: String,
    val cumulativePurchaseQty: String,
    val cumulativePurchaseAmt: String,
    val cumulativeSalesQty: String,
    val cumulativeSalesAmt: String,
    val negativeQty: String,
    val sameDaySalesAmt: String,
    val matchStatus: String,
    val warning: String,
    val sourceState: String,
    val note: String,
    val reviewedReady: Boolean,
)

data class RunHistoryRow(
    val runId: String,
    val status: String,
    val startedAt: String,
    val summary: String,
)

data class InputSourceCard(
    val title: String,
    val statusLine: String,
    val metaLine: String,
    val warningLine: String,
)

data class InputValidationView(
    val xntCard: InputSourceCard,
    val invoiceCard: InputSourceCard,
    val validationLines: List<String>,
    val xntRows: List<List<String>>,
    val purchaseRows: List<List<String>>,
    val salesRows: List<List<String>>,
    val xntTotalRows: Int,
    val xntWarningRows: Int,
    val purchaseTotalRows: Int,
    val purchaseWarningRows: Int,
    val salesTotalRows: Int,
    val salesWarningRows: Int,
)

data class ProgressView(
    val progress: Double,
    val progressLabel: String,
    val summaryLabel: String,
    val stages: List<String>,
    val logs: List<String>,
)

data class HistoryView(
    val runs: List<RunHistoryRow>,
    val logsByRun: Map<String, List<String>>,
)

data class ExportSheetPreview(
    val label: String,
    val rowCount: Int,
    val tone: String,
)

data class ExportPreviewView(
    val outputDirectory: String,
    val fileName: String,
    val sheets: List<ExportSheetPreview>,
    val totalSizeLabel: String,
)

data class XntCatalogOption(
    val code: String,
    val name: String,
    val unit: String,
) {
    val label: String
        get() = "$code • $name • $unit"
}

data class WorkspaceAnalysis(
    val dashboardSummary: DashboardSummary,
    val inputValidationView: InputValidationView,
    val mappingRows: List<MappingRow>,
    val xntCatalog: List<XntCatalogOption>,
    val unitMismatchRows: List<UnitMismatchRow>,
    val detailedRows: List<DetailedResultRow>,
    val negativeInventoryRows: List<NegativeInventoryRow>,
    val progressView: ProgressView,
    val historyView: HistoryView,
    val exportPreviewView: ExportPreviewView,
    val generatedAtLabel: String,
)
