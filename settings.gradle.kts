plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.9.0"
}

rootProject.name = "AstroImageJ"
include("ij")
include("Astronomy_")
include("Nom_Fits")
enableFeaturePreview("STABLE_CONFIGURATION_CACHE")

toolchainManagement {
    jvm {
        // todo automatic repos are deprecated, uncomment this in future
        /*javaRepositories {
            repository("foojay") {
                resolverClass.set(org.gradle.toolchains.foojay.FoojayToolchainResolver::class.java)
            }
        }*/
    }
}