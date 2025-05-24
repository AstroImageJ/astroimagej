package com.astroimagej.updates

import kotlinx.serialization.Serializable

@Serializable
data class Versions(val metaVersion: MetaVersion, val versions: List<Version>)

@Serializable
data class MetaVersion(val major: Int, val minor: Int)

@Serializable
data class Version(val version: String, val url: String, val type: Type,
                   val maxJava: Int? = null, val minJava: Int? = null,
)

@Serializable
enum class Type {
    RELEASE,
    PRERELEASE
}