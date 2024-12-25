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

}
