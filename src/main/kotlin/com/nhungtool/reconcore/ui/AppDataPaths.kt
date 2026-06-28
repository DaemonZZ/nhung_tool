package com.nhungtool.reconcore.ui

import java.nio.file.Files
import java.nio.file.Path

object AppDataPaths {
    private val legacyDirectory: Path = Path.of(System.getProperty("user.dir"), ".reconcore")
    private val userDirectory: Path = Path.of(System.getProperty("user.home"), ".reconcore")

    fun resolve(vararg parts: String): Path {
        val root = when {
            Files.exists(userDirectory) -> userDirectory
            Files.exists(legacyDirectory) && Files.isWritable(legacyDirectory) -> legacyDirectory
            else -> userDirectory
        }
        return parts.fold(root) { current, part -> current.resolve(part) }
    }
}
