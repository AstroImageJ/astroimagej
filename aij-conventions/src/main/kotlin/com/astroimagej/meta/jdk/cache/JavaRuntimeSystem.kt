package com.astroimagej.meta.jdk.cache

import com.astroimagej.meta.jdk.Architecture
import com.astroimagej.meta.jdk.OperatingSystem
import com.astroimagej.meta.jdk.RuntimeType
import kotlinx.serialization.Serializable
import org.apache.tools.ant.taskdefs.condition.Os

/**
 * A system to package AIJ for, and the Java runtime that will be bundled with it.
 *
 * Inputs: [ext], [arch], [os], [vendor], [javaVersion], [pgpKeyId], and optionally [type].
 * Everything else is resolved from the foojay Disco API (or Azul's metadata API, see `javaRuntimeSource`)
 * and stored in the cache.
 */
@Serializable
data class JavaRuntimeSystem(
    val ext: String,
    val arch: Architecture,
    val os: OperatingSystem,
    /** foojay distribution id (`zulu`, `temurin`, `corretto`, ...). Saved in the cache. */
    val vendor: String = DEFAULT_VENDOR,
    /** Java major version for this system, uses the project's `javaShippingVersion` when null. */
    val javaVersion: Int? = null,
    /** Fingerprint of the key the vendor signs with. Signatures are only verified when this is set. */
    val pgpKeyId: String? = null,
    var version: Int? = null,
    /** Java version of the resolved build, e.g. 27.0.1+9 */
    var fullVersion: String? = null,
    /** The vendor's own version of the resolved build, e.g. Zulu's 27.30.201 */
    var distributionVersion: String? = null,
    /** Which API the build was found with (`foojay` or `azul`). Saved in the cache. */
    var source: String? = null,
    var name: String? = null,
    var sha256: String? = null,
    var type: RuntimeType? = null,
    var url: String? = null,
    var sigUrl: String? = null,
    /** Id of the key the signature was made with, as reported by the API (informational, see [pgpKeyId]) */
    var sigKeyId: String? = null,
) {
    /** True once the runtime has been resolved. */
    fun isResolved(): Boolean = version != null && name != null && sha256 != null && url != null

    fun matchesCurrentSystem(): Boolean {
        return when (os) {
            OperatingSystem.MAC -> Os.isFamily(Os.FAMILY_MAC)
            OperatingSystem.LINUX -> Os.isFamily(Os.FAMILY_UNIX) && !Os.isFamily(Os.FAMILY_MAC)
            OperatingSystem.WINDOWS -> Os.isFamily(Os.FAMILY_WINDOWS)
        } && Architecture.getCurrentArch() == arch
    }

    companion object {
        const val DEFAULT_VENDOR = "zulu"
    }
}