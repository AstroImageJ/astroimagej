package com.astroimagej.updates

import kotlinx.serialization.Serializable

@Serializable
data class SpecificVersion(
    val version: String,
    val message: String? = null,
    val artifacts: Array<Artifact>,
    val launchArg: Array<LaunchArg>? = null,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SpecificVersion) return false

        if (version != other.version) return false
        if (message != other.message) return false
        if (!artifacts.contentEquals(other.artifacts)) return false
        if (!launchArg.contentEquals(other.launchArg)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = version.hashCode()
        result = 31 * result + (message?.hashCode() ?: 0)
        result = 31 * result + artifacts.contentHashCode()
        result = 31 * result + (launchArg?.contentHashCode() ?: 0)
        return result
    }
}

@Serializable
data class LaunchArg(
    val arg: String,
    val os: Array<Os>? = null
    ) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is LaunchArg) return false

        if (arg != other.arg) return false
        if (!os.contentEquals(other.os)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = arg.hashCode()
        result = 31 * result + (os?.contentHashCode() ?: 0)
        return result
    }
}

@Serializable
data class Artifact(
    val name: String,
    val destination: String,
    val url: String,
    val sha256: String,
    val os: Array<Os>? = null,
    val requiresElevator: Boolean? = false,
    //todo signature url, and hash for signature file?
    ) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Artifact) return false

        if (name != other.name) return false
        if (destination != other.destination) return false
        if (url != other.url) return false
        if (sha256 != other.sha256) return false
        if (!os.contentEquals(other.os)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + destination.hashCode()
        result = 31 * result + url.hashCode()
        result = 31 * result + sha256.hashCode()
        result = 31 * result + (os?.contentHashCode() ?: 0)
        return result
    }
}

@Serializable
enum class Os {
    WINDOWS,
    MAC,
    LINUX,
}