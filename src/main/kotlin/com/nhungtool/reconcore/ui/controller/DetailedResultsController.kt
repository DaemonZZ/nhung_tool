package com.nhungtool.reconcore.ui.controller

import com.nhungtool.reconcore.ui.AppScreen
import com.nhungtool.reconcore.ui.AppUiCoordinator
import com.nhungtool.reconcore.ui.DetailedResultRow
import com.nhungtool.reconcore.ui.TableBuilders
import com.nhungtool.reconcore.ui.WorkspaceAnalysisService
import javafx.collections.FXCollections
import javafx.fxml.FXML
import javafx.scene.control.Button
import javafx.scene.control.Label
import javafx.scene.control.TableColumn
import javafx.scene.control.TableRow
import javafx.scene.control.TableView
import javafx.scene.control.TextField
import java.text.DecimalFormatSymbols
import java.text.Normalizer
import java.util.Locale
import kotlin.math.absoluteValue

class DetailedResultsController {
    private enum class ResultFilter {
        ALL,
        WARNINGS,
        PENDING,
        READY,
    }

    @FXML private lateinit var resultsTable: TableView<DetailedResultRow>
    @FXML private lateinit var searchField: TextField
    @FXML private lateinit var warningCountLabel: Label
    @FXML private lateinit var pendingCountLabel: Label
    @FXML private lateinit var readyCountLabel: Label
    @FXML private lateinit var varianceCountLabel: Label
    @FXML private lateinit var summaryLabel: Label
    @FXML private lateinit var allRowsButton: Button
    @FXML private lateinit var warningsOnlyButton: Button
    @FXML private lateinit var pendingOnlyButton: Button
    @FXML private lateinit var readyOnlyButton: Button
    @FXML private lateinit var openExportButton: Button
    @FXML private lateinit var inspectorTitleLabel: Label
    @FXML private lateinit var inspectorStatusLabel: Label
    @FXML private lateinit var inspectorWarningLabel: Label
    @FXML private lateinit var inspectorNoteLabel: Label
    @FXML private lateinit var inspectorXntLabel: Label
    @FXML private lateinit var inspectorInvoiceLabel: Label
    @FXML private lateinit var inspectorMetaLabel: Label
    @FXML private lateinit var purchaseDeltaLabel: Label
    @FXML private lateinit var salesDeltaLabel: Label
    @FXML private lateinit var inspectorVarianceLabel: Label

    private val locale = Locale.US
    private val decimalSymbols = DecimalFormatSymbols(locale)
    private var allRows: List<DetailedResultRow> = emptyList()
    private var currentFilter: ResultFilter = ResultFilter.ALL

    @FXML
    fun initialize() {
        resultsTable.columnResizePolicy = TableView.UNCONSTRAINED_RESIZE_POLICY
        setupColumns()
        setupRowStyles()
        searchField.textProperty().addListener { _, _, _ -> applyFilters() }
        resultsTable.selectionModel.selectedItemProperty().addListener { _, _, row ->
            row?.let { renderSelection(it) } ?: clearSelection()
        }
        refreshRows()
    }

    @FXML
    private fun handleShowAllRows() {
        currentFilter = ResultFilter.ALL
        applyFilters()
    }

    @FXML
    private fun handleShowWarningsOnly() {
        currentFilter = ResultFilter.WARNINGS
        applyFilters()
    }

    @FXML
    private fun handleShowPendingOnly() {
        currentFilter = ResultFilter.PENDING
        applyFilters()
    }

    @FXML
    private fun handleShowReadyOnly() {
        currentFilter = ResultFilter.READY
        applyFilters()
    }

    @FXML
    private fun handleOpenExportScreen() {
        AppUiCoordinator.requestRefresh(force = false, screen = AppScreen.EXPORT)
    }

    @FXML
    private fun handleOpenMappingReview() {
        AppUiCoordinator.requestRefresh(force = false, screen = AppScreen.MAPPING_REVIEW)
    }

    @FXML
    private fun handleOpenUnitReview() {
        AppUiCoordinator.requestRefresh(force = false, screen = AppScreen.UNIT_REVIEW)
    }

    private fun refreshRows() {
        allRows = WorkspaceAnalysisService.load().detailedRows
        applyFilters()
    }

    private fun applyFilters(preferredName: String? = null) {
        val query = normalize(searchField.text)
        val filtered = allRows.filter { row ->
            matchesFilter(row) && (
                query.isBlank() || listOf(
                    row.standardizedName,
                    row.xntName,
                    row.invoiceName,
                    row.xntUnit,
                    row.invoiceUnit,
                    row.matchStatus,
                    row.warnings,
                    row.note,
                ).any { normalize(it).contains(query) }
            )
        }
        resultsTable.items = FXCollections.observableArrayList(filtered)
        updateMetrics(filtered)
        updateFilterButtons()

        val target = preferredName?.let { name -> filtered.firstOrNull { it.standardizedName == name } } ?: filtered.firstOrNull()
        if (target != null) {
            resultsTable.selectionModel.clearSelection()
            resultsTable.selectionModel.select(target)
            renderSelection(target)
        } else {
            clearSelection()
        }
    }

    private fun matchesFilter(row: DetailedResultRow): Boolean {
        return when (currentFilter) {
            ResultFilter.ALL -> true
            ResultFilter.WARNINGS -> row.warnings.isNotBlank()
            ResultFilter.PENDING -> !row.reviewedReady
            ResultFilter.READY -> row.reviewedReady
        }
    }

    private fun updateMetrics(filteredRows: List<DetailedResultRow>) {
        val warningCount = allRows.count { it.warnings.isNotBlank() }
        val pendingCount = allRows.count { !it.reviewedReady }
        val readyCount = allRows.count { it.reviewedReady }
        val varianceCount = allRows.count { hasVariance(it) }
        warningCountLabel.text = "$warningCount cảnh báo"
        pendingCountLabel.text = "$pendingCount cần rà soát"
        readyCountLabel.text = "$readyCount đã rà soát xong"
        varianceCountLabel.text = "$varianceCount có chênh lệch"
        summaryLabel.text =
            "${filteredRows.size}/${allRows.size} dòng đang hiển thị • " +
                "${filteredRows.count { it.warnings.isNotBlank() }} cảnh báo • " +
                "${filteredRows.count { !it.reviewedReady }} cần rà soát • " +
                "${filteredRows.count { hasVariance(it) }} có chênh lệch"
    }

    private fun updateFilterButtons() {
        setButtonTone(allRowsButton, currentFilter == ResultFilter.ALL)
        setButtonTone(warningsOnlyButton, currentFilter == ResultFilter.WARNINGS)
        setButtonTone(pendingOnlyButton, currentFilter == ResultFilter.PENDING)
        setButtonTone(readyOnlyButton, currentFilter == ResultFilter.READY)
    }

    private fun setButtonTone(button: Button, active: Boolean) {
        button.styleClass.removeAll("primary-button", "secondary-button")
        button.styleClass += if (active) "primary-button" else "secondary-button"
    }

    private fun setupColumns() {
        resultsTable.columns.clear()

        val identity = groupColumn(
            "Nhận diện",
            stringColumn("Mặt hàng chuẩn", 260.0) { it.standardizedName },
            stringColumn("Mặt hàng XNT", 240.0) { it.xntName },
            stringColumn("Mặt hàng hóa đơn", 260.0) { it.invoiceName },
            stringColumn("ĐVT XNT", 90.0) { it.xntUnit },
            stringColumn("ĐVT hóa đơn", 95.0) { it.invoiceUnit },
            stringColumn("Trạng thái khớp", 130.0) { it.matchStatus },
            stringColumn("Cảnh báo", 220.0) { it.warnings },
        )

        val opening = groupColumn(
            "Đầu kỳ",
            stringColumn("Số lượng", 100.0) { it.openingQty },
            stringColumn("Thành tiền", 120.0) { it.openingAmt },
        )

        val purchaseInbound = groupColumn(
            "Mua vào so với nhập XNT",
            stringColumn("SL mua", 110.0) { it.purchaseQty },
            stringColumn("Tiền mua", 130.0) { it.purchaseAmt },
            stringColumn("SL nhập XNT", 110.0) { it.inboundQty },
            stringColumn("Tiền nhập XNT", 130.0) { it.inboundAmt },
            stringColumn("Chênh SL", 110.0) { it.purchaseInboundDiffQty },
            stringColumn("Chênh tiền", 130.0) { it.purchaseInboundDiffAmt },
        )

        val salesOutbound = groupColumn(
            "Bán ra so với xuất XNT",
            stringColumn("SL bán", 110.0) { it.salesQty },
            stringColumn("Tiền bán", 130.0) { it.salesAmt },
            stringColumn("SL xuất XNT", 110.0) { it.outboundQty },
            stringColumn("Tiền xuất XNT", 130.0) { it.outboundAmt },
            stringColumn("Chênh SL", 110.0) { it.salesOutboundDiffQty },
            stringColumn("Chênh tiền", 130.0) { it.salesOutboundDiffAmt },
        )

        val ending = groupColumn(
            "Tồn cuối",
            stringColumn("SL tính toán", 110.0) { it.calcEndingQty },
            stringColumn("Tiền tính toán", 130.0) { it.calcEndingAmt },
            stringColumn("SL XNT", 110.0) { it.xntEndingQty },
            stringColumn("Tiền XNT", 130.0) { it.xntEndingAmt },
            stringColumn("Chênh SL", 110.0) { it.endingDiffQty },
            stringColumn("Chênh tiền", 130.0) { it.endingDiffAmt },
            stringColumn("Ghi chú", 180.0) { it.note },
        )

        resultsTable.columns += listOf(identity, opening, purchaseInbound, salesOutbound, ending)
    }

    private fun stringColumn(title: String, width: Double, extractor: (DetailedResultRow) -> String): TableColumn<DetailedResultRow, String> {
        return TableBuilders.stringColumn(title, width, extractor)
    }

    private fun groupColumn(title: String, vararg children: TableColumn<DetailedResultRow, *>): TableColumn<DetailedResultRow, String> {
        return TableColumn<DetailedResultRow, String>(title).apply {
            isSortable = false
            columns.addAll(children.toList())
        }
    }

    private fun setupRowStyles() {
        resultsTable.setRowFactory {
            object : TableRow<DetailedResultRow>() {
                override fun updateItem(item: DetailedResultRow?, empty: Boolean) {
                    super.updateItem(item, empty)
                    styleClass.removeAll("detailed-row-pending", "detailed-row-warning", "detailed-row-variance", "detailed-row-ready")
                    if (empty || item == null) return
                    when {
                        !item.reviewedReady -> styleClass.add("detailed-row-pending")
                        item.warnings.isNotBlank() -> styleClass.add("detailed-row-warning")
                        hasVariance(item) -> styleClass.add("detailed-row-variance")
                        else -> styleClass.add("detailed-row-ready")
                    }
                }
            }
        }
    }

    private fun renderSelection(row: DetailedResultRow) {
        inspectorTitleLabel.text = row.standardizedName
        inspectorStatusLabel.text = buildString {
            append(row.matchStatus)
            append(" • ")
            append(if (row.reviewedReady) "Đã rà soát xong" else "Cần rà soát")
            if (row.warnings.isNotBlank()) {
                append(" • Có cảnh báo trong kết quả")
            }
        }
        inspectorWarningLabel.text = row.warnings.ifBlank { "Không có cảnh báo." }
        inspectorNoteLabel.text = row.note.ifBlank { "Không có ghi chú nghiệp vụ thêm." }
        inspectorXntLabel.text = row.xntName.ifBlank { "Không có mặt hàng XNT tương ứng." }
        inspectorInvoiceLabel.text = row.invoiceName.ifBlank { "Không có dòng hóa đơn liên quan." }
        inspectorMetaLabel.text = "XNT ${row.xntUnit.ifBlank { "-" }} • Hóa đơn ${row.invoiceUnit.ifBlank { "-" }} • ${row.matchStatus}"
        purchaseDeltaLabel.text = "Hóa đơn ${row.purchaseQty} / ${row.purchaseAmt} so với XNT ${row.inboundQty} / ${row.inboundAmt} • Chênh ${row.purchaseInboundDiffQty} / ${row.purchaseInboundDiffAmt}"
        salesDeltaLabel.text = "Hóa đơn ${row.salesQty} / ${row.salesAmt} so với XNT ${row.outboundQty} / ${row.outboundAmt} • Chênh ${row.salesOutboundDiffQty} / ${row.salesOutboundDiffAmt}"
        inspectorVarianceLabel.text = "Tính toán ${row.calcEndingQty} / ${row.calcEndingAmt} so với XNT ${row.xntEndingQty} / ${row.xntEndingAmt} • Chênh ${row.endingDiffQty} / ${row.endingDiffAmt}"
    }

    private fun clearSelection() {
        inspectorTitleLabel.text = ""
        inspectorStatusLabel.text = ""
        inspectorWarningLabel.text = ""
        inspectorNoteLabel.text = ""
        inspectorXntLabel.text = ""
        inspectorInvoiceLabel.text = ""
        inspectorMetaLabel.text = ""
        purchaseDeltaLabel.text = ""
        salesDeltaLabel.text = ""
        inspectorVarianceLabel.text = ""
    }

    private fun hasVariance(row: DetailedResultRow): Boolean {
        return listOf(
            row.purchaseInboundDiffQty,
            row.purchaseInboundDiffAmt,
            row.salesOutboundDiffQty,
            row.salesOutboundDiffAmt,
            row.endingDiffQty,
            row.endingDiffAmt,
        ).any { parseNumber(it).absoluteValue > 0.0000001 }
    }

    private fun parseNumber(value: String): Double {
        val normalized = value
            .replace(decimalSymbols.groupingSeparator.toString(), "")
            .trim()
        return normalized.toDoubleOrNull() ?: 0.0
    }

    private fun normalize(text: String): String {
        val lowered = text.lowercase(locale)
            .replace('đ', 'd')
        val noAccent = Normalizer.normalize(lowered, Normalizer.Form.NFD)
            .replace("\\p{M}+".toRegex(), "")
        return noAccent
            .replace("[^a-z0-9]+".toRegex(), " ")
            .trim()
            .replace("\\s+".toRegex(), " ")
    }
}
