package com.nhungtool.reconcore.ui

import javafx.beans.property.ReadOnlyObjectWrapper
import javafx.beans.property.ReadOnlyStringWrapper
import javafx.scene.control.TableColumn
import javafx.scene.control.cell.PropertyValueFactory

object TableBuilders {
    fun <S> stringColumn(title: String, prefWidth: Double, extractor: (S) -> String): TableColumn<S, String> {
        return TableColumn<S, String>(title).apply {
            this.prefWidth = prefWidth
            isSortable = false
            setCellValueFactory { ReadOnlyStringWrapper(extractor(it.value)) }
        }
    }

    fun <S> objectColumn(title: String, prefWidth: Double, extractor: (S) -> Any?): TableColumn<S, Any> {
        return TableColumn<S, Any>(title).apply {
            this.prefWidth = prefWidth
            isSortable = false
            setCellValueFactory { ReadOnlyObjectWrapper(extractor(it.value)) }
        }
    }

    fun <S, T> propertyColumn(title: String, prefWidth: Double, property: String): TableColumn<S, T> {
        return TableColumn<S, T>(title).apply {
            this.prefWidth = prefWidth
            isSortable = false
            cellValueFactory = PropertyValueFactory(property)
        }
    }
}
