package com.nhungtool.reconcore.ui.controller

import com.nhungtool.reconcore.ui.AppUiCoordinator
import com.nhungtool.reconcore.ui.MappingDecisionService
import com.nhungtool.reconcore.ui.TableBuilders
import com.nhungtool.reconcore.ui.UnitMismatchRow
import com.nhungtool.reconcore.ui.UnitReviewDecisionService
import com.nhungtool.reconcore.ui.WorkspaceAnalysisService
import com.nhungtool.reconcore.ui.XntCatalogOption
import javafx.collections.FXCollections
import javafx.fxml.FXML
import javafx.scene.control.Alert
import javafx.scene.control.Button
import javafx.scene.control.ButtonType
import javafx.scene.control.ChoiceDialog
import javafx.scene.control.Label
import javafx.scene.control.SelectionMode
import javafx.scene.control.TableRow
import javafx.scene.control.TableView
import javafx.scene.control.TextField
import javafx.scene.control.TextInputDialog
import java.text.Normalizer
import java.util.Locale

class UnitMismatchController {
    @FXML private lateinit var pendingCountLabel: Label
    @FXML private lateinit var undoHintLabel: Label
    @FXML private lateinit var searchField: TextField
    @FXML private lateinit var resetAllReviewsButton: Button
    @FXML private lateinit var undoButton: Button
    @FXML private lateinit var bulkResolveButton: Button
    @FXML private lateinit var bulkKeepWarningButton: Button
    @FXML private lateinit var unitMismatchTable: TableView<UnitMismatchRow>
    @FXML private lateinit var selectedInvoiceLabel: Label
    @FXML private lateinit var selectedTargetLabel: Label
    @FXML private lateinit var conflictLabel: Label
    @FXML private lateinit var decisionLabel: Label
    @FXML private lateinit var resolutionLabel: Label
    @FXML private lateinit var confirmResolutionButton: Button
    @FXML private lateinit var keepWarningButton: Button
    @FXML private lateinit var remapItemButton: Button
    @FXML private lateinit var resetReviewButton: Button

    private var allRows: List<UnitMismatchRow> = emptyList()
    private var xntCatalog: List<XntCatalogOption> = emptyList()

    @FXML
    fun initialize() {
        unitMismatchTable.selectionModel.selectionMode = SelectionMode.MULTIPLE
        setupColumns()
        setupRowStyles()
        searchField.textProperty().addListener { _, _, _ -> applyFilters() }
        unitMismatchTable.selectionModel.selectedItemProperty().addListener { _, _, row ->
            row?.let { renderSelection(it) } ?: clearSelection()
        }
        refreshRows(force = false)
    }

    @FXML
    private fun handleBulkResolve() {
        val targets = selectedRowsOrFiltered { true }
        if (targets.isEmpty()) {
            showAlert(Alert.AlertType.INFORMATION, "Không có dòng phù hợp", "Không có dòng lệch ĐVT nào để confirm resolution.")
            return
        }
        UnitReviewDecisionService.applyBatch(
            description = "Bulk resolve unit review",
            updates = targets.associate { row ->
                row.mappingKey to UnitReviewDecisionService.UnitReviewDecision(UnitReviewDecisionService.DecisionMode.RESOLVED, row.xntCode)
            },
        )
        refreshRows(force = true)
    }

    @FXML
    private fun handleBulkKeepWarning() {
        val targets = selectedRowsOrFiltered { true }
        if (targets.isEmpty()) {
            showAlert(Alert.AlertType.INFORMATION, "Không có dòng phù hợp", "Không có dòng lệch ĐVT nào để giữ warning.")
            return
        }
        UnitReviewDecisionService.applyBatch(
            description = "Bulk keep unit warning",
            updates = targets.associate { row ->
                row.mappingKey to UnitReviewDecisionService.UnitReviewDecision(UnitReviewDecisionService.DecisionMode.KEEP_WARNING, row.xntCode)
            },
        )
        refreshRows(force = true)
    }

    @FXML
    private fun handleConfirmResolution() {
        val row = selectedRowOrAlert() ?: return
        UnitReviewDecisionService.resolve(row.mappingKey, row.xntCode)
        refreshRows(force = true, preferredKey = row.mappingKey)
    }

    @FXML
    private fun handleKeepWarning() {
        val row = selectedRowOrAlert() ?: return
        UnitReviewDecisionService.keepWarning(row.mappingKey, row.xntCode)
        refreshRows(force = true, preferredKey = row.mappingKey)
    }

    @FXML
    private fun handleRemapItem() {
        val row = selectedRowOrAlert() ?: return
        val chosen = chooseCatalogTarget(row) ?: return
        MappingDecisionService.remap(row.mappingKey, chosen.code)
        refreshRows(force = true, preferredKey = row.mappingKey)
    }

    @FXML
    private fun handleResetReview() {
        val row = selectedRowOrAlert() ?: return
        UnitReviewDecisionService.clear(row.mappingKey)
        refreshRows(force = true, preferredKey = row.mappingKey)
    }

    @FXML
    private fun handleResetAllReviews() {
        val confirmation = Alert(Alert.AlertType.CONFIRMATION).apply {
            title = "ReconCore"
            headerText = "Khôi phục toàn bộ review về trạng thái ban đầu?"
            contentText = "Thao tác này sẽ xóa tất cả quyết định ở Mapping Review và Unit Review đã lưu cục bộ."
            dialogPane.minHeight = 240.0
            buttonTypes.setAll(ButtonType.OK, ButtonType.CANCEL)
        }
        if (confirmation.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) return

        val mappingCount = MappingDecisionService.clearAll()
        val unitCount = UnitReviewDecisionService.clearAll()
        refreshRows(force = true)
        showAlert(
            Alert.AlertType.INFORMATION,
            "Đã khôi phục trạng thái ban đầu",
            "Đã xóa $mappingCount quyết định mapping và $unitCount quyết định unit review.",
        )
    }

    @FXML
    private fun handleUndoLastAction() {
        val result = UnitReviewDecisionService.undoLastChange()
        if (result == null) {
            showAlert(Alert.AlertType.INFORMATION, "Không có thao tác để undo", "Hiện tại chưa có tác vụ unit review nào để hoàn tác.")
            return
        }
        refreshRows(force = true)
        showAlert(Alert.AlertType.INFORMATION, "Đã undo", "Đã hoàn tác: ${result.description} (${result.restoredCount} dòng).")
    }

    private fun setupColumns() {
        unitMismatchTable.columns.clear()
        unitMismatchTable.columns += TableBuilders.stringColumn("Invoice Item", 320.0) { it.invoiceItem }
        unitMismatchTable.columns += TableBuilders.stringColumn("Matched XNT", 300.0) { row ->
            buildString {
                if (row.xntCode.isNotBlank()) {
                    append(row.xntCode)
                    append(" • ")
                }
                append(row.matchedItem)
            }
        }
        unitMismatchTable.columns += TableBuilders.stringColumn("Decision", 135.0) { it.decisionState }
        unitMismatchTable.columns += TableBuilders.stringColumn("Match", 110.0) { it.matchStatus }
        unitMismatchTable.columns += TableBuilders.stringColumn("Inv Unit", 85.0) { it.invoiceUnit }
        unitMismatchTable.columns += TableBuilders.stringColumn("XNT Unit", 85.0) { it.xntUnit }
        unitMismatchTable.columns += TableBuilders.stringColumn("Severity", 90.0) { it.severity }
        unitMismatchTable.columns += TableBuilders.stringColumn("Action", 220.0) { it.action }
        unitMismatchTable.columns += TableBuilders.stringColumn("Reason", 300.0) { it.reason }
    }

    private fun setupRowStyles() {
        unitMismatchTable.setRowFactory {
            object : TableRow<UnitMismatchRow>() {
                override fun updateItem(item: UnitMismatchRow?, empty: Boolean) {
                    super.updateItem(item, empty)
                    styleClass.removeAll("unit-row-pending", "unit-row-resolved", "unit-row-keep-warning")
                    if (empty || item == null) return
                    when {
                        item.pendingReview -> styleClass.add("unit-row-pending")
                        item.warningAppearsInOutput -> styleClass.add("unit-row-keep-warning")
                        else -> styleClass.add("unit-row-resolved")
                    }
                }
            }
        }
    }

    private fun refreshRows(force: Boolean, preferredKey: String? = null) {
        val analysis = WorkspaceAnalysisService.load(forceRefresh = force)
        allRows = analysis.unitMismatchRows
        xntCatalog = analysis.xntCatalog
        applyFilters(preferredKey)
        updatePendingLabel()
        updateUndoState()
        if (force) {
            AppUiCoordinator.requestShellRefresh(force = false)
        }
    }

    private fun applyFilters(preferredKey: String? = null) {
        val query = normalize(searchField.text)
        val filtered = allRows.filter { row ->
            query.isBlank() || listOf(
                row.invoiceItem,
                row.xntCode,
                row.matchedItem,
                row.matchStatus,
                row.invoiceUnit,
                row.xntUnit,
                row.severity,
                row.decisionState,
                row.reason,
                row.action,
            ).any { normalize(it).contains(query) }
        }
        unitMismatchTable.items = FXCollections.observableArrayList(filtered)
        updatePendingLabel(filtered)

        val target = preferredKey?.let { key -> filtered.firstOrNull { it.mappingKey == key } } ?: filtered.firstOrNull()
        if (target != null) {
            unitMismatchTable.selectionModel.clearSelection()
            unitMismatchTable.selectionModel.select(target)
            renderSelection(target)
        } else {
            clearSelection()
        }
    }

    private fun updatePendingLabel(filteredRows: List<UnitMismatchRow> = allRows) {
        val pendingAll = allRows.count { it.pendingReview }
        val pendingFiltered = filteredRows.count { it.pendingReview }
        val resolved = allRows.count { !it.pendingReview && !it.warningAppearsInOutput }
        val kept = allRows.count { !it.pendingReview && it.warningAppearsInOutput }
        pendingCountLabel.text = "$pendingFiltered/$pendingAll pending • $resolved resolved • $kept keep warning"
    }

    private fun renderSelection(row: UnitMismatchRow) {
        selectedInvoiceLabel.text = row.invoiceItem
        selectedTargetLabel.text = buildString {
            if (row.xntCode.isNotBlank()) {
                append(row.xntCode)
                append(" • ")
            }
            append(row.matchedItem)
        }
        conflictLabel.text = "${row.matchStatus} • Invoice ${row.invoiceUnit} vs XNT ${row.xntUnit} • ${row.severity}"
        decisionLabel.text = row.decisionState
        resolutionLabel.text = when {
            row.pendingReview -> "Chưa review. ${row.action}. Warning ĐVT hiện vẫn sẽ đi xuống các màn kết quả."
            row.warningAppearsInOutput -> "User đã review và quyết định giữ warning ĐVT này trong output để tiếp tục QA."
            else -> "User đã confirm cùng mặt hàng và suppress warning ĐVT khỏi output downstream."
        }
        resetReviewButton.isDisable = row.pendingReview
    }

    private fun clearSelection() {
        selectedInvoiceLabel.text = ""
        selectedTargetLabel.text = ""
        conflictLabel.text = ""
        decisionLabel.text = ""
        resolutionLabel.text = ""
        resetReviewButton.isDisable = true
    }

    private fun updateUndoState() {
        val description = UnitReviewDecisionService.peekUndoDescription()
        undoButton.isDisable = description == null
        undoHintLabel.text = description?.let { "Undo available: $it" } ?: "Chưa có thao tác để undo"
    }

    private fun selectedRowOrAlert(): UnitMismatchRow? {
        val row = unitMismatchTable.selectionModel.selectedItem
        if (row == null) {
            showAlert(Alert.AlertType.INFORMATION, "Chưa chọn dòng", "Hãy chọn một dòng lệch ĐVT trước khi thao tác.")
        }
        return row
    }

    private fun selectedRowsOrFiltered(predicate: (UnitMismatchRow) -> Boolean): List<UnitMismatchRow> {
        val selected = unitMismatchTable.selectionModel.selectedItems?.filter(predicate).orEmpty()
        return if (selected.isNotEmpty()) selected else unitMismatchTable.items.filter(predicate)
    }

    private fun chooseCatalogTarget(row: UnitMismatchRow): XntCatalogOption? {
        if (xntCatalog.isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Không có catalog XNT", "Chưa tải được catalog XNT để remap.")
            return null
        }
        val searchDialog = TextInputDialog(row.matchedItem.ifBlank { row.invoiceItem }).apply {
            title = "Search XNT Catalog"
            headerText = "Nhập từ khóa để tìm mặt hàng XNT"
            contentText = "Từ khóa:"
        }
        val query = searchDialog.showAndWait().orElse(null)?.trim().orEmpty()
        if (query.isBlank()) return null

        val normalizedQuery = normalize(query)
        val candidates = xntCatalog.filter {
            normalize(it.code).contains(normalizedQuery) || normalize(it.name).contains(normalizedQuery) || normalize(it.unit).contains(normalizedQuery)
        }.take(30)

        if (candidates.isEmpty()) {
            showAlert(Alert.AlertType.INFORMATION, "Không tìm thấy", "Không có mặt hàng XNT nào khớp với từ khóa '$query'.")
            return null
        }

        val labels = candidates.map { it.label }
        val dialog = ChoiceDialog(labels.first(), labels).apply {
            title = "Select XNT Target"
            headerText = "Chọn mặt hàng XNT để remap"
            contentText = "Candidate:"
        }
        val selectedLabel = dialog.showAndWait().orElse(null) ?: return null
        return candidates.firstOrNull { it.label == selectedLabel }
    }

    private fun normalize(text: String): String {
        val lowered = text.lowercase(Locale.US).replace('đ', 'd')
        val noAccent = Normalizer.normalize(lowered, Normalizer.Form.NFD).replace("\\p{M}+".toRegex(), "")
        return noAccent.replace("[^a-z0-9]+".toRegex(), " ").trim().replace("\\s+".toRegex(), " ")
    }

    private fun showAlert(type: Alert.AlertType, title: String, message: String) {
        Alert(type).apply {
            this.title = title
            headerText = null
            contentText = message
        }.showAndWait()
    }
}
