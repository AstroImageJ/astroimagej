package com.astroimagej.meta.jdk

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class Architecture {
    @SerialName("aarch64") ARM_64,
    @SerialName("x64") X86_64,
    ;

    override fun toString(): String {
        return when (this) {
            ARM_64 -> "aarch64"
            X86_64 -> "x64"
        }
    }

    companion object {
        fun getCurrentArch(): Architecture {
            return when (System.getProperty("os.arch").lowercase()) {
                "amd64", "x86_64", "x86-64", "x8664", "ia32e", "em64t", "x64" -> Architecture.X86_64
                "aarch64", "arm" -> Architecture.ARM_64
                "x86", "i386", "i486", "i586", "i686", "x8632", "ia32", "x32" -> throw UnsupportedOperationException("32-bit architecture is not supported")
                else -> throw UnsupportedOperationException("Unknown architecture: " + System.getProperty("os.arch"))
            }
        }
    }
}