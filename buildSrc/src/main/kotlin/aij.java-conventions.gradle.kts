plugins {
    java
}

repositories {
    mavenCentral()
}

group = "com.astroimagej"

// Work around potential project isolation issues by using the provider to get values from gradle.properties in
// the root directory, as opposed to `Integer.parseInt(properties['javaShippingVersion'] as String)`
// Java version to compile and package with
val shippingJava = providers.gradleProperty("javaShippingVersion").map { it.toInt() }.get()
// Minimum Java version binaries should be compatible with
val targetJava = providers.gradleProperty("minJava").map { it.toInt() }.get()

// Use to share built artifacts with the root project so it can package them
// https://docs.gradle.org/current/userguide/cross_project_publications.html#sec:simple-sharing-artifacts-between-projects
val shippingJar by configurations.creating {
    isCanBeConsumed = true
    isCanBeResolved = false
}

tasks.withType<JavaCompile>().configureEach {
    // Set language level and binary level to targetJava (binary only when building with a JDK 9+)
    // Ensures newer language features aren't used
    options.release.set(targetJava)

    // AIJ is large, so this caches compiling results and only recompiles changed files
    options.isIncremental = true

    // Allow unicode
    options.encoding = "UTF-8"

    options.isFork = true

    options.compilerArgs.add("-Xlint:-dep-ann")

    //options.listFiles = true

    // Set the compiler to the required Java version
    javaCompiler.set(
        javaToolchains.compilerFor {
            languageVersion.set(JavaLanguageVersion.of(shippingJava))
        }
    )
}

java {
    toolchain {
        // Gradle will download the JDK for compiling/running if one isn't found
        // This should prevent anyone from using an outdated JDK
        languageVersion.set(JavaLanguageVersion.of(shippingJava))
    }
}