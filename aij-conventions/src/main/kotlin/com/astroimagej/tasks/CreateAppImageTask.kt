package com.astroimagej.tasks

import com.astroimagej.meta.jdk.Architecture
import com.astroimagej.meta.jdk.OperatingSystem
import org.gradle.api.DefaultTask
import org.gradle.api.file.Directory
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.*
import org.gradle.jvm.toolchain.JavaLauncher
import org.gradle.jvm.toolchain.JavaToolchainService
import org.gradle.kotlin.dsl.getByType
import javax.inject.Inject


abstract class CreateAppImageTask
@Inject constructor(
    private var fileOperations: FileSystemOperations,
    ) : DefaultTask() {

    @get:Input
    abstract val appName: Property<String>

    @get:Input
    abstract val targetOs: Property<OperatingSystem>

    @get:Input
    abstract val targetArch: Property<Architecture>

    // The name of the main JAR inside the input directory
    @get:Input
    @get:Optional
    abstract val mainJarName: Property<String>

    @get:Input
    abstract val appVersion: Property<String>

    // Additional args to use for launching the application
    @get:Input
    @get:Optional
    abstract val javaOpts: ListProperty<String>

    // Directory containing jars/resources to include
    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val inputDir: DirectoryProperty

    @get:InputDirectory
    @get:Optional
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val fileProperties: DirectoryProperty

    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val resourcesDir: DirectoryProperty

    // Overridden Launcher
    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val appLauncher: RegularFileProperty

    @get:Nested
    @get:Optional
    abstract val launcher: Property<JavaLauncher>

    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val runtime: DirectoryProperty

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    init {
        // Configure default toolchain
        val toolchain = project.extensions.getByType<JavaPluginExtension>().toolchain
        val defaultLauncher = javaToolchainService.launcherFor(toolchain)
        launcher.convention(defaultLauncher)

        // Default extraArgs to empty list
        javaOpts.convention(emptyList())
    }

    @TaskAction
    // https://docs.oracle.com/en/java/javase/24/jpackage/packaging-tool-user-guide.pdf
    fun createAppImage() {
        val destDir = when (targetOs.get()) {
            OperatingSystem.MAC -> outputDir.dir(appName.get() + ".app")
            OperatingSystem.LINUX -> outputDir.dir(appName.get().lowercase())
            OperatingSystem.WINDOWS -> outputDir.dir(appName.get())
        }.get()

        val execDir = when (targetOs.get()) {
            OperatingSystem.MAC -> destDir.dir("Contents/MacOS")
            OperatingSystem.LINUX -> destDir.dir("bin")
            OperatingSystem.WINDOWS -> destDir
        }

        val appDir = when (targetOs.get()) {
            OperatingSystem.MAC -> destDir.dir("Contents/app")
            OperatingSystem.LINUX -> destDir.dir("lib/app")
            OperatingSystem.WINDOWS -> destDir.dir("app")
        }

        val runtimeDir = when (targetOs.get()) {
            OperatingSystem.MAC -> destDir.dir("Contents/runtime")
            OperatingSystem.LINUX -> destDir.dir("lib/runtime")
            OperatingSystem.WINDOWS -> destDir.dir("runtime")
        }

        // Clear dest
        fileOperations.delete {
            delete(outputDir)
        }

        // Copy runtime
        fileOperations.copy {
            from(getRuntime(runtime.get()))
            into(runtimeDir)
        }

        // Copy app launcher
        fileOperations.copy {
            from(appLauncher)
            into(execDir)
            rename {
                when (targetOs.get()) {
                    OperatingSystem.MAC -> appName.get()
                    OperatingSystem.LINUX -> appName.get()
                    OperatingSystem.WINDOWS -> appName.get() + ".exe"
                }
            }
            filePermissions {
                user.execute = true
                other.execute = true
                group.execute = true
            }
        }

        // Copy app resources
        fileOperations.copy {
            from(inputDir)
            into(appDir)
        }

        createLauncherCfg(appDir)

        // OS-specific resources
        when (targetOs.get()) {
            OperatingSystem.MAC -> {
                createInfoPlist(destDir)
                fileOperations.copy {
                    from(resourcesDir.file("PkgInfo"))
                    into(destDir.dir("Contents"))
                }
                val icon = resourcesDir.file("${appName.get()}.icns")
                if (icon.isPresent) {
                    fileOperations.copy {
                        from(icon)
                        into(destDir.dir("Contents/Resources"))
                    }
                }
            }
            OperatingSystem.LINUX -> {
                if (resourcesDir.isPresent) {
                    val icon = resourcesDir.file("${appName.get()}.png")
                    if (icon.isPresent) {
                        fileOperations.copy {
                            from(icon)
                            into(destDir.dir("lib"))
                        }
                    }
                }
            }
            OperatingSystem.WINDOWS -> {}
        }

        createJpackageXml(appDir)
    }

    // Create .jpackage.xml file so that jpackage will create the installers
    // https://github.com/openjdk/jdk/blob/master/src/jdk.jpackage/share/classes/jdk/jpackage/internal/AppImageFile.java#L110
    private fun createJpackageXml(appDir: Directory) {
        val osString = when (targetOs.get()) {
            OperatingSystem.WINDOWS -> "windows"
            OperatingSystem.LINUX -> "linux"
            OperatingSystem.MAC -> "macOS"
        }

        // Get java version
        /*val version = Properties().let {
            it.load(launcher.get().metadata.installationPath.file("release").asFile.inputStream())
            it.getProperty("JAVA_VERSION").replace("\"", "")
        }*/

        val text = """
            <?xml version="1.0" ?>
            <jpackage-state version="${'$'}VERSION" platform="${osString}">
              <app-version>${appVersion.get()}</app-version>
              <main-launcher>AstroImageJ</main-launcher>
              <main-class>ij.ImageJ</main-class>
              <signed>${targetOs.get() == OperatingSystem.MAC}</signed>
              <app-store>${targetOs.get() == OperatingSystem.MAC}</app-store></jpackage-state>
        """.trimIndent()

        appDir.file(".jpackage.xml").asFile.writeText(text)
    }

    // Limited writing of launcher config
    private fun createLauncherCfg(dest: Directory) {
        val sb = StringBuilder()
        sb.appendLine("[Application]")
        if (mainJarName.isPresent) {
            sb.appendLine("app.mainjar=${mainJarName.get()}")
        }
        //todo classpath?

        sb.appendLine()

        sb.appendLine("[JavaOptions]")
        sb.appendLine("java-options=-Djpackage.app-version=${appVersion.get()}")

        javaOpts.get().forEach {
            sb.appendLine("java-options=$it")
        }

        dest.file("${appName.get()}.cfg").asFile.writeText(sb.toString())
    }

    private fun createInfoPlist(dest: Directory) {
        //todo create this file properly to handle new file associations
        fileOperations.copy {
            from(resourcesDir.file("Info.plist")) {
                expand(mapOf(
                    "VERSION" to appVersion.get(),
                ))
            }
            into(dest.dir("Contents"))
        }
    }

    /**
     * Allows setting extra args lazily via a Provider
     */
    fun javaOpts(provider: Provider<List<String>>) {
        javaOpts.addAll(provider)
    }

    /**
     * Convenience to append immediate args
     */
    fun javaOpts(vararg args: String) {
        javaOpts.addAll(args.asList())
    }

    /**
     * Convenience to append immediate args
     */
    fun javaOpts(args: Collection<String>) {
        javaOpts.addAll(args)
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