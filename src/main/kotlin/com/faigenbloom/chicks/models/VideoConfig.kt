package com.example.models

import kotlinx.serialization.Serializable

@Serializable
data class VideoConfig(val videos: Map<String, String>)