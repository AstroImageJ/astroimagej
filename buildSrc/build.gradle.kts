plugins {
    `kotlin-dsl`
}

kotlin {
    jvmToolchain(22)
}

repositories {
    // So that external plugins can be resolved in dependencies section
    gradlePluginPortal()
    mavenCentral()
}

dependencies {
    implementation("org.eclipse.jgit:org.eclipse.jgit:7.1.0.202411261347-r")
}
