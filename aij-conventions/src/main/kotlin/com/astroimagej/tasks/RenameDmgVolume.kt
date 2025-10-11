package com.astroimagej.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.gradle.process.ExecOperations
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
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
        logger.lifecycle("Converting ${inDmg.absolutePath} -> ${rwDmg.absolutePath} (UDRW)")
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
        logger.lifecycle("Attaching ${rwDmg.absolutePath} under mount point ${mountRoot.absolutePath}")
        execOperations.exec {
            executable = "hdiutil"
            args = listOf(
                "attach", rwDmg.absolutePath,
                "-mountpoint", mountRoot.absolutePath,
                "-nobrowse"
            )
        }

        logger.lifecycle("Mounted volume detected: ${mountRoot.absolutePath}")

        try {
            // Rename the volume
            println("Renaming volume '${mountRoot.name}' -> '${volume.get()}'")
            execOperations.exec {
                executable = "diskutil"
                args = listOf(
                    "rename", mountRoot.absolutePath, volume.get()
                )
            }

            // Check for .VolumeIcon.icns and SetFile availability
            val volumeIconFile = File(mountRoot, ".VolumeIcon.icns")
            val setFileAvailable = execOperations.exec {
                isIgnoreExitValue = true
                executable = "sh"
                args = listOf(
                    "-c", "command -v SetFile >/dev/null 2>&1"
                )
            }.exitValue == 0

            if (volumeIconFile.exists()) {
                logger.lifecycle(".VolumeIcon.icns present: ${volumeIconFile.absolutePath}")
                if (setFileAvailable) {
                    try {
                        // Make writable, set creator icnC, then mark volume as having custom icon
                        volumeIconFile.setWritable(true)
                        execOperations.exec {
                            executable = "SetFile"
                            args = listOf(
                                "-c", "icnC", volumeIconFile.absolutePath
                            )
                        }
                        volumeIconFile.setReadOnly()

                        execOperations.exec {
                            executable = "SetFile"
                            args = listOf(
                                "-a", "C", mountRoot.absolutePath
                            )
                        }
                        logger.lifecycle("Applied SetFile attributes to preserve volume icon.")
                    } catch (ex: Exception) {
                        logger.error("failed to set icon attributes with SetFile: ${ex.message}")
                    }
                }
            }

            // Detach the specific mounted volume
            logger.lifecycle("Detaching ${mountRoot.absolutePath}")
            execOperations.exec {
                executable = "hdiutil"
                args = listOf(
                    "detach", mountRoot.absolutePath
                )
            }

            // Convert back to compressed UDZO into a temporary file
            val converted = inDmg.resolveSibling("${inDmg.nameWithoutExtension}.converted.dmg")
            logger.lifecycle("Converting ${rwDmg.absolutePath} -> ${converted.absolutePath} (UDZO)")
            execOperations.exec {
                executable = "hdiutil"
                args = listOf(
                    "convert", rwDmg.absolutePath,
                    "-format", "UDZO",
                    "-o", converted.absolutePath
                )
            }

            // Replace original DMG safely (backup original)
            val backup = inDmg.resolveSibling("${inDmg.name}.bak")
            //logger.lifecycle("Backing up original DMG to ${backup.absolutePath}")
            //Files.move(inDmg.toPath(), backup.toPath(), StandardCopyOption.REPLACE_EXISTING)
            Files.move(converted.toPath(), inDmg.toPath(), StandardCopyOption.REPLACE_EXISTING)
            logger.lifecycle("Replaced original DMG.")

        } finally {
            /*try {
                attemptDetachWithRetries(mountRoot)
            } catch (ex: Exception) {
                logger.warn("Normal detach retries failed: ${ex.message}. Attempting force detach.")
                attemptForceDetach(mountRoot)
            }*/

            // cleanup temporary rw image and mount root directory
            try { Files.deleteIfExists(rwDmg.toPath()) } catch (_: Exception) {}
            try { mountRoot.deleteRecursively() } catch (_: Exception) {}
        }
    }

    private fun attemptDetachWithRetries(mounted: File) {
        if (!mounted.exists()) return
        val path = mounted.absolutePath
        repeat(10) { attempt ->
            val result = execOperations.exec {
                isIgnoreExitValue = true
                executable = "hdiutil"
                args = listOf("detach", path)
            }
            if (result.exitValue == 0) {
                logger.info("Detached $path on attempt ${attempt + 1}")
                return
            }
            Thread.sleep(6000)
        }
        throw GradleException("Failed to detach $path after retries")
    }

    private fun attemptForceDetach(mountRootDir: File) {
        execOperations.exec {
            isIgnoreExitValue = true
            executable = "hdiutil"
            args = listOf(
                "detach", "-force", mountRootDir.absolutePath
            )
        }
    }
}