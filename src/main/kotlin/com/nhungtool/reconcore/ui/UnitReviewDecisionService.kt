package com.nhungtool.reconcore.ui

import java.util.ArrayDeque
import java.io.Reader
import java.io.Writer
import java.nio.file.Files
import java.nio.file.Path
import java.util.Properties

object UnitReviewDecisionService {
    enum class DecisionMode { RESOLVED, KEEP_WARNING }

    data class UnitReviewDecision(
        val mode: DecisionMode,
        val xntCode: String? = null,
    ) {
        fun appliesTo(currentXntCode: String): Boolean {
            return xntCode.isNullOrBlank() || xntCode.equals(currentXntCode, ignoreCase = true)
        }
    }

    data class UndoResult(
        val description: String,
        val restoredCount: Int,
    )

    private data class UndoEntry(
        val description: String,
        val previousStates: Map<String, UnitReviewDecision?>,
    )

    private val storagePath: Path = AppDataPaths.resolve("unit-review-decisions.properties")
    private val undoStack = ArrayDeque<UndoEntry>()

    fun loadAll(): Map<String, UnitReviewDecision> {
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

    fun resolve(mappingKey: String, xntCode: String) {
        applyBatch(
            description = "Xác nhận xử lý đơn vị",
            updates = mapOf(mappingKey to UnitReviewDecision(DecisionMode.RESOLVED, xntCode)),
        )
    }

    fun keepWarning(mappingKey: String, xntCode: String) {
        applyBatch(
            description = "Giữ cảnh báo đơn vị",
            updates = mapOf(mappingKey to UnitReviewDecision(DecisionMode.KEEP_WARNING, xntCode)),
        )
    }

    fun clear(mappingKey: String) {
        applyBatch(
            description = "Đặt lại rà soát đơn vị",
            updates = mapOf(mappingKey to null),
        )
    }

    fun clearAll(): Int {
        val existingCount = loadAll().size
        undoStack.clear()
        Files.deleteIfExists(storagePath)
        return existingCount
    }

    fun applyBatch(description: String, updates: Map<String, UnitReviewDecision?>) {
        if (updates.isEmpty()) return
        val all = loadAll().toMutableMap()
        val previousStates = linkedMapOf<String, UnitReviewDecision?>()

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

    private fun writeAll(map: Map<String, UnitReviewDecision>) {
        Files.createDirectories(storagePath.parent)
        val properties = Properties()
        map.toSortedMap().forEach { (key, decision) ->
            properties[key] = encode(decision)
        }
        Files.newBufferedWriter(storagePath).use { writer: Writer ->
            properties.store(writer, "ReconCore unit review decisions")
        }
    }

    private fun pushUndo(description: String, previousStates: Map<String, UnitReviewDecision?>) {
        undoStack += UndoEntry(description, previousStates)
        while (undoStack.size > 100) {
            undoStack.removeFirst()
        }
    }

    private fun encode(decision: UnitReviewDecision): String = listOf(decision.mode.name, decision.xntCode.orEmpty()).joinToString("\t")

    private fun decode(raw: String): UnitReviewDecision {
        val parts = raw.split('\t')
        val mode = runCatching { DecisionMode.valueOf(parts.getOrElse(0) { DecisionMode.KEEP_WARNING.name }.trim()) }.getOrDefault(DecisionMode.KEEP_WARNING)
        val xntCode = parts.getOrElse(1) { "" }.ifBlank { null }
        return UnitReviewDecision(mode, xntCode)
    }
}
