package com.astroimagej.meta.jdk.cache

import com.astroimagej.meta.jdk.Architecture
import com.astroimagej.meta.jdk.OperatingSystem
import com.astroimagej.meta.jdk.RuntimeType
import kotlinx.serialization.Serializable

@Serializable
data class JavaRuntimeSystem(
    val ext: String,
    val arch: Architecture,
    val os: OperatingSystem,
    var version: Int? = null,
    var name: String? = null,
    var sha256: String? = null,
    var type: RuntimeType? = null,
    var url: String? = null
)