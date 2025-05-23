plugins {
    // Apply the java-library plugin to add support for Java Library
    id("aij.java-conventions")
}

dependencies {
    compileOnly("org.apache.maven.wagon:wagon-ssh:3.5.3")
    compileOnly("org.tinyjee.dim:doxia-include-macro:1.1")
    compileOnly("org.apache.maven.doxia:doxia-module-markdown:2.0.0-M12")
    compileOnly("com.puppycrawl.tools:checkstyle:10.14.1")
    //compileOnly("org.apache.maven.doxia:doxia-module-markdown:1.21")
    //implementation("org.apache.commons:commons-compress:1.18")
    compileOnly("com.google.code.findbugs:annotations:3.0.1u2")
    //testImplementation("org.junit.jupiter:junit-jupiter-api:5.3.1")
    //testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.3.1")
    testCompileOnly("junit:junit:4.13.2")
    //testRuntimeOnly("org.junit.vintage:junit-vintage-engine:5.3.1")
    testImplementation("com.nanohttpd:nanohttpd-webserver:2.2.0")
    testImplementation("org.openjdk.jmh:jmh-core:1.37")
    testImplementation("org.openjdk.jmh:jmh-generator-annprocess:1.37")
    implementation("javax.annotation:javax.annotation-api:1.3")
    implementation("org.apache.commons:commons-compress:1.26.1")
}

val testPredicate = providers.provider {
    providers.systemProperty("runTests").isPresent
}

tasks.test {
    isEnabled = testPredicate.get()
    useJUnitPlatform()
}

