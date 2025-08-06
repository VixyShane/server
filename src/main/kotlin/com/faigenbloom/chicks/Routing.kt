package com.faigenbloom.chicks

import com.faigenbloom.chicks.models.PaymentData
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.utils.io.*
import java.io.File

object DataBases {
    val dbs = hashMapOf<String, Database>()
    fun getDB(name: String): Database {
        if (dbs[name] == null) {
            dbs[name] = Database(name)
        }
        return dbs[name]!!
    }
}

fun Application.configureRouting() {
    val dbs = hashMapOf<String, Database>()
    routing {
        get("/{name}/data") {
            val name = call.parameters["name"]!!
            if (dbs[name] == null) {
                dbs[name] = Database(name)
            }
            call.respond(DataBases.getDB(name).loadBabeData())
        }
        get("/{name}/users/likes/{clientId}") {
            val name = call.parameters["name"]!!
            val clientId = call.parameters["clientId"]!!

            call.respond(DataBases.getDB(name).getLiked(clientId, name))
        }

        post("/{name}/users/payment/{clientId}") {
            val name = call.parameters["name"]!!
            val clientId = call.parameters["clientId"]!!
            val payment = call.receive<PaymentData>()
            if (DataBases.getDB(name).checkPayment(clientId, payment)) {
                call.respond(HttpStatusCode.OK, "{\"success\": true}")
            } else {
                call.respond(HttpStatusCode.NotFound)
            }
        }

        get("/{name}/users/like/{fileName}/{clientId}") {
            val name = call.parameters["name"]!!
            val clientId = call.parameters["clientId"]!!
            val fileName = call.parameters["fileName"]!!
            DataBases.getDB(name).like(name, fileName, clientId)
            call.respondText("{}")
        }

        get("/{name}/images/blocked/{clientId}") {
            val name = call.parameters["name"]!!
            val clientId = call.parameters["clientId"]!!

            call.respond(DataBases.getDB(name).getBlocked(clientId))
        }
        get("/{name}/images") {
            val name = call.parameters["name"]!!
            call.respond(DataBases.getDB(name).getImagesList())
        }
        get("/{name}/videos") {
            val name = call.parameters["name"]!!

            call.respond(DataBases.getDB(name).getVideosList())
        }
        get("/{name}/uploads/images/{fileName}/{clientId}") {

            val name = call.parameters["name"]!!
            val clientId = call.parameters["clientId"]!!
            val fileName = call.parameters["fileName"]!!
            val baseImage = DataBases.getDB(name).getImageFile("uploads", fileName)

            if (baseImage != null) {
                call.respondFile(baseImage)
            } else{
                if (DataBases.getDB(name).isPhotoBlured(fileName).not() || DataBases.getDB(name).isUserUnlocked(clientId)) {
                    DataBases.getDB(name).getImageFile("uploads/images", fileName)?.let {
                        call.respondFile(it)
                    } ?: call.respond(HttpStatusCode.NotFound)
                } else{
                    DataBases.getDB(name).getImageFile("uploads/blurred", fileName)?.let {
                        call.respondFile(it)
                    } ?: run {
                        DataBases.getDB(name).getImageFile("uploads/images", fileName)?.let {
                            call.respondFile(DataBases.getDB(name).blurImage(it))
                        }?:call.respond(HttpStatusCode.NotFound)
                    }
                }
            }
        }
        get("/{name}/uploads/videos/{fileName}/{clientId}") {

            val name = call.parameters["name"]!!
            val clientId = call.parameters["clientId"]!!
            val fileName = call.parameters["fileName"]!!

            if (DataBases.getDB(name).isUserUnlocked(clientId) || !DataBases.getDB(name).isPhotoBlured(fileName)) {
                val client = HttpClient(CIO)
                val response = client.get(DataBases.getDB(name).getVideoFromPhoto(name, fileName))
                call.respondBytesWriter(contentType = response.contentType() ?: ContentType.Application.OctetStream) {
                    response.bodyAsChannel().copyTo(this)
                }
            } else {
                call.respond(HttpStatusCode.NotFound)
            }
        }
    }
}

fun printProjectTree(dir: File = File("."), indent: String = ""): String {
    if (!dir.exists()) {

        return "Directory does not exist: ${dir.absolutePath}"
    }

    val files = dir.listFiles()?.sortedBy { it.name } ?: return ""
    var res = ""
    for (file in files) {
        res += "$indent├── ${file.name}\n"
        if (file.isDirectory) {
            res += printProjectTree(file, "$indent│   ")
        }
    }
    return res
}