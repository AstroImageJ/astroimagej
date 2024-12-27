import org.eclipse.jgit.api.Git

tasks.register("preTest") {
    val testDataPath = "${projectDir}/repos/aijtestdata"
    val testData = file(testDataPath)

    doLast {
        if (!testData.exists()) {
            // Clone the repository
            Git.cloneRepository()
                .setDirectory(testData)
                .setURI("https://github.com/AstroImageJ/AijTestData")
                .call()
        } else {
            // Open the existing repository
            val gitRepo = Git.open(testData)
            gitRepo.use {
                // Fetch and pull updates
                it.fetch().call()
                it.pull().call()
            }
        }
    }
}