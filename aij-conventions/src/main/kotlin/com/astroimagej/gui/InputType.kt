package com.astroimagej.gui

import kotlinx.serialization.SerialName

enum class InputType {
    @SerialName("string")
    STRING,
    @SerialName("boolean")
    BOOLEAN,
    @SerialName("choice")
    CHOICE,
}