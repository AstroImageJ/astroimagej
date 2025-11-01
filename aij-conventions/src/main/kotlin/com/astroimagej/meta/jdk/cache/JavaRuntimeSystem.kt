package com.astroimagej.meta.jdk.cache

import com.astroimagej.meta.jdk.Architecture
import com.astroimagej.meta.jdk.OperatingSystem
import com.astroimagej.meta.jdk.RuntimeType
import kotlinx.serialization.Serializable
import org.apache.tools.ant.taskdefs.condition.Os

@Serializable
data class JavaRuntimeSystem(
    val ext: String,
    val arch: Architecture,
    val os: OperatingSystem,
    var version: Int? = null,
    var name: String? = null,
    var sha256: String? = null,
    var type: RuntimeType? = null,
    var url: String? = null,
    var sigUrl: String? = null,
    var jmodName: String? = null,
    var jmodUrl: String? = null,
    var jmodSha256: String? = null,
    var jmodSigUrl: String? = null,
) {

    fun matchesCurrentSystem(): Boolean {
        return when (os) {
            OperatingSystem.MAC -> Os.isFamily(Os.FAMILY_MAC)
            OperatingSystem.LINUX -> Os.isFamily(Os.FAMILY_UNIX)
            OperatingSystem.WINDOWS -> Os.isFamily(Os.FAMILY_WINDOWS)
        } && Architecture.getCurrentArch() == arch
    }
}