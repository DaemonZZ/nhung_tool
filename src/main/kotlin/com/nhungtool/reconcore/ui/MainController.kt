package com.nhungtool.reconcore.ui

import javafx.fxml.FXML
import javafx.fxml.FXMLLoader
import javafx.scene.Node
import javafx.scene.control.Button
import javafx.scene.control.Label
import javafx.scene.layout.StackPane

class MainController {
    @FXML private lateinit var contentPane: StackPane
    @FXML private lateinit var currentScreenLabel: Label
    @FXML private lateinit var periodLabel: Label
    @FXML private lateinit var warningCountLabel: Label
    @FXML private lateinit var lastRunLabel: Label
    @FXML private lateinit var dashboardButton: Button
    @FXML private lateinit var inputValidationButton: Button
    @FXML private lateinit var configButton: Button
    @FXML private lateinit var progressButton: Button
    @FXML private lateinit var mappingReviewButton: Button
    @FXML private lateinit var unitReviewButton: Button
    @FXML private lateinit var detailedResultsButton: Button
    @FXML private lateinit var negativeInventoryButton: Button
    @FXML private lateinit var exportButton: Button
    @FXML private lateinit var historyButton: Button
    @FXML private lateinit var settingsButton: Button
    private var currentScreen: AppScreen = AppScreen.DASHBOARD

    private val buttonMap by lazy {
        mapOf(
            AppScreen.DASHBOARD to dashboardButton,
            AppScreen.INPUT_VALIDATION to inputValidationButton,
            AppScreen.CONFIGURATION to configButton,
            AppScreen.PROGRESS to progressButton,
            AppScreen.MAPPING_REVIEW to mappingReviewButton,
            AppScreen.UNIT_REVIEW to unitReviewButton,
            AppScreen.DETAILED_RESULTS to detailedResultsButton,
            AppScreen.NEGATIVE_INVENTORY to negativeInventoryButton,
            AppScreen.EXPORT to exportButton,
            AppScreen.HISTORY to historyButton,
            AppScreen.SETTINGS to settingsButton,
        )
    }

    @FXML
    fun initialize() {
        AppUiCoordinator.registerRefreshAction { force, screen ->
            refreshWorkspace(force)
            openScreen(screen ?: currentScreen)
        }
        AppUiCoordinator.registerShellRefreshAction { force ->
            refreshWorkspace(force)
        }
        refreshWorkspace(false)

        buttonMap.forEach { (screen, button) ->
            button.setOnAction { openScreen(screen) }
        }

        openScreen(AppScreen.DASHBOARD)
    }

    @FXML
    private fun handleRunReconciliation() {
        refreshWorkspace(true)
        openScreen(AppScreen.PROGRESS)
    }

    private fun refreshWorkspace(force: Boolean) {
        val analysis = WorkspaceAnalysisService.load(force)
        val summary = analysis.dashboardSummary
        val pendingUnit = analysis.unitMismatchRows.count { it.pendingReview }
        val pendingMapping = analysis.mappingRows.count { it.pendingReview }
        val totalWarnings = summary.validationMetric.value + " • Unit pending $pendingUnit • Review $pendingMapping"
        periodLabel.text = "Period: ${summary.periodLabel}"
        warningCountLabel.text = totalWarnings
        lastRunLabel.text = "Refreshed: ${analysis.generatedAtLabel}"
    }

    private fun openScreen(screen: AppScreen) {
        currentScreen = screen
        currentScreenLabel.text = screen.title
        contentPane.children.setAll(loadNode(screen.fxmlPath))
        buttonMap.forEach { (candidate, button) ->
            button.styleClass.remove("nav-button-active")
            if (candidate == screen) {
                button.styleClass.add("nav-button-active")
            }
        }
    }

    private fun loadNode(path: String): Node {
        return FXMLLoader.load<Node>(javaClass.getResource(path))
    }
}
