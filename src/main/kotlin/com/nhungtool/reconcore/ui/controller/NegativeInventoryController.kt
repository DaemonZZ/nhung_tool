package com.nhungtool.reconcore.ui.controller

import com.nhungtool.reconcore.ui.NegativeInventoryRow
import com.nhungtool.reconcore.ui.TableBuilders
import com.nhungtool.reconcore.ui.WorkspaceAnalysisService
import javafx.collections.FXCollections
import javafx.fxml.FXML
import javafx.scene.control.Label
import javafx.scene.control.TableView

class NegativeInventoryController {
    @FXML private lateinit var negativeCountLabel: Label
    @FXML private lateinit var negativeInventoryTable: TableView<NegativeInventoryRow>
    @FXML private lateinit var productLabel: Label
    @FXML private lateinit var explanationLabel: Label
    @FXML private lateinit var sourceStateLabel: Label

    @FXML
    fun initialize() {
        val rows = WorkspaceAnalysisService.load().negativeInventoryRows
        negativeCountLabel.text = "${rows.size} negative moments"

        negativeInventoryTable.columns += TableBuilders.stringColumn("Mặt hàng", 180.0) { it.productName }
        negativeInventoryTable.columns += TableBuilders.stringColumn("Ngày âm", 100.0) { it.date }
        negativeInventoryTable.columns += TableBuilders.stringColumn("Tồn đầu", 90.0) { it.openingQty }
        negativeInventoryTable.columns += TableBuilders.stringColumn("LK mua", 90.0) { it.cumulativePurchaseQty }
        negativeInventoryTable.columns += TableBuilders.stringColumn("LK bán", 90.0) { it.cumulativeSalesQty }
        negativeInventoryTable.columns += TableBuilders.stringColumn("Âm thực tế", 95.0) { it.negativeQty }
        negativeInventoryTable.columns += TableBuilders.stringColumn("GT bán trong ngày", 130.0) { it.sameDaySalesAmt }
        negativeInventoryTable.columns += TableBuilders.stringColumn("Match", 100.0) { it.matchStatus }
        negativeInventoryTable.columns += TableBuilders.stringColumn("Warning", 180.0) { it.warning }
        negativeInventoryTable.columns += TableBuilders.stringColumn("Nguồn", 110.0) { it.sourceState }
        negativeInventoryTable.items = FXCollections.observableArrayList(rows)
        negativeInventoryTable.selectionModel.selectedItemProperty().addListener { _, _, row -> row?.let { renderSelection(it) } }
        negativeInventoryTable.selectionModel.selectFirst()
    }

    private fun renderSelection(row: NegativeInventoryRow) {
        productLabel.text = row.productName
        sourceStateLabel.text = row.sourceState
        explanationLabel.text = "Tồn đầu ${row.openingQty} + lũy kế mua ${row.cumulativePurchaseQty} - lũy kế bán ${row.cumulativeSalesQty} = ${row.negativeQty}. Giả định hiện tại: mua trong ngày được cộng trước khi trừ bán cùng ngày."
    }
}
