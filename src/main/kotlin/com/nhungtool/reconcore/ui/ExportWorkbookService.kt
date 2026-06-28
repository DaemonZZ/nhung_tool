package com.nhungtool.reconcore.ui

import org.apache.poi.ss.usermodel.BorderStyle
import org.apache.poi.ss.usermodel.FillPatternType
import org.apache.poi.ss.usermodel.HorizontalAlignment
import org.apache.poi.ss.usermodel.IndexedColors
import org.apache.poi.ss.usermodel.VerticalAlignment
import org.apache.poi.ss.util.CellRangeAddress
import org.apache.poi.xssf.usermodel.XSSFCellStyle
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.outputStream

object ExportWorkbookService {
    enum class ExportMode {
        ALL_IN_ONE,
        SPLIT_READY_AND_PENDING,
        READY_ONLY,
    }

    data class ExportOptions(
        val outputDirectory: Path,
        val fileName: String,
        val exportMode: ExportMode,
        val includeWarningSheets: Boolean,
        val includeRunLog: Boolean,
    )

    data class ExportResult(
        val outputPath: Path,
        val sheetNames: List<String>,
        val exportedDetailedRows: Int,
        val exportedNegativeRows: Int,
    )

    fun buildPreview(
        analysis: WorkspaceAnalysis,
        outputDirectory: String,
        fileName: String,
        exportMode: ExportMode,
        includeWarningSheets: Boolean,
        includeRunLog: Boolean,
    ): ExportPreviewView {
        val detailedReadyRows = readyDetailedRows(analysis)
        val detailedPendingRows = pendingDetailedRows(analysis)
        val negativeReadyRows = readyNegativeRows(analysis)
        val negativePendingRows = pendingNegativeRows(analysis)
        val mappingWarningRows = filteredMappingWarningRows(analysis, exportMode)
        val unitWarningRows = filteredUnitWarningRows(analysis, exportMode)
        val logs = if (includeRunLog) buildRunLogRows(analysis) else emptyList()

        val sheets = buildList {
            when (exportMode) {
                ExportMode.ALL_IN_ONE -> {
                    add(ExportSheetPreview("KetQua_ChiTiet", detailedReadyRows.size + detailedPendingRows.size, "info"))
                    add(ExportSheetPreview("KetQua_AmKho", negativeReadyRows.size + negativePendingRows.size, "warning"))
                }

                ExportMode.SPLIT_READY_AND_PENDING -> {
                    add(ExportSheetPreview("KetQua_ChiTiet_OK", detailedReadyRows.size, "info"))
                    add(ExportSheetPreview("KetQua_ChiTiet_CanRasoat", detailedPendingRows.size, "danger"))
                    add(ExportSheetPreview("KetQua_AmKho_OK", negativeReadyRows.size, "warning"))
                    add(ExportSheetPreview("KetQua_AmKho_CanRasoat", negativePendingRows.size, "danger"))
                }

                ExportMode.READY_ONLY -> {
                    add(ExportSheetPreview("KetQua_ChiTiet_OK", detailedReadyRows.size, "info"))
                    add(ExportSheetPreview("KetQua_AmKho_OK", negativeReadyRows.size, "warning"))
                }
            }
            if (includeWarningSheets) {
                add(ExportSheetPreview("CanhBao_AnhXa", mappingWarningRows.size, "danger"))
                add(ExportSheetPreview("CanhBao_DonVi", unitWarningRows.size, "neutral"))
            }
            if (includeRunLog) {
                add(ExportSheetPreview("NhatKy_Chay", logs.size, "neutral"))
            }
        }

        return ExportPreviewView(
            outputDirectory = outputDirectory,
            fileName = normalizeFileName(fileName),
            sheets = sheets,
            totalSizeLabel = estimateWorkbookSize(
                detailedRows = detailedReadyRows.size + detailedPendingRows.size,
                negativeRows = negativeReadyRows.size + negativePendingRows.size,
                mappingRows = if (includeWarningSheets) mappingWarningRows.size else 0,
                unitRows = if (includeWarningSheets) unitWarningRows.size else 0,
                logRows = logs.size,
            ),
        )
    }

    fun export(options: ExportOptions): ExportResult {
        val analysis = WorkspaceAnalysisService.load()
        val workbook = XSSFWorkbook()
        try {
            val headerStyle = workbook.createCellStyle().apply {
                setFillForegroundColor(IndexedColors.GREY_25_PERCENT.index)
                fillPattern = FillPatternType.SOLID_FOREGROUND
                borderBottom = BorderStyle.THIN
                borderTop = BorderStyle.THIN
                borderLeft = BorderStyle.THIN
                borderRight = BorderStyle.THIN
                alignment = HorizontalAlignment.CENTER
                verticalAlignment = VerticalAlignment.CENTER
                val font = workbook.createFont().apply {
                    bold = true
                }
                setFont(font)
            }
            val wrappedTextStyle = workbook.createCellStyle().apply {
                wrapText = true
                verticalAlignment = VerticalAlignment.TOP
            }

            val detailedReadyRows = readyDetailedRows(analysis)
            val detailedPendingRows = pendingDetailedRows(analysis)
            val negativeReadyRows = readyNegativeRows(analysis)
            val negativePendingRows = pendingNegativeRows(analysis)
            val mappingWarningRows = filteredMappingWarningRows(analysis, options.exportMode)
            val unitWarningRows = filteredUnitWarningRows(analysis, options.exportMode)
            val logs = if (options.includeRunLog) buildRunLogRows(analysis) else emptyList()

            when (options.exportMode) {
                ExportMode.ALL_IN_ONE -> {
                    createSheet(
                        workbook = workbook,
                        headerStyle = headerStyle,
                        wrappedTextStyle = wrappedTextStyle,
                        sheetName = "KetQua_ChiTiet",
                        headers = detailedHeaders(),
                        rows = (detailedReadyRows + detailedPendingRows).map(::detailedCells),
                    )
                    createSheet(
                        workbook = workbook,
                        headerStyle = headerStyle,
                        wrappedTextStyle = wrappedTextStyle,
                        sheetName = "KetQua_AmKho",
                        headers = negativeHeaders(),
                        rows = (negativeReadyRows + negativePendingRows).map(::negativeCells),
                    )
                }

                ExportMode.SPLIT_READY_AND_PENDING -> {
                    createSheet(
                        workbook = workbook,
                        headerStyle = headerStyle,
                        wrappedTextStyle = wrappedTextStyle,
                        sheetName = "KetQua_ChiTiet_OK",
                        headers = detailedHeaders(),
                        rows = detailedReadyRows.map(::detailedCells),
                    )
                    createSheet(
                        workbook = workbook,
                        headerStyle = headerStyle,
                        wrappedTextStyle = wrappedTextStyle,
                        sheetName = "KetQua_ChiTiet_CanRasoat",
                        headers = detailedHeaders(),
                        rows = detailedPendingRows.map(::detailedCells),
                    )
                    createSheet(
                        workbook = workbook,
                        headerStyle = headerStyle,
                        wrappedTextStyle = wrappedTextStyle,
                        sheetName = "KetQua_AmKho_OK",
                        headers = negativeHeaders(),
                        rows = negativeReadyRows.map(::negativeCells),
                    )
                    createSheet(
                        workbook = workbook,
                        headerStyle = headerStyle,
                        wrappedTextStyle = wrappedTextStyle,
                        sheetName = "KetQua_AmKho_CanRasoat",
                        headers = negativeHeaders(),
                        rows = negativePendingRows.map(::negativeCells),
                    )
                }

                ExportMode.READY_ONLY -> {
                    createSheet(
                        workbook = workbook,
                        headerStyle = headerStyle,
                        wrappedTextStyle = wrappedTextStyle,
                        sheetName = "KetQua_ChiTiet_OK",
                        headers = detailedHeaders(),
                        rows = detailedReadyRows.map(::detailedCells),
                    )
                    createSheet(
                        workbook = workbook,
                        headerStyle = headerStyle,
                        wrappedTextStyle = wrappedTextStyle,
                        sheetName = "KetQua_AmKho_OK",
                        headers = negativeHeaders(),
                        rows = negativeReadyRows.map(::negativeCells),
                    )
                }
            }

            if (options.includeWarningSheets) {
                createSheet(
                    workbook = workbook,
                    headerStyle = headerStyle,
                    wrappedTextStyle = wrappedTextStyle,
                    sheetName = "CanhBao_AnhXa",
                    headers = mappingWarningHeaders(),
                    rows = mappingWarningRows.map(::mappingWarningCells),
                )
                createSheet(
                    workbook = workbook,
                    headerStyle = headerStyle,
                    wrappedTextStyle = wrappedTextStyle,
                    sheetName = "CanhBao_DonVi",
                    headers = unitWarningHeaders(),
                    rows = unitWarningRows.map(::unitWarningCells),
                )
            }

            if (options.includeRunLog) {
                createSheet(
                    workbook = workbook,
                    headerStyle = headerStyle,
                    wrappedTextStyle = wrappedTextStyle,
                    sheetName = "NhatKy_Chay",
                    headers = listOf("MUC", "GIA_TRI"),
                    rows = logs.map { listOf(it.first, it.second) },
                )
            }

            Files.createDirectories(options.outputDirectory)
            val outputPath = options.outputDirectory.resolve(normalizeFileName(options.fileName))
            outputPath.outputStream().use { workbook.write(it) }

            val exportedDetailedCount = when (options.exportMode) {
                ExportMode.ALL_IN_ONE -> detailedReadyRows.size + detailedPendingRows.size
                ExportMode.SPLIT_READY_AND_PENDING -> detailedReadyRows.size + detailedPendingRows.size
                ExportMode.READY_ONLY -> detailedReadyRows.size
            }
            val exportedNegativeCount = when (options.exportMode) {
                ExportMode.ALL_IN_ONE -> negativeReadyRows.size + negativePendingRows.size
                ExportMode.SPLIT_READY_AND_PENDING -> negativeReadyRows.size + negativePendingRows.size
                ExportMode.READY_ONLY -> negativeReadyRows.size
            }

            return ExportResult(
                outputPath = outputPath,
                sheetNames = workbook.map { it.sheetName },
                exportedDetailedRows = exportedDetailedCount,
                exportedNegativeRows = exportedNegativeCount,
            )
        } finally {
            workbook.close()
        }
    }

    private fun readyDetailedRows(analysis: WorkspaceAnalysis): List<DetailedResultRow> = analysis.detailedRows.filter { it.reviewedReady }

    private fun pendingDetailedRows(analysis: WorkspaceAnalysis): List<DetailedResultRow> = analysis.detailedRows.filterNot { it.reviewedReady }

    private fun readyNegativeRows(analysis: WorkspaceAnalysis): List<NegativeInventoryRow> = analysis.negativeInventoryRows.filter { it.reviewedReady }

    private fun pendingNegativeRows(analysis: WorkspaceAnalysis): List<NegativeInventoryRow> = analysis.negativeInventoryRows.filterNot { it.reviewedReady }

    private fun filteredMappingWarningRows(analysis: WorkspaceAnalysis, exportMode: ExportMode): List<MappingRow> {
        val base = analysis.mappingRows.filter {
            it.pendingReview || it.warnings.isNotBlank() || it.decisionState != "Tự động" || it.matchType == "Không có trong XNT"
        }
        return if (exportMode == ExportMode.READY_ONLY) base.filterNot { it.pendingReview } else base
    }

    private fun filteredUnitWarningRows(analysis: WorkspaceAnalysis, exportMode: ExportMode): List<UnitMismatchRow> {
        return if (exportMode == ExportMode.READY_ONLY) analysis.unitMismatchRows.filterNot { it.pendingReview } else analysis.unitMismatchRows
    }

    private fun createSheet(
        workbook: XSSFWorkbook,
        headerStyle: XSSFCellStyle,
        wrappedTextStyle: XSSFCellStyle,
        sheetName: String,
        headers: List<String>,
        rows: List<List<Any?>>,
    ) {
        val sheet = workbook.createSheet(sheetName)
        val headerRow = sheet.createRow(0)
        headers.forEachIndexed { index, header ->
            headerRow.createCell(index).apply {
                setCellValue(header)
                cellStyle = headerStyle
            }
        }

        rows.forEachIndexed { rowIndex, rowValues ->
            val row = sheet.createRow(rowIndex + 1)
            rowValues.forEachIndexed { cellIndex, value ->
                row.createCell(cellIndex).apply {
                    when (value) {
                        null -> setCellValue("")
                        is Number -> setCellValue(value.toDouble())
                        else -> setCellValue(value.toString())
                    }
                    if (value is String && value.length > 30) {
                        cellStyle = wrappedTextStyle
                    }
                }
            }
        }

        sheet.createFreezePane(0, 1)
        sheet.setAutoFilter(CellRangeAddress(0, 0, 0, headers.lastIndex))
        headers.indices.forEach { index ->
            val width = when {
                headers[index].contains("CANH_BAO") || headers[index].contains("LY_DO") || headers[index].contains("TEN_") -> 22 * 256
                headers[index].contains("GHI_CHU") -> 24 * 256
                else -> 16 * 256
            }
            sheet.setColumnWidth(index, width)
        }
    }

    private fun detailedHeaders(): List<String> = listOf(
        "TEN_HANG_HOA_CHUAN",
        "TEN_GOC_XNT",
        "TEN_TREN_HD",
        "DVT_GOC_XNT",
        "DVT_HD",
        "TRANG_THAI_MATCH",
        "CANH_BAO",
        "TON_DAU_KY_SL_XNT",
        "TON_DAU_KY_TIEN_XNT",
        "MUA_VAO_SL_HD",
        "MUA_VAO_TIEN_HD",
        "NHAP_KHO_SL_XNT",
        "NHAP_KHO_TIEN_XNT",
        "CHENH_MUA_NHAP_SL",
        "CHENH_MUA_NHAP_TIEN",
        "BAN_RA_SL_HD",
        "BAN_RA_TIEN_HD",
        "XUAT_KHO_SL_XNT",
        "XUAT_KHO_TIEN_XNT",
        "CHENH_BAN_XUAT_SL",
        "CHENH_BAN_XUAT_TIEN",
        "TON_TINH_TOAN_SL",
        "TON_TINH_TOAN_TIEN",
        "TON_CUOI_KY_SL_XNT",
        "TON_CUOI_KY_TIEN_XNT",
        "CHENH_TON_CUOI_SL",
        "CHENH_TON_CUOI_TIEN",
        "GHI_CHU",
    )

    private fun negativeHeaders(): List<String> = listOf(
        "TEN_HANG_HOA_CHUAN",
        "TEN_GOC_XNT",
        "TEN_TREN_HD",
        "DVT_GOC_XNT",
        "DVT_HD",
        "TRANG_THAI_MATCH",
        "CANH_BAO",
        "NGAY_BAN_PHAT_SINH_AM",
        "TON_DAU_KY_SL_GOC",
        "LUY_KE_MUA_SL_DEN_NGAY",
        "LUY_KE_MUA_TIEN_DEN_NGAY",
        "LUY_KE_BAN_SL_DEN_NGAY",
        "LUY_KE_BAN_TIEN_DEN_NGAY",
        "LUONG_AM_KHO_THUC_TE",
        "TONG_GIA_TRI_BAN_TRONG_NGAY",
        "GHI_CHU",
    )

    private fun mappingWarningHeaders(): List<String> = listOf(
        "MAPPING_KEY",
        "TEN_TREN_HD",
        "MA_XNT",
        "TEN_XNT",
        "TRANG_THAI_MATCH",
        "TRANG_THAI_DECISION",
        "DVT_HD",
        "DVT_XNT",
        "CANH_BAO",
        "LY_DO_MATCH",
        "PENDING_REVIEW",
    )

    private fun unitWarningHeaders(): List<String> = listOf(
        "MAPPING_KEY",
        "MA_XNT",
        "TEN_TREN_HD",
        "TEN_XNT",
        "TRANG_THAI_MATCH",
        "DVT_HD",
        "DVT_XNT",
        "SEVERITY",
        "DECISION_STATE",
        "REASON",
        "ACTION",
        "WARNING_APPEARS_IN_OUTPUT",
        "PENDING_REVIEW",
    )

    private fun detailedCells(row: DetailedResultRow): List<Any?> = listOf(
        row.standardizedName,
        row.xntName,
        row.invoiceName,
        row.xntUnit,
        row.invoiceUnit,
        row.matchStatus,
        row.warnings,
        num(row.openingQty),
        num(row.openingAmt),
        num(row.purchaseQty),
        num(row.purchaseAmt),
        num(row.inboundQty),
        num(row.inboundAmt),
        num(row.purchaseInboundDiffQty),
        num(row.purchaseInboundDiffAmt),
        num(row.salesQty),
        num(row.salesAmt),
        num(row.outboundQty),
        num(row.outboundAmt),
        num(row.salesOutboundDiffQty),
        num(row.salesOutboundDiffAmt),
        num(row.calcEndingQty),
        num(row.calcEndingAmt),
        num(row.xntEndingQty),
        num(row.xntEndingAmt),
        num(row.endingDiffQty),
        num(row.endingDiffAmt),
        row.note,
    )

    private fun negativeCells(row: NegativeInventoryRow): List<Any?> = listOf(
        row.productName,
        row.xntName,
        row.invoiceName,
        row.xntUnit,
        row.invoiceUnit,
        row.matchStatus,
        row.warning,
        row.date,
        num(row.openingQty),
        num(row.cumulativePurchaseQty),
        num(row.cumulativePurchaseAmt),
        num(row.cumulativeSalesQty),
        num(row.cumulativeSalesAmt),
        num(row.negativeQty),
        num(row.sameDaySalesAmt),
        row.note,
    )

    private fun mappingWarningCells(row: MappingRow): List<Any?> = listOf(
        row.mappingKey,
        row.invoiceName,
        row.xntCode,
        row.xntName,
        row.matchType,
        row.decisionState,
        row.invoiceUnit,
        row.xntUnit,
        row.warnings,
        row.matchReason,
        if (row.pendingReview) "CO" else "KHONG",
    )

    private fun unitWarningCells(row: UnitMismatchRow): List<Any?> = listOf(
        row.mappingKey,
        row.xntCode,
        row.invoiceItem,
        row.matchedItem,
        row.matchStatus,
        row.invoiceUnit,
        row.xntUnit,
        row.severity,
        row.decisionState,
        row.reason,
        row.action,
        if (row.warningAppearsInOutput) "CO" else "KHONG",
        if (row.pendingReview) "CO" else "KHONG",
    )

    private fun buildRunLogRows(analysis: WorkspaceAnalysis): List<Pair<String, String>> {
        val summary = analysis.dashboardSummary
        return buildList {
            add("ThoiDiemTao" to analysis.generatedAtLabel)
            add("KyDuLieu" to summary.periodLabel)
            add("KiemTraDuLieu" to summary.validationMetric.value)
            add("HangDoiAnhXa" to summary.mappingMetric.value)
            add("HangDoiDonVi" to summary.unitMetric.value)
            add("AmKho" to summary.negativeMetric.value)
            analysis.progressView.logs.forEachIndexed { index, log ->
                add("NhatKy ${index + 1}" to log)
            }
        }
    }

    private fun normalizeFileName(fileName: String): String {
        val trimmed = fileName.trim().ifBlank { "BaoCao_DoiChieu.xlsx" }
        return if (trimmed.lowercase().endsWith(".xlsx")) trimmed else "$trimmed.xlsx"
    }

    private fun num(text: String): Double? {
        return text.replace(",", "").trim().takeIf { it.isNotBlank() }?.toDoubleOrNull()
    }

    private fun estimateWorkbookSize(
        detailedRows: Int,
        negativeRows: Int,
        mappingRows: Int,
        unitRows: Int,
        logRows: Int,
    ): String {
        val estimatedKb = detailedRows * 0.32 + negativeRows * 0.24 + mappingRows * 0.15 + unitRows * 0.12 + logRows * 0.05 + 80
        return "Dung lượng file ước tính: ${"%.2f".format(estimatedKb / 1024.0)} MB"
    }
}
