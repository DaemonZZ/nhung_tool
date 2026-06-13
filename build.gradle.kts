plugins {
    kotlin("jvm") version "2.0.21"
    application
    id("org.openjfx.javafxplugin") version "0.1.0"
}

group = "com.nhungtool"
version = "0.1.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation("org.apache.poi:poi-ooxml:5.2.5")
    runtimeOnly("org.apache.logging.log4j:log4j-core:2.24.1")
}

kotlin {
    jvmToolchain(17)
}

javafx {
    version = "21.0.5"
    modules = listOf("javafx.controls", "javafx.fxml")
}

application {
    mainClass = "com.nhungtool.reconcore.LauncherKt"
}

tasks.withType<JavaExec>().configureEach {
    jvmArgs = listOf(
        "--add-opens=javafx.graphics/com.sun.javafx.css=ALL-UNNAMED",
    )
}

tasks.test {
    useJUnitPlatform()
}
