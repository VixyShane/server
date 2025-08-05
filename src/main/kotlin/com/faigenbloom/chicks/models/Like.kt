package com.faigenbloom.chicks.models

import kotlinx.serialization.Serializable

@Serializable
data class Likes (
    val likes : ArrayList<String>
)