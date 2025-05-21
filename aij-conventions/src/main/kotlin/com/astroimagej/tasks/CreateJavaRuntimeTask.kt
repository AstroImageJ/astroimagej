package com.astroimagej.tasks

import com.astroimagej.meta.jdk.RuntimeType
import com.astroimagej.meta.jdk.cache.JavaRuntimeSystem
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.*
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.ProviderFactory
import org.gradle.api.tasks.*
import org.gradle.internal.os.OperatingSystem
import org.gradle.jvm.toolchain.JavaLauncher
import org.gradle.jvm.toolchain.JavaToolchainService
import org.gradle.kotlin.dsl.getByType
import org.gradle.process.ExecOperations
import java.io.ByteArrayOutputStream
import java.io.File
import javax.inject.Inject

abstract class CreateJavaRuntimeTask
@Inject constructor(
    private var execOperations: ExecOperations,
    private val archiveOperations: ArchiveOperations,
    private val layout: ProjectLayout,
    private val fs: FileSystemOperations
) : DefaultTask() {

    @get:InputFile
    abstract val bundledRuntime: RegularFileProperty

    @get:Input
    abstract val runtimeType: Property<String>

    @get:Input
    abstract val extension: Property<String>

    @get:Input
    @get:Optional
    abstract val jlinkArgs: ListProperty<String>

    @get:Nested
    @get:Optional
    abstract val launcher: Property<JavaLauncher>

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @get:OutputDirectory
    abstract val unpackedDir: DirectoryProperty

    @get:Inject
    protected abstract val javaToolchainService: JavaToolchainService

    @get:Inject
    protected abstract val factory: ProviderFactory

    init {
        // Configure default toolchain
        val toolchain = project.extensions.getByType<JavaPluginExtension>().toolchain
        val defaultLauncher = javaToolchainService.launcherFor(toolchain)
        launcher.convention(defaultLauncher)

        unpackedDir.convention(layout.buildDirectory.dir("unpackedRuntime-$name"))

        jlinkArgs.convention(listOf(
            "--strip-native-commands",
            "--strip-debug",
            "--no-man-pages",
            "--no-header-files",
            //"--verbose",
        ))
    }

    @TaskAction
    fun buildJavaRuntime() {
        when (runtimeType(runtimeType.get())) {
            null -> GradleException("Missing runtime type information!")
            RuntimeType.JRE -> unpack(outputDir)
            RuntimeType.JDK -> {
                // Unpack JDK
                unpack(unpackedDir)

                // Clean out existing destination
                val destDir = outputDir.get().asFile
                if (destDir.exists()) {
                    destDir.deleteRecursively()
                }

                // Find the jpackage tool
                val jlinkName = if (OperatingSystem.current().isWindows) {
                    "jlink.exe"
                } else {
                    "jlink"
                }

                var jlink = launcher.map {
                    it.executablePath.asFile.resolveSibling(jlinkName)
                }

                val fullArgs = mutableListOf("--output", outputDir.get().asFile.absolutePath)
                fullArgs.addAll(jlinkArgs.get())

                // Specify modules
                if (true) {
                    // We can't use the modules method to build the jre with Adoptium
                    // since https://adoptium.net/blog/2025/03/eclipse-temurin-jdk24-JEP493-enabled/
                    // So cross compilation is no longer possible, but we're using jpackage anyway...
                    //https://github.com/adoptium/adoptium-support/issues/1271

                    // Find all modules
                    val stdout = ByteArrayOutputStream()
                    val exitCode = execOperations.exec {
                        executable = findFile(unpackedDir.get().asFile, if (OperatingSystem.current().isWindows) {
                            "java.exe"
                        } else {
                            "java"
                        }).absolutePath

                        args = listOf(
                            "--list-modules"
                        )

                        standardOutput = stdout
                    }

                    exitCode.rethrowFailure()

                    val modulesCsv = stdout.toString()
                        .lineSequence()
                        .map { it.substringBefore('@') }   // drop the @version
                        .filter {
                            it.isNotBlank() &&
                            // Can't include jlink in this mode
                            it != "jdk.jlink" && it != "jdk.jpackage"
                        }
                        .joinToString(",")

                    jlink = factory.provider { findFile(unpackedDir.get().asFile, jlinkName) }

                    fullArgs.addAll(listOf(
                        "--add-modules", modulesCsv,
                    ))
                } else {
                    fullArgs.addAll(listOf(
                        "--module-path", findJmodsDir(unpackedDir.get().asFile).absolutePath,
                    ))
                }

                // Run jlink
                val exitCode = execOperations.exec {
                    executable = jlink.get().absolutePath
                    args = fullArgs
                }

                exitCode.rethrowFailure()
            }
        }
    }

    private fun unpack(unpackLoc: DirectoryProperty) {
        val result = fs.sync {
            when (extension.get()) {
                "tar.gz", "tgz" -> from(archiveOperations.tarTree(archiveOperations.gzip(bundledRuntime))) {
                    into("")
                }
                "zip" -> from(archiveOperations.zipTree(bundledRuntime)) {
                    into("")
                }
                else -> GradleException("Did not know how to handle ${extension.get()}")
            }

            into(unpackLoc)
        }
    }

    private fun findJmodsDir(root: File): File {
        return root.walkTopDown()
            .firstOrNull { it.isDirectory && it.name.equals("jmods") }
            ?: throw GradleException("'jmods' directory not found under ${root.absolutePath}")
    }

    private fun findFile(root: File, jlinkName: String): File {
        return root.walkTopDown()
            .firstOrNull { it.isFile && it.name.equals(jlinkName) }
            ?: throw GradleException("'jlink' not found under ${root.absolutePath}")
    }

    fun fromRuntimeInfo(runtimeInfo: JavaRuntimeSystem) {
        runtimeType.set(when (runtimeInfo.type) {
            RuntimeType.JDK -> "jdk"
            RuntimeType.JRE -> "jre"
            null -> "null"
        })

        extension.set(runtimeInfo.ext)
    }

    private fun runtimeType(runtimeInfo: String): RuntimeType? {
        return when (runtimeInfo) {
            "jdk"-> return RuntimeType.JDK
            "jre" -> return RuntimeType.JRE
            else -> null
        }
    }
}