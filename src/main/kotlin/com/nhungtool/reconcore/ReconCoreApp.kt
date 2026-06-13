package com.nhungtool.reconcore

import javafx.application.Application
import javafx.fxml.FXMLLoader
import javafx.scene.Scene
import javafx.stage.Stage

class ReconCoreApp : Application() {
    override fun start(stage: Stage) {
        val root = FXMLLoader.load<javafx.scene.Parent>(
            javaClass.getResource("/com/nhungtool/reconcore/fxml/main-shell.fxml")
        )

        stage.title = "ReconCore"
        stage.scene = Scene(root, 1520.0, 920.0)
        stage.minWidth = 1280.0
        stage.minHeight = 760.0
        stage.show()
    }
}
