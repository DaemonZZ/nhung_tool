package com.nhungtool.reconcore.ui

import org.apache.poi.ss.usermodel.DataFormatter
import org.apache.poi.ss.usermodel.Row
import org.apache.poi.ss.usermodel.WorkbookFactory
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.io.path.exists
import kotlin.io.path.fileSize
import kotlin.io.path.getLastModifiedTime
import kotlin.math.roundToLong

data class FileFormatCheck(
    val label: String,
    val path: Path,
    val ready: Boolean,
    val errors: List<String>,
    val warnings: List<String>,
    val infos: List<String>,
) {
    val summary: String
        get() = when {
            errors.isNotEmpty() -> errors.first()
            warnings.isNotEmpty() -> warnings.first()
            infos.isNotEmpty() -> infos.first()
            else -> "Chưa kiểm tra"
        }
}

data class WorkspaceFileValidation(
    val xntCheck: FileFormatCheck,
    val invoiceCheck: FileFormatCheck,
    val readyToImport: Boolean,
    val lines: List<String>,
)

data class WorkspaceInputSnapshot(
    val activeXntPath: Path,
    val activeInvoicePath: Path,
    val activeXntAnalysisPath: Path,
    val activeInvoiceAnalysisPath: Path,
    val pendingXntPath: Path,
    val pendingInvoicePath: Path,
    val lastValidation: WorkspaceFileValidation?,
)

object WorkspaceInputService {
    private val formatter = DataFormatter()
    private val modifiedFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
    private val defaultXntPath: Path = Paths.get(System.getProperty("user.dir"), "docs", "xnt.xlsx")
    private val defaultInvoicePath: Path = Paths.get(System.getProperty("user.dir"), "docs", "chi tiet hoa don anh huy hoang.xlsx")
    private val cacheDirectory: Path = Paths.get(System.getProperty("user.dir"), ".reconcore", "input-cache")

    @Volatile private var activeXntPath: Path = defaultXntPath
    @Volatile private var activeInvoicePath: Path = defaultInvoicePath
    @Volatile private var activeXntAnalysisPath: Path = stagePath("active-xnt", defaultXntPath)
    @Volatile private var activeInvoiceAnalysisPath: Path = stagePath("active-invoice", defaultInvoicePath)
    @Volatile private var pendingXntPath: Path = defaultXntPath
    @Volatile private var pendingInvoicePath: Path = defaultInvoicePath
    @Volatile private var lastValidation: WorkspaceFileValidation? = null

    init {
        ensureActiveCopies()
    }

    fun snapshot(): WorkspaceInputSnapshot {
        return WorkspaceInputSnapshot(
            activeXntPath = activeXntPath,
            activeInvoicePath = activeInvoicePath,
            activeXntAnalysisPath = activeXntAnalysisPath,
            activeInvoiceAnalysisPath = activeInvoiceAnalysisPath,
            pendingXntPath = pendingXntPath,
            pendingInvoicePath = pendingInvoicePath,
            lastValidation = lastValidation,
        )
    }

    fun selectPendingXnt(path: Path) {
        pendingXntPath = path
        lastValidation = null
    }

    fun selectPendingInvoice(path: Path) {
        pendingInvoicePath = path
        lastValidation = null
    }

    fun validatePendingSelection(): WorkspaceFileValidation {
        val xntCheck = validateXntFile(pendingXntPath)
        val invoiceCheck = validateInvoiceFile(pendingInvoicePath)
        val readyToImport = xntCheck.errors.isEmpty() && invoiceCheck.errors.isEmpty()
        val lines = buildList {
            add("Thông tin: File XNT đang chọn: ${pendingXntPath.fileName}")
            add("Thông tin: File hóa đơn đang chọn: ${pendingInvoicePath.fileName}")
            xntCheck.infos.forEach { add("Thông tin: XNT - $it") }
            xntCheck.warnings.forEach { add("Cảnh báo: XNT - $it") }
            xntCheck.errors.forEach { add("Lỗi: XNT - $it") }
            invoiceCheck.infos.forEach { add("Thông tin: Hóa đơn - $it") }
            invoiceCheck.warnings.forEach { add("Cảnh báo: Hóa đơn - $it") }
            invoiceCheck.errors.forEach { add("Lỗi: Hóa đơn - $it") }
            add(if (readyToImport) "OK: Format hợp lệ, có thể nạp dữ liệu" else "Lỗi: Format chưa hợp lệ, chưa thể nạp")
        }
        return WorkspaceFileValidation(xntCheck, invoiceCheck, readyToImport, lines).also {
            lastValidation = it
        }
    }

    fun importValidatedSelection(): Boolean {
        val validation = lastValidation ?: validatePendingSelection()
        if (!validation.readyToImport) return false
        activeXntPath = pendingXntPath
        activeInvoicePath = pendingInvoicePath
        activeXntAnalysisPath = stageCopy(activeXntPath, "active-xnt")
        activeInvoiceAnalysisPath = stageCopy(activeInvoicePath, "active-invoice")
        return true
    }

    fun metaFor(path: Path): String {
        if (!path.exists()) return "Không tìm thấy file"
        val size = formatSize(path.fileSize())
        val modified = path.getLastModifiedTime().toInstant()
            .atZone(ZoneId.systemDefault())
            .toLocalDateTime()
            .format(modifiedFormatter)
        return "$size • cập nhật $modified"
    }

    private fun validateXntFile(path: Path): FileFormatCheck {
        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()
        val infos = mutableListOf<String>()
        validateCommon(path, errors)
        if (errors.isNotEmpty()) return FileFormatCheck("XNT", path, false, errors, warnings, infos)

        runCatching {
            WorkbookFactory.create(path.toFile()).use { workbook ->
                if (workbook.numberOfSheets == 0) {
                    errors += "Tệp Excel không có tab"
                    return@use
                }
                val sheet = workbook.getSheetAt(0)
                infos += "Nhận diện tab ${sheet.sheetName}"
                val dataRowCount = countMeaningfulRows(sheet, startRowIndex = 5) { row ->
                    cellText(row, 1).isNotBlank() || cellText(row, 2).isNotBlank() || cellText(row, 3).isNotBlank()
                }
                infos += "Số dòng dữ liệu XNT: $dataRowCount"
                val headerRow = sheet.getRow(3)
                val subHeaderRow = sheet.getRow(4)
                if (!matches(headerRow, 1, "Diễn giải")) errors += "Thiếu cột 'Diễn giải' ở tiêu đề XNT"
                if (!matches(headerRow, 2, "ĐVT")) errors += "Thiếu cột 'ĐVT' ở tiêu đề XNT"
                if (!matches(headerRow, 3, "MS")) errors += "Thiếu cột 'MS' ở tiêu đề XNT"
                if (!matches(headerRow, 4, "Tồn đầu kỳ")) errors += "Thiếu nhóm cột 'Tồn đầu kỳ'"
                if (!matches(headerRow, 6, "Nhập")) errors += "Thiếu nhóm cột 'Nhập'"
                if (!matches(headerRow, 8, "Xuất")) errors += "Thiếu nhóm cột 'Xuất'"
                if (!matches(headerRow, 10, "Tồn cuối kỳ")) errors += "Thiếu nhóm cột 'Tồn cuối kỳ'"
                if (!matches(subHeaderRow, 4, "Lượng") || !matches(subHeaderRow, 5, "Tiền")) errors += "Thiếu cặp cột 'Lượng/Tiền' cho tồn đầu kỳ"
                if (!matches(subHeaderRow, 6, "Lượng") || !matches(subHeaderRow, 7, "Tiền")) errors += "Thiếu cặp cột 'Lượng/Tiền' cho nhập"
                if (!matches(subHeaderRow, 8, "Lượng") || !matches(subHeaderRow, 9, "Tiền")) errors += "Thiếu cặp cột 'Lượng/Tiền' cho xuất"
                if (!matches(subHeaderRow, 10, "Lượng") || !matches(subHeaderRow, 11, "Tiền")) errors += "Thiếu cặp cột 'Lượng/Tiền' cho tồn cuối kỳ"

                val firstDataRow = sheet.getRow(5)
                if (firstDataRow == null) {
                    warnings += "Tab XNT chưa có dòng dữ liệu đầu tiên"
                } else {
                    infos += "Dữ liệu bắt đầu từ dòng 6"
                }
            }
        }.onFailure { errors += "Không mở được tệp Excel XNT: ${it.message ?: "không rõ lỗi"}" }

        return FileFormatCheck("XNT", path, errors.isEmpty(), errors, warnings, infos)
    }

    private fun validateInvoiceFile(path: Path): FileFormatCheck {
        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()
        val infos = mutableListOf<String>()
        validateCommon(path, errors)
        if (errors.isNotEmpty()) return FileFormatCheck("Invoice", path, false, errors, warnings, infos)

        runCatching {
            WorkbookFactory.create(path.toFile()).use { workbook ->
                val purchaseSheet = workbook.getSheet("MuaVao_1")
                val salesSheet = workbook.getSheet("BanRa_1")
                if (purchaseSheet == null) errors += "Thiếu tab MuaVao_1"
                if (salesSheet == null) errors += "Thiếu tab BanRa_1"
                if (purchaseSheet == null || salesSheet == null) return@use

                infos += "Nhận diện tab MuaVao_1 và BanRa_1"
                infos += "MuaVao_1: ${countMeaningfulRows(purchaseSheet, 2) { row -> cellText(row, 7).isNotBlank() || cellText(row, 6).isNotBlank() }} dòng dữ liệu"
                infos += "BanRa_1: ${countMeaningfulRows(salesSheet, 2) { row -> cellText(row, 7).isNotBlank() || cellText(row, 6).isNotBlank() }} dòng dữ liệu"

                val purchaseHeaders = headerSet(purchaseSheet.getRow(1))
                val salesHeaders = headerSet(salesSheet.getRow(1))

                listOf(
                    "TÊN HÀNG HÓA - DỊCH VỤ",
                    "Đơn vị tính",
                    "Số lượng",
                    "Thành tiền",
                    "Ngày lập hóa đơn (dạng text)",
                ).forEach { required ->
                    if (required !in purchaseHeaders) errors += "Tab MuaVao_1 thiếu cột '$required'"
                }

                listOf(
                    "TÊN HÀNG HÓA - DỊCH VỤ",
                    "Đơn vị tính",
                    "Số lượng",
                    "Thành tiền",
                    "Ngày lập hóa đơn (dạng text)",
                ).forEach { required ->
                    if (required !in salesHeaders) errors += "Tab BanRa_1 thiếu cột '$required'"
                }

                if (purchaseSheet.lastRowNum < 2) warnings += "Tab MuaVao_1 chưa có dòng dữ liệu"
                if (salesSheet.lastRowNum < 2) warnings += "Tab BanRa_1 chưa có dòng dữ liệu"
            }
        }.onFailure { errors += "Không mở được tệp Excel hóa đơn: ${it.message ?: "không rõ lỗi"}" }

        return FileFormatCheck("Invoice", path, errors.isEmpty(), errors, warnings, infos)
    }

    private fun validateCommon(path: Path, errors: MutableList<String>) {
        if (!path.exists()) {
            errors += "Không tìm thấy file"
            return
        }
        val extension = path.fileName.toString().substringAfterLast('.', "").lowercase()
        if (extension !in setOf("xlsx", "xls")) {
            errors += "Chỉ hỗ trợ file Excel .xlsx hoặc .xls"
        }
    }

    private fun headerSet(row: Row?): Set<String> {
        if (row == null) return emptySet()
        return row.map { formatter.formatCellValue(it).trim() }
            .filter { it.isNotBlank() }
            .toSet()
    }

    private fun matches(row: Row?, index: Int, expected: String): Boolean {
        if (row == null) return false
        return formatter.formatCellValue(row.getCell(index)).trim().equals(expected, ignoreCase = true)
    }

    private fun cellText(row: Row?, index: Int): String {
        if (row == null) return ""
        return formatter.formatCellValue(row.getCell(index)).trim()
    }

    private fun countMeaningfulRows(
        sheet: org.apache.poi.ss.usermodel.Sheet,
        startRowIndex: Int,
        predicate: (Row) -> Boolean,
    ): Int {
        var count = 0
        for (rowIndex in startRowIndex..sheet.lastRowNum) {
            val row = sheet.getRow(rowIndex) ?: continue
            if (predicate(row)) {
                count += 1
            }
        }
        return count
    }

    private fun formatSize(bytes: Long): String {
        if (bytes < 1024) return "$bytes B"
        val kb = bytes / 1024.0
        if (kb < 1024) return "${(kb * 10).roundToLong() / 10.0} KB"
        val mb = kb / 1024.0
        return "${(mb * 10).roundToLong() / 10.0} MB"
    }

    private fun ensureActiveCopies() {
        activeXntAnalysisPath = ensureActiveCopy(activeXntPath, activeXntAnalysisPath, "active-xnt")
        activeInvoiceAnalysisPath = ensureActiveCopy(activeInvoicePath, activeInvoiceAnalysisPath, "active-invoice")
    }

    private fun ensureActiveCopy(sourcePath: Path, currentAnalysisPath: Path, prefix: String): Path {
        if (currentAnalysisPath.exists()) return currentAnalysisPath
        if (!sourcePath.exists()) return sourcePath
        return runCatching { stageCopy(sourcePath, prefix) }.getOrElse { sourcePath }
    }

    private fun stageCopy(sourcePath: Path, prefix: String): Path {
        val targetPath = stagePath(prefix, sourcePath)
        Files.createDirectories(cacheDirectory)
        Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES)
        return targetPath
    }

    private fun stagePath(prefix: String, sourcePath: Path): Path {
        val extension = sourcePath.fileName.toString().substringAfterLast('.', "").ifBlank { "xlsx" }
        return cacheDirectory.resolve("$prefix.$extension")
    }
}
