package com.astroimagej.git

import org.eclipse.jgit.api.Git
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import org.gradle.api.provider.Property
import org.gradle.api.provider.ValueSource
import org.gradle.api.provider.ValueSourceParameters
import java.io.File

abstract class GitVersionInfo : ValueSource<String, GitVersionInfo.Params> {
    interface Params : ValueSourceParameters {
        val workDir: Property<File>
    }

    override fun obtain(): String {
        val repo = FileRepositoryBuilder()
            .setWorkTree(parameters.workDir.get())
            .readEnvironment()
            .findGitDir()
            .build()

        return repo.use {
            val git = Git(repo)

            val info = repo.branch + "+" +
                    (repo.resolve("HEAD").abbreviate(7).name()) + "+" +
                    (if (!git.status().call().isClean) "local" else "")

            info
        }
    }
}