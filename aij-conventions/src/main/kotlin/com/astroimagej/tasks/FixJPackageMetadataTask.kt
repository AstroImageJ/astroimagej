package com.astroimagej.tasks

import com.astroimagej.meta.jdk.OperatingSystem
import com.astroimagej.util.fixJpackageVersion
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.gradle.jvm.toolchain.JavaLauncher
import org.gradle.jvm.toolchain.JavaToolchainService
import org.gradle.kotlin.dsl.getByType
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

    @get:Inject
    protected abstract val javaToolchainService: JavaToolchainService
}