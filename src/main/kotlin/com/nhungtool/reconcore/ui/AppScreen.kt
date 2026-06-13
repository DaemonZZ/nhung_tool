package com.nhungtool.reconcore.ui

enum class AppScreen(val title: String, val fxmlPath: String) {
    DASHBOARD("Workspace Dashboard", "/com/nhungtool/reconcore/fxml/dashboard.fxml"),
    INPUT_VALIDATION("Input / Validation", "/com/nhungtool/reconcore/fxml/input-validation.fxml"),
    CONFIGURATION("Processing Configuration", "/com/nhungtool/reconcore/fxml/configuration.fxml"),
    PROGRESS("Execution Progress", "/com/nhungtool/reconcore/fxml/progress.fxml"),
    MAPPING_REVIEW("Product Mapping Review", "/com/nhungtool/reconcore/fxml/mapping-review.fxml"),
    UNIT_REVIEW("Unit Mismatch Review", "/com/nhungtool/reconcore/fxml/unit-review.fxml"),
    DETAILED_RESULTS("Detailed Results", "/com/nhungtool/reconcore/fxml/detailed-results.fxml"),
    NEGATIVE_INVENTORY("Negative Inventory", "/com/nhungtool/reconcore/fxml/negative-inventory.fxml"),
    EXPORT("Export", "/com/nhungtool/reconcore/fxml/export.fxml"),
    HISTORY("Run History", "/com/nhungtool/reconcore/fxml/history.fxml"),
    SETTINGS("Settings", "/com/nhungtool/reconcore/fxml/settings.fxml"),
}
