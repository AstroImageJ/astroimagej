package com.astroimagej.tasks

import com.astroimagej.gui.InputType
import com.astroimagej.gui.WorkflowInput
import com.astroimagej.gui.showReleaseOptionsDialog
import com.astroimagej.updates.Type
import com.astroimagej.updates.Versions
import com.charleskorn.kaml.*
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
import java.net.URI
import java.net.URL
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import javax.inject.Inject
import javax.swing.SwingUtilities
import kotlin.math.max

abstract class TriggerReleaseWorkflow
@Inject constructor(private var providerFactory: ProviderFactory): DefaultTask() {
    @get:Input
    @get:Optional
    abstract val owner: Property<String>

    @get:InputFile
    @get:Optional
    abstract val workflowFile: RegularFileProperty

    @get:InputFile
    @get:Optional
    abstract val versionsFile: RegularFileProperty

    @get:Input
    @get:Optional
    abstract val token: Property<String>

    @get:Input
    @get:Optional
    abstract val releaseType: Property<String>

    @get:Input
    @get:Optional
    abstract val dryRun: Property<Boolean>

    init {
        //version.convention(providerFactory.gradleProperty("version"))
        releaseType.convention(providerFactory.gradleProperty("releaseType"))
        owner.convention("AstroImageJ/AstroImageJ")
        workflowFile.convention(project.layout.projectDirectory.file(".github/workflows/publish.yml"))
        versionsFile.convention(project.layout.projectDirectory.file("website/public/meta/versions.json"))
        token.convention(providerFactory.environmentVariable("GITHUB_TOKEN"))
        dryRun.convention(false)
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

        if (dryRun.get()) {
            logger.lifecycle("Workflow dispatched successfully (dry-run).")
            logger.lifecycle(payload.toString())
            return
        }

        val response = client.send(request, HttpResponse.BodyHandlers.ofString())

        when (response.statusCode()) {
            204 -> logger.lifecycle("Workflow dispatched successfully (HTTP 204).")
            else -> throw IllegalStateException(
                "Failed to dispatch workflow. HTTP ${response.statusCode()} - ${response.body()}"
            )
        }
    }

    fun latestVersion(): Pair<String, List<String>> {
        val versions = Json.decodeFromString<Versions>(URL("https://astroimagej.com/meta/versions.json").readText())
        val vs = versions.versions.map { it.version }
        val latest = vs.map { SemanticVersion(it) }.maxOf { it }
        return Pair(latest.toString(), vs)
    }

    // Default values used for inputs if not specified
    fun createInputs(): Map<String, String> {
        var options: Map<String, String> = emptyMap()
        val (latestVersion, allVersions) = latestVersion()

        SwingUtilities.invokeAndWait {
            options = showReleaseOptionsDialog(null, Type.valueOf(releaseType.get()), latestVersion, readOptions())
            if (options.isNotEmpty()) {
                println("Collected options: $options")
            } else {
                println("Dialog cancelled")
            }
        }

        if (options.isEmpty()) {
            return mapOf()
        }

        if (options.size > 10) {
            throw IllegalArgumentException("Maximum 10 inputs are allowed by Github")
        }

        if (allVersions.contains(options["version"])) {
            throw IllegalArgumentException("Version ${options["version"]} already exists")
        }

        return options
    }

    private fun readOptions(): Map<String, WorkflowInput> {
        val f = workflowFile.get().asFile
        val yaml = Yaml(configuration = YamlConfiguration(strictMode = false, polymorphismStyle = PolymorphismStyle.Property))
        val rootNode = yaml.parseToYamlNode(f.readText())

        val onNode = rootNode.yamlMap.get<YamlNode>("on") ?: return emptyMap()

        val onMap = onNode.yamlMap
        val wdNode = onMap.get<YamlNode>("workflow_dispatch") ?: return emptyMap()

        if (wdNode is YamlNull) return emptyMap()

        val wdMap = wdNode.yamlMap
        val inputsMap = wdMap.get<YamlMap>("inputs") ?: return emptyMap()

        val result = mutableMapOf<String, WorkflowInput>()

        // inputsMap.entries is Map<YamlNode, YamlNode>
        inputsMap.entries.forEach { (keyNode, valueNode) ->
            val key = keyNode.yamlScalar.content

            valueNode.yamlMap.let { m ->
                val desc = m.get<YamlScalar>("description")?.content
                val required = m.get<YamlScalar>("required")?.content?.toBoolean() ?: false
                val def = m.get<YamlScalar>("default")?.content
                val typeStr = m.get<YamlScalar>("type")?.content
                val options = m.get<YamlList>("options")?.items?.map { it.yamlScalar.content }
                val type = when (typeStr?.lowercase()) {
                    "boolean" -> InputType.BOOLEAN
                    "choice" -> InputType.CHOICE
                    else -> InputType.STRING
                }
                result[key] = WorkflowInput(description = desc, required = required, default = def, type = type, options = options)
                return@forEach
            }
        }

        result.forEach { (k, v) ->
            logger.debug(
                "input: {} -> description='{}', required={}, default='{}', type={}",
                k,
                v.description,
                v.required,
                v.default,
                v.type
            )
        }

        return result
    }

    @Serializable
    data class Payload(val ref: String, val inputs: Map<String, String>) {
        override fun toString(): String {
            return "ref: $ref, inputs: $inputs"
        }
    }

    data class SemanticVersion(val core: IntArray) : Comparable<SemanticVersion> {
        constructor(version: String) : this(getCore(version))

        companion object {
            fun getCore(s: String): IntArray {
                var s = s
                val start = s.indexOf('+')
                if (start != -1) s = s.take(start)
                var end = s.indexOf('-')
                if (end == -1) end = s.length

                val parts: Array<String?> =
                    s.take(end).split("\\.".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()

                val core = IntArray(parts.size)
                for (i in parts.indices) {
                    core[i] = parts[i]!!.toInt()
                }

                return core
            }
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is SemanticVersion) return false

            if (!core.contentEquals(other.core)) return false

            return true
        }

        override fun hashCode(): Int {
            return core.contentHashCode()
        }

        override fun compareTo(other: SemanticVersion): Int {
            for (i in 0..<max(this.core.size, other.core.size)) {
                val thisVal = if (i < this.core.size) this.core[i] else 0
                val otherVal: Int = (if (i < other.core.size) other.core[i] else 0)

                if (thisVal != otherVal) {
                    return thisVal.compareTo(otherVal)
                }
            }

            return 0
        }

        override fun toString(): String {
            val sb = StringBuilder()
            for (i in core.indices) {
                // Add leading 0 for AIJ daily builds
                /*if (i == 3) {
                    sb.append('0')
                }*/

                sb.append(core[i])
                if (i < core.size - 1) {
                    sb.append('.')
                }
            }

            return sb.toString()
        }
    }
}