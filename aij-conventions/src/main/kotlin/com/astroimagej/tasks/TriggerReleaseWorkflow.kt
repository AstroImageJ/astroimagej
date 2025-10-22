package com.astroimagej.tasks

import com.astroimagej.gui.ReleaseOptions
import com.astroimagej.gui.showReleaseOptionsDialog
import com.astroimagej.updates.Type
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.ProviderFactory
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.support.uppercaseFirstChar
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import javax.inject.Inject
import javax.swing.SwingUtilities

abstract class TriggerReleaseWorkflow
@Inject constructor(private var providerFactory: ProviderFactory): DefaultTask() {
    @get:Input
    @get:Optional
    abstract val owner: Property<String>

    @get:InputFile
    @get:Optional
    abstract val workflowFile: RegularFileProperty

    @get:Input
    @get:Optional
    abstract val version: Property<String>

    @get:Input
    @get:Optional
    abstract val token: Property<String>

    @get:Input
    @get:Optional
    abstract val releaseType: Property<String>

    init {
        version.convention(providerFactory.gradleProperty("version"))
        releaseType.convention(providerFactory.gradleProperty("releaseType"))
        owner.convention("AstroImageJ/AstroImageJ")
        workflowFile.convention(project.layout.projectDirectory.file(".github/workflows/publish.yml"))
        token.convention(providerFactory.environmentVariable("GITHUB_TOKEN"))
    }

    @TaskAction
    fun run() {
        val token = token.orNull
            ?: throw IllegalStateException("GITHUB_TOKEN environment variable is not set")

        val apiUrl = "https://api.github.com/repos/${owner.get()}/actions/workflows/${workflowFile.get().asFile.name}/dispatches"

        val inputs = createInputs()

        if (inputs.isEmpty()) {
            logger.lifecycle("No inputs were specified. Skipping dispatch.")
            return
        }

        logger.lifecycle("Inputs: $inputs")

        // Default values used for inputs if not specified
        val payload = Payload("master", inputs)

        logger.lifecycle("Dispatching workflow...")

        val client = HttpClient.newBuilder().build()
        val request = HttpRequest.newBuilder()
            .uri(URI.create(apiUrl))
            .header("Accept", "application/vnd.github+json")
            .header("Authorization", "Bearer $token")
            .header("X-GitHub-Api-Version", "2022-11-28")
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(Json.encodeToString(payload)))
            .build()

        val response = client.send(request, HttpResponse.BodyHandlers.ofString())

        when (response.statusCode()) {
            204 -> logger.lifecycle("Workflow dispatched successfully (HTTP 204).")
            else -> throw IllegalStateException(
                "Failed to dispatch workflow. HTTP ${response.statusCode()} - ${response.body()}"
            )
        }
    }

    // Default values used for inputs if not specified
    fun createInputs(): Map<String, String> {
        val inputs = mutableMapOf<String, String>()

        var options: ReleaseOptions? = null
        SwingUtilities.invokeAndWait {
            options = showReleaseOptionsDialog(null, Type.valueOf(releaseType.get()), version.get())
            if (options != null) {
                println("Collected options: $options")
            } else {
                println("Dialog cancelled")
            }
        }

        if (options == null) {
            return mapOf()
        }

        options?.let {
            inputs["version"] = options!!.version
            inputs["skip_release"] = options!!.skipRelease.toString()
            inputs["notarize"] = options!!.notarize.toString()
            inputs["windows_sign"] = options!!.windowsSign.toString()
            inputs["crosspackage"] = options!!.crosspackage.toString()
            inputs["release_type"] = options!!.releaseType.name.lowercase().uppercaseFirstChar()
        }

        if (inputs.size > 10) {
            throw IllegalArgumentException("Maximum 10 inputs are allowed by Github")
        }

        return inputs
    }

    @Serializable
    data class Payload(val ref: String, val inputs: Map<String, String>) {
        override fun toString(): String {
            return "ref: $ref, inputs: $inputs"
        }
    }
}