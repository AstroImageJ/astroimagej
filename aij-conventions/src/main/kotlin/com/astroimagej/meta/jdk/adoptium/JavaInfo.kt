package com.astroimagej.meta.jdk.adoptium

import com.astroimagej.meta.jdk.RuntimeType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.net.URL

class JavaInfo {
    @Serializable
    private data class RawRelease(
        val binary: RawBinary,
        val version: RawVersion
    )

    @Serializable
    private data class RawBinary(
        @SerialName("image_type") val imageType: RuntimeType,
        @SerialName("package") val pkg: RawPackage
    )

    @Serializable
    private data class RawPackage(
        val name: String,
        val checksum: String,
        val link: String
    )

    @Serializable
    private data class RawVersion(
        val major: Int
    )

    data class JdkInfo(
        val version: Int,
        val name: String,
        val sha256: String,
        val type: RuntimeType,
        val url: String
    )

    companion object {
        fun parseJdkInfoFromUrl(jsonString: URL): List<JdkInfo> {
            // Use ignoreUnknownKeys so we don’t have to model every field
            val format = Json { ignoreUnknownKeys = true }
            val rawList: List<RawRelease> = format.decodeFromString(jsonString.readText())

            return rawList.map { raw ->
                JdkInfo(
                    version = raw.version.major,
                    name = raw.binary.pkg.name,
                    sha256 = raw.binary.pkg.checksum,
                    type = raw.binary.imageType,
                    url = raw.binary.pkg.link
                )
            }
        }
    }
}