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
}