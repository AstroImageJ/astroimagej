package com.astroimagej.meta.jdk

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.*

@Serializable
enum class OperatingSystem {
    @SerialName("mac") MAC,
    @SerialName("linux") LINUX,
    @SerialName("windows") WINDOWS,
    ;

    override fun toString(): String {
        return super.toString().lowercase(Locale.US)
    }
}