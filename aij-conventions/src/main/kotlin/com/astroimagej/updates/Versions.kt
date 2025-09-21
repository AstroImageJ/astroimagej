package com.astroimagej.updates

import kotlinx.serialization.Serializable
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@Serializable
data class Versions(val metaVersion: MetaVersion, val versions: List<Version>)

@Serializable
data class MetaVersion(val major: Int, val minor: Int)

@OptIn(ExperimentalTime::class)
@Serializable
data class Version(val version: String, val url: String, val type: Type,
                   val releaseTime: Instant,
)

@Serializable
enum class Type {
    RELEASE,
    PRERELEASE,
    DAILY_BUILD,
}