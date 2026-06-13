package com.nhungtool.reconcore.ui.controller

import com.nhungtool.reconcore.ui.AppScreen
import com.nhungtool.reconcore.ui.AppUiCoordinator
import com.nhungtool.reconcore.ui.NegativeInventoryRow
import com.nhungtool.reconcore.ui.TableBuilders
import com.nhungtool.reconcore.ui.WorkspaceAnalysisService
import javafx.collections.FXCollections
import javafx.fxml.FXML
import javafx.scene.control.Button
import javafx.scene.control.Label
import javafx.scene.control.TableRow
import javafx.scene.control.TableView
import javafx.scene.control.TextField
import java.text.Normalizer
import java.util.Locale

class NegativeInventoryController {
    private enum class ResultFilter {
        ALL,
        WARNINGS,
        PENDING,
        INVOICE_ONLY,
        READY,
    }

    @FXML private lateinit var negativeCountLabel: Label
    @FXML private lateinit var warningCountLabel: Label
    @FXML private lateinit var pendingCountLabel: Label
    @FXML private lateinit var invoiceOnlyCountLabel: Label
    @FXML private lateinit var summaryLabel: Label
    @FXML private lateinit var searchField: TextField
    @FXML private lateinit var allRowsButton: Button
    @FXML private lateinit var warningsOnlyButton: Button
    @FXML private lateinit var pendingOnlyButton: Button
    @FXML private lateinit var invoiceOnlyButton: Button
    @FXML private lateinit var readyOnlyButton: Button
    @FXML private lateinit var negativeInventoryTable: TableView<NegativeInventoryRow>
    @FXML private lateinit var productLabel: Label
    @FXML private lateinit var sourceStateLabel: Label
    @FXML private lateinit var reviewStateLabel: Label
    @FXML private lateinit var warningLabel: Label
    @FXML private lateinit var noteLabel: Label
    @FXML private lateinit var xntLabel: Label
    @FXML private lateinit var invoiceLabel: Label
    @FXML private lateinit var metaLabel: Label
    @FXML private lateinit var eventLabel: Label
    @FXML private lateinit var explanationLabel: Label

    private val locale = Locale.US
    private var allRows: List<NegativeInventoryRow> = emptyList()
    private var currentFilter: ResultFilter = ResultFilter.ALL

    @FXML
    fun initialize() {
        resultsSetup()
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
    private fun handleShowInvoiceOnly() {
        currentFilter = ResultFilter.INVOICE_ONLY
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

    private fun resultsSetup() {
        negativeInventoryTable.columnResizePolicy = TableView.UNCONSTRAINED_RESIZE_POLICY
        setupColumns()
        setupRowStyles()
        searchField.textProperty().addListener { _, _, _ -> applyFilters() }
        negativeInventoryTable.selectionModel.selectedItemProperty().addListener { _, _, row ->
            row?.let { renderSelection(it) } ?: clearSelection()
        }
    }

    private fun refreshRows() {
        allRows = WorkspaceAnalysisService.load().negativeInventoryRows
        applyFilters()
    }

    private fun applyFilters(preferredKey: String? = null) {
        val query = normalize(searchField.text)
        val filtered = allRows.filter { row ->
            matchesFilter(row) && (
                query.isBlank() || listOf(
                    row.productName,
                    row.xntName,
                    row.invoiceName,
                    row.date,
                    row.matchStatus,
                    row.warning,
                    row.sourceState,
                    row.note,
                ).any { normalize(it).contains(query) }
            )
        }
        negativeInventoryTable.items = FXCollections.observableArrayList(filtered)
        updateMetrics(filtered)
        updateFilterButtons()

        val target = preferredKey?.let { key -> filtered.firstOrNull { rowKey(it) == key } } ?: filtered.firstOrNull()
        if (target != null) {
            negativeInventoryTable.selectionModel.clearSelection()
            negativeInventoryTable.selectionModel.select(target)
            renderSelection(target)
        } else {
            clearSelection()
        }
    }

    private fun matchesFilter(row: NegativeInventoryRow): Boolean {
        return when (currentFilter) {
            ResultFilter.ALL -> true
            ResultFilter.WARNINGS -> row.warning.isNotBlank()
            ResultFilter.PENDING -> !row.reviewedReady
            ResultFilter.INVOICE_ONLY -> row.sourceState == "Chỉ có trên hóa đơn"
            ResultFilter.READY -> row.reviewedReady
        }
    }

    private fun updateMetrics(filteredRows: List<NegativeInventoryRow>) {
        val warningCount = allRows.count { it.warning.isNotBlank() }
        val pendingCount = allRows.count { !it.reviewedReady }
        val invoiceOnlyCount = allRows.count { it.sourceState == "Chỉ có trên hóa đơn" }
        negativeCountLabel.text = "${allRows.size} sự kiện âm kho"
        warningCountLabel.text = "$warningCount cảnh báo"
        pendingCountLabel.text = "$pendingCount cần rà soát"
        invoiceOnlyCountLabel.text = "$invoiceOnlyCount chỉ có trên hóa đơn"
        summaryLabel.text =
            "${filteredRows.size}/${allRows.size} sự kiện đang hiển thị • " +
                "${filteredRows.count { it.warning.isNotBlank() }} cảnh báo • " +
                "${filteredRows.count { !it.reviewedReady }} cần rà soát • " +
                "${filteredRows.count { it.sourceState == "Chỉ có trên hóa đơn" }} chỉ có trên hóa đơn"
    }

    private fun updateFilterButtons() {
        setButtonTone(allRowsButton, currentFilter == ResultFilter.ALL)
        setButtonTone(warningsOnlyButton, currentFilter == ResultFilter.WARNINGS)
        setButtonTone(pendingOnlyButton, currentFilter == ResultFilter.PENDING)
        setButtonTone(invoiceOnlyButton, currentFilter == ResultFilter.INVOICE_ONLY)
        setButtonTone(readyOnlyButton, currentFilter == ResultFilter.READY)
    }

    private fun setButtonTone(button: Button, active: Boolean) {
        button.styleClass.removeAll("primary-button", "secondary-button")
        button.styleClass += if (active) "primary-button" else "secondary-button"
    }

    private fun setupColumns() {
        negativeInventoryTable.columns.clear()
        negativeInventoryTable.columns += TableBuilders.stringColumn("Mặt hàng chuẩn", 260.0) { it.productName }
        negativeInventoryTable.columns += TableBuilders.stringColumn("Mặt hàng XNT", 220.0) { it.xntName }
        negativeInventoryTable.columns += TableBuilders.stringColumn("Mặt hàng hóa đơn", 240.0) { it.invoiceName }
        negativeInventoryTable.columns += TableBuilders.stringColumn("Ngày", 110.0) { it.date }
        negativeInventoryTable.columns += TableBuilders.stringColumn("Tồn đầu", 110.0) { it.openingQty }
        negativeInventoryTable.columns += TableBuilders.stringColumn("Lũy kế mua SL", 130.0) { it.cumulativePurchaseQty }
        negativeInventoryTable.columns += TableBuilders.stringColumn("Lũy kế mua tiền", 140.0) { it.cumulativePurchaseAmt }
        negativeInventoryTable.columns += TableBuilders.stringColumn("Lũy kế bán SL", 120.0) { it.cumulativeSalesQty }
        negativeInventoryTable.columns += TableBuilders.stringColumn("Lũy kế bán tiền", 130.0) { it.cumulativeSalesAmt }
        negativeInventoryTable.columns += TableBuilders.stringColumn("SL âm kho", 110.0) { it.negativeQty }
        negativeInventoryTable.columns += TableBuilders.stringColumn("Tiền bán trong ngày", 150.0) { it.sameDaySalesAmt }
        negativeInventoryTable.columns += TableBuilders.stringColumn("Trạng thái khớp", 130.0) { it.matchStatus }
        negativeInventoryTable.columns += TableBuilders.stringColumn("Cảnh báo", 220.0) { it.warning }
        negativeInventoryTable.columns += TableBuilders.stringColumn("Trạng thái nguồn", 140.0) { it.sourceState }
        negativeInventoryTable.columns += TableBuilders.stringColumn("Ghi chú", 180.0) { it.note }
    }

    private fun setupRowStyles() {
        negativeInventoryTable.setRowFactory {
            object : TableRow<NegativeInventoryRow>() {
                override fun updateItem(item: NegativeInventoryRow?, empty: Boolean) {
                    super.updateItem(item, empty)
                    styleClass.removeAll("negative-row-pending", "negative-row-warning", "negative-row-invoice-only", "negative-row-ready")
                    if (empty || item == null) return
                    when {
                        !item.reviewedReady -> styleClass.add("negative-row-pending")
                        item.warning.isNotBlank() -> styleClass.add("negative-row-warning")
                        item.sourceState == "Chỉ có trên hóa đơn" -> styleClass.add("negative-row-invoice-only")
                        else -> styleClass.add("negative-row-ready")
                    }
                }
            }
        }
    }

    private fun renderSelection(row: NegativeInventoryRow) {
        productLabel.text = row.productName
        sourceStateLabel.text = row.sourceState
        reviewStateLabel.text = buildString {
            append(row.matchStatus)
            append(" • ")
            append(if (row.reviewedReady) "Đã rà soát xong" else "Cần rà soát")
            if (row.warning.isNotBlank()) {
                append(" • Có cảnh báo trong kết quả")
            }
        }
        warningLabel.text = row.warning.ifBlank { "Không có cảnh báo." }
        noteLabel.text = row.note.ifBlank { "Không có ghi chú nghiệp vụ thêm." }
        xntLabel.text = row.xntName.ifBlank { "Không có mặt hàng XNT tương ứng." }
        invoiceLabel.text = row.invoiceName.ifBlank { "Không có dòng hóa đơn liên quan." }
        metaLabel.text = "XNT ${row.xntUnit.ifBlank { "-" }} • Hóa đơn ${row.invoiceUnit.ifBlank { "-" }} • ${row.matchStatus}"
        eventLabel.text = "Ngày ${row.date} • Tồn đầu ${row.openingQty} • Tiền bán trong ngày ${row.sameDaySalesAmt}"
        explanationLabel.text =
            "Tồn đầu ${row.openingQty} + lũy kế mua ${row.cumulativePurchaseQty} " +
                "(${row.cumulativePurchaseAmt}) - lũy kế bán ${row.cumulativeSalesQty} " +
                "(${row.cumulativeSalesAmt}) = ${row.negativeQty}. " +
                "Sự kiện này cho thấy tồn chạy đã xuống dưới 0 vào ngày ${row.date}."
    }

    private fun clearSelection() {
        productLabel.text = ""
        sourceStateLabel.text = ""
        reviewStateLabel.text = ""
        warningLabel.text = ""
        noteLabel.text = ""
        xntLabel.text = ""
        invoiceLabel.text = ""
        metaLabel.text = ""
        eventLabel.text = ""
        explanationLabel.text = ""
    }

    private fun rowKey(row: NegativeInventoryRow): String {
        return listOf(row.productName, row.date, row.negativeQty, row.sourceState).joinToString("|")
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
