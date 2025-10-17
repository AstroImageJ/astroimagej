package com.astroimagej.tasks

import com.astroimagej.updates.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.ProviderFactory
import org.gradle.api.tasks.*
import java.io.File
import java.security.MessageDigest
import java.util.*
import javax.inject.Inject
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

abstract class GenerateMetadata
@Inject constructor(private var providerFactory: ProviderFactory): DefaultTask() {
    @get:Input
    abstract val baseArtifactUrl: Property<String>

    @get:Input
    abstract val baseMetaUrl: Property<String>

    @get:Input
    abstract val version: Property<String>

    @get:Input
    abstract val releaseType: Property<String>

    @get:InputFile
    abstract val updateDataJson: RegularFileProperty

    @get:InputFiles
    abstract val files: ConfigurableFileCollection

    @get:OutputFile
    abstract val generalJson: RegularFileProperty

    @get:OutputFile
    abstract val specificJson: RegularFileProperty

    init {
        baseArtifactUrl.convention("https://www.astro.louisville.edu/software/astroimagej/updates/updatesjava17")
    }

    @TaskAction
    fun generateSpecificJson() {
        val version = version.get()

        val updateData = Json.decodeFromString<UpdateData>(
            providerFactory.fileContents(updateDataJson).asText.get()
                .replace("\$VERSION", version)
        )

        val specificVersion = SpecificVersion(
            version,
            null,
            buildArtifacts(version, updateData),
        )

        val json = Json { prettyPrint = true }
        val outputFile = specificJson.get().asFile

        outputFile.writeText(json.encodeToString(specificVersion))

        val versions = Json.decodeFromString<Versions>(providerFactory.fileContents(generalJson).asText.get())

        if (versions.versions.find { it.version == version } != null) {
            return
        }

        val newVersions = buildList {
            add(buildVersion(version, updateData, baseMetaUrl.get()))
            addAll(versions.versions)
        }

        generalJson.get().asFile.writeText(json.encodeToString(Versions(versions.metaVersion, newVersions)))
    }

    @OptIn(ExperimentalTime::class)
    fun buildVersion(version: String, updateData: UpdateData, baseUrl: String): Version {
        return Version(
            version = version,
            url = "$baseUrl/versions/$version.json",
            type = type(version),
            releaseTime = Clock.System.now(),
        )
    }

    fun filename(name: String, version: String): String {
        if (baseArtifactUrl.get().contains("github", true)) {
            return "$version/$name";
        }

        return "${name.substringBeforeLast(".")}${version.replace(".00", "")}.${name.substringAfterLast(".")}"
    }

    fun buildArtifacts(version: String, updateData: UpdateData): Array<com.astroimagej.updates.Artifact> {
        val artifacts = updateData.files.map {
            Artifact(
                it.artifact,
                "${baseArtifactUrl.get()}/${filename(it.artifact, version)}",
                getSha256(lookupFile(it.artifact)),
                it.os.toTypedArray(),
                it.arch.toTypedArray(),
                "${baseMetaUrl.get()}/signatures/${filename(it.artifact, version)}.sigstore.json",
                getSha256(lookupFile("${it.artifact}.sigstore.json"))
            )
        }

        return artifacts.toTypedArray()
    }

    fun lookupFile(name: String): File {
        val ffc = files.filter { it.name == name }

        if (ffc.isEmpty) {
            throw GradleException("Input collection is missing $name")
        }

        return ffc.singleFile
    }

    fun getSha256(file: File): String {
        val md = MessageDigest.getInstance("SHA256")
        md.update(file.readBytes())

        return HexFormat.of().formatHex(md.digest())
    }

    fun type(version: String): Type {
        return Type.valueOf(releaseType.get().uppercase(Locale.US))
    }

    @Serializable
    data class UpdateData(
        val files: List<Artifact>,
    )

    @Serializable
    data class Artifact(
        val artifact: String,
        val os: List<Os>,
        val arch: List<Arch>,
    )
}