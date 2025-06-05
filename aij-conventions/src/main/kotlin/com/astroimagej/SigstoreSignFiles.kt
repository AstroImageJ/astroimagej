package com.astroimagej

import dev.sigstore.KeylessSigner
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.ProjectLayout
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ProviderFactory
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault
import java.nio.file.Files
import javax.inject.Inject

@DisableCachingByDefault(because = "Sigstore signatures are true-timestamp dependent, so we should not cache signatures")
abstract class SigstoreSignFiles : DefaultTask() {
    @get:InputFiles
    abstract val filesToSign: ConfigurableFileCollection

    @get:OutputDirectory
    abstract val signatureDirectory: DirectoryProperty

    @get:Inject
    protected abstract val providers: ProviderFactory

    @get:Inject
    protected abstract val objects: ObjectFactory

    @get:Inject
    protected abstract val layout: ProjectLayout

    init {
        outputs.upToDateWhen {
            // Sigstore signatures are true-timestamp dependent, so we should not cache signatures
            false
        }

        outputs.cacheIf("Sigstore signatures are true-timestamp dependent, so we should not cache signatures") {
            false
        }
    }

    @TaskAction
    fun signFiles() {
        val sigDir = signatureDirectory.get().asFile
        if (!sigDir.exists()) {
            sigDir.mkdirs()
        }

        val signer = KeylessSigner.builder().apply {
            sigstorePublicDefaults()
            /*GithubActionsOidcClient.builder()
                .audience("sigstore")
                .build()*/
        }.build()

        filesToSign.files.forEach { inputFile ->
            val bundle = signer.signFile(inputFile.toPath())
            val json = bundle.toJson()

            val outName = "${inputFile.name}.sigstore.json"
            val outPath = signatureDirectory.get().file(outName)

            Files.writeString(outPath.asFile.toPath(), json, Charsets.UTF_8)

            logger.lifecycle("Signed '${inputFile.name}'-> '${outPath}'")
        }
    }
}