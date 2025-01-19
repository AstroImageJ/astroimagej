import de.undercouch.gradle.tasks.download.Download
import de.undercouch.gradle.tasks.download.Verify
import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import io.github.fvarrui.javapackager.gradle.PackagePluginExtension
import io.github.fvarrui.javapackager.gradle.PackageTask
import io.github.fvarrui.javapackager.model.FileAssociation
import io.github.fvarrui.javapackager.model.MacStartup
import io.github.fvarrui.javapackager.model.WindowsExeCreationTool
import org.apache.tools.ant.taskdefs.condition.Os
import java.io.IOException
import java.net.URI
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.security.MessageDigest
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*

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
    dependencies {
        classpath("io.github.fvarrui:javapackager:1.7.6")
    }

    // Needed for GrGit 5+
    if (!JavaVersion.current().isCompatibleWith(JavaVersion.VERSION_11)) {
        throw GradleException("Gradle daemon must be running on Java 11 or greater.")
    }
}

plugins {
    // Apply the java plugin to add support for Java
    id("java")

    // Plugin to download files
    id("de.undercouch.download") version "5.6.0"

    // Used to download test data
    id("aij.test-conventions")
}

apply(plugin = "io.github.fvarrui.javapackager.plugin")

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
 * See https://app.swaggerhub.com/apis-docs/azul/zulu-download-community/1.0#/bundles/get_bundles_latest_
 *
 * The file's name (name), major Java version (version), and MD5 hash (md5) are also added to this map. Those entries
 * are excluded from the definition for brevity.
 */
val javaRuntimeSystems = mapOf(
        "mac" to mutableMapOf("ext" to "zip", "arch" to "x86", "os" to "macos", "hw_bitness" to "64"),
        "armMac" to mutableMapOf("ext" to "zip", "arch" to "arm", "os" to "macos", "hw_bitness" to "64"),
        "linux" to mutableMapOf("ext" to "tar.gz", "arch" to "x86", "os" to "linux", "hw_bitness" to "64"),
        "windows" to mutableMapOf("ext" to "zip", "arch" to "x86", "os" to "windows", "hw_bitness" to "64")
)

val javaRuntimeSystemsProperty = project.objects.mapProperty(String::class.java, Map::class.java)

val javaRuntimeHashProvider: Provider<String> = providers.provider {
    val inputMapAsString = javaRuntimeSystems.map { (k, v) ->
            "$k:${v["ext"]}-${v["arch"]}-${v["os"]}-${v["hw_bitness"]}"
    }.joinToString(",")
    val md = MessageDigest.getInstance("MD5")
    md.digest(inputMapAsString.toByteArray()).joinToString("") { "%02x".format(it) }
}

val javaRuntimeFileNameProvider: Provider<String> = providers.provider {
    "javaRuntimeSystems-${shippingJava}-${javaRuntimeHashProvider.get()}.json"
}

val javaRuntimeCacheFileProvider: Provider<RegularFile> = providers.provider {
    layout.projectDirectory.file("jres/${javaRuntimeFileNameProvider.get()}")
}

// Wrap the logic in a lazy property that caches the result
javaRuntimeSystemsProperty.convention(providers.provider {
    val jsonFile = javaRuntimeCacheFileProvider.get().asFile

    // If the JSON file exists and is recent, load the cached data
    if (jsonFile.exists()) {
        val lastModified = jsonFile.lastModified()
        val now = Instant.now().toEpochMilli()
        val daysSinceLastModified = ChronoUnit.DAYS.between(Instant.ofEpochMilli(lastModified), Instant.ofEpochMilli(now))

        if (daysSinceLastModified <= 30) {
            logger.lifecycle("Loading cached Java Runtime Systems data from JSON for Java version $shippingJava (last modified $daysSinceLastModified days ago)")
            @Suppress("UNCHECKED_CAST")
            val cacheData = JsonSlurper().parse(jsonFile) as Map<String, Map<String, Any>>
            if (cacheData.values.all { it["version"] != null }) {
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
    @Suppress("UNUSED_DESTRUCTURED_PARAMETER_ENTRY")
    javaRuntimeSystems.forEach { (sys, sysInfo) ->
        val url = "https://api.azul.com/metadata/v1/zulu/packages?availability_types=ca&latest=true&" +
                "crac_supported=false&crs_supported=false&" +
                "os=${sysInfo["os"]}&arch=${sysInfo["arch"]}&hw_bitness=${sysInfo["hw_bitness"]}" +
                "&archive_type=${sysInfo["ext"]}&java_version=$shippingJava"

        // Find latest JDK
        @Suppress("UNCHECKED_CAST")
        val meta = try {
            JsonSlurper().parse(URI(url).toURL())
        } catch (e: Exception) {
            logger.error(e.toString())
            logger.warn("A runtime (sys = {}, {}, {}, {}) failed to return from Azul!",
                    sysInfo["os"], sysInfo["arch"], sysInfo["ext"], sysInfo["type"])
            return@forEach
        } as List<Map<String, Any>>

        // Find info of latest JDK
        @Suppress("UNCHECKED_CAST")
        val jdkMeta = try {
            JsonSlurper().parse(URI("https://api.azul.com/metadata/v1/zulu/packages/${meta[0]["package_uuid"]}").toURL())
        } catch (ignored: Exception) {
            logger.warn("A runtime (sys = {}, {}, {}, {}) failed to return from Azul!",
                sysInfo["os"], sysInfo["arch"], sysInfo["ext"], sysInfo["type"])
            return@forEach
        } as Map<String, Any>

        // Update the maps with the metadata
        @Suppress("UNCHECKED_CAST")
        sysInfo["version"] = (jdkMeta["java_version"] as List<String>)[0]
        sysInfo["ext"] = jdkMeta["archive_type"] as String
        sysInfo["name"] = jdkMeta["name"] as String
        sysInfo["md5"] = jdkMeta["md5_hash"] as String
        sysInfo["type"] = jdkMeta["java_package_type"] as String
        sysInfo["url"] = jdkMeta["download_url"] as String
    }

    // Save the fetched data to the JSON file
    jsonFile.parentFile.mkdirs() // Ensure the directory exists
    jsonFile.writeText(JsonOutput.prettyPrint(JsonOutput.toJson(javaRuntimeSystems)))
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
        filesMatching("AstroImageJ.cfg") {
            permissions {
                user {
                    read = true
                    write = true
                }
                group {
                    read = true
                    write = true
                }
                other {
                    read = true
                    write = true
                }
            }
        }
    }

    into("plugins") {
        from(file("${projectDir}/packageFiles/plugins"))
    }

    // Copy ij.jar to correct place in distribution
    from(configurations.getByName("shippingIJ")) {
        into("")
    }
}

// Create commonFiles directory for use in runAij and distribution generation
// This is needed as we create the file paths for the package tasks at config time
project.sync {
    with(commonDist)
    into(layout.buildDirectory.dir("commonFiles"))
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
    // dependsOn("commonFiles")

    with(commonDist)

    // Don't consider aij.log when copying common files, AIJ already resets it on launch
    // Doing so allows the UP-TO-DATE check to pass
    exclude("aij.log")

    // Copy launch options to cfg file so editing can be tested
    from(file("${projectDir}/devLaunchOptions.txt")) {
        duplicatesStrategy = DuplicatesStrategy.INCLUDE
        rename { "AstroImageJ.cfg" }
    }

    destinationDir = file("${projectDir}/AIJ-Run")
}

// Generates a working install directory of AIJ and launches it - make sure to not add it to git!
tasks.register<JavaExec>("aijRun") {
    dependsOn("sync")
    group = "AstroImageJ Development"

    workingDir = file("${projectDir}/AIJ-Run/")

    allJvmArgs = readConfigFile()

    // Force it to use the toolchain java version
    javaLauncher.set(javaToolchains.launcherFor {
        languageVersion.set(JavaLanguageVersion.of(shippingJava))
    })

    classpath = files(file("${projectDir}/AIJ-Run/ij.jar"))
    mainClass.set("ij.ImageJ")
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

configure<PackagePluginExtension> {
    // common configuration
    mainClass("ij.ImageJ")
    bundleJre(true)
    customizedJre(false)
    outputDirectory(file("${project.layout.buildDirectory.get()}/distributions"))
    additionalResources(file("${project.layout.buildDirectory.get()}/commonFiles/").listFiles()?.toList() ?: emptyList())
    runnableJar(file("${project.layout.buildDirectory.get()}/commonFiles/ij.jar"))
    assetsDir(file("${projectDir}/packageFiles/assets"))
    name("AstroImageJ")

    winConfig.apply {
        isWrapJar = false // Don't merge the ij.jar into the exe file
        productVersion = "1.1.1"
        productName = "Why Java Launcher"
        isGenerateMsi = false
        isDisableDirPage = false
        isDisableFinishedPage = false
        isDisableRunAfterInstall = false
        exeCreationTool = WindowsExeCreationTool.why
    }

    macConfig.apply {
        isRelocateJar = false // Don't place ij.jar in Java/ folder
        isGeneratePkg = false
        appId = "AstroImageJ"
        macStartup = MacStartup.UNIVERSAL
        isCodesignApp =
            project.hasProperty("codeSignAndNotarize") && project.property("codeSignAndNotarize").toString().toBoolean()
        // To set a DeveloperID in the environment, see comments in the gradle.properties file.
        isNotarizeApp = System.getenv("DeveloperId") != null && project.property("codeSignAndNotarize").toString().toBoolean()
        keyChainProfile = "AC_PASSWORD"
        developerId = System.getenv("DeveloperId")
        customLauncher = file("${projectDir}/packageFiles/assets/mac/nativeJavaApplicationStub")
    }

    linuxConfig.apply {
        isWrapJar = false
        isGenerateRpm = false
    }

    // Don't copy deps into a "libs" folder - we bundle them into ij.jar or Astronomy_.jar and the plugins folder
    copyDependencies(false)

    organizationName("AstroImageJ")
    vmArgs(emptyList())
    version("dev-x86") // Dummy version to keep it from saying "unspecified"
    generateInstaller(true)

    fileAssociations(listOf(
        association("application/fits", "fits", "FITS File"),
        association("application/fits", "fit", "FITS File"),
        association("application/fits", "fts", "FITS File"),
        association("application/fits", "fits.fz", "FITS File"),
        association("application/fits", "fit.fz", "FITS File"),
        association("application/fits", "fts.fz", "FITS File"),
        association("application/fits", "fits.gz", "FITS File"),
        association("application/fits", "fit.gz", "FITS File"),
        association("application/fits", "fts.gz", "FITS File"),
        association("text/tbl", "tbl", "Table"),
        association("text/radec", "radec", "AIJ radec file"),
        association("text/apertures", "apertures", "AIJ apertures file"),
        association("text/plotcfg", "plotcfg", "AIJ plot config file"),
    ))
}

fun association(mt: String, ext: String, desc: String): FileAssociation {
    val a = FileAssociation()
    a.mimeType = mt
    a.extension = ext
    a.description = desc

    return a
}

// Use toolchain for packaging
val packagingJdkToolchain = javaToolchains.launcherFor {
    languageVersion.set(JavaLanguageVersion.of(shippingJava))
}

tasks.register("packageAij")

javaRuntimeSystemsProperty.get().forEach { (sys, sysInfo) ->
    val sysId = "${
        (sysInfo["os"] as String).replaceFirstChar { 
            if (it.isLowerCase()) {
                it.titlecase(Locale.US)
            } else {
                it.toString()
            }
        }
    }_${sysInfo["arch"]}_${sysInfo["hw_bitness"]}Bit"
    val packageTaskName = "packageAijFor${sysId}_Java${sysInfo["version"]}"
    val downloadTaskName = "downloadJavaRuntimeFor${sysId}"
    val verifyTaskName = "verifyJavaRuntimeFor${sysId}"
    val unzipTaskName = "unzipJavaRuntimeFor${sysId}"
    val deleteTaskName = "deleteJavaRuntimeFor${sysId}"
    val cleanTaskName = "cleanJavaRuntimeFor${sysId}"

    tasks.register<Download>(downloadTaskName) {
        finalizedBy(cleanTaskName, verifyTaskName)
        mkdir(file("${projectDir}/jres"))

        src(sysInfo["url"])
        overwrite(false)
        onlyIfModified(true)
        dest(layout.projectDirectory.dir("jres").dir(sysId).file("$sysId-${sysInfo["name"]}"))
    }

    tasks.register<Delete>(cleanTaskName) {
        val directory = layout.projectDirectory.dir("jres").dir(sysId)

        delete(directory.asFileTree.matching {
            include("$sysId*")
            exclude("$sysId-${sysInfo["name"]}")
        })
    }

    tasks.register<Delete>(deleteTaskName) {
        onlyIf {
            Os.isFamily(Os.FAMILY_MAC)
        }

        val folderName = (sysInfo["name"] as String).replace(".${sysInfo["ext"]}", "")
        val folder = file("${projectDir}/jres/$sysId/unpacked/$folderName")

        delete(folder)
    }

    tasks.register<Verify>(verifyTaskName) {
        dependsOn(downloadTaskName)
        inputs.file(layout.projectDirectory.dir("jres").dir(sysId).file("$sysId-${sysInfo["name"]}"))
        outputs.upToDateWhen { false }

        src(layout.projectDirectory.dir("jres").dir(sysId).file("$sysId-${sysInfo["name"]}"))
        algorithm("MD5")
        checksum(sysInfo["md5"] as String)
    }

    tasks.register<Sync>(unzipTaskName) {
        dependsOn(verifyTaskName, deleteTaskName)
        group = "AstroImageJ Development"

        val archive = layout.projectDirectory.dir("jres").dir(sysId).file("$sysId-${sysInfo["name"]}")

        inputs.file(archive)
        outputs.dir(layout.projectDirectory.dir("jres/$sysId/unpacked"))

        when (sysInfo["ext"]) {
            "tar.gz" -> from(tarTree(resources.gzip(archive))) {
                into("")
            }
            "zip" -> from(zipTree(archive)) {
                into("")
            }
            else -> logger.error("Did not know how to handle ${sysInfo["ext"]} for $sys")
        }

        into("jres/$sysId/unpacked")
    }

    tasks.register<PackageTask>(packageTaskName) {
        group = "AstroImageJ Development"

        //todo fix incompatibility
        // This prevents unzip tasks from running in parallel
        notCompatibleWithConfigurationCache("Package task issue")

        // Set the platform
        io.github.fvarrui.javapackager.model.Platform.values().forEach pLoop@{
            if (it == io.github.fvarrui.javapackager.model.Platform.auto) return@pLoop
            val osName = if (sys.lowercase().contains("mac")) "mac" else sys.lowercase()
            if (it.name == osName) {
                platform = it
            }
        }

        packagingJdk = packagingJdkToolchain.map { it.metadata.installationPath }.get().asFile

        version = project.version.toString()

        // Get Java bin fold location
        val java = file("${projectDir}/jres/$sysId/unpacked/${(sysInfo["name"] as String).replace("." + sysInfo["ext"], "")}")
        val runtimePath: File? = if (java.exists()) {
            java.walkTopDown().filter { it.name == "release" }.firstOrNull {
                val binFolder = it.resolveSibling("bin")
                it.isFile && binFolder.isDirectory && binFolder.exists()
            }?.parentFile
        } else {
            logger.error("A JRE was missing, run unZipJresTask first! $sys will fallback to the default runtime path")
            File(java, if (sysInfo["os"] == "macos") "zulu-${shippingJava}.${sysInfo["type"]}/Contents/Home" else "")
        }

        // Sanity check
        runtimePath?.let {
            if (sysInfo["type"] == "jre") {
                jrePath = it
            } else if (sysInfo["type"] == "jdk") {
                jdkPath = it
            }
        }

        if (sysInfo["os"] == "linux") {
            isCreateTarball = project.property("createTarBalls").toString().toBoolean()
        } else {
            isCreateZipball = project.property("createZip").toString().toBoolean()
        }

        if (sysInfo["os"] == "macos") {
            isGenerateInstaller = project.property("createDMG").toString().toBoolean()
        }
    }

    logger.quiet("Added a distribution task: $packageTaskName")

    tasks.named(packageTaskName) {
        dependsOn(unzipTaskName, "commonFiles")
        mustRunAfter("commonFiles", deleteTaskName)
    }

    tasks.named("packageAij") {
        group = "AstroImageJ Development"
        dependsOn(packageTaskName)
    }
}

tasks.withType<PackageTask>().configureEach {
    if (name == "package") return@configureEach

    val platformInfoRegex = Regex("packageAijFor(.+)_((?:x86)|(?:arm))_(\\d{2})Bit_Java(\\d{2})")
    if (!name.matches(platformInfoRegex)) {
        throw GradleException("Package task '${name}' failed to match, could not determine platform information")
    }
    val platformInfo = name.replace(platformInfoRegex, "$2_$3Bit")

    doLast {
        logger.lifecycle("Renaming outputs for platform information...")

        outputFiles.forEach { outputFile ->
            if (!outputFile.exists()) {
                logger.lifecycle("\tSkipping file that does not exist: {}", outputFile.name)
                return@forEach
            }

            val fileNameRegex = Regex("AstroImageJ[-_]${version}(?:[-_]\\w+)?(\\.\\w+|\\.tar\\.gz)\$")
            if (!outputFile.name.matches(fileNameRegex)) {
                logger.warn("\tGiven a file '${outputFile.name}' that cannot be handled")
            } else {
                val newName = outputFile.name.replace(fileNameRegex, "AstroImageJ_v${version}-${platform}-${platformInfo}\$1")
                val newPath = outputFile.toPath().resolveSibling(newName)

                logger.lifecycle("\tRenaming '{}' to '{}'...", outputFile, newName)
                logger.lifecycle("\t\t" + newPath.toUri())

                try {
                    Files.move(outputFile.toPath(), newPath, StandardCopyOption.REPLACE_EXISTING)
                } catch (e: IOException) {
                    logger.error("\tFailed to rename '{}' to '{}'", outputFile.name, newName)
                }
            }
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

tasks.register<Sync>("makeReleaseFiles") {
    group = "AstroImageJ Development"
    dependsOn("packageAijForWindows_x86_64Bit_Java$shippingJava")

    val buildDir = layout.buildDirectory.get()
    val output = buildDir.dir("updatesjava$targetJava")

    val fullVersion = project.version as String
    val semverVersion = fullVersion.substring(0, fullVersion.lastIndexOf('.'))

    // Copy files, no renames
    from("$buildDir/distributions/AstroImageJ") {
        include("**/ij.jar", "**/StartupMacros.txt", "**/AstroImageJ.exe", "**/Astronomy_.jar", "**/release_notes.html")
        eachFile {
            relativePath = RelativePath(true, *relativePath.segments.drop(relativePath.segments.size - 1).toTypedArray())
        }
        includeEmptyDirs = false
    }

    // Copy files, with renames
    from("$buildDir/distributions/AstroImageJ") {
        include("**/ij.jar", "**/StartupMacros.txt", "**/AstroImageJ.exe", "**/Astronomy_.jar")
        eachFile {
            relativePath = RelativePath(true, *relativePath.segments.drop(relativePath.segments.size - 1).toTypedArray())
        }
        includeEmptyDirs = false

        rename("(.+)\\.(.+)", "$1$semverVersion.$2")
    }

    into(output)
    doLast {
        // Update versions.txt
        val versionsTxt = output.file("versions.txt").asFile

        Files.createFile(versionsTxt.toPath())

        versionsTxt.appendText("$semverVersion\n")

        URI("https://www.astro.louisville.edu/software/astroimagej/updates/updatesjava17/versions.txt")
            .toURL().openStream().use { inputStream ->
                versionsTxt.appendText(inputStream.bufferedReader().use { it.readText() })
            }
    }
}

tasks.register<Sync>("makeDailyBuildFiles") {
    group = "AstroImageJ Development"
    dependsOn("packageAijForWindows_x86_64Bit_Java$shippingJava")

    from(layout.buildDirectory.dir("distributions/AstroImageJ")) {
        include("**/ij.jar", "**/StartupMacros.txt", "**/AstroImageJ.exe", "**/Astronomy_.jar", "**/release_notes.html")
        eachFile {
            relativePath = RelativePath(true, *relativePath.segments.drop(relativePath.segments.size - 1).toTypedArray())
        }
        includeEmptyDirs = false
    }

    into(layout.buildDirectory.dir("updatesjava$targetJava"))
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

// Make Idea's hammer icon run copyBuiltJars
tasks.named("classes").configure {
    finalizedBy("copyBuiltJars")
}
