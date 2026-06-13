package com.nhungtool.reconcore.ui.controller

import com.nhungtool.reconcore.ui.WorkspaceAnalysisService
import javafx.collections.FXCollections
import javafx.fxml.FXML
import javafx.scene.control.Label
import javafx.scene.control.ListView
import javafx.scene.control.ProgressBar
import javafx.scene.control.TextArea

class ProgressController {
    @FXML private lateinit var progressLabel: Label
    @FXML private lateinit var summaryLabel: Label
    @FXML private lateinit var stageList: ListView<String>
    @FXML private lateinit var logArea: TextArea
    @FXML private lateinit var progressBar: ProgressBar

    @FXML
    fun initialize() {
        val view = WorkspaceAnalysisService.load().progressView
        progressBar.progress = view.progress
        progressLabel.text = view.progressLabel
        summaryLabel.text = view.summaryLabel
        stageList.items = FXCollections.observableArrayList(view.stages)
        logArea.text = view.logs.joinToString("\n")
    }
}
