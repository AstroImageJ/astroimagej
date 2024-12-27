plugins {
    `kotlin-dsl`
}

kotlin {
    // Kotlin has somewhat strict Java requirements,
    // but JVM auto-provisioning does not seem to work here,
    // so set a lower Java version.
    // Set to 11 to match the JGit requirement set in the root build.gradle.kts
    jvmToolchain(11)
}

repositories {
    // So that external plugins can be resolved in dependencies section
    gradlePluginPortal()
    mavenCentral()
}

dependencies {
    implementation("org.eclipse.jgit:org.eclipse.jgit:7.1.0.202411261347-r")
}
