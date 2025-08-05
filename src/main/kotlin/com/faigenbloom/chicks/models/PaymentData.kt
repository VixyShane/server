package com.faigenbloom.chicks.models

import kotlinx.serialization.Serializable

@Serializable
data class PaymentData(
    val orderId: String,
    val payerId: String,
    val amount: String,
    val currency: String
)
