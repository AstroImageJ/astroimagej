package com.astroimagej.meta.jdk

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.*

@Serializable
enum class RuntimeType {
    @SerialName("jdk") JDK,
    @SerialName("jre") JRE,
    @SerialName("jmods") JMODS,
    ;

    override fun toString(): String {
        return super.toString().lowercase(Locale.US)
    }
}