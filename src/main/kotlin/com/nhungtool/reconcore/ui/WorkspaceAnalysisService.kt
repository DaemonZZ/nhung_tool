package com.nhungtool.reconcore.ui

import org.apache.poi.ss.usermodel.Cell
import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.ss.usermodel.DataFormatter
import org.apache.poi.ss.usermodel.DateUtil
import org.apache.poi.ss.usermodel.Row
import org.apache.poi.ss.usermodel.WorkbookFactory
import java.nio.file.Files
import java.nio.file.Path
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.io.path.exists
import kotlin.io.path.fileSize
import kotlin.io.path.getLastModifiedTime
import kotlin.math.absoluteValue
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToLong

object WorkspaceAnalysisService {
    private val formatter = DataFormatter()
    private val locale = Locale.US
    private val numberSymbols = DecimalFormatSymbols(locale)
    private val quantityFormat = DecimalFormat("#,##0.########", numberSymbols)
    private val amountFormat = DecimalFormat("#,##0.##", numberSymbols)
    private val percentFormat = DecimalFormat("0", numberSymbols)
    private val modifiedFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
    private val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
    private val generatedFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")

    @Volatile
    private var cached: CacheEntry? = null

    fun load(forceRefresh: Boolean = false): WorkspaceAnalysis {
        val inputSnapshot = WorkspaceInputService.snapshot()
        val currentSignature = CacheSignature(
            signatureFor(inputSnapshot.activeXntPath),
            signatureFor(inputSnapshot.activeInvoicePath),
        )
        val existing = cached
        if (!forceRefresh && existing != null && existing.signature == currentSignature) {
            return existing.analysis
        }

        val analysis = buildAnalysis(inputSnapshot.activeXntPath, inputSnapshot.activeInvoicePath)
        cached = CacheEntry(currentSignature, analysis)
        return analysis
    }

    private fun buildAnalysis(xntPath: Path, invoicePath: Path): WorkspaceAnalysis {
        val generatedAt = LocalDateTime.now()
        val xntResult = loadXntSheet(xntPath)
        val invoiceResult = loadInvoiceWorkbook(invoicePath)
        val mappingDecisions = MappingDecisionService.loadAll()
        val unitDecisions = UnitReviewDecisionService.loadAll()
        val mappings = buildMappings(xntResult.items, invoiceResult.purchaseLines + invoiceResult.salesLines, mappingDecisions)
        val unitMismatchRows = buildUnitMismatchRows(mappings, unitDecisions)
        val detailedRows = buildDetailedRows(xntResult.items, invoiceResult.purchaseLines, invoiceResult.salesLines, mappings, unitDecisions)
        val negativeRows = buildNegativeInventoryRows(xntResult.items, invoiceResult.purchaseLines, invoiceResult.salesLines, mappings, unitDecisions)
        val xntCatalog = xntResult.items.sortedBy { it.name.lowercase(locale) }.map { XntCatalogOption(it.code, it.name, it.unit) }

        val xntSource = buildSourceSummary(xntPath, xntResult.sheetNames)
        val invoiceSource = buildSourceSummary(invoicePath, invoiceResult.sheetNames)
        val validationLines = buildValidationLines(xntResult, invoiceResult)
        val validationMetric = buildValidationMetric(validationLines)
        val reviewCount = mappings.count { it.pendingReview }
        val exactCount = mappings.count { it.matchType == MatchType.EXACT }
        val normalizedCount = mappings.count { it.matchType == MatchType.NORMALIZED }
        val heuristicCount = mappings.count { it.matchType == MatchType.HEURISTIC }
        val confirmedCount = mappings.count { it.matchType == MatchType.CONFIRMED }
        val remappedCount = mappings.count { it.matchType == MatchType.REMAPPED }
        val notFoundCount = mappings.count { it.matchType == MatchType.NOT_IN_XNT }
        val pendingUnitCount = unitMismatchRows.count { it.pendingReview }
        val resolvedUnitCount = unitMismatchRows.count { !it.pendingReview && !it.warningAppearsInOutput }
        val keptUnitCount = unitMismatchRows.count { !it.pendingReview && it.warningAppearsInOutput }

        val dashboardSummary = DashboardSummary(
            periodLabel = detectPeriod(xntPath) ?: detectPeriod(invoicePath) ?: "Không xác định",
            xntSource = xntSource,
            invoiceSource = invoiceSource,
            validationMetric = validationMetric,
            mappingMetric = MetricSummary(
                title = "Rà soát ánh xạ",
                value = "$reviewCount cần rà soát",
                detail = "Khớp chính xác $exactCount • Khớp chuẩn hóa $normalizedCount • Khớp suy luận $heuristicCount • Đã xác nhận $confirmedCount • Đã ánh xạ lại $remappedCount • Không có trong XNT $notFoundCount",
            ),
            unitMetric = MetricSummary(
                title = "Hàng đợi lệch đơn vị",
                value = "$pendingUnitCount cần rà soát",
                detail = when {
                    unitMismatchRows.isEmpty() -> "Không phát hiện lệch ĐVT"
                    else -> "Tổng ${unitMismatchRows.size} • Đã xử lý $resolvedUnitCount • Giữ cảnh báo $keptUnitCount"
                },
            ),
            negativeMetric = MetricSummary(
                title = "Âm kho",
                value = "${negativeRows.size} thời điểm âm",
                detail = if (negativeRows.isEmpty()) "Không phát hiện âm kho" else "${negativeRows.map { it.productName }.distinct().size} mặt hàng có âm kho theo dữ liệu hóa đơn",
            ),
        )

        val inputValidationView = InputValidationView(
            xntCard = InputSourceCard(
                title = xntSource.fileName,
                statusLine = "Trạng thái: ${if (xntResult.items.isNotEmpty()) "sẵn sàng" else "lỗi"} • ${xntResult.sheetNames.size} tab • ${xntResult.items.size} dòng",
                metaLine = xntSource.meta,
                warningLine = xntResult.warningCount.takeIf { it > 0 }?.let { "Cảnh báo: $it dòng thiếu trường hoặc lỗi đọc số liệu" } ?: "Cảnh báo: không có lỗi chặn nhập",
            ),
            invoiceCard = InputSourceCard(
                title = invoiceSource.fileName,
                statusLine = "Trạng thái: ${if (invoiceResult.warningCount > 0) "cảnh báo" else "sẵn sàng"} • ${invoiceResult.sheetNames.joinToString(" + ")}",
                metaLine = "Dòng dữ liệu: ${invoiceResult.purchaseLines.size} mua vào / ${invoiceResult.salesLines.size} bán ra",
                warningLine = "Cảnh báo: MuaVao ${invoiceResult.purchaseWarningCount} • BanRa ${invoiceResult.salesWarningCount} • Tổng ${invoiceResult.warningCount}",
            ),
            validationLines = validationLines,
            xntRows = xntResult.previewRows,
            purchaseRows = invoiceResult.purchasePreviewRows,
            salesRows = invoiceResult.salesPreviewRows,
            xntTotalRows = xntResult.items.size,
            xntWarningRows = xntResult.warningCount,
            purchaseTotalRows = invoiceResult.purchaseLines.size,
            purchaseWarningRows = invoiceResult.purchaseWarningCount,
            salesTotalRows = invoiceResult.salesLines.size,
            salesWarningRows = invoiceResult.salesWarningCount,
        )

        val progressLogs = buildProgressLogs(xntPath, invoicePath, xntResult, invoiceResult, mappings, unitMismatchRows, detailedRows, negativeRows)
        val historyRow = RunHistoryRow(
            runId = "SCAN-${generatedAt.format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"))}",
            status = if (validationMetric.value == "Sẵn sàng") "Sẵn sàng" else "Cảnh báo",
            startedAt = generatedAt.format(generatedFormatter),
            summary = "${xntSource.fileName} + ${invoiceSource.fileName}",
        )

        return WorkspaceAnalysis(
            dashboardSummary = dashboardSummary,
            inputValidationView = inputValidationView,
            mappingRows = mappings.map { it.toUiRow(applicableUnitDecision(it, unitDecisions)) },
            xntCatalog = xntCatalog,
            unitMismatchRows = unitMismatchRows,
            detailedRows = detailedRows,
            negativeInventoryRows = negativeRows,
            progressView = ProgressView(
                progress = 1.0,
                progressLabel = "100%",
                summaryLabel = "Đã xử lý ${xntResult.items.size + invoiceResult.purchaseLines.size + invoiceResult.salesLines.size} dòng • Cảnh báo ${xntResult.warningCount + invoiceResult.warningCount}",
                stages = buildProgressStages(xntResult, invoiceResult, mappings, unitMismatchRows, detailedRows, negativeRows),
                logs = progressLogs,
            ),
            historyView = HistoryView(
                runs = listOf(historyRow),
                logsByRun = mapOf(historyRow.runId to progressLogs),
            ),
            exportPreviewView = ExportPreviewView(
                outputDirectory = Path.of(System.getProperty("user.home"), "Exports").toString(),
                fileName = "BaoCao_DoiChieu_${dashboardSummary.periodLabel}.xlsx",
                sheets = listOf(
                    ExportSheetPreview("KetQua_ChiTiet", detailedRows.size, "info"),
                    ExportSheetPreview("KetQua_AmKho", negativeRows.size, "warning"),
                    ExportSheetPreview("CanhBao_AnhXa", mappings.count { it.warnings.isNotEmpty() || it.matchType == MatchType.NOT_IN_XNT }, "danger"),
                    ExportSheetPreview("CanhBao_DonVi", unitMismatchRows.count { it.warningAppearsInOutput }, "neutral"),
                ),
                totalSizeLabel = estimateWorkbookSize(detailedRows.size, negativeRows.size, mappings.size, unitMismatchRows.size),
            ),
            generatedAtLabel = generatedAt.format(generatedFormatter),
        )
    }

    private fun buildValidationMetric(validationLines: List<String>): MetricSummary {
        val blocking = validationLines.count { it.startsWith("Lỗi:") }
        val warnings = validationLines.count { it.startsWith("Cảnh báo:") }
        return when {
            blocking == 0 && warnings == 0 -> MetricSummary(
                title = "Kiểm tra dữ liệu",
                value = "Sẵn sàng",
                detail = "Cấu trúc và dữ liệu đầu vào hợp lệ ở mức cơ bản",
            )
            blocking == 0 -> MetricSummary(
                title = "Kiểm tra dữ liệu",
                value = "$warnings cảnh báo",
                detail = validationLines.joinToString(" • "),
            )
            else -> MetricSummary(
                title = "Kiểm tra dữ liệu",
                value = "$blocking lỗi / $warnings cảnh báo",
                detail = validationLines.joinToString(" • "),
            )
        }
    }

    private fun buildValidationLines(xntResult: XntLoadResult, invoiceResult: InvoiceLoadResult): List<String> {
        val lines = mutableListOf<String>()
        if (xntResult.items.isEmpty()) {
            lines += "Lỗi: không đọc được dòng dữ liệu XNT"
        }
        if (!invoiceResult.sheetNames.contains("MuaVao_1")) {
            lines += "Lỗi: thiếu tab MuaVao_1"
        }
        if (!invoiceResult.sheetNames.contains("BanRa_1")) {
            lines += "Lỗi: thiếu tab BanRa_1"
        }
        if (invoiceResult.purchaseLines.isEmpty()) {
            lines += "Cảnh báo: không có dòng mua vào hợp lệ"
        }
        if (invoiceResult.salesLines.isEmpty()) {
            lines += "Cảnh báo: không có dòng bán ra hợp lệ"
        }
        if (xntResult.warningCount > 0) {
            lines += "Cảnh báo: XNT có ${xntResult.warningCount} dòng thiếu mã / tên / đơn vị hoặc số liệu"
        }
        if (invoiceResult.warningCount > 0) {
            lines += "Cảnh báo: hóa đơn có ${invoiceResult.warningCount} dòng thiếu ngày / tên / đơn vị / số lượng / thành tiền"
        }
        return lines
    }

    private fun buildProgressStages(
        xntResult: XntLoadResult,
        invoiceResult: InvoiceLoadResult,
        mappings: List<MatchResult>,
        unitMismatchRows: List<UnitMismatchRow>,
        detailedRows: List<DetailedResultRow>,
        negativeRows: List<NegativeInventoryRow>,
    ): List<String> {
        return listOf(
            "1. Đọc file - thành công (${xntResult.items.size} dòng XNT, ${invoiceResult.purchaseLines.size + invoiceResult.salesLines.size} dòng hóa đơn)",
            "2. Kiểm tra cấu trúc - ${if (xntResult.warningCount + invoiceResult.warningCount > 0) "cảnh báo" else "thành công"}",
            "3. Chuẩn hóa mặt hàng - thành công (${mappings.size} định danh mặt hàng hóa đơn)",
            "4. So khớp mặt hàng - thành công (${mappings.count { it.matchType == MatchType.EXACT }} khớp chính xác, ${mappings.count { it.matchType == MatchType.NORMALIZED }} khớp chuẩn hóa, ${mappings.count { it.matchType == MatchType.HEURISTIC }} khớp suy luận)",
            "5. Đối chiếu chi tiết - thành công (${detailedRows.size} dòng)",
            "6. Phân tích lệch đơn vị - thành công (${unitMismatchRows.size} cảnh báo)",
            "7. Phân tích âm kho - thành công (${negativeRows.size} thời điểm âm)",
            "8. Tạo xem trước tệp Excel - thành công (4 tab xem trước)",
        )
    }

    private fun buildProgressLogs(
        xntPath: Path,
        invoicePath: Path,
        xntResult: XntLoadResult,
        invoiceResult: InvoiceLoadResult,
        mappings: List<MatchResult>,
        unitMismatchRows: List<UnitMismatchRow>,
        detailedRows: List<DetailedResultRow>,
        negativeRows: List<NegativeInventoryRow>,
    ): List<String> {
        val pendingUnitCount = unitMismatchRows.count { it.pendingReview }
        return listOf(
            "[THÔNG TIN] Đã nạp tệp Excel ${xntPath.fileName} (${xntResult.items.size} dòng XNT)",
            "[THÔNG TIN] Đã nạp tệp Excel ${invoicePath.fileName} (MuaVao_1=${invoiceResult.purchaseLines.size}, BanRa_1=${invoiceResult.salesLines.size})",
            "[THÔNG TIN] Cảnh báo kiểm tra dữ liệu: XNT=${xntResult.warningCount}, Hóa đơn=${invoiceResult.warningCount}",
            "[THÔNG TIN] Đã chuẩn hóa ${mappings.size} định danh mặt hàng hóa đơn",
            "[THÔNG TIN] Phân bố so khớp: khớp_chính_xác=${mappings.count { it.matchType == MatchType.EXACT }}, khớp_chuẩn_hóa=${mappings.count { it.matchType == MatchType.NORMALIZED }}, khớp_suy_luận=${mappings.count { it.matchType == MatchType.HEURISTIC }}, cần_rà_soát=${mappings.count { it.matchType == MatchType.NEEDS_REVIEW }}, không_có_trong_xnt=${mappings.count { it.matchType == MatchType.NOT_IN_XNT }}",
            "[CẢNH BÁO] Hàng đợi lệch đơn vị: chờ_rà_soát=$pendingUnitCount, tổng=${unitMismatchRows.size}",
            "[THÔNG TIN] Số dòng đối chiếu chi tiết: ${detailedRows.size}",
            "[THÔNG TIN] Số thời điểm âm kho: ${negativeRows.size}",
            "[GIẢ ĐỊNH] Âm kho hiện đang cộng mua trong ngày trước rồi mới trừ bán trong ngày",
            "[HOÀN TẤT] Đã làm mới phân tích không gian làm việc thành công",
        )
    }

    private fun buildDetailedRows(
        xntItems: List<XntItem>,
        purchaseLines: List<InvoiceLine>,
        salesLines: List<InvoiceLine>,
        mappings: List<MatchResult>,
        unitDecisions: Map<String, UnitReviewDecisionService.UnitReviewDecision>,
    ): List<DetailedResultRow> {
        val mappingByKey = mappings.associateBy { it.identity.key }
        val xntByCode = xntItems.associateBy { it.code }
        val buckets = linkedMapOf<String, DetailBucket>()

        fun ensureBucket(mapping: MatchResult): DetailBucket {
            val key = mapping.matchedXnt?.let { "xnt:${it.code}" } ?: "invoice:${mapping.identity.key}"
            return buckets.getOrPut(key) {
                DetailBucket(
                    matchedXnt = mapping.matchedXnt,
                    standardizedName = mapping.matchedXnt?.name ?: mapping.identity.displayName,
                    xntName = mapping.matchedXnt?.name.orEmpty(),
                    xntUnit = mapping.matchedXnt?.unit.orEmpty(),
                )
            }
        }

        purchaseLines.forEach { line ->
            val mapping = mappingByKey[line.identity.key] ?: return@forEach
            val unitDecision = applicableUnitDecision(mapping, unitDecisions)
            val bucket = ensureBucket(mapping)
            bucket.invoiceNames += line.name
            bucket.invoiceUnits += line.unit
            bucket.purchaseQty += line.quantity
            bucket.purchaseAmt += line.amount
            bucket.statuses += mapping.matchType
            bucket.warnings += mapping.effectiveWarnings(unitDecision)
            bucket.reviewedReady = bucket.reviewedReady && mapping.isReviewedReady(unitDecision)
        }

        salesLines.forEach { line ->
            val mapping = mappingByKey[line.identity.key] ?: return@forEach
            val unitDecision = applicableUnitDecision(mapping, unitDecisions)
            val bucket = ensureBucket(mapping)
            bucket.invoiceNames += line.name
            bucket.invoiceUnits += line.unit
            bucket.salesQty += line.quantity
            bucket.salesAmt += line.amount
            bucket.statuses += mapping.matchType
            bucket.warnings += mapping.effectiveWarnings(unitDecision)
            bucket.reviewedReady = bucket.reviewedReady && mapping.isReviewedReady(unitDecision)
        }

        xntItems.forEach { xnt ->
            buckets.getOrPut("xnt:${xnt.code}") {
                DetailBucket(
                    matchedXnt = xnt,
                    standardizedName = xnt.name,
                    xntName = xnt.name,
                    xntUnit = xnt.unit,
                )
            }
        }

        return buckets.values
            .map { bucket ->
                val xnt = bucket.matchedXnt ?: xntByCode[bucket.matchedXnt?.code]
                val openingQty = xnt?.openingQty ?: 0.0
                val openingAmt = xnt?.openingAmt ?: 0.0
                val inboundQty = xnt?.inboundQty ?: 0.0
                val inboundAmt = xnt?.inboundAmt ?: 0.0
                val outboundQty = xnt?.outboundQty ?: 0.0
                val outboundAmt = xnt?.outboundAmt ?: 0.0
                val endingQty = xnt?.endingQty ?: 0.0
                val endingAmt = xnt?.endingAmt ?: 0.0
                val calcEndingQty = openingQty + bucket.purchaseQty - bucket.salesQty
                val calcEndingAmt = openingAmt + bucket.purchaseAmt - bucket.salesAmt
                val warnings = bucket.warnings.filter { it.isNotBlank() }.distinct().joinToString(" | ")
                DetailedResultRow(
                    standardizedName = bucket.standardizedName,
                    xntName = bucket.xntName,
                    invoiceName = bucket.invoiceNames.toDisplayAlias(),
                    xntUnit = bucket.xntUnit,
                    invoiceUnit = bucket.invoiceUnits.toDisplayAlias(),
                    matchStatus = bucket.statuses.maxByRisk().label,
                    warnings = warnings,
                    openingQty = formatQty(openingQty),
                    openingAmt = formatAmt(openingAmt),
                    purchaseQty = formatQty(bucket.purchaseQty),
                    purchaseAmt = formatAmt(bucket.purchaseAmt),
                    inboundQty = formatQty(inboundQty),
                    inboundAmt = formatAmt(inboundAmt),
                    purchaseInboundDiffQty = formatQty(bucket.purchaseQty - inboundQty),
                    purchaseInboundDiffAmt = formatAmt(bucket.purchaseAmt - inboundAmt),
                    salesQty = formatQty(bucket.salesQty),
                    salesAmt = formatAmt(bucket.salesAmt),
                    outboundQty = formatQty(outboundQty),
                    outboundAmt = formatAmt(outboundAmt),
                    salesOutboundDiffQty = formatQty(bucket.salesQty - outboundQty),
                    salesOutboundDiffAmt = formatAmt(bucket.salesAmt - outboundAmt),
                    calcEndingQty = formatQty(calcEndingQty),
                    calcEndingAmt = formatAmt(calcEndingAmt),
                    xntEndingQty = formatQty(endingQty),
                    xntEndingAmt = formatAmt(endingAmt),
                    endingDiffQty = formatQty(calcEndingQty - endingQty),
                    endingDiffAmt = formatAmt(calcEndingAmt - endingAmt),
                    note = when {
                        !bucket.reviewedReady -> "Chưa hoàn tất rà soát"
                        bucket.matchedXnt == null -> "Mặt hàng chỉ có trên hóa đơn"
                        else -> ""
                    },
                    reviewedReady = bucket.reviewedReady,
                )
            }
            .sortedBy { it.standardizedName.lowercase(locale) }
    }

    private fun buildNegativeInventoryRows(
        xntItems: List<XntItem>,
        purchaseLines: List<InvoiceLine>,
        salesLines: List<InvoiceLine>,
        mappings: List<MatchResult>,
        unitDecisions: Map<String, UnitReviewDecisionService.UnitReviewDecision>,
    ): List<NegativeInventoryRow> {
        val mappingByKey = mappings.associateBy { it.identity.key }
        val buckets = linkedMapOf<String, MovementBucket>()

        fun bucketFor(mapping: MatchResult): MovementBucket {
            val key = mapping.matchedXnt?.let { "xnt:${it.code}" } ?: "invoice:${mapping.identity.key}"
            return buckets.getOrPut(key) {
                MovementBucket(
                    productName = mapping.matchedXnt?.name ?: mapping.identity.displayName,
                    xntName = mapping.matchedXnt?.name.orEmpty(),
                    xntUnit = mapping.matchedXnt?.unit.orEmpty(),
                    openingQty = mapping.matchedXnt?.openingQty ?: 0.0,
                    sourceState = if (mapping.matchedXnt == null) "Chỉ có trên hóa đơn" else "Có trong XNT",
                )
            }
        }

        purchaseLines.forEach { line ->
            val mapping = mappingByKey[line.identity.key] ?: return@forEach
            val date = line.invoiceDate ?: return@forEach
            val unitDecision = applicableUnitDecision(mapping, unitDecisions)
            val bucket = bucketFor(mapping)
            bucket.invoiceNames += line.name
            bucket.invoiceUnits += line.unit
            bucket.matchTypes += mapping.matchType
            bucket.warnings += mapping.effectiveWarnings(unitDecision)
            bucket.reviewedReady = bucket.reviewedReady && mapping.isReviewedReady(unitDecision)
            bucket.purchaseByDate[date] = (bucket.purchaseByDate[date] ?: 0.0) + line.quantity
            bucket.purchaseAmtByDate[date] = (bucket.purchaseAmtByDate[date] ?: 0.0) + line.amount
        }

        salesLines.forEach { line ->
            val mapping = mappingByKey[line.identity.key] ?: return@forEach
            val date = line.invoiceDate ?: return@forEach
            val unitDecision = applicableUnitDecision(mapping, unitDecisions)
            val bucket = bucketFor(mapping)
            bucket.invoiceNames += line.name
            bucket.invoiceUnits += line.unit
            bucket.matchTypes += mapping.matchType
            bucket.warnings += mapping.effectiveWarnings(unitDecision)
            bucket.reviewedReady = bucket.reviewedReady && mapping.isReviewedReady(unitDecision)
            bucket.salesQtyByDate[date] = (bucket.salesQtyByDate[date] ?: 0.0) + line.quantity
            bucket.salesAmtByDate[date] = (bucket.salesAmtByDate[date] ?: 0.0) + line.amount
        }

        val rows = mutableListOf<NegativeInventoryRow>()
        buckets.values.forEach { bucket ->
            var cumulativePurchase = 0.0
            var cumulativePurchaseAmt = 0.0
            var cumulativeSales = 0.0
            var cumulativeSalesAmt = 0.0
            bucket.salesQtyByDate.keys.sorted().forEach { date ->
                cumulativePurchase += bucket.purchaseByDate[date] ?: 0.0
                cumulativePurchaseAmt += bucket.purchaseAmtByDate[date] ?: 0.0
                cumulativeSales += bucket.salesQtyByDate[date] ?: 0.0
                cumulativeSalesAmt += bucket.salesAmtByDate[date] ?: 0.0
                val runningQty = bucket.openingQty + cumulativePurchase - cumulativeSales
                if (runningQty < 0.0) {
                    rows += NegativeInventoryRow(
                        productName = bucket.productName,
                        xntName = bucket.xntName,
                        invoiceName = bucket.invoiceNames.toDisplayAlias(),
                        xntUnit = bucket.xntUnit,
                        invoiceUnit = bucket.invoiceUnits.toDisplayAlias(),
                        date = date.format(dateFormatter),
                        openingQty = formatQty(bucket.openingQty),
                        cumulativePurchaseQty = formatQty(cumulativePurchase),
                        cumulativePurchaseAmt = formatAmt(cumulativePurchaseAmt),
                        cumulativeSalesQty = formatQty(cumulativeSales),
                        cumulativeSalesAmt = formatAmt(cumulativeSalesAmt),
                        negativeQty = formatQty(runningQty),
                        sameDaySalesAmt = formatAmt(bucket.salesAmtByDate[date] ?: 0.0),
                        matchStatus = bucket.matchTypes.maxByRisk().label,
                        warning = bucket.warnings.filter { it.isNotBlank() }.joinToString(" | "),
                        sourceState = bucket.sourceState,
                        note = when {
                            !bucket.reviewedReady -> "Chưa hoàn tất rà soát"
                            bucket.sourceState == "Chỉ có trên hóa đơn" -> "Mặt hàng chỉ có trên hóa đơn"
                            else -> ""
                        },
                        reviewedReady = bucket.reviewedReady,
                    )
                }
            }
        }
        return rows.sortedWith(compareBy<NegativeInventoryRow> { parseDisplayDate(it.date) ?: LocalDate.MIN }.thenBy { it.productName.lowercase(locale) })
    }

    private fun buildMappings(
        xntItems: List<XntItem>,
        invoiceLines: List<InvoiceLine>,
        decisions: Map<String, MappingDecisionService.MappingDecision>,
    ): List<MatchResult> {
        val identities = invoiceLines.map { it.identity }.distinctBy { it.key }
        val xntFingerprints = xntItems.map { item -> item to NameFingerprint.from(item.name) }
        val exactIndex = xntFingerprints.groupBy({ it.second.strict }, { it.first })
        val codeIndex = xntItems.groupBy { normalizeCode(it.code) }
        val normalizedIndex = xntFingerprints.groupBy({ it.second.relaxedKey() }, { it.first })
        val xntByCode = xntItems.associateBy { normalizeCode(it.code) }

        return identities.map { identity ->
            val decision = decisions[identity.key]
            if (decision != null) {
                when (decision.mode) {
                    MappingDecisionService.DecisionMode.NOT_IN_XNT -> {
                        return@map MatchResult(
                            identity = identity,
                            matchedXnt = null,
                            matchType = MatchType.NOT_IN_XNT,
                            confidence = 1.0,
                            unitMismatchWarning = null,
                            warnings = if (decision.ignoreWarnings) emptyList() else listOf("Người dùng xác nhận: không có trong XNT"),
                            decisionMode = decision.mode,
                            warningIgnored = decision.ignoreWarnings,
                            matchReason = "Người dùng đã chốt không có trong XNT",
                        )
                    }

                    MappingDecisionService.DecisionMode.CONFIRMED,
                    MappingDecisionService.DecisionMode.REMAPPED,
                    -> {
                        val mapped = decision.xntCode?.let { xntByCode[normalizeCode(it)] }
                        if (mapped != null) {
                            return@map MatchResult(
                                identity = identity,
                                matchedXnt = mapped,
                                matchType = if (decision.mode == MappingDecisionService.DecisionMode.CONFIRMED) MatchType.CONFIRMED else MatchType.REMAPPED,
                                confidence = 1.0,
                                unitMismatchWarning = unitWarning(identity.unit, mapped.unit),
                                warnings = emptyList(),
                                decisionMode = decision.mode,
                                warningIgnored = decision.ignoreWarnings,
                                matchReason = if (decision.mode == MappingDecisionService.DecisionMode.CONFIRMED) {
                                    "Người dùng xác nhận ghép với ${mapped.code}"
                                } else {
                                    "Người dùng ánh xạ lại sang ${mapped.code}"
                                },
                            )
                        }
                    }
                }
            }

            val fingerprint = NameFingerprint.from(identity.displayName)
            val unitNormalized = normalizeUnit(identity.unit)
            val exactCandidates = exactIndex[fingerprint.strict].orEmpty().preferUnit(unitNormalized)
            if (exactCandidates.isNotEmpty()) {
                return@map buildMatch(
                    identity = identity,
                    matched = exactCandidates.first(),
                    type = MatchType.EXACT,
                    confidence = 1.0,
                    unitMismatchWarning = unitWarning(identity.unit, exactCandidates.first().unit),
                    warnings = emptyList(),
                )
            }

            val normalizedCandidates = normalizedIndex[fingerprint.relaxedKey()].orEmpty().preferUnit(unitNormalized)
            if (normalizedCandidates.isNotEmpty()) {
                val matched = normalizedCandidates.first()
                return@map buildMatch(
                    identity = identity,
                    matched = matched,
                    type = MatchType.NORMALIZED,
                    confidence = 0.94,
                    unitMismatchWarning = unitWarning(identity.unit, matched.unit),
                    warnings = emptyList(),
                )
            }

            val codeCandidates = identity.code.takeIf { it.isNotBlank() }?.let { codeIndex[normalizeCode(it)].orEmpty() }.orEmpty()
            val bestScored = scoreCandidates(identity, fingerprint, xntFingerprints, codeCandidates)
            if (bestScored == null || bestScored.score < 0.58) {
                return@map MatchResult(
                    identity = identity,
                    matchedXnt = null,
                    matchType = MatchType.NOT_IN_XNT,
                    confidence = 0.0,
                    unitMismatchWarning = null,
                    warnings = listOf("Không tìm thấy trong XNT"),
                    decisionMode = null,
                    warningIgnored = false,
                    matchReason = "Không tìm thấy ứng viên phù hợp trong XNT",
                )
            }

            val unitMismatchWarning = unitWarning(identity.unit, bestScored.item.unit)
            val warnings = mutableListOf<String>()
            if (bestScored.closeAlternative) {
                warnings += "Có nhiều ứng viên gần nhau, cần rà soát"
            }
            if (bestScored.usedCodeAssist) {
                warnings += "Mã hóa đơn hỗ trợ so khớp, nhưng vẫn nên kiểm tra lại tên hàng"
            }

            val matchType = when {
                bestScored.score >= 0.82 -> MatchType.HEURISTIC
                else -> MatchType.NEEDS_REVIEW
            }
            buildMatch(identity, bestScored.item, matchType, bestScored.score, unitMismatchWarning, warnings.distinct())
        }.sortedBy { it.identity.displayName.lowercase(locale) }
    }

    private fun scoreCandidates(
        identity: InvoiceIdentity,
        fingerprint: NameFingerprint,
        candidates: List<Pair<XntItem, NameFingerprint>>,
        codeCandidates: List<XntItem>,
    ): ScoredCandidate? {
        val byCode = codeCandidates.toSet()
        return candidates.asSequence()
            .map { (item, candidateFingerprint) ->
                val numericCompatible = fingerprint.numericTokens.isEmpty() || candidateFingerprint.numericTokens.isEmpty() || fingerprint.numericTokens == candidateFingerprint.numericTokens
                if (!numericCompatible) {
                    return@map null
                }

                val sharedWords = fingerprint.wordTokens.intersect(candidateFingerprint.wordTokens)
                val dice = if (fingerprint.wordTokens.isEmpty() && candidateFingerprint.wordTokens.isEmpty()) 0.0
                else (2.0 * sharedWords.size) / max(1, fingerprint.wordTokens.size + candidateFingerprint.wordTokens.size)
                val coverage = if (fingerprint.wordTokens.isEmpty()) 0.0 else sharedWords.size.toDouble() / fingerprint.wordTokens.size
                val containsBonus = if (candidateFingerprint.strict.contains(fingerprint.strict) || fingerprint.strict.contains(candidateFingerprint.strict)) 0.10 else 0.0
                val codeBonus = if (byCode.contains(item)) 0.18 else 0.0
                val unitBonus = if (normalizeUnit(identity.unit) == normalizeUnit(item.unit)) 0.04 else 0.0
                val score = min(0.99, 0.55 * dice + 0.30 * coverage + containsBonus + codeBonus + unitBonus)
                CandidateScore(item, score, codeBonus > 0.0)
            }
            .filterNotNull()
            .sortedByDescending { it.score }
            .toList()
            .let { ranked ->
                val best = ranked.firstOrNull() ?: return null
                val second = ranked.getOrNull(1)
                ScoredCandidate(
                    item = best.item,
                    score = best.score,
                    closeAlternative = second != null && (best.score - second.score) <= 0.05,
                    usedCodeAssist = best.usedCodeAssist,
                )
            }
    }

    private fun buildMatch(
        identity: InvoiceIdentity,
        matched: XntItem,
        type: MatchType,
        confidence: Double,
        unitMismatchWarning: String?,
        warnings: List<String>,
    ): MatchResult {
        return MatchResult(
            identity = identity,
            matchedXnt = matched,
            matchType = type,
            confidence = confidence,
            unitMismatchWarning = unitMismatchWarning,
            warnings = warnings.filter { it.isNotBlank() }.distinct(),
            decisionMode = null,
            warningIgnored = false,
            matchReason = when (type) {
                MatchType.EXACT -> "Tự động khớp chính xác"
                MatchType.NORMALIZED -> "Tự động khớp sau chuẩn hóa"
                MatchType.HEURISTIC -> "Tự động khớp suy luận"
                MatchType.NEEDS_REVIEW -> "Tự động gợi ý nhưng cần người dùng rà soát"
                else -> ""
            },
        )
    }

    private fun buildUnitMismatchRows(
        mappings: List<MatchResult>,
        unitDecisions: Map<String, UnitReviewDecisionService.UnitReviewDecision>,
    ): List<UnitMismatchRow> {
        return mappings.asSequence()
            .filter { it.matchedXnt != null }
            .filter { it.unitMismatchWarning != null }
            .map { match ->
                val xnt = match.matchedXnt!!
                val unitDecision = applicableUnitDecision(match, unitDecisions)
                val decisionState = when (unitDecision?.mode) {
                    UnitReviewDecisionService.DecisionMode.RESOLVED -> "Đã xử lý"
                    UnitReviewDecisionService.DecisionMode.KEEP_WARNING -> "Giữ cảnh báo"
                    null -> "Chờ rà soát"
                }
                val warningAppearsInOutput = unitDecision?.mode != UnitReviewDecisionService.DecisionMode.RESOLVED
                UnitMismatchRow(
                    mappingKey = match.identity.key,
                    xntCode = xnt.code,
                    invoiceItem = match.identity.displayName,
                    matchedItem = xnt.name,
                    invoiceUnit = match.identity.unit,
                    xntUnit = xnt.unit,
                    matchStatus = match.matchType.label,
                    severity = if (match.matchType == MatchType.NEEDS_REVIEW || match.matchType == MatchType.NOT_IN_XNT) "Cao" else "Trung bình",
                    decisionState = decisionState,
                    reason = buildUnitReason(match),
                    action = when (unitDecision?.mode) {
                        UnitReviewDecisionService.DecisionMode.RESOLVED -> "Đã rà soát • ẩn cảnh báo"
                        UnitReviewDecisionService.DecisionMode.KEEP_WARNING -> "Đã rà soát • giữ cảnh báo"
                        null -> if (match.matchType == MatchType.NEEDS_REVIEW) "Ánh xạ lại hoặc rà soát match" else "Xử lý hoặc giữ cảnh báo"
                    },
                    pendingReview = unitDecision == null,
                    warningAppearsInOutput = warningAppearsInOutput,
                )
            }
            .sortedBy { it.invoiceItem.lowercase(locale) }
            .toList()
    }

    private fun applicableUnitDecision(
        match: MatchResult,
        unitDecisions: Map<String, UnitReviewDecisionService.UnitReviewDecision>,
    ): UnitReviewDecisionService.UnitReviewDecision? {
        val decision = unitDecisions[match.identity.key] ?: return null
        val xntCode = match.matchedXnt?.code ?: return null
        return decision.takeIf { it.appliesTo(xntCode) }
    }

    private fun buildUnitReason(match: MatchResult): String {
        val reasons = mutableListOf<String>()
        match.unitMismatchWarning?.let { reasons += it }
        if (match.matchType == MatchType.NEEDS_REVIEW) {
            reasons += "Match hiện tại vẫn cần người dùng kiểm tra thêm"
        }
        reasons += match.warnings.filter { it.isNotBlank() }
        return reasons.distinct().joinToString(" | ")
    }

    private fun loadXntSheet(path: Path): XntLoadResult {
        if (!path.exists()) {
            return XntLoadResult(emptyList(), emptyList(), emptyList(), 0)
        }

        WorkbookFactory.create(path.toFile()).use { workbook ->
            val sheet = workbook.getSheetAt(0)
            val items = mutableListOf<XntItem>()
            val previewRows = mutableListOf<List<String>>()
            var warningCount = 0

            for (rowIndex in 5..sheet.lastRowNum) {
                val row = sheet.getRow(rowIndex) ?: continue
                val name = cellText(row, 1)
                val unit = cellText(row, 2)
                val code = cellText(row, 3)
                if (name.isBlank() && unit.isBlank() && code.isBlank()) continue
                val warnings = mutableListOf<String>()

                val openingQty = numericCell(row, 4)
                val openingAmt = numericCell(row, 5)
                val inboundQty = numericCell(row, 6)
                val inboundAmt = numericCell(row, 7)
                val outboundQty = numericCell(row, 8)
                val outboundAmt = numericCell(row, 9)
                val endingQty = numericCell(row, 10)
                val endingAmt = numericCell(row, 11)

                if (name.isBlank() || unit.isBlank() || code.isBlank()) {
                    if (name.isBlank()) warnings += "Thiếu tên hàng"
                    if (unit.isBlank()) warnings += "Thiếu ĐVT"
                    if (code.isBlank()) warnings += "Thiếu mã hàng"
                    warningCount += 1
                }

                items += XntItem(
                    code = code,
                    name = name,
                    unit = unit,
                    openingQty = openingQty,
                    openingAmt = openingAmt,
                    inboundQty = inboundQty,
                    inboundAmt = inboundAmt,
                    outboundQty = outboundQty,
                    outboundAmt = outboundAmt,
                    endingQty = endingQty,
                    endingAmt = endingAmt,
                )

                previewRows += listOf(
                    code,
                    name,
                    unit,
                    formatQty(openingQty),
                    formatQty(inboundQty),
                    formatQty(outboundQty),
                    formatQty(endingQty),
                    warnings.joinToString(" | "),
                )
            }

            return XntLoadResult(
                items = items,
                sheetNames = listOf(sheet.sheetName),
                previewRows = previewRows,
                warningCount = warningCount,
            )
        }
    }

    private fun loadInvoiceWorkbook(path: Path): InvoiceLoadResult {
        if (!path.exists()) {
            return InvoiceLoadResult(emptyList(), emptyList(), emptyList(), emptyList(), 0, 0, 0, emptyList())
        }

        WorkbookFactory.create(path.toFile()).use { workbook ->
            val sheetNames = (0 until workbook.numberOfSheets).map { workbook.getSheetAt(it).sheetName }
            val purchase = workbook.getSheet("MuaVao_1")
            val sales = workbook.getSheet("BanRa_1")
            val purchaseLoad = purchase?.let { loadInvoiceSheet(it, InvoiceKind.PURCHASE) } ?: InvoiceSheetLoad(emptyList(), emptyList(), 0)
            val salesLoad = sales?.let { loadInvoiceSheet(it, InvoiceKind.SALE) } ?: InvoiceSheetLoad(emptyList(), emptyList(), 0)
            return InvoiceLoadResult(
                purchaseLines = purchaseLoad.lines,
                salesLines = salesLoad.lines,
                purchasePreviewRows = purchaseLoad.previewRows,
                salesPreviewRows = salesLoad.previewRows,
                warningCount = purchaseLoad.warningCount + salesLoad.warningCount,
                purchaseWarningCount = purchaseLoad.warningCount,
                salesWarningCount = salesLoad.warningCount,
                sheetNames = sheetNames,
            )
        }
    }

    private fun loadInvoiceSheet(sheet: org.apache.poi.ss.usermodel.Sheet, kind: InvoiceKind): InvoiceSheetLoad {
        val lines = mutableListOf<InvoiceLine>()
        val previewRows = mutableListOf<List<String>>()
        var warningCount = 0

        for (rowIndex in 2..sheet.lastRowNum) {
            val row = sheet.getRow(rowIndex) ?: continue
            val code = when (kind) {
                InvoiceKind.PURCHASE -> firstNotBlank(cellText(row, 6), cellText(row, 8))
                InvoiceKind.SALE -> cellText(row, 6)
            }
            val name = cellText(row, 7)
            val unit = cellText(row, if (kind == InvoiceKind.PURCHASE) 9 else 8)
            val quantity = numericCell(row, if (kind == InvoiceKind.PURCHASE) 10 else 9)
            val amount = numericCell(row, if (kind == InvoiceKind.PURCHASE) 12 else 11)
            val dateText = cellText(row, if (kind == InvoiceKind.PURCHASE) 21 else 16)
            val invoiceDate = parseInvoiceDate(dateText) ?: parseDateCell(row.getCell(2))
            val warnings = mutableListOf<String>()

            if (name.isBlank()) warnings += "Thiếu tên hàng"
            if (unit.isBlank()) warnings += "Thiếu ĐVT"
            if (invoiceDate == null) warnings += "Thiếu ngày"
            if (quantity == 0.0 && rawCellBlank(row, if (kind == InvoiceKind.PURCHASE) 10 else 9)) warnings += "Thiếu số lượng"
            if (amount == 0.0 && rawCellBlank(row, if (kind == InvoiceKind.PURCHASE) 12 else 11)) warnings += "Thiếu thành tiền"

            if (name.isBlank() && code.isBlank()) continue
            if (warnings.isNotEmpty()) warningCount += 1

            val identity = InvoiceIdentity(
                key = listOf(normalizeName(name), normalizeUnit(unit), normalizeCode(code)).joinToString("|"),
                displayName = name.ifBlank { code },
                unit = unit,
                code = code,
            )

            lines += InvoiceLine(
                kind = kind,
                invoiceDate = invoiceDate,
                identity = identity,
                code = code,
                name = name,
                unit = unit,
                quantity = quantity,
                amount = amount,
                warnings = warnings,
            )

            previewRows += listOf(
                invoiceDate?.format(dateFormatter) ?: dateText,
                name,
                unit,
                formatQty(quantity),
                formatAmt(amount),
                warnings.joinToString(" | "),
            )
        }

        return InvoiceSheetLoad(lines, previewRows, warningCount)
    }

    private fun buildSourceSummary(path: Path, sheetNames: List<String>): SourceSummary {
        if (!path.exists()) {
            return SourceSummary(
                fileName = path.fileName.toString(),
                status = "Thiếu file",
                meta = "Không tìm thấy file",
                sheets = emptyList(),
            )
        }
        return SourceSummary(
            fileName = path.fileName.toString(),
            status = "Đã nạp",
            meta = buildMeta(path),
            sheets = sheetNames.map { name -> "$name • đã nhận diện" },
        )
    }

    private fun detectPeriod(path: Path): String? {
        if (!Files.exists(path)) return null

        WorkbookFactory.create(path.toFile()).use { workbook ->
            for (sheetIndex in 0 until workbook.numberOfSheets) {
                val sheet = workbook.getSheetAt(sheetIndex)
                for (row in sheet) {
                    for (cell in row) {
                        val text = formatter.formatCellValue(cell).trim()
                        val year = Regex("""20\d{2}""").find(text)?.value
                        if (year != null) {
                            return year
                        }
                    }
                }
            }
        }
        return null
    }

    private fun buildMeta(path: Path): String {
        val size = formatSize(path.fileSize())
        val modified = path.getLastModifiedTime()
            .toInstant()
            .atZone(ZoneId.systemDefault())
            .toLocalDateTime()
            .format(modifiedFormatter)
        return "$size • cập nhật $modified"
    }

    private fun formatSize(bytes: Long): String {
        if (bytes < 1024) return "$bytes B"
        val kb = bytes / 1024.0
        if (kb < 1024) return "${(kb * 10).roundToLong() / 10.0} KB"
        val mb = kb / 1024.0
        return "${(mb * 10).roundToLong() / 10.0} MB"
    }

    private fun estimateWorkbookSize(detailedRows: Int, negativeRows: Int, mappingRows: Int, unitRows: Int): String {
        val estimatedKb = detailedRows * 0.25 + negativeRows * 0.18 + mappingRows * 0.12 + unitRows * 0.10 + 80
        return "Dung lượng file ước tính: ${amountFormat.format(estimatedKb / 1024.0)} MB"
    }

    private fun signatureFor(path: Path): FileSignature? {
        if (!path.exists()) return null
        return FileSignature(path.toAbsolutePath().toString(), path.fileSize(), path.getLastModifiedTime().toMillis())
    }

    private fun cellText(row: Row, index: Int): String {
        val cell = row.getCell(index, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL) ?: return ""
        return formatter.formatCellValue(cell).trim()
    }

    private fun rawCellBlank(row: Row, index: Int): Boolean {
        val cell = row.getCell(index, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL) ?: return true
        return formatter.formatCellValue(cell).trim().isBlank()
    }

    private fun numericCell(row: Row, index: Int): Double {
        val cell = row.getCell(index, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL) ?: return 0.0
        return when (cell.cellType) {
            CellType.NUMERIC -> cell.numericCellValue
            CellType.STRING -> parseNumber(cell.stringCellValue)
            CellType.FORMULA -> cell.numericCellValue.takeUnless { it.isNaN() } ?: parseNumber(formatter.formatCellValue(cell))
            else -> parseNumber(formatter.formatCellValue(cell))
        }
    }

    private fun parseNumber(text: String?): Double {
        if (text == null) return 0.0
        val cleaned = text.trim().replace(",", "").replace(" ", "")
        return cleaned.toDoubleOrNull() ?: 0.0
    }

    private fun parseInvoiceDate(text: String): LocalDate? {
        if (text.isBlank()) return null
        return runCatching { LocalDate.parse(text.trim(), dateFormatter) }.getOrNull()
    }

    private fun parseDateCell(cell: Cell?): LocalDate? {
        if (cell == null) return null
        return when {
            cell.cellType == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell) -> cell.localDateTimeCellValue.toLocalDate()
            cell.cellType == CellType.NUMERIC -> runCatching { DateUtil.getLocalDateTime(cell.numericCellValue).toLocalDate() }.getOrNull()
            else -> parseInvoiceDate(formatter.formatCellValue(cell))
        }
    }

    private fun parseDisplayDate(text: String): LocalDate? = runCatching { LocalDate.parse(text, dateFormatter) }.getOrNull()

    private fun firstNotBlank(vararg values: String): String = values.firstOrNull { it.isNotBlank() }.orEmpty()

    private fun formatQty(value: Double): String = quantityFormat.format(normalizeZero(value))

    private fun formatAmt(value: Double): String = amountFormat.format(normalizeZero(value))

    private fun normalizeZero(value: Double): Double = if (value.absoluteValue < 0.0000001) 0.0 else value

    private fun normalizeCode(text: String): String = normalizeText(text).replace(" ", "")

    private fun normalizeUnit(text: String): String = normalizeText(text).replace(" ", "")

    private fun normalizeName(text: String): String = normalizeText(text)

    private fun normalizeText(text: String): String {
        val lowered = text.lowercase(locale)
            .replace('đ', 'd')
            .replace('Đ', 'd')
        val noAccent = java.text.Normalizer.normalize(lowered, java.text.Normalizer.Form.NFD)
            .replace("\\p{M}+".toRegex(), "")
        return noAccent
            .replace(',', '.')
            .replace("[^a-z0-9]+".toRegex(), " ")
            .trim()
            .replace("\\s+".toRegex(), " ")
    }

    private fun relaxedText(text: String): String {
        return normalizeText(text)
            .replace('y', 'i')
            .replace('z', 'j')
    }

    private fun unitWarning(invoiceUnit: String, xntUnit: String): String? {
        return if (invoiceUnit.isBlank() || normalizeUnit(invoiceUnit) == normalizeUnit(xntUnit)) null else "Lệch ĐVT: ${invoiceUnit} vs ${xntUnit}"
    }

    private fun List<XntItem>.preferUnit(unit: String): List<XntItem> {
        if (unit.isBlank()) return this
        return sortedWith(compareByDescending<XntItem> { normalizeUnit(it.unit) == unit }.thenBy { it.name.lowercase(locale) })
    }

    private fun Set<String>.toDisplayAlias(): String {
        if (isEmpty()) return ""
        val sorted = this.sorted()
        return when {
            sorted.size == 1 -> sorted.first()
            sorted.size == 2 -> sorted.joinToString(" | ")
            else -> sorted.take(2).joinToString(" | ") + " (+${sorted.size - 2})"
        }
    }

    private fun List<MatchType>.maxByRisk(): MatchType {
        return maxByOrNull { it.risk } ?: MatchType.EXACT
    }

    private data class CacheEntry(
        val signature: CacheSignature,
        val analysis: WorkspaceAnalysis,
    )

    private data class CacheSignature(
        val xnt: FileSignature?,
        val invoice: FileSignature?,
    )

    private data class FileSignature(
        val absolutePath: String,
        val size: Long,
        val modifiedAtMillis: Long,
    )

    private data class XntItem(
        val code: String,
        val name: String,
        val unit: String,
        val openingQty: Double,
        val openingAmt: Double,
        val inboundQty: Double,
        val inboundAmt: Double,
        val outboundQty: Double,
        val outboundAmt: Double,
        val endingQty: Double,
        val endingAmt: Double,
    )

    private data class XntLoadResult(
        val items: List<XntItem>,
        val sheetNames: List<String>,
        val previewRows: List<List<String>>,
        val warningCount: Int,
    )

    private enum class InvoiceKind { PURCHASE, SALE }

    private data class InvoiceIdentity(
        val key: String,
        val displayName: String,
        val unit: String,
        val code: String,
    )

    private data class InvoiceLine(
        val kind: InvoiceKind,
        val invoiceDate: LocalDate?,
        val identity: InvoiceIdentity,
        val code: String,
        val name: String,
        val unit: String,
        val quantity: Double,
        val amount: Double,
        val warnings: List<String>,
    )

    private data class InvoiceSheetLoad(
        val lines: List<InvoiceLine>,
        val previewRows: List<List<String>>,
        val warningCount: Int,
    )

    private data class InvoiceLoadResult(
        val purchaseLines: List<InvoiceLine>,
        val salesLines: List<InvoiceLine>,
        val purchasePreviewRows: List<List<String>>,
        val salesPreviewRows: List<List<String>>,
        val warningCount: Int,
        val purchaseWarningCount: Int,
        val salesWarningCount: Int,
        val sheetNames: List<String>,
    )

    private data class NameFingerprint(
        val strict: String,
        val relaxed: String,
        val wordTokens: Set<String>,
        val numericTokens: Set<String>,
    ) {
        fun relaxedKey(): String {
            val numeric = numericTokens.sorted().joinToString(" ")
            val words = wordTokens.sorted().joinToString(" ")
            return "$words|$numeric"
        }

        companion object {
            fun from(text: String): NameFingerprint {
                val strict = normalizeStatic(text)
                val relaxed = relaxedStatic(text)
                val tokens = relaxed.split(' ').filter { it.isNotBlank() }
                val numericTokens = tokens.filter { token -> token.any(Char::isDigit) }.toSet()
                val wordTokens = tokens.filter { token -> token.any(Char::isLetter) }.toSet()
                return NameFingerprint(strict, relaxed, wordTokens, numericTokens)
            }

            private fun normalizeStatic(text: String): String {
                val lowered = text.lowercase(Locale.US).replace('đ', 'd')
                val noAccent = java.text.Normalizer.normalize(lowered, java.text.Normalizer.Form.NFD)
                    .replace("\\p{M}+".toRegex(), "")
                return noAccent
                    .replace(',', '.')
                    .replace("[^a-z0-9]+".toRegex(), " ")
                    .trim()
                    .replace("\\s+".toRegex(), " ")
            }

            private fun relaxedStatic(text: String): String {
                return normalizeStatic(text)
                    .replace('y', 'i')
                    .replace('z', 'j')
            }
        }
    }

    private enum class MatchType(val label: String, val risk: Int) {
        CONFIRMED("Đã xác nhận", 0),
        REMAPPED("Đã ánh xạ lại", 0),
        EXACT("Khớp chính xác", 1),
        NORMALIZED("Khớp chuẩn hóa", 2),
        HEURISTIC("Khớp suy luận", 3),
        NEEDS_REVIEW("Cần rà soát", 4),
        NOT_IN_XNT("Không có trong XNT", 5),
    }

    private data class MatchResult(
        val identity: InvoiceIdentity,
        val matchedXnt: XntItem?,
        val matchType: MatchType,
        val confidence: Double,
        val unitMismatchWarning: String?,
        val warnings: List<String>,
        val decisionMode: MappingDecisionService.DecisionMode?,
        val warningIgnored: Boolean,
        val matchReason: String,
    ) {
        val pendingReview: Boolean
            get() = decisionMode == null && (matchType == MatchType.NEEDS_REVIEW || matchType == MatchType.NOT_IN_XNT)

        fun effectiveWarnings(unitDecision: UnitReviewDecisionService.UnitReviewDecision?): List<String> {
            val resolvedUnit = unitDecision?.mode == UnitReviewDecisionService.DecisionMode.RESOLVED
            return buildList {
                if (!resolvedUnit) {
                    unitMismatchWarning?.let { add(it) }
                }
                if (!warningIgnored) {
                    addAll(warnings)
                }
            }.filter { it.isNotBlank() }.distinct()
        }

        fun isReviewedReady(unitDecision: UnitReviewDecisionService.UnitReviewDecision?): Boolean {
            return !pendingReview && (unitMismatchWarning == null || unitDecision != null)
        }

        fun toUiRow(unitDecision: UnitReviewDecisionService.UnitReviewDecision?): MappingRow {
            val displayWarnings = effectiveWarnings(unitDecision)
            return MappingRow(
                mappingKey = identity.key,
                invoiceName = identity.displayName,
                xntCode = matchedXnt?.code.orEmpty(),
                xntName = matchedXnt?.name.orEmpty(),
                matchType = matchType.label,
                confidence = if (matchType == MatchType.NOT_IN_XNT) "0%" else "${percentFormat.format(confidence * 100)}%",
                invoiceUnit = identity.unit,
                xntUnit = matchedXnt?.unit.orEmpty(),
                warnings = displayWarnings.joinToString(" | "),
                decisionState = when (decisionMode) {
                    MappingDecisionService.DecisionMode.CONFIRMED -> "Người dùng xác nhận"
                    MappingDecisionService.DecisionMode.REMAPPED -> "Người dùng ánh xạ lại"
                    MappingDecisionService.DecisionMode.NOT_IN_XNT -> "Người dùng đánh dấu không có trong XNT"
                    null -> "Tự động"
                },
                matchReason = matchReason,
                warningIgnored = warningIgnored,
                pendingReview = pendingReview,
            )
        }
    }

    private data class CandidateScore(
        val item: XntItem,
        val score: Double,
        val usedCodeAssist: Boolean,
    )

    private data class ScoredCandidate(
        val item: XntItem,
        val score: Double,
        val closeAlternative: Boolean,
        val usedCodeAssist: Boolean,
    )

    private data class DetailBucket(
        val matchedXnt: XntItem?,
        val standardizedName: String,
        val xntName: String,
        val xntUnit: String,
        val invoiceNames: MutableSet<String> = linkedSetOf(),
        val invoiceUnits: MutableSet<String> = linkedSetOf(),
        val warnings: MutableList<String> = mutableListOf(),
        val statuses: MutableList<MatchType> = mutableListOf(),
        var purchaseQty: Double = 0.0,
        var purchaseAmt: Double = 0.0,
        var salesQty: Double = 0.0,
        var salesAmt: Double = 0.0,
        var reviewedReady: Boolean = true,
    )

    private data class MovementBucket(
        val productName: String,
        val xntName: String,
        val xntUnit: String,
        val openingQty: Double,
        val sourceState: String,
        val warnings: MutableSet<String> = linkedSetOf(),
        val matchTypes: MutableList<MatchType> = mutableListOf(),
        val invoiceNames: MutableSet<String> = linkedSetOf(),
        val invoiceUnits: MutableSet<String> = linkedSetOf(),
        val purchaseByDate: MutableMap<LocalDate, Double> = sortedMapOf(),
        val purchaseAmtByDate: MutableMap<LocalDate, Double> = sortedMapOf(),
        val salesQtyByDate: MutableMap<LocalDate, Double> = sortedMapOf(),
        val salesAmtByDate: MutableMap<LocalDate, Double> = sortedMapOf(),
        var reviewedReady: Boolean = true,
    )
}
