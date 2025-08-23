package com.astroimagej.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.file.Directory
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
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
    @get:Optional
    abstract val mainJarName: Property<String>

    // Additional args to pass to jpackage
    @get:Input
    @get:Optional
    abstract val extraArgs: ListProperty<String>

    // Directory containing jars/resources to include
    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val inputDir: DirectoryProperty

    // Overridden Launcher
    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    @get:Optional
    abstract val launcherOverride: RegularFileProperty

    @get:Input
    @get:Optional
    abstract val appFileOverride: Property<String>

    @get:Nested
    @get:Optional
    abstract val launcher: Property<JavaLauncher>

    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.NONE)
    @get:Optional
    abstract val runtime: DirectoryProperty

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

        val buildingApp = mainJarName.isPresent

        // Build the arguments list
        val fullArgs = mutableListOf(
            "--name", appName.get(),
            "--dest", destDir.absolutePath,
        )

        if (buildingApp) {
            fullArgs.addAll(
                listOf(
                    "--main-jar", mainJarName.get(),
                    "--input", inputDir.get().asFile.absolutePath,
                )
            )

            fullArgs.addAll(
                runtime.map {
                    listOf("--runtime-image", getRuntime(it))
                }.getOrElse(listOf())
            )
        } else {
            fullArgs.addAll(
                listOf(
                    "--app-image", inputDir.get().asFile.absolutePath,
                )
            )
        }

        // Append any additional args
        fullArgs.addAll(extraArgs.get())

        //logger.lifecycle("Ran jpackage with args: {}", fullArgs)

        // Run jpackage
        val exitCode = execOperations.exec {
            executable = jpackage.get().absolutePath
            args = fullArgs
            if (launcherOverride.isPresent) {
                environment("CustomLauncherPath", launcherOverride.get().asFile.absolutePath)
            }

            if (appFileOverride.isPresent) {
                environment("JpAppImageDir", appFileOverride.get())
            }
        }

        exitCode.rethrowFailure()

        // Set writable so we can replace the launcher
        if (buildingApp) {
            outputDir.get().asFileTree.forEach {
                it.setWritable(true)
            }
        }
    }

    /**
     * Allows setting extra args lazily via a Provider
     */
    fun extraArgs(provider: Provider<List<String>>) {
        extraArgs.addAll(provider)
    }

    /**
     * Convenience to append immediate args
     */
    fun extraArgs(vararg args: String) {
        extraArgs.addAll(args.asList())
    }

    /**
     * Convenience to append immediate args
     */
    fun extraArgs(args: Collection<String>) {
        extraArgs.addAll(args)
    }

    /**
     * The unzip tasks contain a subfolder, but we need the actual jre folder
     */
    private fun getRuntime(dir: Directory): String {
        val fs = dir.asFile.listFiles()

        if (fs.size > 1) {
            return dir.asFile.absolutePath
        }

        return fs.single().absolutePath
    }

    @get:Inject
    protected abstract val javaToolchainService: JavaToolchainService
}