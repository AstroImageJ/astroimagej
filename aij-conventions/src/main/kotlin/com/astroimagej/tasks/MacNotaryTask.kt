package com.astroimagej.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.gradle.process.ExecOperations
import javax.inject.Inject

abstract class MacNotaryTask
@Inject constructor(private var execOperations: ExecOperations) : DefaultTask() {
    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val inputDir: DirectoryProperty

    @get:Input
    abstract val keychainProfile: Property<String>

    @TaskAction
    fun notarize() {
        val dmgPath = inputDir.files().singleFile.absolutePath

        logger.lifecycle("Submitting for notarization...")
        val submitResult = execOperations.exec {
            executable = "xcrun"
            args = listOf(
                "notarytool", "submit",
                dmgPath,
                "--keychain-profile", keychainProfile.get(),
                "--wait"
            )
        }

        submitResult.rethrowFailure()

        logger.lifecycle("Stapling...")
        val stapleResult = execOperations.exec {
            executable = "xcrun"
            args = listOf("stapler", "staple", dmgPath)
        }

        stapleResult.rethrowFailure()

        logger.lifecycle("Notarization and stapling complete for $dmgPath")
    }
}