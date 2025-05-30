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

abstract class GenerateMetadata
@Inject constructor(private var providerFactory: ProviderFactory): DefaultTask() {
    @get:Input
    abstract val baseArtifactUrl: Property<String>

    @get:Input
    abstract val baseMetaUrl: Property<String>

    @get:Input
    abstract val version: Property<String>

    @get:Input
    abstract val minJava: Property<Int>

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

        val updateData = Json.decodeFromString<UpdateData>(providerFactory.fileContents(updateDataJson).asText.get())

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
            add(buildVersion(version, updateData, minJava.get(), baseMetaUrl.get()))
            addAll(versions.versions)
        }

        generalJson.get().asFile.writeText(json.encodeToString(Versions(versions.metaVersion, newVersions)))
    }

    fun buildVersion(version: String, updateData: UpdateData, minJava: Int, baseUrl: String): Version {
        return Version(
            version = version,
            url = "$baseUrl/versions/$version.json",
            type = type(version),
            updateData.maxJava,
            minJava,
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
                it.destination,
                "${baseArtifactUrl.get()}/${filename(it.artifact, version)}",
                getSha256(lookupFile(it.artifact)),
                it.os?.toTypedArray(),
                requiresElevator = it.requiresElevator,
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
        if (version.contains(Regex("[a-zA-Z]"))) {
            return Type.PRERELEASE
        }

        val r = Regex("^(?<major>0|[1-9]\\d*)\\.(?<minor>0|[1-9]\\d*)\\.(?<patch>0|[1-9]\\d*)\\.(?<daily>[0-9]\\d*)")
        val m = r.find(version)
        if (m != null && m.groups["daily"]?.value != "00") {
            return Type.PRERELEASE
        }

        return Type.RELEASE
    }

    @Serializable
    data class UpdateData(
        val maxJava: Int? = null,
        val files: List<Artifact>,
    )

    @Serializable
    data class Artifact(
        val requiresElevator: Boolean? = false,
        val artifact: String,
        val destination: String,
        val os: List<Os>? = null,
    )
}