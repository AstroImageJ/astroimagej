package com.astroimagej.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.gradle.api.tasks.Optional
import org.gradle.process.ExecOperations
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.PosixFilePermission
import java.util.*
import javax.inject.Inject


// Adapted from https://github.com/openjdk/jdk/blob/master/src/jdk.jpackage/macosx/classes/jdk/jpackage/internal/AppImageSigner.java
abstract class MacSignTask
@Inject constructor(private var execOperations: ExecOperations) : DefaultTask() {
    /*@get:Input
    abstract val keychainProfile: Property<String>*/

    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val inputDir: DirectoryProperty

    @get:Input
    abstract val signingIdentity: Property<String>

    @get:InputFile
    @get:Optional
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val entitlementsFile: RegularFileProperty

    @get:InputFile
    @get:Optional
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val inheritEntitlementsFile: RegularFileProperty

    private val prefix = "com.astroimagej.AstroImageJ."

    @TaskAction
    fun signAction() {
        logger.lifecycle("Signing...")

        // Make sure the launcher is executable
        inputDir.get().file("Contents/MacOS/AstroImageJ").asFile.setExecutable(true, false)

        signImage(inputDir.get().asFile)
    }

    private fun unsign(p: Path) {
        logger.lifecycle("Unsigning ${p.toUri()}")
        execOperations.exec {
            executable = "/usr/bin/codesign"
            args = listOf("--remove-signature", p.toAbsolutePath().toString())
        }.rethrowFailure()
    }

    private fun signImage(f: File) {
        val p = f.toPath()

        // Use inherited entitlements if available
        val inheritEntitlements = if (inheritEntitlementsFile.isPresent) {
            inheritEntitlementsFile.get().asFile.absolutePath
        } else {
            entitlementsFile.orNull?.asFile?.absolutePath
        }

        // Walk bundle to sign extracted files (executables, dylibs, jars)
        Files.walk(p).use { content ->
            content.filter { shouldSign(it, p) }.forEach { path ->
                // skip symlinks
                if (Files.isSymbolicLink(path)) {
                    logger.lifecycle("Ignoring symlink: $path")
                    return@forEach
                }

                // ensure writable
                val origPerms = ensureCanWrite(path)
                try {
                    // remove existing signature (ignore failure)
                    unsign(path)

                    // If jar -> extract .dylib, sign them and re-add into jar
                    if (path.fileName.toString().endsWith(".jar")) {
                        //Extract dylibs in jar to sign, then readd to the jar
                    } else {
                        sign(path, inheritEntitlements)
                    }
                } finally {
                    if (!origPerms.isEmpty()) {
                        Files.setPosixFilePermissions(path, origPerms)
                    }
                }
            }
        }

        // Sign the runtime directory (use inherited entitlements)
        val runtime = p.resolve("Contents/runtime")
        if (Files.isDirectory(runtime)) {
            sign(runtime, inheritEntitlements)
        }

        // Sign frameworks
        val framework = p.resolve("Contents/Frameworks")
        if (Files.isDirectory(framework)) {
            Files.list(framework).forEach { path ->
                sign(path, inheritEntitlements)
            }
        }

        // Sign the app bundle itself with the main entitlements
        val mainEnt = if (entitlementsFile.isPresent) entitlementsFile.get().asFile.absolutePath else null
        sign(p, mainEnt)
    }

    private fun shouldSign(p: Path, bundleRoot: Path): Boolean {
        // Skip anything that is not a regular file or a jar
        if (!Files.isRegularFile(p)) {
            return false
        }

        // Exclude the launcher from the walk (jpackage excludes)
        val launcherRel = bundleRoot.resolve("Contents/MacOS/AstroImageJ").toAbsolutePath()
        if (p.toAbsolutePath() == launcherRel) {
            return false
        }

        // Exclude dSYM content for dylib.dSYM/Contents
        if (p.toString().contains("dylib.dSYM/Contents")) {
            return false
        }

        val name = p.fileName.toString()
        return Files.isExecutable(p) || name.endsWith(".dylib") || name.endsWith(".jar")
    }

    private fun ensureCanWrite(path: Path): MutableSet<PosixFilePermission> {
        try {
            val origPerms = Files.getPosixFilePermissions(path)
            if (origPerms.contains(PosixFilePermission.OWNER_WRITE)) {
                return mutableSetOf()
            } else {
                val newPerms = EnumSet.copyOf(origPerms)
                newPerms.add(PosixFilePermission.OWNER_WRITE)
                Files.setPosixFilePermissions(path, newPerms)
                return origPerms
            }
        } catch (ex: IOException) {
            throw ex
        }
    }

    private fun sign(path: Path, entitlements: String?) {
        sign(path.toAbsolutePath().toString(), entitlements)
    }

    private fun sign(targetAbsPath: String, entitlements: String?) {
        val argsList = mutableListOf<String>()

        argsList.addAll(listOf("--force", "--timestamp", "-vvvv", "--options", "runtime"))
        argsList.addAll(listOf("--sign", signId()))

        if (!entitlements.isNullOrBlank()) {
            argsList.addAll(listOf("--entitlements", entitlements))
        }

        argsList.addAll(listOf("--prefix", prefix))

        argsList.add(targetAbsPath)

        logger.lifecycle("codesign with ${when {
            entitlements == null -> "no"
            entitlements.contains("inherit") -> "inherited"
            else -> "primary"
        }
        } entitlements $targetAbsPath")

        val r = execOperations.exec {
            executable = "/usr/bin/codesign"
            args = argsList
        }
        r.rethrowFailure()
    }

    private fun signId(): String {
        // if the signing identity includes a parenthesized team, return the content inside parentheses (TEAMID),
        // otherwise return the full signing identity string
        val r = Regex("\\(([^(]+)\\)")
        val m = r.find(signingIdentity.get())
        return m?.value ?: signingIdentity.get()
    }
}