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
    api("dev.sigstore:sigstore-java:2.4.0")
    implementation("ch.randelshofer:fastdoubleparser:2.0.1")
    api("org.hipparchus:hipparchus-stat:3.1")
    api("org.jocl:jocl:2.0.5")
    api("ai.tegmentum:wasmtime4j:48.0.0-2.0.0")
    api("ai.tegmentum:wasmtime4j-panama:48.0.0-2.0.0")
    api("io.github.beehive-lab:tornado-runtime:8.1.0-jdk22plus")
    api("io.github.beehive-lab:tornado-drivers-opencl:8.1.0-jdk22plus")
    api("io.github.beehive-lab:tornado-drivers-metal:8.1.0-jdk22plus")
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
        attributes(
            "Main-Class" to mainClassName,
            "Enable-Native-Access" to "ALL-UNNAMED"
        )
        // "Class-Path": "plugins/ ${n.collect {'plugins/' + ((File)it).getName()}.join(' ')}"
    }
}

tasks.shadowJar {
    // outputs.upToDateWhen { false }
    archiveFileName.set("ij.jar")
}