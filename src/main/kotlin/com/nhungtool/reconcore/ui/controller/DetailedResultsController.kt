package com.nhungtool.reconcore.ui.controller

import com.nhungtool.reconcore.ui.DetailedResultRow
import com.nhungtool.reconcore.ui.TableBuilders
import com.nhungtool.reconcore.ui.WorkspaceAnalysisService
import javafx.collections.FXCollections
import javafx.fxml.FXML
import javafx.scene.control.Label
import javafx.scene.control.TableView

class DetailedResultsController {
    @FXML private lateinit var resultsTable: TableView<DetailedResultRow>
    @FXML private lateinit var inspectorTitleLabel: Label
    @FXML private lateinit var inspectorVarianceLabel: Label
    @FXML private lateinit var inspectorWarningLabel: Label

    @FXML
    fun initialize() {
        addColumn("Tên chuẩn", 180.0) { it.standardizedName }
        addColumn("XNT", 160.0) { it.xntName }
        addColumn("HD", 190.0) { it.invoiceName }
        addColumn("ĐVT XNT", 80.0) { it.xntUnit }
        addColumn("ĐVT HD", 80.0) { it.invoiceUnit }
        addColumn("Match", 110.0) { it.matchStatus }
        addColumn("Warnings", 180.0) { it.warnings }
        addColumn("Tồn đầu SL", 90.0) { it.openingQty }
        addColumn("Tồn đầu tiền", 110.0) { it.openingAmt }
        addColumn("Mua vào SL", 90.0) { it.purchaseQty }
        addColumn("Mua vào tiền", 110.0) { it.purchaseAmt }
        addColumn("Nhập XNT SL", 95.0) { it.inboundQty }
        addColumn("Nhập XNT tiền", 115.0) { it.inboundAmt }
        addColumn("Chênh mua/nhập SL", 120.0) { it.purchaseInboundDiffQty }
        addColumn("Chênh mua/nhập tiền", 130.0) { it.purchaseInboundDiffAmt }
        addColumn("Bán ra SL", 90.0) { it.salesQty }
        addColumn("Bán ra tiền", 110.0) { it.salesAmt }
        addColumn("Xuất XNT SL", 95.0) { it.outboundQty }
        addColumn("Xuất XNT tiền", 115.0) { it.outboundAmt }
        addColumn("Chênh bán/xuất SL", 120.0) { it.salesOutboundDiffQty }
        addColumn("Chênh bán/xuất tiền", 130.0) { it.salesOutboundDiffAmt }
        addColumn("Tồn tính SL", 95.0) { it.calcEndingQty }
        addColumn("Tồn tính tiền", 115.0) { it.calcEndingAmt }
        addColumn("Tồn XNT SL", 95.0) { it.xntEndingQty }
        addColumn("Tồn XNT tiền", 115.0) { it.xntEndingAmt }
        addColumn("Chênh tồn SL", 105.0) { it.endingDiffQty }
        addColumn("Chênh tồn tiền", 120.0) { it.endingDiffAmt }

        resultsTable.items = FXCollections.observableArrayList(WorkspaceAnalysisService.load().detailedRows)
        resultsTable.selectionModel.selectedItemProperty().addListener { _, _, row -> row?.let { renderSelection(it) } }
        resultsTable.selectionModel.selectFirst()
    }

    private fun addColumn(title: String, width: Double, extractor: (DetailedResultRow) -> String) {
        resultsTable.columns += TableBuilders.stringColumn(title, width, extractor)
    }

    private fun renderSelection(row: DetailedResultRow) {
        inspectorTitleLabel.text = row.standardizedName
        inspectorVarianceLabel.text = "Chênh tồn: ${row.endingDiffQty} / ${row.endingDiffAmt}"
        inspectorWarningLabel.text = row.warnings.ifBlank { "Không có cảnh báo" }
    }
}
