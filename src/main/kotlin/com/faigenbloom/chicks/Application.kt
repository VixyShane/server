package com.faigenbloom.chicks

import PayPalCredentials
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.util.reflect.*
import kotlinx.serialization.json.Json

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module() {
    install(CORS) {
        allowHost("localhost:4200", schemes = listOf("http"))
        allowHost("vixyshane.com", schemes = listOf("https"))
        allowHeader(HttpHeaders.ContentType)
        allowHeader(HttpHeaders.Authorization)
        allowMethod(HttpMethod.Get)
        allowMethod(HttpMethod.Post)
        allowMethod(HttpMethod.Put)
        allowMethod(HttpMethod.Delete)
    }
    install(StatusPages) {
        exception<Throwable> { call, cause ->
            println("❌ Exception occurred: ${cause.message}")
            cause.printStackTrace()
            call.respond(
                mapOf("error" to "${cause.message}"),
                typeInfo<Map<String, String>>()

            )

        }
    }
    configureRouting()
    install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
    PayPalCredentials.PAYPAL_CLIENT_ID = environment.config.property("paypal.clientId").getString()
    PayPalCredentials.PAYPAL_CLIENT_SECRET = environment.config.property("paypal.clientSecret").getString()
}
