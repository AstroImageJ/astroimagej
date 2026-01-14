plugins {
    id("aij.java-conventions")
    `jvm-test-suite`
}

dependencies {
	implementation(project(":ij"))
}

artifacts {
    add("shippingJar", tasks.jar)
}

tasks.jar {
    archiveFileName = "Astronomy_.jar"
}

testing {
    suites {
        val test by getting(JvmTestSuite::class) {
            useJUnitJupiter()
        }

        register<JvmTestSuite>("integrationTest") {
            dependencies {
                implementation(project())
            }

            targets {
                all {
                    testTask.configure {
                        shouldRunAfter(test)
                    }
                }
            }
        }
    }
}

tasks.named("check") {
    dependsOn(testing.suites.named("integrationTest"))
}