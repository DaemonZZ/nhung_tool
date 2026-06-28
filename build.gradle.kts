plugins {
    kotlin("jvm") version "2.0.21"
    application
    id("org.openjfx.javafxplugin") version "0.1.0"
}

group = "com.nhungtool"
version = "0.1.2"

val nativeAppName = "ReconCore"
val appMainClass = "com.nhungtool.reconcore.LauncherKt"
val appJvmArgs = listOf(
    "--add-opens=javafx.graphics/com.sun.javafx.css=ALL-UNNAMED",
)

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation(kotlin("reflect"))
    implementation("org.apache.poi:poi-ooxml:5.2.5")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.18.2")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.18.2")
    runtimeOnly("org.apache.logging.log4j:log4j-core:2.24.1")
    testImplementation(kotlin("test"))
}

kotlin {
    jvmToolchain(17)
}

javafx {
    version = "21.0.5"
    modules = listOf("javafx.controls", "javafx.fxml")
}

application {
    mainClass = appMainClass
    applicationDefaultJvmArgs = appJvmArgs
}

tasks.withType<JavaExec>().configureEach {
    jvmArgs = appJvmArgs
}

tasks.test {
    useJUnitPlatform()
}

fun currentInstallerType(): String {
    val osName = System.getProperty("os.name").lowercase()
    return when {
        osName.contains("mac") -> "dmg"
        osName.contains("win") -> "exe"
        else -> "deb"
    }
}

fun jpackageExecutable(): String {
    val osName = System.getProperty("os.name").lowercase()
    return if (osName.contains("win")) "jpackage.exe" else "jpackage"
}

fun currentNativeAppVersion(): String {
    val versionParts = project.version
        .toString()
        .substringBefore("-")
        .split(".")
        .map { it.toIntOrNull() ?: 0 }
        .take(3)

    val normalizedParts = versionParts.ifEmpty { listOf(1, 0, 0) }.toMutableList()
    while (normalizedParts.size < 3) {
        normalizedParts += 0
    }
    if (normalizedParts.first() <= 0) {
        normalizedParts[0] = 1
    }

    return normalizedParts.take(3).joinToString(".")
}

val installDistLibDir = layout.buildDirectory.dir("install/${project.name}/lib")
val nativeImageOutputDir = layout.buildDirectory.dir("jpackage/image")
val nativeInstallerOutputDir = layout.buildDirectory.dir("jpackage/installer")
val mainJarFileName = tasks.named<org.gradle.jvm.tasks.Jar>("jar").flatMap { it.archiveFileName }
val installerType = providers.gradleProperty("installerType").orElse(currentInstallerType())
val nativeAppVersion = providers.gradleProperty("nativeAppVersion").orElse(currentNativeAppVersion())
val macBundleIdentifier = providers.gradleProperty("macBundleIdentifier").orElse("com.nhungtool.reconcore")
val macPackageSigningPrefix = providers.gradleProperty("macPackageSigningPrefix").orElse("com.nhungtool.reconcore")
val macSign = providers.gradleProperty("macSign").map { it.toBooleanStrictOrNull() ?: false }.orElse(false)
val macSigningKeyUserName = providers.gradleProperty("macSigningKeyUserName").orElse("")
val macSigningKeychain = providers.gradleProperty("macSigningKeychain").orElse("")
val macEntitlements = providers.gradleProperty("macEntitlements")
    .orElse(layout.projectDirectory.file("packaging/macos/entitlements.plist").asFile.absolutePath)

fun isMacHost(): Boolean = System.getProperty("os.name").lowercase().contains("mac")

fun jpackageArgs(type: String, outputDir: File): List<String> {
    val baseArgs = listOf(
        jpackageExecutable(),
        "--type",
        type,
        "--name",
        nativeAppName,
        "--app-version",
        nativeAppVersion.get(),
        "--vendor",
        "NhungTool",
        "--dest",
        outputDir.absolutePath,
        "--input",
        installDistLibDir.get().asFile.absolutePath,
        "--main-jar",
        mainJarFileName.get(),
        "--main-class",
        appMainClass,
    ) + appJvmArgs.flatMap { listOf("--java-options", it) }

    val macArgs = if (isMacHost()) {
        mutableListOf(
            "--mac-package-identifier",
            macBundleIdentifier.get(),
            "--mac-package-name",
            nativeAppName,
            "--mac-app-category",
            "public.app-category.business",
        ).apply {
            if (macSign.get()) {
                add("--mac-sign")
                macSigningKeyUserName.get().takeIf { it.isNotBlank() }?.let {
                    add("--mac-signing-key-user-name")
                    add(it)
                }
                macSigningKeychain.get().takeIf { it.isNotBlank() }?.let {
                    add("--mac-signing-keychain")
                    add(it)
                }
                macEntitlements.get().takeIf { File(it).exists() }?.let {
                    add("--mac-entitlements")
                    add(it)
                }
                add("--mac-package-signing-prefix")
                add(macPackageSigningPrefix.get())
            }
        }
    } else {
        emptyList()
    }

    return baseArgs + macArgs
}

tasks.register<Exec>("packageNativeImage") {
    group = "distribution"
    description = "Builds a native app image with jpackage."
    dependsOn(tasks.named("installDist"))

    doFirst {
        val outputDir = nativeImageOutputDir.get().asFile
        project.delete(outputDir)
        outputDir.mkdirs()
        commandLine(jpackageArgs("app-image", outputDir))
    }
}

tasks.register<Exec>("packageNativeInstaller") {
    group = "distribution"
    description = "Builds a native installer with jpackage. Override type with -PinstallerType=dmg|pkg|exe|msi|deb|rpm."
    dependsOn(tasks.named("installDist"))

    doFirst {
        val outputDir = nativeInstallerOutputDir.get().asFile
        project.delete(outputDir)
        outputDir.mkdirs()
        commandLine(jpackageArgs(installerType.get(), outputDir))
    }
}

tasks.register("release") {
    group = "distribution"
    description = "Runs tests and builds the zip distribution plus the native app image."
    dependsOn(tasks.named("test"), tasks.named("distZip"), tasks.named("packageNativeImage"))
}
