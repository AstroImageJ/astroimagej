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
        @SerialName("distribution_version") val distributionVersion: String = "",
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

    // Azul metadata API, https://api.azul.com/metadata/v1/docs/swagger
    @Serializable
    private data class AzulPackage(
        val name: String,
        @SerialName("download_url") val downloadUrl: String,
        @SerialName("java_version") val javaVersion: List<Int> = emptyList(),
        @SerialName("distro_version") val distroVersion: List<Int> = emptyList(),
        @SerialName("openjdk_build_number") val buildNumber: Int? = null,
        // Only present because it is requested through include_fields
        @SerialName("sha256_hash") val sha256: String? = null,
        val signatures: List<AzulSignature> = emptyList(),
    )

    @Serializable
    private data class AzulSignature(
        val type: String = "",
        val url: String = "",
        val details: AzulSignatureDetails? = null,
    )

    @Serializable
    private data class AzulSignatureDetails(
        @SerialName("key_id") val keyId: String? = null,
    )

    /** Where to look up runtimes. */
    enum class Source {
        FOOJAY,
        AZUL;

        companion object {
            fun parse(value: String): Source = entries.firstOrNull { it.name.equals(value.trim(), ignoreCase = true) }
                ?: throw IllegalArgumentException(
                    "Unknown Java runtime source '$value', expected one of ${entries.joinToString { it.name.lowercase() }}"
                )
        }
    }

    data class JdkInfo(
        val vendor: String,
        /** Major Java version, e.g. 21 */
        val version: Int,
        /** Full version string, e.g. 21.0.5+11 */
        val fullVersion: String,
        /** The vendor's own version, e.g. Zulu's 21.38.21 */
        val distributionVersion: String,
        val name: String,
        val sha256: String,
        val type: RuntimeType,
        val url: String,
        /** Detached signature of the archive, if the vendor publishes one. */
        val sigUrl: String?,
        /** Id of the key the signature was made with, as reported by the API. Informational only. */
        val sigKeyId: String? = null,
    )

    companion object {
        private const val API_BASE = "https://api.foojay.io/disco/v3.0"
        private const val AZUL_API_BASE = "https://api.azul.com/metadata/v1/zulu"

        // Ignore unknown keys so we don't have to model every field
        private val format = Json { ignoreUnknownKeys = true }

        private fun osParam(os: OperatingSystem) = when (os) {
            OperatingSystem.MAC -> "macos"
            OperatingSystem.LINUX -> "linux"
            OperatingSystem.WINDOWS -> "windows"
        }

        // Compares version numbers segment by segment, so 27.30.201 > 27.30.107 and 21.0.10 > 21.0.9
        private val versionOrder = Comparator<List<Int>> { a, b ->
            for (i in 0 until maxOf(a.size, b.size)) {
                val c = a.getOrElse(i) { 0 }.compareTo(b.getOrElse(i) { 0 })
                if (c != 0) return@Comparator c
            }
            0
        }

        private fun enc(s: String) = URLEncoder.encode(s, Charsets.UTF_8)

        private fun fetch(url: String) = URI(url).toURL().readText()

        /**
         * Find the latest GA package of [vendor] for the given Java major version and platform.
         *
         * @param source the API to ask. foojay is slow to pick up new releases, so Azul's own API
         * can be used instead for Zulu. Azul only has Zulu, so other vendors always use foojay.
         * @param type the package type to look for, `jre` when null
         * @return the package, or null if the API has no matching package
         * @throws Exception if the API cannot be reached, or the package has no usable SHA-256 checksum
         */
        fun findPackage(
            source: Source,
            vendor: String,
            majorVersion: Int,
            type: RuntimeType?,
            os: OperatingSystem,
            arch: Architecture,
            ext: String,
        ): JdkInfo? {
            return if (source == Source.AZUL && vendor == "zulu") {
                findAzulPackage(majorVersion, type, os, arch, ext)
            } else {
                findFoojayPackage(vendor, majorVersion, type, os, arch, ext)
            }
        }

        /**
         * Find the newest GA Zulu community build using Azul's metadata API.
         * The API does the filtering, so the checksum and signature are requested along with the package list.
         */
        private fun findAzulPackage(
            majorVersion: Int,
            type: RuntimeType?,
            os: OperatingSystem,
            arch: Architecture,
            ext: String,
        ): JdkInfo? {
            val packageType = type?.name?.lowercase() ?: "jre"

            val azulArch = when (arch) {
                Architecture.X86_64 -> "x64"
                Architecture.ARM_64 -> "aarch64"
                else -> error("Unsupported architecture for the Azul API: $arch")
            }

            val azulOs = when (os) {
                OperatingSystem.WINDOWS -> "windows"
                OperatingSystem.LINUX -> "linux_glibc"
                OperatingSystem.MAC -> "macos"
            }

            val query = buildList {
                add("java_version" to majorVersion.toString())
                add("os" to azulOs)
                add("arch" to azulArch)
                add("archive_type" to ext)
                add("java_package_type" to packageType)
                // Plain builds only
                add("javafx_bundled" to "false")
                add("crac_supported" to "false")
                add("crs_supported" to "false")
                add("latest" to "true")
                add("release_status" to "ga")
                // Community builds
                add("availability_types" to "ca")
                // The response only contains these if asked for
                add("include_fields" to "sha256_hash")
                add("include_fields" to "signatures")
                add("page" to "1")
                add("page_size" to "1")
            }.joinToString("&") { (k, v) -> "$k=${enc(v)}" }

            val packages = format.decodeFromString<List<AzulPackage>>(fetch("$AZUL_API_BASE/packages/?$query"))

            val pkg = packages.first()

            val sha256 = pkg.sha256?.takeIf { it.isNotBlank() }
                ?: error("Azul package ${pkg.name} has no sha256 hash")

            val signature = pkg.signatures.firstOrNull { it.type == "openpgp" && it.url.isNotBlank() }

            return JdkInfo(
                vendor = "zulu",
                version = majorVersion,
                fullVersion = pkg.javaVersion.joinToString(".") + (pkg.buildNumber?.let { "+$it" } ?: ""),
                distributionVersion = pkg.distroVersion.joinToString("."),
                name = pkg.name,
                sha256 = sha256,
                type = if (packageType == "jdk") RuntimeType.JDK else RuntimeType.JRE,
                url = pkg.downloadUrl,
                sigUrl = signature?.url,
                sigKeyId = signature?.details?.keyId,
            )
        }

        private fun findFoojayPackage(
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
                distributionVersion = pkg.distributionVersion,
                name = details.filename,
                sha256 = checksum,
                type = pkg.packageType,
                url = url,
                sigUrl = details.signatureUri?.takeIf { it.isNotBlank() },
            )
        }
    }
}