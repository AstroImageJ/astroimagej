package com.astroimagej.util

import org.gradle.api.file.Directory
import org.gradle.api.provider.Property
import org.gradle.jvm.toolchain.JavaLauncher
import java.util.*

// When creating ap image manually, we need to generate a .jpackage.xml with the correct
// version information for jpackage to then build the installer
fun fixJpackageVersion(dir: Directory, launcher: Property<JavaLauncher>, targetOs: Property<com.astroimagej.meta.jdk.OperatingSystem>) {
    val version = Properties().let {
        it.load(launcher.get().metadata.installationPath.file("release").asFile.inputStream())
        it.getProperty("JAVA_VERSION").replace("\"", "")
    }

    val path = when (targetOs.get()) {
        com.astroimagej.meta.jdk.OperatingSystem.WINDOWS -> "app/.jpackage.xml"
        com.astroimagej.meta.jdk.OperatingSystem.LINUX -> "lib/app/.jpackage.xml"
        com.astroimagej.meta.jdk.OperatingSystem.MAC -> "Contents/app/.jpackage.xml"
    }

    val file = dir.file(path).asFile
    val content = file.readText()
        .replace("\$VERSION", version)
    file.writeText(content)
}