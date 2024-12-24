plugins {
    id("aij.java-conventions")
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