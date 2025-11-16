package com.astroimagej.tasks

import com.sun.jna.platform.win32.Advapi32Util
import com.sun.jna.platform.win32.WinReg
import org.apache.tools.ant.taskdefs.condition.Os
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.provider.Property
import org.gradle.api.provider.ProviderFactory
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecOperations
import javax.inject.Inject

abstract class ModifyWindowsRegistry
@Inject constructor(private var execOperations: ExecOperations,
    providers: ProviderFactory) : DefaultTask() {

    @get:Input
    @get:Optional
    abstract val version: Property<String>

    @get:Input
    @get:Optional
    abstract val appId: Property<String>

    init {
        version.convention(providers.gradleProperty("version"))
        appId.convention("AstroImageJ")
    }

    @TaskAction
    fun execute() {
        if (!Os.isFamily(Os.FAMILY_WINDOWS)) {
            throw GradleException("This task can only be run on Windows!")
        }

        val regKey = Pair(WinReg.HKEY_LOCAL_MACHINE, "Software\\Unknown\\${appId.get()}")

        if (!Advapi32Util.registryKeyExists(regKey.first, regKey.second)) {
            logger.warn("The registry key does not exist!")
            return
        }

        if (Advapi32Util.registryKeyExists(regKey.first, regKey.second + "\\" + version.get())) {
            logger.warn("The registry key for the specified version already exists!")
            return
        }

        logger.info("Renaming registry key for version ${version.get()}...")

        val keysToCopy = Advapi32Util.registryGetKeys(regKey.first, regKey.second)
        if (keysToCopy.isEmpty() || keysToCopy.size > 1) {
            throw GradleException("Expected a single key to be present in the registry!")
        }

        val existingKey = keysToCopy.single()

        // this works Rename-Item -Path "HKLM:\Software\Unknown\AstroImageJ\6.0.0.00" -NewName "5.5.0.00"
        execOperations.exec {
            executable = "powershell.exe"
            args = listOf(
                "-NoProfile",
                "Start-Process",
                "powershell.exe",
                "-Verb",
                "RunAs",
                "-ArgumentList",
                "\'-NoProfile -Command \"Rename-Item -Path \"HKLM:\\${regKey.second}\\$existingKey\" -NewName \"${version.get()}\"\"\'"
            )
            //logger.error("Executing: ${commandLine.joinToString(" ")}")
        }.rethrowFailure()

        logger.lifecycle("Renamed registry key for version ${version.get()}")

        /*println(
            "PowerShell runtime version " +
                    PowerShellExecutor.instance().version().orElseThrow({ RuntimeException("No PowerShell runtime available") })
        )

        val s = PowerShellExecutor.instance().execute(
            "Rename-Item -Path \"HKLM:\\${regKey.second}\\$existingKey\" -NewName \"${version.get()}\""
        )

        if (!s.isSuccess) {
            logger.error(s.errorOutput)
            throw GradleException("Failed to rename registry key")
        }*/
    }
}