plugins {
    id("aij.java-library-conventions")
    id("com.gradleup.shadow") version "8.3.3"
}

dependencies {
    api(project(":Nom_Fits"))
    implementation("org.openjdk.nashorn:nashorn-core:15.4")
    api("de.rototor.pdfbox:graphics2d:3.0.2")
    api("net.java.dev.jna:jna-platform:5.17.0")
    api(fileTree(mapOf("dir" to "${rootDir}/aijLibs", "include" to listOf("*.jar"))))
    implementation("ch.randelshofer:fastdoubleparser:2.0.1")
    api("org.hipparchus:hipparchus-stat:3.1")
}

artifacts {
    add("shippingJar", tasks.shadowJar)
}

val mainClassName = "ij.ImageJ"

tasks.processResources {
    // Use a provider to work around the fact that we reference project and the version is dynamic
    // Have to do it this way with go-between variable to force it to get the updated value
    val v = project.version
    inputs.property("version", v)

    filesMatching("aij_version.txt") {
        expand("version" to v)
    }
}

tasks.jar {
    // FileTree m = fileTree("${rootProject.projectDir}/src/main/resources/plugins/")
    // Collection n = m.include("**/*.jar").grep()

    archiveFileName.set("ij-standalone.jar")

    manifest {
        attributes("Main-Class" to mainClassName)
        // "Class-Path": "plugins/ ${n.collect {'plugins/' + ((File)it).getName()}.join(' ')}"
    }
}

tasks.shadowJar {
    // outputs.upToDateWhen { false }
    archiveFileName.set("ij.jar")

    // Exclude patterns
    val excludePatterns = listOf(
            "**/*.jnilib",
            "**/*.so",
            "**/*.a"
    )

    // Set excludes to the task inputs
    inputs.property("excludes", excludePatterns)

    // Exclude non-Windows binaries
    exclude(*excludePatterns.toTypedArray())
}