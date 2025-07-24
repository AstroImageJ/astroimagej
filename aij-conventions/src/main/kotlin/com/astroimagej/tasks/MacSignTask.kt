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

    @TaskAction
    fun signAction() {
        logger.lifecycle("Signing...")

        signImage(inputDir.get().asFile)
    }

    private fun unsign(p: Path) {
        logger.lifecycle("Unsigning ${p.toUri()}")
        execOperations.exec {
            executable = "codesign"
            args = listOf("--remove-signature", p.toAbsolutePath().toString())
        }.rethrowFailure()
    }

    private fun signImage(f: File) {
        val p = f.toPath()

        Files.walk(p).use { content ->
            content.filter(this::shouldSign).forEach { path ->
                val origPerms = ensureCanWrite(path)

                try {
                    unsign(path)
                    sign(path)
                } finally {
                    if (!origPerms.isEmpty()) {
                        Files.setPosixFilePermissions(path, origPerms)
                    }
                }
            }
        }

        sign(p.resolve("Contents/runtime"))

        val framework = p.resolve("Contents/Frameworks")
        if (Files.isDirectory(framework)) {
            val content = Files.list(framework)
            content.forEach { path ->
                sign(path)
            }
        }

        sign(p)
    }

    private fun sign(p: Path) {
        when {
            Files.isDirectory(p) -> {
                signDir(p)
            }
            Files.isRegularFile(p) -> {
                when {
                    Files.isExecutable(p) -> {
                        signExec(p)
                    }
                    else -> {
                        signFile(p)
                    }
                }
            }
        }
    }

    //no entitelements, force
    private fun signDir(p: Path) {
        logger.lifecycle("Signing dir ${p.toUri()}")
        val r = execOperations.exec {
            executable = "codesign"
            args = buildList {
                addAll(
                    listOf("-s", signId(), "-vvvv")
                )
                addAll(
                    listOf("--timestamp", "--options", "runtime")
                )
                /*addAll(
                    listOf("--keychain", keychainProfile.get())
                )*/
                addAll(
                    listOf("--prefix", "com.astroimagej.AstroImageJ.")
                )
                add("--force")
                add(p.toAbsolutePath().toString())
            }
            logger.lifecycle(args.toString())
        }
        //logger.lifecycle(r.toString())
        r.rethrowFailure()
    }

    //no entitelements
    private fun signFile(p: Path) {
        logger.lifecycle("Signing file ${p.toUri()}")
        val r = execOperations.exec {
            executable = "/usr/bin/codesign"
            args = buildList {
                addAll(
                    listOf("-s", signId(), "-vvvv")
                )
                addAll(
                    listOf("--timestamp", "--options", "runtime")
                )
                /*addAll(
                    listOf("--keychain", keychainProfile.get())
                )*/
                addAll(
                    listOf("--prefix", "com.astroimagej.AstroImageJ.")
                )
                add(p.toAbsolutePath().toString())
            }
            logger.lifecycle(args.toString())
        }
        //logger.lifecycle(r.toString())
        r.rethrowFailure()
    }

    private fun signExec(p: Path) {
        logger.lifecycle("Signing exec ${p.toUri()}")
        val r = execOperations.exec {
            executable = "codesign"
            args = buildList {
                addAll(
                    listOf("-s", signId(), "-vvvv")
                )
                addAll(
                    listOf("--timestamp", "--options", "runtime")
                )
                /*addAll(
                    listOf("--keychain", keychainProfile.get())
                )*/
                addAll(
                    listOf("--entitlements", entitlementsFile.get().asFile.absolutePath)
                )
                addAll(
                    listOf("--prefix", "com.astroimagej.AstroImageJ.")
                )
                add(p.toAbsolutePath().toString())
            }
            logger.lifecycle(args.toString())
        }
        //logger.lifecycle(r.toString())
        r.rethrowFailure()
    }

    private fun shouldSign(p: Path): Boolean {
        // jpackage excludes the launcher from this signing
        if (!Files.isRegularFile(p) /*|| otherExcludePaths.contains(p)*/) {
            return false
        }

        if (Files.isExecutable(p) || p.fileName.toString().endsWith(".dylib")) {
            return !p.toString().contains("dylib.dSYM/Contents")
        }

        return false
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

    private fun signId(): String {
        val r = Regex("\\(([^(]+)\\)")
        val m = r.find(signingIdentity.get())
        return m?.value ?: signingIdentity.get()
    }
}