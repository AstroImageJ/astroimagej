package com.astroimagej.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.gradle.internal.os.OperatingSystem
import org.gradle.jvm.toolchain.JavaLauncher
import org.gradle.jvm.toolchain.JavaToolchainService
import org.gradle.kotlin.dsl.getByType
import org.gradle.process.ExecOperations
import javax.inject.Inject

abstract class JPackageTask
@Inject constructor(private var execOperations: ExecOperations) : DefaultTask() {

    @get:Input
    abstract val appName: Property<String>

    // The name of the main JAR inside the input directory
    @get:Input
    abstract val mainJarName: Property<String>

    // Additional args to pass to jpackage
    @get:Input
    abstract val extraArgs: ListProperty<String>

    // Directory containing jars/resources to include
    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val inputDir: DirectoryProperty

    @get:Nested
    abstract val launcher: Property<JavaLauncher>

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    init {
        // Configure default toolchain
        val toolchain = project.extensions.getByType<JavaPluginExtension>().toolchain
        val defaultLauncher = javaToolchainService.launcherFor(toolchain)
        launcher.convention(defaultLauncher)

        // Default extraArgs to empty list
        extraArgs.convention(emptyList())
    }

    @TaskAction
    fun jPackage() {
        // Clean out existing destination
        val destDir = outputDir.get().asFile
        if (destDir.exists()) {
            destDir.deleteRecursively()
        }

        // Find the jpackage tool
        val jpackageName = if (OperatingSystem.current().isWindows) {
            "jpackage.exe"
        } else {
            "jpackage"
        }

        val jpackage = launcher.map {
            it.executablePath.asFile.resolveSibling(jpackageName)
        }

        // Build the arguments list
        val fullArgs = mutableListOf(
            "--name", appName.get(),
            "--input", inputDir.get().asFile.absolutePath,
            "--main-jar", mainJarName.get(),
            "--dest", destDir.absolutePath,
        )

        // Append any additional args
        fullArgs.addAll(extraArgs.get())

        // Run jpackage
        val exitCode = execOperations.exec {
            executable = jpackage.get().absolutePath
            args = fullArgs
        }

        exitCode.rethrowFailure()
    }

    @get:Inject
    protected abstract val javaToolchainService: JavaToolchainService
}