package com.nhungtool.reconcore.ui

import java.io.Reader
import java.io.Writer
import java.nio.file.Files
import java.nio.file.Path
import java.util.ArrayDeque
import java.util.Properties

object MappingDecisionService {
    enum class DecisionMode { CONFIRMED, REMAPPED, NOT_IN_XNT }

    data class MappingDecision(
        val mode: DecisionMode,
        val xntCode: String? = null,
        val ignoreWarnings: Boolean = false,
    )

    data class UndoResult(
        val description: String,
        val restoredCount: Int,
    )

    private data class UndoEntry(
        val description: String,
        val previousStates: Map<String, MappingDecision?>,
    )

    private val storagePath: Path = Path.of(System.getProperty("user.dir"), ".reconcore", "mapping-decisions.properties")
    private val undoStack = ArrayDeque<UndoEntry>()

    fun loadAll(): Map<String, MappingDecision> {
        if (!Files.exists(storagePath)) return emptyMap()
        val properties = Properties()
        Files.newBufferedReader(storagePath).use { reader: Reader ->
            properties.load(reader)
        }
        return properties.entries.associate { entry ->
            val key = entry.key.toString()
            key to decode(entry.value.toString())
        }
    }

    fun confirm(mappingKey: String, xntCode: String, keepIgnoreWarnings: Boolean = false) {
        update(mappingKey, "Xác nhận khớp") {
            MappingDecision(DecisionMode.CONFIRMED, xntCode, keepIgnoreWarnings || it?.ignoreWarnings == true)
        }
    }

    fun remap(mappingKey: String, xntCode: String, keepIgnoreWarnings: Boolean = false) {
        update(mappingKey, "Ánh xạ lại") {
            MappingDecision(DecisionMode.REMAPPED, xntCode, keepIgnoreWarnings || it?.ignoreWarnings == true)
        }
    }

    fun markNotInXnt(mappingKey: String, keepIgnoreWarnings: Boolean = false) {
        update(mappingKey, "Đánh dấu không có trong XNT") {
            MappingDecision(DecisionMode.NOT_IN_XNT, null, keepIgnoreWarnings || it?.ignoreWarnings == true)
        }
    }

    fun toggleIgnoreWarnings(mappingKey: String): MappingDecision? {
        var updatedDecision: MappingDecision? = null
        update(mappingKey, "Đổi trạng thái cảnh báo") { existing ->
            updatedDecision = when {
                existing == null -> MappingDecision(DecisionMode.NOT_IN_XNT, null, true)
                else -> existing.copy(ignoreWarnings = !existing.ignoreWarnings)
            }
            updatedDecision
        }
        return updatedDecision
    }

    fun setIgnoreWarnings(mappingKey: String, enabled: Boolean, fallbackMode: DecisionMode, fallbackXntCode: String?) {
        update(mappingKey, if (enabled) "Bỏ qua cảnh báo" else "Khôi phục cảnh báo") { existing ->
            when {
                existing != null -> existing.copy(ignoreWarnings = enabled)
                else -> MappingDecision(fallbackMode, fallbackXntCode, enabled)
            }
        }
    }

    fun clear(mappingKey: String) {
        applyBatch(
            description = "Xóa quyết định ánh xạ",
            updates = mapOf(mappingKey to null),
        )
    }

    fun clearAll(): Int {
        val existingCount = loadAll().size
        undoStack.clear()
        Files.deleteIfExists(storagePath)
        return existingCount
    }

    fun applyBatch(description: String, updates: Map<String, MappingDecision?>) {
        if (updates.isEmpty()) return
        val all = loadAll().toMutableMap()
        val previousStates = linkedMapOf<String, MappingDecision?>()

        updates.forEach { (mappingKey, next) ->
            val previous = all[mappingKey]
            if (previous == next) return@forEach
            previousStates[mappingKey] = previous
            if (next == null) {
                all.remove(mappingKey)
            } else {
                all[mappingKey] = next
            }
        }

        if (previousStates.isEmpty()) return
        writeAll(all)
        pushUndo(description, previousStates)
    }

    fun canUndo(): Boolean = undoStack.isNotEmpty()

    fun peekUndoDescription(): String? = undoStack.lastOrNull()?.description

    fun undoLastChange(): UndoResult? {
        if (undoStack.isEmpty()) return null
        val entry = undoStack.removeLast()
        val all = loadAll().toMutableMap()
        entry.previousStates.forEach { (mappingKey, previous) ->
            if (previous == null) {
                all.remove(mappingKey)
            } else {
                all[mappingKey] = previous
            }
        }
        writeAll(all)
        return UndoResult(entry.description, entry.previousStates.size)
    }

    private fun update(mappingKey: String, description: String, updater: (MappingDecision?) -> MappingDecision?) {
        val current = loadAll()[mappingKey]
        val next = updater(current)
        applyBatch(description, mapOf(mappingKey to next))
    }

    private fun pushUndo(description: String, previousStates: Map<String, MappingDecision?>) {
        undoStack += UndoEntry(description, previousStates)
        while (undoStack.size > 100) {
            undoStack.removeFirst()
        }
    }

    private fun writeAll(map: Map<String, MappingDecision>) {
        Files.createDirectories(storagePath.parent)
        val properties = Properties()
        map.toSortedMap().forEach { (key, decision) ->
            properties[key] = encode(decision)
        }
        Files.newBufferedWriter(storagePath).use { writer: Writer ->
            properties.store(writer, "ReconCore mapping decisions")
        }
    }

    private fun encode(decision: MappingDecision): String {
        return listOf(decision.mode.name, decision.xntCode.orEmpty(), decision.ignoreWarnings.toString()).joinToString("\t")
    }

    private fun decode(raw: String): MappingDecision {
        val parts = raw.split('\t')
        val mode = runCatching { DecisionMode.valueOf(parts.getOrElse(0) { DecisionMode.NOT_IN_XNT.name }) }.getOrDefault(DecisionMode.NOT_IN_XNT)
        val xntCode = parts.getOrElse(1) { "" }.ifBlank { null }
        val ignoreWarnings = parts.getOrElse(2) { "false" }.toBooleanStrictOrNull() ?: false
        return MappingDecision(mode, xntCode, ignoreWarnings)
    }
}
