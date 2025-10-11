package com.astroimagej.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.support.listFilesOrdered
import org.gradle.process.ExecOperations
import java.nio.file.Files
import javax.inject.Inject

abstract class RenameDmgVolume
@Inject constructor(private var execOperations: ExecOperations) : DefaultTask() {
    @get:Input
    abstract val volume: Property<String>

    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val inputDir: DirectoryProperty

    @TaskAction
    fun execute() {
        val inDmg = inputDir.asFileTree.singleFile

        // Convert to R/W
        val rwDmg = inDmg.resolveSibling("${inDmg.nameWithoutExtension}.rw.dmg")
        execOperations.exec {
            executable = "hdiutil"
            args = listOf(
                "convert", inDmg.absolutePath,
                "-format", "UDRW",
                "-o", rwDmg.absolutePath
            )
        }

        // Mount the volume
        val mountRoot = Files.createTempDirectory(inDmg.toPath().parent, "dmg-mount-").toFile()
        execOperations.exec {
            executable = "hdiutil"
            args = listOf(
                "attach", rwDmg.absolutePath,
                "-mountpoint", mountRoot.absolutePath,
                "-nobrowse"
            )
        }

        try {
            execOperations.exec {
                executable = "diskutil"
                args = listOf(
                    "rename", mountRoot.absolutePath,
                    volume.get()
                )
            }

            println("Detaching ${mountRoot.absolutePath}")
            execOperations.exec {
                executable = "hdiutil"
                args = listOf(
                    "detach", mountRoot.absolutePath
                )
            }

            Files.deleteIfExists(inDmg.toPath())

            println("Converting ${rwDmg.absolutePath} to compressed image")
            execOperations.exec {
                executable = "hdiutil"
                args = listOf(
                    "convert", rwDmg.absolutePath,
                    "-format", "UDZO",
                    "-o", inDmg.absolutePath
                )
            }

            Files.deleteIfExists(rwDmg.toPath())
            Files.deleteIfExists(mountRoot.toPath())
        } finally {
            //todo try a few retries w/ delay before forcing it to detach
            /*execOperations.exec {
                executable = "hdiutil"
                args = listOf(
                    "detach", mountRoot.absolutePath,
                    "-force"
                )
                isIgnoreExitValue = true
            }*/
        }
    }
}