package com.nhungtool.reconcore.ui

enum class AppScreen(val title: String, val fxmlPath: String) {
    DASHBOARD("Bảng điều khiển tổng quan", "/com/nhungtool/reconcore/fxml/dashboard.fxml"),
    INPUT_VALIDATION("Đầu vào / Kiểm tra", "/com/nhungtool/reconcore/fxml/input-validation.fxml"),
    CONFIGURATION("Cấu hình xử lý", "/com/nhungtool/reconcore/fxml/configuration.fxml"),
    PROGRESS("Tiến trình xử lý", "/com/nhungtool/reconcore/fxml/progress.fxml"),
    MAPPING_REVIEW("Rà soát ánh xạ mặt hàng", "/com/nhungtool/reconcore/fxml/mapping-review.fxml"),
    UNIT_REVIEW("Rà soát đơn vị", "/com/nhungtool/reconcore/fxml/unit-review.fxml"),
    DETAILED_RESULTS("Kết quả chi tiết", "/com/nhungtool/reconcore/fxml/detailed-results.fxml"),
    NEGATIVE_INVENTORY("Âm kho", "/com/nhungtool/reconcore/fxml/negative-inventory.fxml"),
    EXPORT("Xuất báo cáo", "/com/nhungtool/reconcore/fxml/export.fxml"),
    HISTORY("Lịch sử chạy", "/com/nhungtool/reconcore/fxml/history.fxml"),
    SETTINGS("Thiết lập", "/com/nhungtool/reconcore/fxml/settings.fxml"),
}
