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
import java.net.URI
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

    id("dev.sigstore.sign-base") version "2.0.0-rc1"
}

repositories {
    mavenCentral()
}

// See https://guides.gradle.org/creating-multi-project-builds/

// Java version to compile and package with
val shippingJava = (properties["javaShippingVersion"] as String).toInt()
// Minimum Java version binaries should be compatible with
val targetJava = (properties["minJava"] as String).toInt()

configurations {
    create("shippingIJ") {
        isCanBeConsumed = false
        isCanBeResolved = true
    }

    create("shippingAstro") {
        isCanBeConsumed = false
        isCanBeResolved = true
    }
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
 * Terms in the map are based on the query parameters.
 * To add a distribution, simply give it a unique name and fill out its map with the proper values.
 * See https://api.adoptium.net/q/swagger-ui/#/Assets/getLatestAssets
 *
 * The file's name (name), major Java version (version), and SHA256 hash (sha256) are also added to this map. Those entries
 * are excluded from the definition for brevity.
 */
val javaRuntimeSystems = mapOf(
    "mac" to JavaRuntimeSystem(ext = "tar.gz", arch = X86_64, os = MAC, type = RuntimeType.JDK),
    "armMac" to JavaRuntimeSystem(ext = "tar.gz", arch = ARM_64, os = MAC, type = RuntimeType.JDK),
    "linux" to JavaRuntimeSystem(ext = "tar.gz", arch = X86_64, os = LINUX),
    "windows" to JavaRuntimeSystem(ext = "zip", arch = X86_64, os = WINDOWS),
)

val javaRuntimeSystemsProperty = project.objects.mapProperty(String::class.java, JavaRuntimeSystem::class.java)

// Wrap the logic in a lazy property that caches the result
javaRuntimeSystemsProperty.convention(providers.provider {
    val hasher = MessageDigest.getInstance("MD5")

    javaRuntimeSystems.forEach { (k, v) ->
        hasher.update(k.toByteArray())
        hasher.update(v.ext.toByteArray())
        hasher.update(v.arch.toString().toByteArray())
        hasher.update(v.os.toString().toByteArray())
        hasher.update(JavaRuntimeSystem::class.declaredMemberProperties.toString().toByteArray())
        if (v.type != null) {
            hasher.update(v.type.toString().toByteArray())
        }
    }

    val hash = HexFormat.of().formatHex(hasher.digest())

    val fileName = "javaRuntimeSystems-${shippingJava}-$hash.json"
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
            logger.lifecycle("Loading cached Java Runtime Systems data from JSON for Java version $shippingJava (last modified $daysSinceLastModified days ago)")
            val cacheData = Json.decodeFromString<Map<String, JavaRuntimeSystem>>(jsonFile.readText())
            if (cacheData.values.all { it.version != null }) {
                return@provider cacheData
            } else {
                logger.lifecycle("Cached data for Java version $shippingJava seems to be missing some data, fetching...")
            }
        } else {
            logger.lifecycle("Cached data for Java version $shippingJava is older than 30 days, refetching...")
        }
    }

    // Otherwise, simulate a network query to populate the data
    logger.lifecycle("Fetching Java Runtime Systems data from network")
    javaRuntimeSystems.mapValues { (_, sysInfo) ->
        val url = "https://api.adoptium.net/v3/assets/latest/${shippingJava}/hotspot?" +
                "architecture=${sysInfo.arch}&" +
                "image_type=${sysInfo.type ?: "jre"}&" +
                "os=${sysInfo.os}&" +
                "vendor=eclipse"

        // Find latest JDK
        val meta = try {
            JavaInfo.parseJdkInfoFromUrl(URI(url).toURL())
        } catch (e: Exception) {
            logger.error(e.toString())
            logger.warn(
                "A runtime (sys = {}, {}, {}, {}) failed to return from Adoptium!",
                sysInfo.os, sysInfo.arch, sysInfo.ext, sysInfo.type
            )
            return@mapValues
        }

        val jdkMeta = meta.first()

        // Update the maps with the metadata
        sysInfo.apply {
            version = jdkMeta.version
            name = jdkMeta.name
            sha256 = jdkMeta.sha256
            type = jdkMeta.type
            this.url = jdkMeta.url
            sigUrl = jdkMeta.sigUrl
        }
    }

    // Pull JMods
    javaRuntimeSystems.mapValues { (_, sysInfo) ->
        val url = "https://api.adoptium.net/v3/assets/latest/${shippingJava}/hotspot?" +
                "architecture=${sysInfo.arch}&" +
                "image_type=jmods&" +
                "os=${sysInfo.os}&" +
                "vendor=eclipse"

        // Find latest JDK
        val meta = try {
            JavaInfo.parseJdkInfoFromUrl(URI(url).toURL())
        } catch (e: Exception) {
            logger.error(e.toString())
            logger.warn(
                "A runtime (sys = {}, {}, {}, {}) failed to return from Adoptium!",
                sysInfo.os, sysInfo.arch, sysInfo.ext, sysInfo.type
            )
            return@mapValues
        }

        val jdkMeta = meta.first()

        // Update the maps with the metadata
        sysInfo.apply {
            jmodName = jdkMeta.name
            jmodSha256 = jdkMeta.sha256
            this.jmodUrl = jdkMeta.url
            jmodSigUrl = jdkMeta.sigUrl
        }
    }

    // Save the fetched data to the JSON file
    jsonFile.parentFile.mkdirs() // Ensure the directory exists
    jsonFile.writeText(Json { prettyPrint = true }.encodeToString(javaRuntimeSystems))
    javaRuntimeSystems
})

// Define what files belong to all distributions and their location
val commonDist = project.copySpec {
    // Ensure `shippingAstro` contains exactly one file (Astronomy_.jar)
    if (configurations.getByName("shippingAstro").files.size != 1) {
        throw GradleException("shippingAstro configuration must contain exactly one file")
    }

    // Ensure `shippingIJ` contains exactly one file (ij.jar)
    if (configurations.getByName("shippingIJ").files.size != 1) {
        throw GradleException("shippingIJ configuration must contain exactly one file")
    }

    // Copy Astronomy_.jar to correct place in distribution
    from(configurations.getByName("shippingAstro")) {
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
    from(configurations.getByName("shippingIJ")) {
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

    destinationDir = file("${projectDir}/AIJ-Run")
}

// Generates a working install directory of AIJ and launches it - make sure to not add it to git!
tasks.register<JavaExec>("aijRun") {
    group = "AstroImageJ Development"

    val runFolder = tasks.named<Sync>("sync").map { it.destinationDir }

    inputs.dir(runFolder)
    workingDir(runFolder)

    allJvmArgs = readConfigFile()

    // Force it to use the toolchain java version
    javaLauncher.set(javaToolchains.launcherFor {
        languageVersion.set(JavaLanguageVersion.of(shippingJava))
    })

    enableAssertions = true

    classpath = files(file("${projectDir}/AIJ-Run/ij.jar"))
    // Don't specify main class so gradle reads from the manifest which also reads the native access enabler
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
        line.split(" ").forEach { arg -> args.add(arg) }
    }
    args.add("-Daij.dev") // Always show full version metadata when running via dev

    logger.lifecycle("Launching with the following arguments: $args")
    return args
}

// Use toolchain for packaging
val packagingJdkToolchain = javaToolchains.launcherFor {
    languageVersion.set(JavaLanguageVersion.of(shippingJava))
    vendor.set(JvmVendorSpec.ADOPTIUM)
}

val crossbuildAppImage = providers.provider {
    val e = providers.environmentVariable("CROSSBUILD_APP_IMAGE")
    e.isPresent && e.get().toBoolean()
}

javaRuntimeSystemsProperty.get().forEach { (_, sysInfo) ->
    val sysId = "${sysInfo.os.toString().uppercaseFirstChar()}_${sysInfo.arch}"
    val packageTaskName = "packageAijFor${sysId}"
    val createAppImageTaskName = "createAppImageFor${sysId}"
    val downloadTaskName = "downloadJavaRuntimeFor${sysId}"
    val verifyTaskName = "verifyJavaRuntimeFor${sysId}"
    val verifySigTaskName = "verifySigFor${sysId}"
    val verifyJmodTaskName = "verifyJavaJmodFor${sysId}"
    val verifyJmodSigTaskName = "verifyJmodSigFor${sysId}"
    val cleanTaskName = "cleanJavaRuntimeFor${sysId}"
    val notaryTaskName = "notarizeFor${sysId}"
    val installerTaskName = "installerFor${sysId}"
    val getRuntimeTaskName = "getRuntimeFor${sysId}"

    val downloadTask = tasks.register<Download>(downloadTaskName) {
        finalizedBy(cleanTaskName, verifyTaskName, verifySigTaskName, verifyJmodSigTaskName, verifyJmodTaskName)
        mkdir(file("${projectDir}/jres"))

        src(listOf(sysInfo.url, sysInfo.sigUrl, sysInfo.jmodUrl, sysInfo.jmodSigUrl))
        overwrite(false)
        onlyIfModified(true)
        dest(layout.projectDirectory.dir("jres").dir(sysId))
        eachFile {
            val type = if (sourceURL.toString().contains("jmod")) "-jmod" else ""
            name = if (!sourceURL.toString().endsWith(".sig")) {
                "$sysId-${sysInfo.name}${type}"
            } else {
                "$sysId-${sysInfo.name}${type}.sig"
            }
        }
    }

    val isSig = { file: File ->
        file.name.endsWith(".sig")
    }

    val isRuntime = { file: File ->
        !file.name.contains("jmod") && !isSig(file)
    }

    val isRuntimeSig = { file: File ->
        !file.name.contains("jmod") && isSig(file)
    }

    val isJmod = { file: File ->
        file.name.contains("jmod") && !isSig(file)
    }

    val isJmodSig = { file: File ->
        file.name.contains("jmod") && isSig(file)
    }

    tasks.register<Delete>(cleanTaskName) {
        val directory = layout.projectDirectory.dir("jres").dir(sysId)

        delete(directory.asFileTree.matching {
            include("$sysId*")
            exclude("$sysId-${sysInfo.name}")
            exclude("$sysId-${sysInfo.name}.sig")
            exclude("$sysId-${sysInfo.name}-jmod")
            exclude("$sysId-${sysInfo.name}-jmod.sig")
        })
    }

    tasks.register<Verify>(verifyTaskName) {
        val bundledRuntime = layout.file(downloadTask.map { it.outputFiles.single { f -> isRuntime(f) } })

        inputs.file(bundledRuntime)
        src(bundledRuntime)
        algorithm("SHA256")
        checksum(sysInfo.sha256)
    }

    tasks.register<PGPVerify>(verifySigTaskName) {
        val bundledRuntime = layout.file(downloadTask.map { it.outputFiles.single { f -> isRuntime(f) } })
        val signatureFile = layout.file(downloadTask.map { it.outputFiles.single { f -> isRuntimeSig(f) } })

        file = bundledRuntime
        signature = signatureFile
        keyId = "3B04D753C9050D9A5D343F39843C48A565F8F04B"
    }

    tasks.register<Verify>(verifyJmodTaskName) {
        val bundledRuntime = layout.file(downloadTask.map { it.outputFiles.single { f -> isJmod(f) } })

        inputs.file(bundledRuntime)
        src(bundledRuntime)
        algorithm("SHA256")
        checksum(sysInfo.jmodSha256)
    }

    tasks.register<PGPVerify>(verifyJmodSigTaskName) {
        val bundledRuntime = layout.file(downloadTask.map { it.outputFiles.single { f -> isJmod(f) } })
        val signatureFile = layout.file(downloadTask.map { it.outputFiles.single { f -> isJmodSig(f) } })

        file = bundledRuntime
        signature = signatureFile
        keyId = "3B04D753C9050D9A5D343F39843C48A565F8F04B"
    }

    val getRuntimeTask = tasks.register<CreateJavaRuntimeTask>(getRuntimeTaskName) {
        launcher = packagingJdkToolchain
        fromRuntimeInfo(sysInfo)

        bundledRuntime = layout.file(downloadTask.map { it.outputFiles.single { f -> isRuntime(f) } })
        bundledJmods = layout.file(downloadTask.map { it.outputFiles.single { f -> isJmod(f) } })

        outputDir = layout.projectDirectory.dir("jres/$sysId/runtime")
    }

    val downloadedAppImage = layout.projectDirectory.dir("images/$sysId")

    val appImageDir: Provider<Directory> = if (crossbuildAppImage.get()) {
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
                LINUX -> Os.isFamily(Os.FAMILY_UNIX)
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
                "--app-version", version.toString(),
                "--java-options", "-XX:MaxRAMPercentage=75"
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
                                "--mac-app-store"
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
                        //"--linux-shortcut",
                    )
                }
                WINDOWS -> {
                    listOf(
                        "--type", "app-image",
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

    tasks.register<FixJPackageMetadataTask>("fixJpackageMetadataFor$sysId") {
        enabled = crossbuildAppImage.get()

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
        if (!crossbuildAppImage.get()) {
            mustRunAfter(tasks.named("replaceLauncherFor$sysId"))
            if (sysInfo.os == MAC) {
                mustRunAfter(tasks.named("signFor$sysId"))
            }
        } else {
            mustRunAfter(tasks.named("fixJpackageMetadataFor$sysId"))
            if (sysInfo.os == MAC) {
                mustRunAfter(tasks.named("signFor$sysId"))
            }
        }

        appName.set("AstroImageJ")

        targetOs = sysInfo.os

        // Mac only allows 3 version components, but right now jpackage seems to strip them for us
        //https://github.com/openjdk/jdk/blob/master/src/jdk.jpackage/macosx/classes/jdk/jpackage/internal/model/MacApplication.java#L42
        //https://developer.apple.com/documentation/bundleresources/information-property-list/cfbundleversion
        extraArgs = listOf(
            "--resource-dir", layout.projectDirectory.dir("packageFiles/assets/${sysInfo.os}").asFile.absolutePath,
            //"--verbose",
            "--app-version", version.toString(),
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
                            "--type", "dmg",
                            "--mac-package-identifier", "com.astroimagej.AstroImageJ",
                            "--about-url", "https://astroimagej.com",
                            "--license-file", layout.projectDirectory.file("LICENSE").asFile.absolutePath,
                            "--mac-app-store"
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
            }
            WINDOWS -> {
                inputs.dir(appImageDir.map { it.dir("AstroImageJ") })
                inputDir.set(appImageDir.map { it.dir("AstroImageJ") })

                extraArgs(listOf(
                    "--type", "msi",
                    "--win-dir-chooser",
                    "--win-help-url", "https://github.com/AstroImageJ/astroimagej/discussions",
                    "--win-shortcut",
                    "--win-shortcut-prompt",
                    "--win-update-url", "https://astroimagej.com",
                    "--about-url", "https://astroimagej.com",
                    "--license-file", layout.projectDirectory.file("LICENSE").asFile.absolutePath,
                    "--win-upgrade-uuid", "83f529ac-39a3-4fe7-9f97-e9f259321c26",
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
            mustRunAfter(tasks.named("fixJpackageMetadataFor$sysId"))
            dependsOn(tasks.named("fixJpackageMetadataFor$sysId"))

            inputs.dir(appImageDir)

            inputDir.set(appImageDir.map { it.dir("AstroImageJ.app") })

            signingIdentity = providers.environmentVariable("DeveloperId")
            entitlementsFile = layout.projectDirectory.file("packageFiles/assets/${sysInfo.os}/entitlements.plist")
        }

        val notaryTask = tasks.register<MacNotaryTask>(notaryTaskName) {
            enabled = System.getenv("DeveloperId") != null &&
                    project.property("codeSignAndNotarize").toString().toBoolean() &&
                    Os.isFamily(Os.FAMILY_MAC) && sysInfo.os == MAC
            inputDir.set(installerTask.map { it.outputDir.get() })
            keychainProfile = "AC_PASSWORD"
        }

        installerTask {
            finalizedBy(notaryTask)
        }

        if (!crossbuildAppImage.get()) {
            tasks.named("replaceLauncherFor$sysId") {
                finalizedBy(signAppImage)
            }
        } else {
            tasks.named(createAppImageTaskName) {
                finalizedBy(signAppImage)
            }

            if (downloadedAppImage.asFile.exists()) {
                tasks.named(installerTaskName).configure {
                    dependsOn("signFor$sysId")
                }
            }
        }
    }

    if (Os.isFamily(Os.FAMILY_UNIX) && sysInfo.os == LINUX) {
        val bundleTask = tasks.register<Tar>(packageTaskName) {
            if (!crossbuildAppImage.get()) {
                mustRunAfter(tasks.named("replaceLauncherFor$sysId"))
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
}

tasks.register<Copy>("copyBuiltJars") {
    group = "AstroImageJ Development"

    // Check that shippingAstro has only one file (Astronomy_.jar)
    if (configurations.getByName("shippingAstro").files.size != 1) {
        throw GradleException("shippingAstro configuration must contain exactly one file")
    }

    // cause copybuiltjars to always run, even if no compile changes
    doNotTrackState("Always copy built jars to destination")

    // Check that shippingIJ has only one file (ij.jar)
    if (configurations.getByName("shippingIJ").files.size != 1) {
        throw GradleException("shippingIJ configuration must contain exactly one file")
    }

    // Workaround for config cache by using provider outside of onlyIf
    val predicate = providers.provider {
        val properDirectory = layout.projectDirectory.file("jarLocation.txt").asFile.exists() &&
                outputDestination().toPath().resolve("ij.jar").toFile().exists()
        if (!properDirectory) {
            logger.error("[copyBuiltJars] Was not given the correct path! Must be the absolute path to the folder containing ij.jar!")
        }
        properDirectory
    }

    onlyIf {
        predicate.get()
    }

    val destination = outputDestination()

    // Copy astronomy_.jar to correct place in distribution
    from(configurations.getByName("shippingAstro")) {
        rename { "Astronomy_.jar" }
        into("plugins")
    }

    // Copy release notes
    from(file("packageFiles/common/release_notes.html")) {
        into("")
    }

    // Copy ij.jar to correct place in distribution
    from(configurations.getByName("shippingIJ")) {
        into("")
    }
    into(destination)

    doLast {
        logger.quiet("[copyBuiltJars] Copying jars to destination...")
    }
}

fun outputDestination(): File {
    val locCfg = file("${projectDir}/jarLocation.txt")
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

// Make Idea's hammer icon run copyBuiltJars
tasks.named("classes").configure {
    finalizedBy("copyBuiltJars")
}
