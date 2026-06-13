package com.nhungtool.reconcore.ui.controller

import com.nhungtool.reconcore.ui.AppUiCoordinator
import com.nhungtool.reconcore.ui.MappingDecisionService
import com.nhungtool.reconcore.ui.MappingRow
import com.nhungtool.reconcore.ui.TableBuilders
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

class MappingReviewController {
    @FXML private lateinit var pendingCountLabel: Label
    @FXML private lateinit var searchField: TextField
    @FXML private lateinit var undoButton: Button
    @FXML private lateinit var undoHintLabel: Label
    @FXML private lateinit var mappingTable: TableView<MappingRow>
    @FXML private lateinit var invoiceNameLabel: Label
    @FXML private lateinit var xntNameLabel: Label
    @FXML private lateinit var confidenceLabel: Label
    @FXML private lateinit var decisionStateLabel: Label
    @FXML private lateinit var extractedTokensLabel: Label
    @FXML private lateinit var warningsLabel: Label
    @FXML private lateinit var matchReasonLabel: Label
    @FXML private lateinit var approveSafeRowsButton: Button
    @FXML private lateinit var bulkIgnoreWarningButton: Button
    @FXML private lateinit var bulkMarkNotInXntButton: Button
    @FXML private lateinit var searchCatalogButton: Button
    @FXML private lateinit var confirmMatchButton: Button
    @FXML private lateinit var remapButton: Button
    @FXML private lateinit var markNotInXntButton: Button
    @FXML private lateinit var ignoreWarningButton: Button
    @FXML private lateinit var resetAllReviewsButton: Button

    private val locale = Locale.US
    private var allRows: List<MappingRow> = emptyList()
    private var xntCatalog: List<XntCatalogOption> = emptyList()

    @FXML
    fun initialize() {
        mappingTable.selectionModel.selectionMode = SelectionMode.MULTIPLE
        setupColumns()
        setupRowStyles()
        searchField.textProperty().addListener { _, _, _ -> applyFilters() }
        mappingTable.selectionModel.selectedItemProperty().addListener { _, _, row ->
            row?.let { renderSelection(it) } ?: clearSelection()
        }
        refreshRows(force = false)
    }

    @FXML
    private fun handleSearchCatalog() {
        handleRemap()
    }

    @FXML
    private fun handleApproveSafeRows() {
        val targets = selectedRowsOrFiltered { row ->
            row.xntCode.isNotBlank() && row.matchType in setOf("Exact", "Normalized", "Heuristic", "Needs review")
        }
        if (targets.isEmpty()) {
            showAlert(Alert.AlertType.INFORMATION, "Không có dòng phù hợp", "Không có dòng an toàn để confirm trong tập đang chọn/lọc.")
            return
        }
        MappingDecisionService.applyBatch(
            description = "Bulk approve safe rows",
            updates = targets.associate { row ->
                row.mappingKey to MappingDecisionService.MappingDecision(
                    mode = MappingDecisionService.DecisionMode.CONFIRMED,
                    xntCode = row.xntCode,
                    ignoreWarnings = row.warningIgnored,
                )
            },
        )
        refreshRows(force = true)
    }

    @FXML
    private fun handleBulkIgnoreWarning() {
        val targets = selectedRowsOrFiltered { it.warnings.isNotBlank() || it.warningIgnored }
        if (targets.isEmpty()) {
            showAlert(Alert.AlertType.INFORMATION, "Không có warning", "Không có dòng warning để cập nhật.")
            return
        }
        MappingDecisionService.applyBatch(
            description = "Bulk ignore warning",
            updates = targets.associate { row ->
                row.mappingKey to buildDecisionForRow(row, ignoreWarnings = true)
            },
        )
        refreshRows(force = true)
    }

    @FXML
    private fun handleBulkMarkNotInXnt() {
        val targets = selectedRowsOrFiltered { it.xntCode.isBlank() || it.pendingReview }
        if (targets.isEmpty()) {
            showAlert(Alert.AlertType.INFORMATION, "Không có dòng phù hợp", "Không có dòng để đánh dấu Not in XNT.")
            return
        }
        MappingDecisionService.applyBatch(
            description = "Bulk mark not in XNT",
            updates = targets.associate { row ->
                row.mappingKey to MappingDecisionService.MappingDecision(
                    mode = MappingDecisionService.DecisionMode.NOT_IN_XNT,
                    xntCode = null,
                    ignoreWarnings = row.warningIgnored,
                )
            },
        )
        refreshRows(force = true)
    }

    @FXML
    private fun handleConfirmMatch() {
        val row = selectedRowOrAlert() ?: return
        if (row.xntCode.isBlank()) {
            showAlert(Alert.AlertType.ERROR, "Không thể confirm", "Dòng này chưa có target XNT. Hãy remap hoặc mark Not in XNT.")
            return
        }
        MappingDecisionService.confirm(row.mappingKey, row.xntCode, row.warningIgnored)
        refreshRows(force = true, preferredKey = row.mappingKey)
    }

    @FXML
    private fun handleRemap() {
        val row = selectedRowOrAlert() ?: return
        val chosen = chooseCatalogTarget(row) ?: return
        MappingDecisionService.remap(row.mappingKey, chosen.code, row.warningIgnored)
        refreshRows(force = true, preferredKey = row.mappingKey)
    }

    @FXML
    private fun handleMarkNotInXnt() {
        val row = selectedRowOrAlert() ?: return
        MappingDecisionService.markNotInXnt(row.mappingKey, row.warningIgnored)
        refreshRows(force = true, preferredKey = row.mappingKey)
    }

    @FXML
    private fun handleIgnoreWarning() {
        val row = selectedRowOrAlert() ?: return
        MappingDecisionService.setIgnoreWarnings(
            mappingKey = row.mappingKey,
            enabled = !row.warningIgnored,
            fallbackMode = fallbackModeFor(row),
            fallbackXntCode = row.xntCode.ifBlank { null },
        )
        refreshRows(force = true, preferredKey = row.mappingKey)
    }

    @FXML
    private fun handleUndoLastAction() {
        val result = MappingDecisionService.undoLastChange()
        if (result == null) {
            showAlert(Alert.AlertType.INFORMATION, "Không có thao tác để undo", "Hiện tại chưa có tác vụ mapping nào để hoàn tác.")
            return
        }
        refreshRows(force = true)
        showAlert(Alert.AlertType.INFORMATION, "Đã undo", "Đã hoàn tác: ${result.description} (${result.restoredCount} dòng).")
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

    private fun setupColumns() {
        mappingTable.columns.clear()
        mappingTable.columns += TableBuilders.stringColumn("Invoice Product", 260.0) { it.invoiceName }
        mappingTable.columns += TableBuilders.stringColumn("Matched XNT", 240.0) { it.xntName }
        mappingTable.columns += TableBuilders.stringColumn("Decision", 120.0) { it.decisionState }
        mappingTable.columns += TableBuilders.stringColumn("Match Type", 120.0) { it.matchType }
        mappingTable.columns += TableBuilders.stringColumn("Confidence", 90.0) { it.confidence }
        mappingTable.columns += TableBuilders.stringColumn("Inv Unit", 85.0) { it.invoiceUnit }
        mappingTable.columns += TableBuilders.stringColumn("XNT Unit", 85.0) { it.xntUnit }
        mappingTable.columns += TableBuilders.stringColumn("Warnings", 180.0) { it.warnings }
        mappingTable.columns += TableBuilders.stringColumn("Reason", 220.0) { it.matchReason }
    }

    private fun setupRowStyles() {
        mappingTable.setRowFactory {
            object : TableRow<MappingRow>() {
                override fun updateItem(item: MappingRow?, empty: Boolean) {
                    super.updateItem(item, empty)
                    styleClass.removeAll("mapping-row-pending", "mapping-row-manual", "mapping-row-warning")
                    if (empty || item == null) return
                    if (item.pendingReview) styleClass.add("mapping-row-pending")
                    if (item.decisionState != "Auto") styleClass.add("mapping-row-manual")
                    if (item.warnings.isNotBlank() && !item.warningIgnored) styleClass.add("mapping-row-warning")
                }
            }
        }
    }

    private fun refreshRows(force: Boolean, preferredKey: String? = null) {
        val analysis = WorkspaceAnalysisService.load(forceRefresh = force)
        allRows = analysis.mappingRows
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
                row.invoiceName,
                row.xntCode,
                row.xntName,
                row.matchType,
                row.warnings,
                row.matchReason,
                row.decisionState,
            ).any { normalize(it).contains(query) }
        }
        mappingTable.items = FXCollections.observableArrayList(filtered)
        updatePendingLabel(filtered)

        val target = preferredKey?.let { key -> filtered.firstOrNull { it.mappingKey == key } } ?: filtered.firstOrNull()
        if (target != null) {
            mappingTable.selectionModel.clearSelection()
            mappingTable.selectionModel.select(target)
            renderSelection(target)
        } else {
            clearSelection()
        }
    }

    private fun updatePendingLabel(filteredRows: List<MappingRow> = allRows) {
        val pendingAll = allRows.count { it.pendingReview }
        val pendingFiltered = filteredRows.count { it.pendingReview }
        val manual = allRows.count { it.decisionState != "Auto" }
        pendingCountLabel.text = "$pendingFiltered/$pendingAll pending • $manual manual"
    }

    private fun renderSelection(row: MappingRow) {
        invoiceNameLabel.text = row.invoiceName
        xntNameLabel.text = if (row.xntName.isBlank()) "Chưa có record XNT" else "${row.xntCode} • ${row.xntName}"
        confidenceLabel.text = "${row.matchType} • ${row.confidence}"
        decisionStateLabel.text = row.decisionState + if (row.warningIgnored) " • Warning ignored" else ""
        extractedTokensLabel.text = extractTokens(row.invoiceName)
        warningsLabel.text = when {
            row.warningIgnored && row.warnings.isBlank() -> "Warning đã được user bỏ qua"
            row.warningIgnored -> "${row.warnings} | User đã bỏ qua warning"
            row.warnings.isBlank() -> "Không có cảnh báo"
            else -> row.warnings
        }
        matchReasonLabel.text = row.matchReason
        ignoreWarningButton.text = if (row.warningIgnored) "Restore Warning" else "Ignore Warning"
        confirmMatchButton.isDisable = row.xntCode.isBlank()
    }

    private fun clearSelection() {
        invoiceNameLabel.text = ""
        xntNameLabel.text = ""
        confidenceLabel.text = ""
        decisionStateLabel.text = ""
        extractedTokensLabel.text = ""
        warningsLabel.text = ""
        matchReasonLabel.text = ""
        ignoreWarningButton.text = "Ignore Warning"
        confirmMatchButton.isDisable = true
    }

    private fun updateUndoState() {
        val description = MappingDecisionService.peekUndoDescription()
        undoButton.isDisable = description == null
        undoHintLabel.text = description?.let { "Undo available: $it" } ?: "Chưa có thao tác để undo"
    }

    private fun selectedRowOrAlert(): MappingRow? {
        val row = mappingTable.selectionModel.selectedItem
        if (row == null) {
            showAlert(Alert.AlertType.INFORMATION, "Chưa chọn dòng", "Hãy chọn một dòng trong bảng mapping trước.")
        }
        return row
    }

    private fun selectedRowsOrFiltered(predicate: (MappingRow) -> Boolean): List<MappingRow> {
        val selected = mappingTable.selectionModel.selectedItems?.filter(predicate).orEmpty()
        return if (selected.isNotEmpty()) selected else mappingTable.items.filter(predicate)
    }

    private fun chooseCatalogTarget(row: MappingRow): XntCatalogOption? {
        if (xntCatalog.isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Không có catalog XNT", "Chưa tải được catalog XNT để remap.")
            return null
        }
        val searchDialog = TextInputDialog(row.xntName.ifBlank { row.invoiceName }).apply {
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

    private fun fallbackModeFor(row: MappingRow): MappingDecisionService.DecisionMode {
        return when {
            row.decisionState == "User remapped" -> MappingDecisionService.DecisionMode.REMAPPED
            row.decisionState == "User marked not in XNT" || row.xntCode.isBlank() -> MappingDecisionService.DecisionMode.NOT_IN_XNT
            else -> MappingDecisionService.DecisionMode.CONFIRMED
        }
    }

    private fun buildDecisionForRow(
        row: MappingRow,
        ignoreWarnings: Boolean = row.warningIgnored,
    ): MappingDecisionService.MappingDecision {
        return MappingDecisionService.MappingDecision(
            mode = fallbackModeFor(row),
            xntCode = row.xntCode.ifBlank { null },
            ignoreWarnings = ignoreWarnings,
        )
    }

    private fun extractTokens(text: String): String {
        val numericTokens = Regex("""(?=[A-Za-z0-9./*-]*\d)[A-Za-z0-9./*-]+""")
            .findAll(text)
            .map { it.value }
            .distinct()
            .toList()
        return if (numericTokens.isEmpty()) {
            "Không có token số/model nổi bật, match dựa trên từ khóa tên hàng"
        } else {
            "Token số/model: ${numericTokens.joinToString(" | ")}"
        }
    }

    private fun normalize(text: String): String {
        return Normalizer.normalize(text.lowercase(locale).replace('đ', 'd'), Normalizer.Form.NFD)
            .replace("\\p{M}+".toRegex(), "")
            .replace("[^a-z0-9]+".toRegex(), " ")
            .trim()
            .replace("\\s+".toRegex(), " ")
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
