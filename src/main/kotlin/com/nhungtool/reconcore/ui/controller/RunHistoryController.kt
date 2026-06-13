package com.nhungtool.reconcore.ui.controller

import com.nhungtool.reconcore.ui.RunHistoryRow
import com.nhungtool.reconcore.ui.WorkspaceAnalysisService
import javafx.collections.FXCollections
import javafx.fxml.FXML
import javafx.scene.control.Label
import javafx.scene.control.ListView
import javafx.scene.control.TextArea

class RunHistoryController {
    @FXML private lateinit var runList: ListView<RunHistoryRow>
    @FXML private lateinit var runIdLabel: Label
    @FXML private lateinit var runSummaryLabel: Label
    @FXML private lateinit var runStatusLabel: Label
    @FXML private lateinit var runLogArea: TextArea

    @FXML
    fun initialize() {
        val view = WorkspaceAnalysisService.load().historyView
        runList.items = FXCollections.observableArrayList(view.runs)
        runList.setCellFactory {
            object : javafx.scene.control.ListCell<RunHistoryRow>() {
                override fun updateItem(item: RunHistoryRow?, empty: Boolean) {
                    super.updateItem(item, empty)
                    text = if (empty || item == null) null else "${item.runId} • ${item.status} • ${item.startedAt}"
                }
            }
        }
        runList.selectionModel.selectedItemProperty().addListener { _, _, row -> row?.let { renderSelection(it, view.logsByRun[it.runId].orEmpty()) } }
        runList.selectionModel.selectFirst()
    }

    private fun renderSelection(row: RunHistoryRow, logs: List<String>) {
        runIdLabel.text = row.runId
        runStatusLabel.text = row.status
        runSummaryLabel.text = row.summary
        runLogArea.text = logs.joinToString("\n")
    }
}
