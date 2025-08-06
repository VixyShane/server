package com.faigenbloom.chicks.models

import kotlinx.serialization.Serializable

@Serializable
data class DownloadsConfig(
    val items: List<Item>
)
@Serializable
data class Item(
    val id: String,
    val open: String,
    val blurred: String,
    val video: String,
){
    fun getBlurredAsUrl(domain:String): String {
        return "${domain}/uploads/blurred/$blurred"
    }
    fun getOpenAsUrl(domain:String): String {
        return "${domain}/uploads/images/$open"
    }
    fun getVideoAsUrl(domain:String): String {
        return "${domain}/uploads/videos/$video"
    }
}

