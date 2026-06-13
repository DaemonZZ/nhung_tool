package com.nhungtool.reconcore.ui.controller

import com.nhungtool.reconcore.ui.AppScreen
import com.nhungtool.reconcore.ui.AppUiCoordinator
import com.nhungtool.reconcore.ui.FileFormatCheck
import com.nhungtool.reconcore.ui.InputSourceCard
import com.nhungtool.reconcore.ui.InputValidationView
import com.nhungtool.reconcore.ui.TableBuilders
import com.nhungtool.reconcore.ui.WorkspaceAnalysisService
import com.nhungtool.reconcore.ui.WorkspaceInputService
import javafx.collections.FXCollections
import javafx.fxml.FXML
import javafx.scene.control.Alert
import javafx.scene.control.Button
import javafx.scene.control.CheckBox
import javafx.scene.control.Label
import javafx.scene.control.TableRow
import javafx.scene.control.TableView
import javafx.scene.layout.VBox
import javafx.stage.FileChooser
import java.nio.file.Path

class InputValidationController {
    @FXML private lateinit var xntTitleLabel: Label
    @FXML private lateinit var xntStatusLabel: Label
    @FXML private lateinit var xntMetaLabel: Label
    @FXML private lateinit var xntWarningLabel: Label
    @FXML private lateinit var invoiceTitleLabel: Label
    @FXML private lateinit var invoiceStatusLabel: Label
    @FXML private lateinit var invoiceMetaLabel: Label
    @FXML private lateinit var invoiceWarningLabel: Label
    @FXML private lateinit var validateButton: Button
    @FXML private lateinit var importValidatedButton: Button
    @FXML private lateinit var warningsOnlyCheckBox: CheckBox
    @FXML private lateinit var previewSummaryLabel: Label
    @FXML private lateinit var validationDetailBox: VBox
    @FXML private lateinit var xntTable: TableView<List<String>>
    @FXML private lateinit var muaVaoTable: TableView<List<String>>
    @FXML private lateinit var banRaTable: TableView<List<String>>

    private var xntRows: List<List<String>> = emptyList()
    private var purchaseRows: List<List<String>> = emptyList()
    private var salesRows: List<List<String>> = emptyList()
    private var currentView: InputValidationView? = null

    @FXML
    fun initialize() {
        warningsOnlyCheckBox.selectedProperty().addListener { _, _, _ -> renderPreviewTables() }
        render()
    }

    @FXML
    private fun handleSelectXntFile() {
        chooseFile("Chọn file XNT", WorkspaceInputService.snapshot().pendingXntPath)?.let {
            WorkspaceInputService.selectPendingXnt(it)
            render()
        }
    }

    @FXML
    private fun handleSelectInvoiceFile() {
        chooseFile("Chọn file hóa đơn", WorkspaceInputService.snapshot().pendingInvoicePath)?.let {
            WorkspaceInputService.selectPendingInvoice(it)
            render()
        }
    }

    @FXML
    private fun handleValidatePendingFiles() {
        val validation = WorkspaceInputService.validatePendingSelection()
        render()
        showAlert(
            if (validation.readyToImport) Alert.AlertType.INFORMATION else Alert.AlertType.ERROR,
            if (validation.readyToImport) "Format hợp lệ" else "Format chưa hợp lệ",
            validation.lines.joinToString("\n"),
        )
    }

    @FXML
    private fun handleImportValidatedFiles() {
        val imported = WorkspaceInputService.importValidatedSelection()
        if (!imported) {
            val validation = WorkspaceInputService.snapshot().lastValidation ?: WorkspaceInputService.validatePendingSelection()
            render()
            showAlert(Alert.AlertType.ERROR, "Chưa thể nạp file", validation.lines.joinToString("\n"))
            return
        }
        AppUiCoordinator.requestRefresh(force = true, screen = AppScreen.INPUT_VALIDATION)
        showAlert(Alert.AlertType.INFORMATION, "Đã nạp file", "Dữ liệu nguồn đã được cập nhật vào workspace.")
    }

    private fun render() {
        val snapshot = WorkspaceInputService.snapshot()
        val view = WorkspaceAnalysisService.load()
        val validation = snapshot.lastValidation
        currentView = view.inputValidationView

        renderCard(
            titleLabel = xntTitleLabel,
            statusLabel = xntStatusLabel,
            metaLabel = xntMetaLabel,
            warningLabel = xntWarningLabel,
            pendingPath = snapshot.pendingXntPath,
            activePath = snapshot.activeXntPath,
            check = validation?.xntCheck,
            activeCard = view.inputValidationView.xntCard,
        )
        renderCard(
            titleLabel = invoiceTitleLabel,
            statusLabel = invoiceStatusLabel,
            metaLabel = invoiceMetaLabel,
            warningLabel = invoiceWarningLabel,
            pendingPath = snapshot.pendingInvoicePath,
            activePath = snapshot.activeInvoicePath,
            check = validation?.invoiceCheck,
            activeCard = view.inputValidationView.invoiceCard,
        )

        validationDetailBox.children.setAll(
            ((validation?.lines ?: defaultInstructionLines()) + view.inputValidationView.validationLines)
                .distinct()
                .map { line -> buildValidationLabel(line) }
        )

        importValidatedButton.isDisable = validation?.readyToImport != true
        validateButton.isDisable = false

        xntRows = view.inputValidationView.xntRows
        purchaseRows = view.inputValidationView.purchaseRows
        salesRows = view.inputValidationView.salesRows
        renderPreviewTables()
    }

    private fun renderCard(
        titleLabel: Label,
        statusLabel: Label,
        metaLabel: Label,
        warningLabel: Label,
        pendingPath: Path,
        activePath: Path,
        check: FileFormatCheck?,
        activeCard: InputSourceCard,
    ) {
        val sameAsActive = pendingPath.toAbsolutePath().normalize() == activePath.toAbsolutePath().normalize()
        val pendingStatus = when {
            check == null && sameAsActive -> "Pending = Active • Chưa kiểm tra lại format"
            check == null -> "Pending chờ kiểm tra format"
            check.ready -> "Pending format: hợp lệ"
            else -> "Pending format: chưa hợp lệ"
        }
        titleLabel.text = if (sameAsActive) activeCard.title else pendingPath.fileName.toString()
        statusLabel.text = "${activeCard.statusLine} • $pendingStatus"
        metaLabel.text = buildList {
            add("Active: ${activeCard.metaLine}")
            add("Pending: ${WorkspaceInputService.metaFor(pendingPath)}")
            check?.infos?.forEach { add("Pending info: $it") }
        }.joinToString("\n")
        warningLabel.text = buildList {
            add("Active: ${activeCard.warningLine}")
            add("Pending: ${check?.summary ?: "Chưa kiểm tra format cho file đang chọn"}")
            check?.warnings?.forEach { add("Pending warning: $it") }
            check?.errors?.forEach { add("Pending error: $it") }
        }.distinct().joinToString("\n")
    }

    private fun defaultInstructionLines(): List<String> {
        return listOf(
            "Info: Chọn file XNT và file hóa đơn trước khi nạp",
            "Info: Bấm 'Kiểm tra format' để kiểm tra sheet và header bắt buộc",
            "Info: Chỉ khi format hợp lệ mới bật nút 'Nạp file đã kiểm tra'",
            "Info: Có thể bật filter để chỉ xem các dòng có warning",
        )
    }

    private fun buildValidationLabel(line: String): Label {
        return Label(line).apply {
            isWrapText = true
            when {
                line.startsWith("Error:") -> styleClass.add("status-danger")
                line.startsWith("Warning:") -> styleClass.add("status-warn")
                line.startsWith("OK:") -> styleClass.add("status-ok")
                else -> styleClass.add("muted-label")
            }
        }
    }

    private fun chooseFile(title: String, initialPath: Path): Path? {
        val chooser = FileChooser().apply {
            this.title = title
            extensionFilters += FileChooser.ExtensionFilter("Excel files", "*.xlsx", "*.xls")
            initialPath.parent?.toFile()?.takeIf { it.exists() }?.let { initialDirectory = it }
            initialFileName = initialPath.fileName.toString()
        }
        val window = xntTable.scene?.window ?: return null
        return chooser.showOpenDialog(window)?.toPath()
    }

    private fun showAlert(type: Alert.AlertType, header: String, content: String) {
        Alert(type).apply {
            title = "ReconCore"
            this.headerText = header
            contentText = content
            dialogPane.minHeight = 320.0
        }.showAndWait()
    }

    private fun setupTable(table: TableView<List<String>>, headers: List<String>, rows: List<List<String>>) {
        table.columns.clear()
        headers.forEachIndexed { index, header ->
            table.columns += TableBuilders.stringColumn<List<String>>(header, 160.0) { row -> row.getOrElse(index) { "" } }
        }
        table.setRowFactory {
            object : TableRow<List<String>>() {
                override fun updateItem(item: List<String>?, empty: Boolean) {
                    super.updateItem(item, empty)
                    styleClass.remove("warning-table-row")
                    if (!empty && item != null && hasWarning(item)) {
                        styleClass.add("warning-table-row")
                    }
                }
            }
        }
        table.items = FXCollections.observableArrayList(rows)
    }

    private fun renderPreviewTables() {
        val filteredXnt = filteredRows(xntRows)
        val filteredPurchase = filteredRows(purchaseRows)
        val filteredSales = filteredRows(salesRows)

        previewSummaryLabel.text = buildSummary(filteredXnt, filteredPurchase, filteredSales)

        setupTable(
            xntTable,
            listOf("Mã hàng", "Tên hàng", "ĐVT", "Tồn đầu", "Nhập", "Xuất", "Tồn cuối", "Cảnh báo"),
            filteredXnt,
        )
        setupTable(
            muaVaoTable,
            listOf("Ngày", "Tên hàng", "ĐVT", "Số lượng", "Thành tiền", "Cảnh báo"),
            filteredPurchase,
        )
        setupTable(
            banRaTable,
            listOf("Ngày", "Tên hàng", "ĐVT", "Số lượng", "Thành tiền", "Cảnh báo"),
            filteredSales,
        )
    }

    private fun filteredRows(rows: List<List<String>>): List<List<String>> {
        return if (!warningsOnlyCheckBox.isSelected) rows else rows.filter(::hasWarning)
    }

    private fun hasWarning(row: List<String>): Boolean {
        return row.lastOrNull().orEmpty().isNotBlank()
    }

    private fun buildSummary(
        filteredXnt: List<List<String>>,
        filteredPurchase: List<List<String>>,
        filteredSales: List<List<String>>,
    ): String {
        val view = currentView
        val xntWarnings = view?.xntWarningRows ?: xntRows.count(::hasWarning)
        val purchaseWarnings = view?.purchaseWarningRows ?: purchaseRows.count(::hasWarning)
        val salesWarnings = view?.salesWarningRows ?: salesRows.count(::hasWarning)
        val xntTotalRows = view?.xntTotalRows ?: xntRows.size
        val purchaseTotalRows = view?.purchaseTotalRows ?: purchaseRows.size
        val salesTotalRows = view?.salesTotalRows ?: salesRows.size
        val scopeLabel = if (warningsOnlyCheckBox.isSelected) {
            "Đang lọc chỉ hiện dòng warning"
        } else {
            "Đang hiển thị toàn bộ dữ liệu"
        }
        return "$scopeLabel • XNT ${filteredXnt.size}/$xntTotalRows dòng, warning $xntWarnings • MuaVao ${filteredPurchase.size}/$purchaseTotalRows dòng, warning $purchaseWarnings • BanRa ${filteredSales.size}/$salesTotalRows dòng, warning $salesWarnings"
    }
}
