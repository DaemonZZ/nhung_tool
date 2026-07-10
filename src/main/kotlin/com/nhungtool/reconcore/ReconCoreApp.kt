package com.nhungtool.reconcore

import javafx.application.Application
import javafx.fxml.FXMLLoader
import javafx.scene.Scene
import javafx.scene.image.Image
import javafx.stage.Screen
import javafx.stage.Stage

class ReconCoreApp : Application() {
    override fun start(stage: Stage) {
        val root = FXMLLoader.load<javafx.scene.Parent>(
            javaClass.getResource("/com/nhungtool/reconcore/fxml/main-shell.fxml")
        )
        val visualBounds = Screen.getPrimary().visualBounds
        val width = minOf(1280.0, visualBounds.width * 0.94).coerceAtLeast(minOf(1024.0, visualBounds.width))
        val height = minOf(820.0, visualBounds.height * 0.92).coerceAtLeast(minOf(660.0, visualBounds.height))
        javaClass.getResourceAsStream("/com/nhungtool/reconcore/icons/reconcore.png")?.use {
            stage.icons.add(Image(it))
        }

        stage.title = "ReconCore"
        stage.scene = Scene(root, width, height)
        stage.minWidth = minOf(1024.0, visualBounds.width)
        stage.minHeight = minOf(660.0, visualBounds.height)
        if (visualBounds.width <= 1400.0 || visualBounds.height <= 820.0) {
            stage.isMaximized = true
        }
        stage.show()
    }
}
