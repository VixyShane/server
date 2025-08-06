package com.faigenbloom.chicks

import com.faigenbloom.chicks.models.PaymentData
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlin.collections.hashMapOf
import kotlin.collections.set

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
    routing {
        get("/{name}/data") {
            val name = call.parameters["name"]!!
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

            call.respondRedirect(DataBases.getDB(name).getImageLink(fileName, clientId))
        }
        get("/{name}/uploads/videos/{fileName}/{clientId}") {

            val name = call.parameters["name"]!!
            val clientId = call.parameters["clientId"]!!
            val fileName = call.parameters["fileName"]!!

            call.respondRedirect(DataBases.getDB(name).getVideoFromPhoto(clientId, fileName))

        }
    }
}
