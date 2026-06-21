package com.nhungtool.reconcore.ui

import javafx.concurrent.Task
import javafx.fxml.FXML
import javafx.fxml.FXMLLoader
import javafx.scene.Node
import javafx.scene.control.Alert
import javafx.scene.control.Button
import javafx.scene.control.Label
import javafx.scene.layout.StackPane
import javafx.scene.layout.VBox

class MainController {
    @FXML private lateinit var contentPane: StackPane
    @FXML private lateinit var loadingOverlay: VBox
    @FXML private lateinit var loadingMessageLabel: Label
    @FXML private lateinit var currentScreenLabel: Label
    @FXML private lateinit var periodLabel: Label
    @FXML private lateinit var systemStatusLabel: Label
    @FXML private lateinit var warningCountLabel: Label
    @FXML private lateinit var lastRunLabel: Label
    @FXML private lateinit var runReconciliationButton: Button
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
    private var activeRefreshTask: Task<WorkspaceAnalysis>? = null
    private var pendingRefreshRequest: RefreshRequest? = null

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

    private data class RefreshRequest(
        val force: Boolean,
        val screen: AppScreen?,
    )

    @FXML
    fun initialize() {
        AppUiCoordinator.registerRefreshAction { force, screen ->
            requestRefresh(force, screen)
        }
        AppUiCoordinator.registerShellRefreshAction { force ->
            requestRefresh(force, null)
        }

        buttonMap.forEach { (screen, button) ->
            button.setOnAction { openScreen(screen) }
        }

        requestRefresh(force = false, screen = AppScreen.DASHBOARD)
    }

    @FXML
    private fun handleRunReconciliation() {
        requestRefresh(force = true, screen = AppScreen.PROGRESS)
    }

    private fun requestRefresh(force: Boolean, screen: AppScreen?) {
        val cached = if (!force) WorkspaceAnalysisService.peekCached() else null
        if (cached != null && activeRefreshTask == null) {
            applyAnalysis(cached)
            if (screen != null) {
                openScreen(screen)
            }
            return
        }

        val request = RefreshRequest(force, screen)
        if (activeRefreshTask != null) {
            pendingRefreshRequest = mergeRequests(pendingRefreshRequest, request)
            return
        }
        startRefresh(request)
    }

    private fun startRefresh(request: RefreshRequest) {
        setBusy(true, request)
        val task = object : Task<WorkspaceAnalysis>() {
            override fun call(): WorkspaceAnalysis = WorkspaceAnalysisService.load(request.force)
        }
        activeRefreshTask = task
        task.setOnSucceeded {
            activeRefreshTask = null
            applyAnalysis(task.value)
            setBusy(false, null)
            openScreen(request.screen ?: currentScreen)
            drainPendingRefresh()
        }
        task.setOnFailed {
            activeRefreshTask = null
            setBusy(false, null)
            val message = task.exception?.message ?: "Không xác định"
            showError("Không thể tải dữ liệu", "Không thể làm mới dữ liệu workspace: $message")
            drainPendingRefresh()
        }
        Thread(task, "workspace-refresh").apply {
            isDaemon = true
            start()
        }
    }

    private fun drainPendingRefresh() {
        val next = pendingRefreshRequest ?: return
        pendingRefreshRequest = null
        requestRefresh(next.force, next.screen)
    }

    private fun mergeRequests(existing: RefreshRequest?, incoming: RefreshRequest): RefreshRequest {
        if (existing == null) return incoming
        return RefreshRequest(
            force = existing.force || incoming.force,
            screen = incoming.screen ?: existing.screen,
        )
    }

    private fun applyAnalysis(analysis: WorkspaceAnalysis) {
        val summary = analysis.dashboardSummary
        val pendingUnit = analysis.unitMismatchRows.count { it.pendingReview }
        val pendingMapping = analysis.mappingRows.count { it.pendingReview }
        val totalWarnings = "${summary.validationMetric.value} • Chờ rà soát đơn vị $pendingUnit • Chờ rà soát ánh xạ $pendingMapping"
        periodLabel.text = "Kỳ dữ liệu: ${summary.periodLabel}"
        warningCountLabel.text = totalWarnings
        lastRunLabel.text = "Cập nhật lúc: ${analysis.generatedAtLabel}"
    }

    private fun setBusy(busy: Boolean, request: RefreshRequest?) {
        runReconciliationButton.isDisable = busy
        buttonMap.values.forEach { it.isDisable = busy }
        loadingOverlay.isVisible = busy
        loadingOverlay.isManaged = busy
        systemStatusLabel.text = if (busy) "Hệ thống: đang tải dữ liệu" else "Hệ thống: Đang hoạt động"
        loadingMessageLabel.text = when {
            !busy -> ""
            request?.force == true -> "Đang phân tích lại dữ liệu và cập nhật toàn bộ workspace..."
            else -> "Đang tải dữ liệu để mở màn hình..."
        }
        if (busy) {
            currentScreenLabel.text = "Đang làm mới dữ liệu"
        } else {
            currentScreenLabel.text = currentScreen.title
        }
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

    private fun showError(title: String, content: String) {
        Alert(Alert.AlertType.ERROR).apply {
            headerText = title
            contentText = content
        }.showAndWait()
    }
}
