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
object DataBases{
    val dbs = hashMapOf<String, Database>()
    fun getDB(name: String): Database {
        if (dbs[name] == null){
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
            if (dbs[name] == null){
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
            } else{
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
            val imageDir = File("$name/uploads/images")
            val images = imageDir.listFiles()
                ?.filter { it.isFile }
                ?.map { it.name }

            call.respond(images ?: emptyList())
        }
        get("/{name}/videos") {
            val name = call.parameters["name"]!!

            call.respond(DataBases.getDB(name).getVideosList())
        }
        get("/{name}/uploads/images/{fileName}/{clientId}") {

            val name = call.parameters["name"]!!
            val clientId = call.parameters["clientId"]!!
            val fileName = call.parameters["fileName"]!!

            val file = File("$name/uploads/images/$fileName")

            if (!file.exists()) {
                val file2 = File("$name/uploads/$fileName")
                if (!file2.exists()) {
                    call.respond(HttpStatusCode.NotFound)
                } else {
                    call.respondFile(file2)
                }
            } else {
                if (DataBases.getDB(name).isUserUnlocked(clientId)) {
                    call.respondFile(file)
                } else if (DataBases.getDB(name).isPhotoBlured(fileName)) {

                    val bluredFile = File("$name/uploads/blurred/$fileName")
                    if (bluredFile.exists()) {
                        call.respondFile(bluredFile)
                    } else {
                        call.respondFile(DataBases.getDB(name).blurImage(file))
                    }
                } else {
                    call.respondFile(file)
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

