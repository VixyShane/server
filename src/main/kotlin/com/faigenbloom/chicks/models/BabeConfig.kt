package com.example.models

import kotlinx.serialization.Serializable

@Serializable
data class BabeConfig (
    val name: String,
    val domain: String,
    val description: String,
    val message: String,
    val instagram: String,
    val tiktok: String,
    val donations: Int,
    val publications: Int,
)