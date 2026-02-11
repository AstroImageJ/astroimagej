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
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")
    implementation("com.charleskorn.kaml:kaml:0.83.0")

    implementation("org.bouncycastle:bcprov-jdk18on:1.80")
    implementation("org.bouncycastle:bcpg-jdk18on:1.80")
    implementation("org.bouncycastle:bcpkix-jdk18on:1.80")

    implementation("dev.sigstore:sigstore-java:2.0.0")

    implementation("net.java.dev.jna:jna:5.18.1")
    implementation("net.java.dev.jna:jna-platform:5.18.1")
    implementation("com.github.frimtec:jpse:1.4.0")
}
