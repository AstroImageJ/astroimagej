plugins {
    // Apply the java-library plugin to add support for Java Library
    id("aij.java-conventions")
}

dependencies {
    compileOnly("org.apache.maven.wagon:wagon-ssh:3.5.3")
    compileOnly("org.tinyjee.dim:doxia-include-macro:1.1")
    compileOnly("org.apache.maven.doxia:doxia-module-markdown:2.1.0")
    compileOnly("com.puppycrawl.tools:checkstyle:13.9.0")

    implementation("com.github.spotbugs:spotbugs-annotations:4.10.3")
    implementation("jakarta.annotation:jakarta.annotation-api:3.0.0")
    implementation("org.apache.commons:commons-compress:1.28.0")

    testImplementation("org.junit.jupiter:junit-jupiter-api:6.1.3")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:6.1.3")

    testImplementation("org.nanohttpd:nanohttpd-webserver:2.3.1")

    testImplementation("org.openjdk.jmh:jmh-core:1.37")
    testAnnotationProcessor("org.openjdk.jmh:jmh-generator-annprocess:1.37")
}

val testPredicate = providers.provider {
    providers.systemProperty("runTests").isPresent
}

tasks.test {
    isEnabled = testPredicate.get()
    useJUnitPlatform()
}

