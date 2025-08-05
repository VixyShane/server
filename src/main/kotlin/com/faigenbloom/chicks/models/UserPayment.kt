package com.faigenbloom.chicks.models

import kotlinx.serialization.Serializable

@Serializable
data class UserPayment(
    val ownerName: String = "",
    val payerId: String = "",
    val date: String = "",
)

