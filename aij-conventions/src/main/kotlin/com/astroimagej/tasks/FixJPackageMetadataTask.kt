package com.astroimagej.tasks

import com.astroimagej.meta.jdk.OperatingSystem
import org.gradle.api.DefaultTask
import org.gradle.api.file.Directory
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.gradle.api.tasks.Optional
import org.gradle.jvm.toolchain.JavaLauncher
import org.gradle.jvm.toolchain.JavaToolchainService
import org.gradle.kotlin.dsl.getByType
import java.util.*
import javax.inject.Inject

abstract class FixJPackageMetadataTask@Inject constructor(
    private var fileOperations: FileSystemOperations,
) : DefaultTask() {
    @get:Input
    abstract val targetOs: Property<OperatingSystem>

    @get:Nested
    @get:Optional
    abstract val launcher: Property<JavaLauncher>

    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val inputDir: DirectoryProperty

    init {
        // Configure default toolchain
        val toolchain = project.extensions.getByType<JavaPluginExtension>().toolchain
        val defaultLauncher = javaToolchainService.launcherFor(toolchain)
        launcher.convention(defaultLauncher)
    }

    @TaskAction
    fun execute() {
        fixJpackageVersion(inputDir.get(), launcher, targetOs)
    }

    // When creating ap image manually, we need to generate a .jpackage.xml with the correct
    // version information for jpackage to then build the installer
    fun fixJpackageVersion(dir: Directory, launcher: Property<JavaLauncher>, targetOs: Property<com.astroimagej.meta.jdk.OperatingSystem>) {
        val version = Properties().let {
            it.load(launcher.get().metadata.installationPath.file("release").asFile.inputStream())
            it.getProperty("JAVA_VERSION").replace("\"", "")
        }

        val path = when (targetOs.get()) {
            OperatingSystem.WINDOWS -> "app/.jpackage.xml"
            OperatingSystem.LINUX -> "lib/app/.jpackage.xml"
            OperatingSystem.MAC -> "Contents/app/.jpackage.xml"
        }

        val file = dir.file(path).asFile
        val content = file.readText()
            .replace("\$VERSION", version)
        file.writeText(content)

        logger.lifecycle("Fixed jpackage version to: $version")
    }

    @get:Inject
    protected abstract val javaToolchainService: JavaToolchainService
}