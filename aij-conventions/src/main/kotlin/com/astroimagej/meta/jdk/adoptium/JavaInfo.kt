package com.astroimagej.meta.jdk.adoptium

import com.astroimagej.meta.jdk.Architecture
import com.astroimagej.meta.jdk.OperatingSystem
import com.astroimagej.meta.jdk.RuntimeType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.net.URI
import java.net.URLEncoder

class JavaInfo {
    @Serializable
    private data class PackagesResponse(
        val result: List<RawPackage> = emptyList(),
    )

    @Serializable
    private data class RawPackage(
        val id: String,
        val distribution: String = "",
        @SerialName("major_version") val majorVersion: Int,
        @SerialName("java_version") val javaVersion: String = "",
        @SerialName("package_type") val packageType: RuntimeType,
        val filename: String = "",
    )

    @Serializable
    private data class DetailsResponse(
        val result: List<RawDetails> = emptyList(),
    )

    @Serializable
    private data class RawDetails(
        val filename: String,
        @SerialName("direct_download_uri") val directDownloadUri: String? = null,
        @SerialName("signature_uri") val signatureUri: String? = null,
        val checksum: String? = null,
        @SerialName("checksum_uri") val checksumUri: String? = null,
        @SerialName("checksum_type") val checksumType: String? = null,
    )

    data class JdkInfo(
        val vendor: String,
        /** Major Java version, e.g. 21 */
        val version: Int,
        /** Full version string, e.g. 21.0.5+11 */
        val fullVersion: String,
        val name: String,
        val sha256: String,
        val type: RuntimeType,
        val url: String,
        /** Detached signature of the archive, if the vendor publishes one. */
        val sigUrl: String?,
    )

    companion object {
        private const val API_BASE = "https://api.foojay.io/disco/v3.0"

        // Ignore unknown keys so we don't have to model every field
        private val format = Json { ignoreUnknownKeys = true }

        private fun osParam(os: OperatingSystem) = when (os) {
            OperatingSystem.MAC -> "macos"
            OperatingSystem.LINUX -> "linux"
            OperatingSystem.WINDOWS -> "windows"
        }

        private fun enc(s: String) = URLEncoder.encode(s, Charsets.UTF_8)

        private fun fetch(url: String) = URI(url).toURL().readText()

        /**
         * Find the latest GA package of [vendor] for the given Java major version and platform.
         *
         * @param type the package type to look for, `jre` when null
         * @return the package, or null if foojay has no matching package
         * @throws Exception if the API cannot be reached, or the package has no usable SHA-256 checksum
         */
        fun findPackage(
            vendor: String,
            majorVersion: Int,
            type: RuntimeType?,
            os: OperatingSystem,
            arch: Architecture,
            ext: String,
        ): JdkInfo? {
            val packageType = type?.name?.lowercase() ?: "jre"

            val query = buildList {
                add("version" to majorVersion.toString())
                add("distro" to vendor)
                add("architecture" to arch.toString())
                add("operating_system" to osParam(os))
                add("archive_type" to ext)
                add("package_type" to packageType)
                add("release_status" to "ga")
                add("latest" to "available")
                add("directly_downloadable" to "true")
                add("javafx_bundled" to "false")
                // Otherwise musl/alpine builds can show up in the results
                if (os == OperatingSystem.LINUX) add("lib_c_type" to "glibc")
            }.joinToString("&") { (k, v) -> "$k=${enc(v)}" }

            val packages = format.decodeFromString<PackagesResponse>(fetch("$API_BASE/packages?$query"))
                .result
                .filter { it.majorVersion == majorVersion }

            val pkg = packages.firstOrNull() ?: return null

            val details = format.decodeFromString<DetailsResponse>(fetch("$API_BASE/ids/${pkg.id}"))
                .result.firstOrNull() ?: error("foojay returned no details for package ${pkg.id}")

            val url = details.directDownloadUri?.takeIf { it.isNotBlank() }
                ?: error("foojay package ${pkg.id} (${details.filename}) has no direct download link")

            val checksumType = details.checksumType?.lowercase()?.replace("-", "")
            if (checksumType != "sha256") {
                error("Package ${details.filename} has checksum type '${details.checksumType}', only sha256 is supported")
            }

            // Some vendors only provide the checksum as a separate file, formatted as "<hash>  <filename>"
            val checksum = details.checksum?.takeIf { it.isNotBlank() }
                ?: details.checksumUri?.takeIf { it.isNotBlank() }?.let {
                    fetch(it).trim().split(Regex("\\s+")).firstOrNull()
                }
                ?: error("Package ${details.filename} has no checksum")

            return JdkInfo(
                vendor = vendor,
                version = pkg.majorVersion,
                fullVersion = pkg.javaVersion,
                name = details.filename,
                sha256 = checksum,
                type = pkg.packageType,
                url = url,
                sigUrl = details.signatureUri?.takeIf { it.isNotBlank() },
            )
        }
    }
}