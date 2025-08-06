package com.faigenbloom.chicks

import com.example.models.BabeData
import com.example.models.BlurConfig
import com.faigenbloom.chicks.models.PaymentData
import com.example.models.VideoConfig
import com.faigenbloom.chicks.models.Likes
import com.faigenbloom.chicks.models.UserPayment
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import kotlinx.serialization.json.Json
import verifyPayPalOrder
import java.awt.image.BufferedImage
import java.awt.image.ConvolveOp
import java.awt.image.Kernel
import java.io.File
import javax.imageio.ImageIO
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

class Database(private val prefix: String) {
    val blurConfig = loadBlurConfig()
    val tempFiles = HashMap<File, File?>()
    val liked = HashMap<String, ArrayList<String>>()
    val videosList = HashMap<String, String>()
    val payedUsers = ArrayList<String>()
    val json = Json { ignoreUnknownKeys = true }

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

    fun isUserUnlocked(userId: String): Boolean {
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

    fun blurImage(file: File): File {
        return tempFiles[file] ?: run {
            val image = ImageIO.read(file)
            var blurred = BufferedImage(image.width, image.height, BufferedImage.TYPE_INT_ARGB)
            val gaussian11x11 = floatArrayOf(
                0.00000067f, 0.00002292f, 0.00019117f, 0.00038771f, 0.00019117f, 0.00002292f, 0.00000067f,
                0.00002292f, 0.00078634f, 0.00655601f, 0.01330373f, 0.00655601f, 0.00078634f, 0.00002292f,
                0.00019117f, 0.00655601f, 0.05472157f, 0.11153763f, 0.05472157f, 0.00655601f, 0.00019117f,
                0.00038771f, 0.01330373f, 0.11153763f, 0.22793469f, 0.11153763f, 0.01330373f, 0.00038771f,
                0.00019117f, 0.00655601f, 0.05472157f, 0.11153763f, 0.05472157f, 0.00655601f, 0.00019117f,
                0.00002292f, 0.00078634f, 0.00655601f, 0.01330373f, 0.00655601f, 0.00078634f, 0.00002292f,
                0.00000067f, 0.00002292f, 0.00019117f, 0.00038771f, 0.00019117f, 0.00002292f, 0.00000067f
            )

            val op = ConvolveOp(Kernel(7, 7, gaussian11x11), ConvolveOp.EDGE_NO_OP, null)
            op.filter(image, blurred)
            repeat(100) {
                val temp = BufferedImage(image.width, image.height, BufferedImage.TYPE_INT_ARGB)
                op.filter(blurred, temp)
                blurred = temp
            }
            val tempFile = File.createTempFile("blurred_${file.path}", ".png")
            ImageIO.write(blurred, "png", tempFile)
            tempFiles[file] = tempFile
            val outputDir = File("C:\\Users\\Kostiantyn\\IdeaProjects\\PS\\$prefix\\uploads\\blurred")
            outputDir.mkdir()
            val outputFile = File(outputDir, file.name)
            outputFile.createNewFile()

            ImageIO.write(blurred, "png", outputFile)
            tempFile
        }
    }

    fun isPhotoBlured(fileName: String): Boolean {
        if (fileName == "horizontal.png" || fileName == "fab.png" || fileName == "fab2.png") {
            return false
        }

        return blurConfig.blur.contains(fileName)
    }


    fun getImagesList(): List<String> {

        val resourcePath = "$prefix/uploads/images"

        val resourceUrl = this::class.java.classLoader.getResource(resourcePath)

        val uri = resourceUrl.toURI()
        val imageDir = File(uri)

        val images = imageDir.listFiles()
            ?.filter { it.isFile }
            ?.map { it.name }

      return images ?: emptyList()
    }
    fun getFile(name:String): String {
    return this::class.java.classLoader
        .getResource("$prefix/$name")
        ?.readText() ?: ""
}
    fun getImageFile(path:String, name:String): File? {
        val resourceUrl = this::class.java.classLoader.getResource("$prefix/$path/$name")
        return resourceUrl?.let{File(it.toURI())}
    }

    private fun loadBlurConfig(): BlurConfig {
        return Json.decodeFromString(getFile("blur-config.json"))
    }

    fun getVideosList(): VideoConfig {
        if (videosList.isEmpty()) {
            val videoConfig = Json.decodeFromString<VideoConfig>(getFile("video-config.json"))
            videosList.putAll(videoConfig.videos)
        }
        return VideoConfig(videosList)
    }

    fun getBlocked(clientId: String): List<String> {
        return if (isUserUnlocked(clientId)) {
            return listOf()
        } else {
            blurConfig.blur
        }
    }

   fun getVideoFromPhoto(name: String, fileName: String): String {
        return "https://$name.com/files/videos/${videosList[fileName]}"
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

    fun loadBabeData(): BabeData {
        return Json.decodeFromString<BabeData>(getFile("babe-config.json"))
    }
}