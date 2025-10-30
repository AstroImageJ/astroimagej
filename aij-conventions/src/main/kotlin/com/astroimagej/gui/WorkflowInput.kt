package com.astroimagej.gui

import kotlinx.serialization.Serializable

@Serializable
data class WorkflowInput(
    val description: String? = null,
    val required: Boolean = false,
    val default: String? = null,
    val type: InputType = InputType.STRING,
    val options: List<String>? = null,
)