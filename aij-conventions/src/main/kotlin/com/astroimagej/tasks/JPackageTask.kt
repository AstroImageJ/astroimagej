package com.astroimagej.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import java.util.spi.ToolProvider

abstract class JPackageTask : DefaultTask() {
    @get:Input
    abstract val appName: Property<String>

    // The name of the main JAR inside the input directory
    @get:Input
    abstract val mainJarName: Property<String>

    // Directory (or task output) containing jars/resources to include
    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val inputDir: DirectoryProperty

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @TaskAction
    fun jPackage() {
        // Clean out existing destination
        val destDir = outputDir.get().asFile
        if (destDir.exists()) {
            destDir.deleteRecursively()
        }

        // Find the jpackage tool
        val provider = ToolProvider.findFirst("jpackage")
            .orElseThrow { GradleException("Could not find 'jpackage' on the tool path") }

        // Build the arguments list
        val args = listOf(
            "--type", "app-image",//todo take this in as property
            "--name", appName.get(),
            "--input", inputDir.get().asFile.absolutePath,
            "--main-jar", mainJarName.get(),
            "--dest", destDir.absolutePath,
            "--java-options", "-Duser.dir=\$APPDIR" //todo property to take more params
        )

        // Run jpackage
        val exitCode = provider.run(System.out, System.err, *args.toTypedArray())
        if (exitCode != 0) {
            throw GradleException("jpackage failed with exit code $exitCode")
        }
    }
}