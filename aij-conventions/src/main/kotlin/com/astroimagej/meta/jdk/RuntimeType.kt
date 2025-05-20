package com.astroimagej.meta.jdk

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class RuntimeType {
    @SerialName("jdk") JDK,
    @SerialName("jre") JRE,
    ;
}