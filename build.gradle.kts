import com.astroimagej.tasks.JPackageTask
import com.astroimagej.tasks.MacNotaryTask
import de.undercouch.gradle.tasks.download.Download
import de.undercouch.gradle.tasks.download.Verify
import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import org.apache.tools.ant.taskdefs.condition.Os
import java.net.URI
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
}

plugins {
    // Apply the java plugin to add support for Java
    id("java")

    // Plugin to download files
    id("de.undercouch.download") version "5.6.0"

    // Used to download test data
    id("aij.test-conventions")
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
 * The file's name (name), major Java version (version), and MD5 hash (md5) are also added to this map. Those entries
 * are excluded from the definition for brevity.
 */
val javaRuntimeSystems = mapOf(
        "mac" to mutableMapOf("ext" to "tar.gz", "arch" to "x64", "os" to "mac"),
        "armMac" to mutableMapOf("ext" to "tar.gz", "arch" to "aarch64", "os" to "mac"),
        "linux" to mutableMapOf("ext" to "tar.gz", "arch" to "x64", "os" to "linux"),
        "windows" to mutableMapOf("ext" to "zip", "arch" to "x64", "os" to "windows")
)

val javaRuntimeSystemsProperty = project.objects.mapProperty(String::class.java, Map::class.java)

val javaRuntimeHashProvider: Provider<String> = providers.provider {
    val inputMapAsString = javaRuntimeSystems.map { (k, v) ->
            "$k:${v["ext"]}-${v["arch"]}-${v["os"]}}"
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
        val url = "https://api.adoptium.net/v3/assets/latest/${shippingJava}/hotspot?" +
                "architecture=${sysInfo["arch"]}&" +
                "image_type=jre&" +
                "os=${sysInfo["os"]}&" +
                "vendor=eclipse"

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

        val jdkMeta = meta[0]
        val binaryMeta = (jdkMeta["binary"] as Map<String, Any>)
        val packageMeta = (binaryMeta["package"] as Map<String, Any>)

        // Update the maps with the metadata
        @Suppress("UNCHECKED_CAST")
        sysInfo["version"] = ((jdkMeta["version"] as Map<String, Any>)["major"] as Integer).toString()
        sysInfo["name"] = packageMeta["name"] as String
        sysInfo["sha256"] = packageMeta["checksum"] as String
        sysInfo["type"] = binaryMeta["image_type"] as String
        sysInfo["url"] = packageMeta["link"] as String
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
    // dependsOn("commonFiles")

    with(commonDist)

    // Don't consider aij.log when copying common files, AIJ already resets it on launch
    // Doing so allows the UP-TO-DATE check to pass
    exclude("aij.log")

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

    enableAssertions = true

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

// Use toolchain for packaging
val packagingJdkToolchain = javaToolchains.launcherFor {
    languageVersion.set(JavaLanguageVersion.of(shippingJava))
}

javaRuntimeSystemsProperty.get().forEach { (sys, sysInfo) ->
    val sysId = "${
        (sysInfo["os"] as String).replaceFirstChar { 
            if (it.isLowerCase()) {
                it.titlecase(Locale.US)
            } else {
                it.toString()
            }
        }
    }_${sysInfo["arch"]}"
    val packageTaskName = "packageAijFor${sysId}"
    val downloadTaskName = "downloadJavaRuntimeFor${sysId}"
    val verifyTaskName = "verifyJavaRuntimeFor${sysId}"
    val unzipTaskName = "unzipJavaRuntimeFor${sysId}"
    val deleteTaskName = "deleteJavaRuntimeFor${sysId}"
    val cleanTaskName = "cleanJavaRuntimeFor${sysId}"
    val notaryTaskName = "notarizeFor${sysId}"

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
        algorithm("SHA256")
        checksum(sysInfo["sha256"] as String)
    }

    val unzipTask = tasks.register<Sync>(unzipTaskName) {
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

    val packageTask = tasks.register<JPackageTask>(packageTaskName) {
        group = "distribution"
        description = "Bundles the application into a native installer/image via jpackage"

        enabled = when (sysInfo["os"]) {
            "mac" -> Os.isFamily(Os.FAMILY_MAC)
            "windows" -> Os.isFamily(Os.FAMILY_WINDOWS)
            "linux" -> Os.isFamily(Os.FAMILY_UNIX)
            else -> throw GradleException("Unknown OS type: ${sysInfo["os"]}")
        } && version.toString().matches(Regex("^(?<major>0|[1-9]\\d*)\\.(?<minor>0|[1-9]\\d*)\\.(?<patch>0|[1-9]\\d*)\\.(00)"))

        inputs.files(layout.projectDirectory.dir("packageFiles/assets/associations").asFileTree)
            .optional()
            .withPropertyName("File associations")
        inputs.dir(layout.projectDirectory.dir("packageFiles/assets/${sysInfo["os"]}"))
            .optional()
            .withPropertyName("Resource overrides")

        appName.set("AstroImageJ")

        // Wire inputDir to any task's output
        inputDir = tasks.named<Sync>("commonFiles").map { it.destinationDir }

        // Specify the name of your main jar within that inputDir
        mainJarName.set("ij.jar")

        extraArgs = listOf(
            "--java-options", "-Duser.dir=\$APPDIR",
            "--resource-dir", layout.projectDirectory.dir("packageFiles/assets/${sysInfo["os"]}").asFile.absolutePath,
            //"--temp", layout.buildDirectory.dir("temp").map { it.asFile.absolutePath }.get(),
            //"--verbose",
            "--app-version", version.toString().replace(".00", ""),
            "--about-url", "https://astroimagej.com",
            "--license-file", layout.projectDirectory.file("LICENSE").asFile.absolutePath,
        )

        launcher = packagingJdkToolchain

        extraArgs(when (sysInfo["os"]) {
            "mac" -> {
                buildList {
                    addAll(
                        listOf(
                            "--type", "dmg",
                            "--mac-package-identifier", "com.astroimagej.AstroImageJ",
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
            "linux" -> {
                listOf(
                    "--type", "app-image",//todo tar.gz
                    "--linux-shortcut",
                )
            }
            "windows" -> {
                listOf(
                    "--type", "msi",
                    "--win-dir-chooser",
                    "--win-help-url", "https://github.com/AstroImageJ/astroimagej/discussions",
                    "--win-shortcut",
                    "--win-shortcut-prompt",
                    "--win-update-url", "https://astroimagej.com",
                )
            }
            else -> listOf()
        })

        // Add file associations
        layout.projectDirectory.dir("packageFiles/assets/associations").asFileTree.forEach {
            extraArgs(listOf("--file-associations", it.absolutePath))
        }

        runtime.set(layout.dir(unzipTask.map { it.destinationDir }))

        // Destination for the generated installer/image
        outputDir.set(layout.buildDirectory.dir("distrbutions/$sysId"))
    }

    if (Os.isFamily(Os.FAMILY_MAC) && sysInfo["os"] == "mac") {
        val notaryTask = tasks.register<MacNotaryTask>(notaryTaskName) {
            enabled = System.getenv("DeveloperId") != null &&
                    project.property("codeSignAndNotarize").toString().toBoolean()
            inputDir.set(packageTask.map { it.outputDir.get() })
            keychainProfile = "AC_PASSWORD"
        }

        packageTask {
            finalizedBy(notaryTask)
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

// Make Idea's hammer icon run copyBuiltJars
tasks.named("classes").configure {
    finalizedBy("copyBuiltJars")
}
