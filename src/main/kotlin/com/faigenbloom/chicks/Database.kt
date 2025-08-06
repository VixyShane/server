package com.faigenbloom.chicks

import com.example.models.BabeConfig
import com.faigenbloom.chicks.models.DownloadsConfig
import com.faigenbloom.chicks.models.Likes
import com.faigenbloom.chicks.models.PaymentData
import com.faigenbloom.chicks.models.UserPayment
import kotlinx.serialization.json.Json
import java.io.File
import kotlin.collections.set

class Database(private val prefix: String) {
    val liked = HashMap<String, ArrayList<String>>()
    val payedUsers = ArrayList<String>()
    val downloadsConfig: DownloadsConfig
    val babeConfig: BabeConfig
    val json = Json { ignoreUnknownKeys = true }

    init {
        downloadsConfig = Json.decodeFromString<DownloadsConfig>(getFile("downloads-config.json"))
        babeConfig = Json.decodeFromString<BabeConfig>(getFile("babe-config.json"))
    }

    fun getLiked(userId: String, girlName: String): ArrayList<String> {
        if (liked.contains(userId)) {
            return liked[userId]!!
        } else {
            val stringFromStore = SimpleStore.get("$userId$girlName")
            if (stringFromStore.isNullOrBlank()) {
                return arrayListOf()
            } else {
                liked[userId] = json.decodeFromString<Likes>(stringFromStore).likes
            }
            if (liked.contains(userId)) {
                return liked[userId]!!
            }
            return arrayListOf()
        }
    }

    fun like(girlName: String, fileName: String, userId: String) {
        if (liked.contains(userId)) {
            if (liked[userId]!!.contains(fileName)) {
                liked[userId]!!.remove(fileName)
                SimpleStore.put("$userId$girlName", json.encodeToString(Likes(likes = liked[userId]!!)))
            } else {
                liked[userId]!!.add(fileName)
                SimpleStore.put("$userId$girlName", json.encodeToString(Likes(likes = liked[userId]!!)))
            }
        } else {
            liked[userId] = arrayListOf(fileName)
            SimpleStore.put("$userId$girlName", json.encodeToString(Likes(likes = liked[userId]!!)))
        }
    }

    private fun isUserUnlocked(userId: String): Boolean {
        return payedUsers.contains(userId) || run {
            val sringFromStore = SimpleStore.get(userId)
            if (!sringFromStore.isNullOrBlank()) {
                val fromStore = json.decodeFromString<UserPayment>(sringFromStore)
                if (fromStore.payerId.isNotBlank() && System.currentTimeMillis() - fromStore.date.toLong() < 20 * 24 * 60 * 60 * 1000) {
                    payedUsers.add(userId)
                    true
                } else {
                    false
                }
            } else false
        }
    }

    fun getImagesList(): List<String> {
        return downloadsConfig.items.map { it.id }
    }

    private fun getFile(name: String): String {
        return this::class.java.classLoader
            .getResource("$prefix/$name")
            ?.readText() ?: ""
    }

    fun getImageLink(fileName: String, userId: String): String {
        return if (fileName == "horizontal.png" || fileName == "fab.png" || fileName == "fab2.png") {
            "${babeConfig.domain}/uploads/$fileName"
        } else {
            val config = downloadsConfig.items.first { it.id == fileName }
            if (config.blurred.isNotBlank() && isUserUnlocked(userId).not()) {
                config.getBlurredAsUrl(babeConfig.domain)
            } else {
                config.getOpenAsUrl(babeConfig.domain)
            }
        }
    }

    fun getVideosList(): Map<String, String> {
        val map = HashMap<String, String>()

        downloadsConfig.items.forEach { if (it.video.isNotBlank()) map[it.id] = it.video }
        return map
    }

    fun getBlocked(clientId: String): List<String> {
        return if (isUserUnlocked(clientId)) {
            return listOf()
        } else {
            downloadsConfig.items.mapNotNull { if (it.blurred.isNotBlank()) it.id else null }
        }
    }

    fun getVideoFromPhoto(userId: String, fileName: String): String {
        val config = downloadsConfig.items.first { it.id == fileName }
        return if (config.blurred.isNotBlank() && isUserUnlocked(userId).not()) {
            ""
        } else {
            config.getVideoAsUrl(babeConfig.domain)
        }
    }

    suspend fun checkPayment(clientId: String, payment: PaymentData): Boolean {
        val isValid = verifyPayPalOrder(payment.orderId, payment.amount, payment.currency)
        if (isValid) {
            SimpleStore.put(
                clientId,
                Json.encodeToString(UserPayment(prefix, payment.payerId, System.currentTimeMillis().toString()))
            )
            payedUsers.add(clientId)

        }
        return isValid
    }

    fun loadBabeData(): BabeConfig {
        return babeConfig
    }
}