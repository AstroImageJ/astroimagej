import com.astroimagej.SigstoreSignFiles
import com.astroimagej.meta.jdk.Architecture.ARM_64
import com.astroimagej.meta.jdk.Architecture.X86_64
import com.astroimagej.meta.jdk.OperatingSystem.*
import com.astroimagej.meta.jdk.RuntimeType
import com.astroimagej.meta.jdk.adoptium.JavaInfo
import com.astroimagej.meta.jdk.cache.JavaRuntimeSystem
import com.astroimagej.tasks.*
import de.undercouch.gradle.tasks.download.Download
import de.undercouch.gradle.tasks.download.Verify
import kotlinx.serialization.json.Json
import org.apache.tools.ant.taskdefs.condition.Os
import org.gradle.kotlin.dsl.support.uppercaseFirstChar
import java.security.MessageDigest
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*
import kotlin.reflect.full.declaredMemberProperties

buildscript {
    repositories {
        // Uncomment to use mavenLocal()
        // mavenLocal()
        /* maven {
            url = uri("https://oss.sonatype.org/content/repositories/snapshots")
        } */
        maven {
            url = uri("gradlePlugins")
        }
        mavenCentral()
    }
}

plugins {
    // Apply the java plugin to add support for Java
    id("java")

    // Plugin to download files
    id("de.undercouch.download") version "5.6.0"

    // Used to download test data
    id("aij.test-conventions")

    id("aij.java-reproducible-builds")

    id("dev.sigstore.sign-base") version "2.3.0"
}

repositories {
    mavenCentral()
}

// See https://guides.gradle.org/creating-multi-project-builds/

// Java version to compile and package with
val shippingJava: Int = providers.gradleProperty("javaShippingVersion").map { it.toInt() }.get()
// Minimum Java version binaries should be compatible with
val targetJava: Int = providers.gradleProperty("minJava").map { it.toInt() }.get()

val shippingIJ = configurations.create("shippingIJ") {
    isCanBeConsumed = false
    isCanBeResolved = true
}

val shippingAstro = configurations.create("shippingAstro") {
    isCanBeConsumed = false
    isCanBeResolved = true
}

dependencies {
    // These are needed to run the tests
    implementation(project(":Nom_Fits"))
    implementation(project(":ij"))
    implementation(project(":Astronomy_"))

    // Jars to be packaged and shipped
    add("shippingIJ", project(mapOf("path" to ":ij", "configuration" to "shippingJar")))
    add("shippingAstro", project(mapOf("path" to ":Astronomy_", "configuration" to "shippingJar")))

    // Use JUnit test framework
    testImplementation(platform("org.junit:junit-bom:5.11.0"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.0")
    testImplementation("org.awaitility:awaitility:4.2.1")
    testImplementation("org.assertj:assertj-core:3.26.3")
    testImplementation("org.assertj:assertj-swing-junit:3.17.1")
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
    }

    systemProperty("enforceEDTValidation", "false")

    javaLauncher.set(javaToolchains.launcherFor { // Force it to use the toolchain java version
        languageVersion.set(JavaLanguageVersion.of(shippingJava))
    })

    jvmArgs(readConfigFile())
    workingDir = file("${projectDir}/AIJ-Run/")

    classpath += files(file("${projectDir}/AIJ-Run/ij.jar"))
}

tasks.test {
    dependsOn("preTest")
    val testPredicate = providers.provider {
        project.hasProperty("runTest")
    }
    onlyIf { testPredicate.get() }
}

/**
 * The set of Java runtimes to download and package AIJ for.
 *
 * Runtimes are discovered through the foojay Disco API, see https://api.foojay.io/swagger-ui
 * Each system can use its own vendor and Java version:
 *  - `vendor`: defaults to zulu
 *  - `javaVersion`: major Java version, defaults to the `javaShippingVersion` property
 *  - `pgpKeyId`: fingerprint of the key the vendor signs releases with. Signature verification is only done
 *    when it is set and foojay lists a signature for the package.
 *
 * The file's name (name), major Java version (version), and SHA256 hash (sha256) are also added to this map. Those entries
 * are excluded from the definition for brevity.
 */
val javaRuntimeSystems = mapOf(
    "mac" to JavaRuntimeSystem(ext = "tar.gz", arch = X86_64, os = MAC, type = RuntimeType.JDK),
    "armMac" to JavaRuntimeSystem(ext = "tar.gz", arch = ARM_64, os = MAC, type = RuntimeType.JDK),
    "linux" to JavaRuntimeSystem(ext = "tar.gz", arch = X86_64, os = LINUX, type = RuntimeType.JDK),
    "armLinux" to JavaRuntimeSystem(ext = "tar.gz", arch = ARM_64, os = LINUX, type = RuntimeType.JDK),
    "windows" to JavaRuntimeSystem(ext = "zip", arch = X86_64, os = WINDOWS, type = RuntimeType.JDK),
)

val javaRuntimeSystemsProperty = project.objects.mapProperty(String::class.java, JavaRuntimeSystem::class.java)

// Wrap the logic in a lazy property that caches the result
javaRuntimeSystemsProperty.convention(providers.provider {
    val hasher = MessageDigest.getInstance("MD5")
    hasher.update(shippingJava.toString().toByteArray())

    javaRuntimeSystems.forEach { (k, v) ->
        hasher.update(k.toByteArray())
        hasher.update(v.ext.toByteArray())
        hasher.update(v.arch.toString().toByteArray())
        hasher.update(v.os.toString().toByteArray())
        hasher.update(v.vendor.toByteArray())
        hasher.update((v.javaVersion ?: shippingJava).toString().toByteArray())
        hasher.update((v.pgpKeyId ?: "").toByteArray())
        hasher.update(JavaRuntimeSystem::class.declaredMemberProperties.toString().toByteArray())
        if (v.type != null) {
            hasher.update(v.type.toString().toByteArray())
        }
    }

    val hash = HexFormat.of().formatHex(hasher.digest())

    val fileName = "javaRuntimeSystems-$hash.json"
    val jsonFile = layout.projectDirectory.file("jres/$fileName").asFile

    // If the JSON file exists and is recent, load the cached data
    if (jsonFile.exists()) {
        val lastModified = jsonFile.lastModified()
        val now = Instant.now().toEpochMilli()
        val daysSinceLastModified =
            ChronoUnit.DAYS.between(
                Instant.ofEpochMilli(lastModified),
                Instant.ofEpochMilli(now)
            )

        if (daysSinceLastModified <= 30) {
            logger.lifecycle("Loading cached Java Runtime Systems data from JSON (last modified $daysSinceLastModified days ago)")
            val cacheData = Json { ignoreUnknownKeys = true }
                .decodeFromString<Map<String, JavaRuntimeSystem>>(jsonFile.readText())
            val matchesRequest = cacheData.keys == javaRuntimeSystems.keys &&
                    cacheData.all { (k, cached) -> cached.vendor == javaRuntimeSystems.getValue(k).vendor }
            if (matchesRequest && cacheData.values.all { it.isResolved() }) {
                cacheData.forEach { (k, cached) ->
                    logger.lifecycle("  $k: ${cached.vendor} ${cached.fullVersion ?: cached.version}")
                }
                return@provider cacheData
            } else {
                logger.lifecycle("Cached Java Runtime Systems data seems to be missing some data, fetching...")
            }
        } else {
            logger.lifecycle("Cached Java Runtime Systems data is older than 30 days, refetching...")
        }
    }

    // Otherwise, query foojay to populate the data
    logger.lifecycle("Fetching Java Runtime Systems data from foojay")
    javaRuntimeSystems.forEach { (key, sysInfo) ->
        val major = sysInfo.javaVersion ?: shippingJava
        val desc = "$key (vendor = ${sysInfo.vendor}, java = $major, ${sysInfo.os}, ${sysInfo.arch}, ${sysInfo.ext})"

        try {
            // Find the latest runtime
            val runtime = JavaInfo.findPackage(
                vendor = sysInfo.vendor,
                majorVersion = major,
                type = sysInfo.type,
                os = sysInfo.os,
                arch = sysInfo.arch,
                ext = sysInfo.ext,
            ) ?: run {
                logger.error("Found no runtime downloads for $desc")
                return@forEach
            }

            // Update the maps with the metadata
            sysInfo.apply {
                version = runtime.version
                fullVersion = runtime.fullVersion
                name = runtime.name
                sha256 = runtime.sha256
                type = runtime.type
                this.url = runtime.url
                sigUrl = runtime.sigUrl
            }
        } catch (e: Exception) {
            logger.error(e.toString())
            logger.warn("A runtime ({}) failed to return from foojay!", desc)
        }
    }

    // Only cache complete data, so a partial failure is retried next time
    if (javaRuntimeSystems.values.all { it.isResolved() }) {
        jsonFile.parentFile.mkdirs() // Ensure the directory exists
        // encodeDefaults so the vendor is always written to the cache, even when it is the default
        jsonFile.writeText(Json { prettyPrint = true; encodeDefaults = true }.encodeToString(javaRuntimeSystems))
    } else {
        logger.warn("Not all Java runtimes could be resolved, the result will not be cached")
    }
    javaRuntimeSystems
})

// Define what files belong to all distributions and their location
val commonDist = project.copySpec {
    // Ensure `shippingAstro` contains exactly one file (Astronomy_.jar)
    if (shippingAstro.files.size != 1) {
        throw GradleException("shippingAstro configuration must contain exactly one file")
    }

    // Ensure `shippingIJ` contains exactly one file (ij.jar)
    if (shippingIJ.files.size != 1) {
        throw GradleException("shippingIJ configuration must contain exactly one file")
    }

    // Copy Astronomy_.jar to correct place in distribution
    from(shippingAstro) {
        rename { "Astronomy_.jar" }
        into("plugins")
    }

    from(layout.projectDirectory.dir("packageFiles/common")) {
        into("")
    }

    into("plugins") {
        from(file("${projectDir}/packageFiles/plugins"))
    }

    // Copy ij.jar to correct place in distribution
    from(shippingIJ) {
        into("")
    }
}

/**
 * Copy the common files into the build directory to simplify their use.
 *
 * This is run when building a distribution to refresh the files in case of a change.
 */
tasks.register<Sync>("commonFiles") {
    with(commonDist)
    into(layout.buildDirectory.dir("commonFiles"))
}

tasks.register<Delete>("cleanRun") {
    delete("${projectDir}/AIJ-Run")
}

tasks.named("clean").configure {
    finalizedBy("cleanRun")
}

// Generate AIJ-Run directory and set it up for usage
tasks.register<Sync>("sync") {
    with(commonDist)

    // Don't consider aij.log when copying common files, AIJ already resets it on launch
    // Doing so allows the UP-TO-DATE check to pass
    exclude("aij.log")

    val prefs = providers.environmentVariable("AIJ_PREFS")
    if (prefs.isPresent) {
        from(prefs.get()) {
            rename { "AIJ_Prefs.txt" }
        }
    }

    destinationDir = file("${projectDir}/AIJ-Run")
}

// Generates a working install directory of AIJ and launches it - make sure to not add it to git!
tasks.register<JavaExec>("aijRun") {
    group = "AstroImageJ Development"

    val runFolder = tasks.named<Sync>("sync").map { it.destinationDir }

    inputs.dir(runFolder)
    workingDir(runFolder)

    jvmArgs = readConfigFile()

    // Force it to use the toolchain java version
    javaLauncher.set(javaToolchains.launcherFor {
        languageVersion.set(JavaLanguageVersion.of(shippingJava))
        vendor.set(JvmVendorSpec.AZUL)
    })

    enableAssertions = true

    // Don't specify main class so gradle reads from the manifest which also reads the native access enabler
    classpath = files(file("${projectDir}/AIJ-Run/ij.jar"))

    openFileInAij()?.let {
        args(it.absolutePath)
    }
}

fun readConfigFile(): List<String> {
    val devCfg = providers.fileContents(project.layout.projectDirectory.file("devLaunchOptions.txt"))
    val args = mutableListOf<String>()

    if (!devCfg.asText.isPresent) {
        logger.lifecycle("Launching using default options")
        return args
    }

    logger.lifecycle("Launching using options from devLaunchOptions")

    devCfg.asText.get().lineSequence().forEach { line ->
        if (line.startsWith("#")) return@forEach
        line.split(" ").filter { it.isNotBlank() }.forEach { arg -> args.add(arg) }
    }
    args.add("-Daij.dev") // Always show full version metadata when running via dev

    logger.lifecycle("Launching with the following arguments: $args")
    return args
}

// Use toolchain for packaging
val packagingJdkToolchain = javaToolchains.launcherFor {
    languageVersion.set(JavaLanguageVersion.of(shippingJava))
    vendor.set(JvmVendorSpec.AZUL)
}

val crossBuildAppImage = providers.gradleProperty("crossBuildAppImage")
    .map { it.toBoolean() }
    .orElse(true)

javaRuntimeSystemsProperty.get().forEach { (_, sysInfo) ->
    if (!sysInfo.isResolved()) {
        logger.warn("Skipping packaging tasks for ${sysInfo.os} ${sysInfo.arch}, its Java runtime was not resolved")
        return@forEach
    }

    val sysId = "${sysInfo.os.toString().uppercaseFirstChar()}_${sysInfo.arch}"
    val packageTaskName = "packageAijFor${sysId}"
    val createAppImageTaskName = "createAppImageFor${sysId}"
    val downloadTaskName = "downloadJavaRuntimeFor${sysId}"
    val downloadSigTaskName = "downloadSigFor${sysId}"
    val verifyTaskName = "verifyJavaRuntimeFor${sysId}"
    val verifySigTaskName = "verifySigFor${sysId}"
    val cleanTaskName = "cleanJavaRuntimeFor${sysId}"
    val notaryTaskName = "notarizeFor${sysId}"
    val installerTaskName = "installerFor${sysId}"
    val getRuntimeTaskName = "getRuntimeFor${sysId}"

    // Each system can use its own vendor and Java version
    val pgpKeyId = sysInfo.pgpKeyId

    val jresDir = layout.projectDirectory.dir("jres").dir(sysId)
    val runtimeFileName = "$sysId-${sysInfo.name}"
    val runtimeSigFileName = "$runtimeFileName.sig"

    fun registerDownload(taskName: String, url: String, fileName: String) = tasks.register<Download>(taskName) {
        mkdir(file("${projectDir}/jres"))

        src(url)
        overwrite(false)
        onlyIfModified(true)
        dest(jresDir.file(fileName).asFile)
    }

    val downloadTask = registerDownload(downloadTaskName, sysInfo.url!!, runtimeFileName)

    val downloadSigTask = sysInfo.sigUrl?.takeIf { pgpKeyId != null }
        ?.let { registerDownload(downloadSigTaskName, it, runtimeSigFileName) }

    if (pgpKeyId == null) {
        logger.lifecycle("No PGP key known for vendor ${sysInfo.vendor}, only checksums will be verified for $sysId")
    } else if (sysInfo.sigUrl == null) {
        logger.warn("Vendor ${sysInfo.vendor} publishes no signature for $sysId, only the checksum will be verified")
    }

    val bundledRuntimeFile = layout.file(downloadTask.map { it.outputFiles.single() })
    downloadTask {
        finalizedBy(cleanTaskName, verifyTaskName)
    }
    downloadSigTask?.configure {
        finalizedBy(verifySigTaskName)
    }
    tasks.register<Delete>(cleanTaskName) {
        val directory = layout.projectDirectory.dir("jres").dir(sysId)

        delete(directory.asFileTree.matching {
            include("$sysId*")
            exclude(runtimeFileName)
            exclude(runtimeSigFileName)
        })
    }

    tasks.register<Verify>(verifyTaskName) {
        inputs.file(bundledRuntimeFile)
        src(bundledRuntimeFile)
        algorithm("SHA256")
        checksum(sysInfo.sha256)
    }

    if (downloadSigTask != null) {
        tasks.register<PGPVerify>(verifySigTaskName) {
            val signatureFile = layout.file(downloadSigTask.map { it.outputFiles.single() })

            file = bundledRuntimeFile
            signature = signatureFile
            keyId = pgpKeyId!!
        }
    }

    val getRuntimeTask = tasks.register<CreateJavaRuntimeTask>(getRuntimeTaskName) {
        launcher = packagingJdkToolchain
        fromRuntimeInfo(sysInfo)

        bundledRuntime = bundledRuntimeFile
        //bundledJmods = unpackedJmodDir // Disabled

        outputDir = layout.projectDirectory.dir("jres/$sysId/runtime")
    }

    val downloadedAppImage = layout.projectDirectory.dir("images/$sysId")

    val appImageDir: Provider<Directory> = if (crossBuildAppImage.get()) {
        val packageTask = tasks.register<CreateAppImageTask>(createAppImageTaskName) {
            group = "distribution"

            inputs.files(layout.projectDirectory.dir("packageFiles/assets/associations").asFileTree)
                .optional()
                .withPropertyName("File associations")
            inputs.files(layout.projectDirectory.dir("packageFiles/assets/${sysInfo.os}").asFileTree)
                .optional()
                .withPropertyName("Resource overrides")

            resourcesDir = layout.projectDirectory.dir("packageFiles/assets/${sysInfo.os}")

            targetOs = sysInfo.os

            targetArch = sysInfo.arch

            appName.set("AstroImageJ")

            mainJarName.set("ij.jar")

            appVersion = version.toString()

            javaOpts("-XX:MaxRAMPercentage=75")
            javaOpts("--enable-native-access=ALL-UNNAMED")
            javaOpts("-Duser.dir=\$APPDIR")

            inputDir = tasks.named<Sync>("commonFiles").map { it.destinationDir }

            runtime = getRuntimeTask.flatMap { it.outputDir }

            launcher = packagingJdkToolchain

            val suffix = if (sysInfo.os == WINDOWS) ".exe" else ""
            appLauncher = layout.projectDirectory.file("packageFiles/assets/launchers/${sysId}/JavaLauncher$suffix")

            outputDir.set(layout.buildDirectory.dir("distributions/images/$sysId"))
        }

        if (downloadedAppImage.asFile.exists()) {
            logger.lifecycle("Using prebuilt app image: ${downloadedAppImage.asFile.absolutePath}")

            providers.provider { downloadedAppImage }
        } else {
            packageTask.map { it.outputDir.get() }
        }
    } else {
        val packageTask = tasks.register<JPackageTask>(createAppImageTaskName) {
            group = "distribution"

            enabled = when (sysInfo.os) {
                MAC -> Os.isFamily(Os.FAMILY_MAC)
                WINDOWS -> Os.isFamily(Os.FAMILY_WINDOWS)
                LINUX -> Os.isFamily(Os.FAMILY_UNIX) && !Os.isFamily(Os.FAMILY_MAC)
            } && version.toString()
                .matches(Regex("^(?<major>0|[1-9]\\d*)\\.(?<minor>0|[1-9]\\d*)\\.(?<patch>0|[1-9]\\d*)\\.(00)"))

            inputs.files(layout.projectDirectory.dir("packageFiles/assets/associations").asFileTree)
                .optional()
                .withPropertyName("File associations")
            inputs.files(layout.projectDirectory.dir("packageFiles/assets/${sysInfo.os}").asFileTree)
                .optional()
                .withPropertyName("Resource overrides")

            appName.set("AstroImageJ")

            inputDir = tasks.named<Sync>("commonFiles").map { it.destinationDir }

            // Specify the name of your main jar within that inputDir
            mainJarName.set("ij.jar")

            targetOs = sysInfo.os

            extraArgs = listOf(
                "--java-options", "-Duser.dir=\$APPDIR",
                "--resource-dir", layout.projectDirectory.dir("packageFiles/assets/${sysInfo.os}").asFile.absolutePath,
                //"--temp", layout.buildDirectory.dir("temp").map { it.asFile.absolutePath }.get(),
                //"--verbose",
                "--java-options", "-XX:MaxRAMPercentage=75", "--enable-native-access=ALL-UNNAMED"
            )

            launcher = packagingJdkToolchain

            extraArgs(when (sysInfo.os) {
                MAC -> {
                    buildList {
                        addAll(
                            listOf(
                                "--type", "app-image",
                                "--mac-package-identifier", "com.astroimagej.AstroImageJ",
                                //"--about-url", "https://astroimagej.com",
                                //"--license-file", layout.projectDirectory.file("LICENSE").asFile.absolutePath,
                                "--mac-app-store",
                                // Mac must be 3 ints for the version number
                                "--app-version", version.toString().split(".").take(3).joinToString("."),
                            )
                        )

                        if (System.getenv("DeveloperId") != null &&
                            project.property("codeSignAndNotarize").toString().toBoolean()) {
                            addAll(
                                listOf(
                                    "--mac-sign",
                                    "--mac-signing-key-user-name", System.getenv("DeveloperId"),
                                )
                            )
                        }
                    }
                }
                LINUX -> {
                    listOf(
                        "--type", "app-image",
                        "--app-version", version.toString(),
                        //"--linux-shortcut",
                    )
                }
                WINDOWS -> {
                    listOf(
                        "--type", "app-image",
                        "--app-version", version.toString(),
                    )
                }
            })

            // Add file associations
            if (sysInfo.os == MAC) { // app-image type cannot have file associations
                layout.projectDirectory.dir("packageFiles/assets/associations").asFileTree.forEach {
                    extraArgs(listOf("--file-associations", it.absolutePath))
                }
            }

            runtime = getRuntimeTask.flatMap { it.outputDir }

            outputDir.set(layout.buildDirectory.dir("distributions/images/$sysId"))
        }

        val replaceExecTask = tasks.register<Copy>("replaceLauncherFor$sysId") {
            inputs.dir(packageTask.flatMap { it.outputDir })
            mustRunAfter(packageTask)

            val suffix = if (sysInfo.os == WINDOWS) ".exe" else ""
            from(layout.projectDirectory.file("packageFiles/assets/launchers/${sysId}/JavaLauncher$suffix")) {
                rename {
                    "AstroImageJ$suffix"
                }
                filePermissions {
                    user.execute = true
                    other.execute = true
                    group.execute = true
                }
            }

            when (sysInfo.os) {
                MAC -> into(packageTask.map { it.outputDir.get() }.map { it.dir("AstroImageJ.app/Contents/MacOS") })
                LINUX -> into(packageTask.map { it.outputDir.get() }.map { it.dir("astroimagej/bin") })
                WINDOWS -> into(packageTask.map { it.outputDir.get() }.map { it.dir("AstroImageJ") })
            }
        }

        packageTask {
            finalizedBy(replaceExecTask)
        }

        packageTask.map { it.outputDir.get() }
    }

    val fixJPackageMetadataTask = tasks.register<FixJPackageMetadataTask>("fixJpackageMetadataFor$sysId") {
        enabled = crossBuildAppImage.get()

        targetOs = sysInfo.os

        launcher = packagingJdkToolchain

        when (sysInfo.os) {
            WINDOWS -> {
                inputs.dir(appImageDir.map { it.dir("AstroImageJ") })
                inputDir.set(appImageDir.map { it.dir("AstroImageJ") })
            }
            LINUX -> {
                inputs.dir(appImageDir.map { it.dir("astroimagej") })
                inputDir.set(appImageDir.map { it.dir("astroimagej") })
            }
            MAC -> {
                inputs.dir(appImageDir.map { it.dir("AstroImageJ.app") })
                inputDir.set(appImageDir.map { it.dir("AstroImageJ.app") })
            }
        }
    }

    val installerTask = tasks.register<JPackageTask>(installerTaskName) {
        if (!crossBuildAppImage.get()) {
            mustRunAfter(tasks.named("replaceLauncherFor$sysId"))
            if (sysInfo.os == MAC) {
                mustRunAfter(tasks.named("signFor$sysId"))
            }
        } else {
            dependsOn(fixJPackageMetadataTask)
            mustRunAfter(fixJPackageMetadataTask)
            if (sysInfo.os == MAC) {
                mustRunAfter(tasks.named("signFor$sysId"))
            }
        }

        appName.set("AstroImageJ")

        targetOs = sysInfo.os

        extraArgs = listOf(
            "--resource-dir", layout.projectDirectory.dir("packageFiles/assets/${sysInfo.os}").asFile.absolutePath,
            //"--verbose",
        )

        launcher = packagingJdkToolchain

        outputDir.set(layout.buildDirectory.dir("distributions/$sysId"))

        when (sysInfo.os) {
            MAC -> {
                inputs.dir(appImageDir.map { it.dir("AstroImageJ.app") })
                inputDir.set(appImageDir.map { it.dir("AstroImageJ.app") })

                extraArgs(buildList {
                    addAll(
                        listOf(
                            "--type", "pkg",
                            "--mac-package-identifier", "com.astroimagej.AstroImageJ",
                            "--about-url", "https://astroimagej.com",
                            "--license-file", layout.projectDirectory.file("LICENSE").asFile.absolutePath,
                            "--mac-app-store",
                            // Mac must be 3 ints for the version number
                            "--app-version", version.toString().split(".").take(3).joinToString("."),
                        )
                    )

                    if (System.getenv("DeveloperId") != null &&
                        project.property("codeSignAndNotarize").toString().toBoolean()) {
                        addAll(
                            listOf(
                                "--mac-sign",
                                "--mac-signing-key-user-name", System.getenv("DeveloperId"),
                            )
                        )
                    }
                })
            }
            LINUX -> {
                inputs.dir(appImageDir.map { it.dir("astroimagej") })
                inputDir.set(appImageDir.map { it.dir("astroimagej") })
                extraArgs(listOf(
                    "--app-version", version.toString(),
                ))
            }
            WINDOWS -> {
                inputs.dir(appImageDir.map { it.dir("AstroImageJ") })
                inputDir.set(appImageDir.map { it.dir("AstroImageJ") })

                extraArgs(listOf(
                    "--type", "msi",
                    "--win-dir-chooser",
                    "--win-help-url", "https://github.com/AstroImageJ/astroimagej/discussions?discussions_q=",
                    "--win-shortcut",
                    "--win-menu",
                    "--win-shortcut-prompt",
                    "--win-update-url", "https://astroimagej.com",
                    "--about-url", "https://astroimagej.com",
                    "--license-file", layout.projectDirectory.file("LICENSE").asFile.absolutePath,
                    "--win-upgrade-uuid", "83f529ac-39a3-4fe7-9f97-e9f259321c26",
                    "--app-version", version.toString(),
                ))

                layout.projectDirectory.dir("packageFiles/assets/associations").asFileTree.forEach {
                    extraArgs(listOf("--file-associations", it.absolutePath))
                }
            }
        }
    }

    if (sysInfo.os == MAC) {
        val signAppImage = tasks.register<MacSignTask>("signFor$sysId") {
            enabled = providers.environmentVariable("DeveloperId").isPresent &&
                    project.property("codeSignAndNotarize").toString().toBoolean() &&
                    Os.isFamily(Os.FAMILY_MAC) && sysInfo.os == MAC
            mustRunAfter(fixJPackageMetadataTask)

            inputs.dir(appImageDir)

            inputDir.set(appImageDir.map { it.dir("AstroImageJ.app") })

            signingIdentity = providers.environmentVariable("DeveloperId")
            entitlementsFile = layout.projectDirectory.file("packageFiles/assets/${sysInfo.os}/entitlements.plist")
            inheritEntitlementsFile = layout.projectDirectory.file("packageFiles/assets/${sysInfo.os}/inheritEntitlements.plist")
        }

        val notaryTask = tasks.register<MacNotaryTask>(notaryTaskName) {
            enabled = System.getenv("DeveloperId") != null &&
                    project.property("codeSignAndNotarize").toString().toBoolean() &&
                    Os.isFamily(Os.FAMILY_MAC) && sysInfo.os == MAC
            mustRunAfter(installerTask)
            inputDir.set(installerTask.map { it.outputDir.get() })
            keychainProfile = "AC_PASSWORD"
        }

        installerTask {
            finalizedBy(notaryTask)
        }

        if (!crossBuildAppImage.get()) {
            tasks.named("replaceLauncherFor$sysId") {
                finalizedBy(signAppImage)
            }
        } else {
            tasks.named(createAppImageTaskName) {
                finalizedBy(signAppImage)
            }

            if (downloadedAppImage.asFile.exists()) {
                tasks.named(installerTaskName).configure {
                    dependsOn(signAppImage)
                }
            }
        }
    }

    if (Os.isFamily(Os.FAMILY_UNIX) && sysInfo.os == LINUX && !Os.isFamily(Os.FAMILY_MAC)) {
        val bundleTask = tasks.register<Tar>(packageTaskName) {
            if (!crossBuildAppImage.get()) {
                mustRunAfter(tasks.named("replaceLauncherFor$sysId"))
            } else {
                dependsOn(fixJPackageMetadataTask)
            }

            destinationDirectory = layout.buildDirectory.dir("distributions/$sysId")
            archiveBaseName = "AstroImageJ"
            archiveVersion = version.toString()
            compression = Compression.GZIP

            from(appImageDir) {
                exclude("*.tgz")

                filePermissions {
                    user.execute = true
                    other.execute = true
                    group.execute = true
                }
            }
        }
    } else {
        tasks.register(packageTaskName) {
            dependsOn(installerTask)
        }
    }

    if (sysInfo.matchesCurrentSystem()) {
        tasks.register<Copy>("copyAppImage") {
            group = "AstroImageJ Development"
            if (!crossBuildAppImage.get()) {
                mustRunAfter(tasks.named("replaceLauncherFor$sysId"))
            }
            if (Os.isFamily(Os.FAMILY_WINDOWS)) {
                finalizedBy("changeRegistryVersion")
            }

            // cause copyAppImage to always run, even if no compile changes
            doNotTrackState("Always copy app image to destination")

            // Workaround for config cache by using provider outside of onlyIf
            val predicate = providers.provider {
                val p = when (sysInfo.os) {
                    LINUX -> "lib/app/ij.jar"
                    WINDOWS -> "app/ij.jar"
                    MAC -> ""
                }
                val properDirectory = layout.projectDirectory.file("appImageLocation.txt").asFile.exists() &&
                        (sysInfo.os == com.astroimagej.meta.jdk.OperatingSystem.MAC || outputDestination().toPath().resolve(p).toFile().exists())
                if (!properDirectory) {
                    logger.error("[copyAppImage] Was not given the correct path! Must be the absolute path to the folder containing ij.jar!")
                }
                properDirectory
            }

            onlyIf {
                predicate.get()
            }

            val destination = outputDestination()
            val appImageName = when (sysInfo.os) {
                LINUX -> "astroimagej"
                WINDOWS -> "AstroImageJ"
                MAC -> "AstroImageJ.app"
            }

            if (sysInfo.os == com.astroimagej.meta.jdk.OperatingSystem.MAC) {
                from(appImageDir) {
                    into("")
                }
            } else {
                from(appImageDir.map { it.dir(appImageName) }) {
                    into("")
                }
            }
            into(destination)

            doLast {
                logger.quiet("[copyAppImage] Copied app image to destination...")
            }
        }
    }
}

fun outputDestination(): File {
    val locCfg = file("${projectDir}/appImageLocation.txt")
    var destination = "${projectDir}/out"

    if (locCfg.exists()) {
        locCfg.readLines().forEach {
            if (it.startsWith("#")) { // Comments
                return@forEach
            }
            destination = it
        }
    }

    return file(destination)
}

fun openFileInAij(): File? {
    val locCfg = file("${projectDir}/openImage.txt")
    var destination: String? = null

    if (locCfg.exists()) {
        locCfg.readLines().forEach {
            if (it.startsWith("#")) { // Comments
                return@forEach
            }
            destination = it
        }
    }

    return destination?.let { file(it) }
}

// We don't have reproducible build fully working,
// so override the files used to match what are uploaded
// Override on command line via -PupdateMetadataFiles="pathToFolder"
val updaterFiles = providers.gradleProperty("updateMetadataFiles")
    .flatMap {
        providers.provider { layout.files(layout.projectDirectory.dir(it).asFileTree) }
    }

val signTask = tasks.register<SigstoreSignFiles>("signAssets") {
    signatureDirectory = layout.projectDirectory.dir("website/public/meta/signatures")
        .dir(providers.gradleProperty("version"))

    filesToSign = files(updaterFiles)
}

tasks.register<GenerateMetadata>("updateMetadata") {
    version = providers.gradleProperty("version")
    releaseType = providers.gradleProperty("releaseType")
    specificJson = layout.projectDirectory.file("website/public/meta/versions/${version.get()}.json")
    generalJson = layout.projectDirectory.file("website/public/meta/versions.json")
    baseMetaUrl = "https://astroimagej.com/meta"
    updateDataJson = layout.projectDirectory.file("packageFiles/assets/github/updateData.json")
    baseArtifactUrl = "https://github.com/AstroImageJ/astroimagej/releases/download"

    // Make sure Gradle knows to run signTask first
    inputs.files(signTask.map { task -> task.signatureDirectory.asFileTree })
        .withPropertyName("signatures")

    files = layout.files(updaterFiles, signTask.map { it.signatureDirectory.asFileTree })
}

tasks.register<TriggerReleaseWorkflow>("triggerReleaseWorkflow") {

}

tasks.register<ModifyWindowsRegistry>("changeRegistryVersion") {
    enabled = Os.isFamily(Os.FAMILY_WINDOWS)
}

// Make Idea's hammer icon run copyAppImage
tasks.named("classes").configure {
    finalizedBy("copyAppImage")
}

tasks.register<VerifyLaunchersTask>("verifyLaunchers") {
    group = "verification"
    description = "Verifies launcher binaries match their os/arch, Windows exes are signed, and unix exes are executable."
    launchersDir.set(layout.projectDirectory.dir("packageFiles/assets/launchers"))
}

// Run as part of `gradle check` when the lifecycle task exists.
tasks.named("check").configure {
    dependsOn("verifyLaunchers")
}