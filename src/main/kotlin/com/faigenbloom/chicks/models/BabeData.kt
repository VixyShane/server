package com.example.models

import kotlinx.serialization.Serializable

@Serializable
data class BabeData (
    val name: String,
    val description: String,
    val message: String,
    val instagram: String,
    val tiktok: String,
    val donations: Int,
    val publications: Int,
)