import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.auth.*
import io.ktor.client.plugins.auth.providers.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.jsonObject

private const val PAYPAL_BASE_URL = "https://api-m.paypal.com" // sandbox: https://api-m.sandbox.paypal.com

// ✅ Сериализуем ответ
@Serializable
data class PayPalOrderResponse(
    val id: String,
    val status: String,
    @SerialName("purchase_units") val purchaseUnits: List<PurchaseUnit>
)

@Serializable
object PayPalCredentials {
    var PAYPAL_CLIENT_ID = ""
    var PAYPAL_CLIENT_SECRET = ""
}

@Serializable
data class PurchaseUnit(val amount: Amount)

@Serializable
data class Amount(
    @SerialName("currency_code") val currencyCode: String,
    val value: String
)

object PayPalTokenStorage {
    var accessToken: String? = null
    var expiresAt: Long = 0L
}

val json = Json { ignoreUnknownKeys = true }

suspend fun getPayPalAccessToken(): String {
    if (PayPalTokenStorage.accessToken != null && System.currentTimeMillis() < PayPalTokenStorage.expiresAt) {
        return PayPalTokenStorage.accessToken!!
    }

    val client = HttpClient(CIO) {
        install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
    }

    val response = client.submitForm(
        url = "$PAYPAL_BASE_URL/v1/oauth2/token",
        formParameters = parameters {
            append("grant_type", "client_credentials")
        }
    ) {
        headers {
            basicAuth(PayPalCredentials.PAYPAL_CLIENT_ID, PayPalCredentials.PAYPAL_CLIENT_SECRET)
            append(HttpHeaders.ContentType, ContentType.Application.FormUrlEncoded)
        }
    }

    val jsonResp = json.parseToJsonElement(response.bodyAsText()).jsonObject
    val token = jsonResp["access_token"]?.jsonPrimitive?.content ?: error("No access token")
    val expiresIn = jsonResp["expires_in"]?.jsonPrimitive?.int ?: 300

    PayPalTokenStorage.accessToken = token
    PayPalTokenStorage.expiresAt = System.currentTimeMillis() + (expiresIn - 60) * 1000

    return token
}

suspend fun verifyPayPalOrder(orderId: String, expectedAmount: String, expectedCurrency: String): Boolean {
    val token = getPayPalAccessToken()

    val client = HttpClient(CIO) {
        install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        install(Auth) {
            bearer { loadTokens { BearerTokens(token, "") } }
        }
    }

    val response: PayPalOrderResponse = client.get("$PAYPAL_BASE_URL/v2/checkout/orders/$orderId").body()

    val amount = response.purchaseUnits.firstOrNull()?.amount
    return response.status == "COMPLETED"
            && amount?.value == expectedAmount
            && amount.currencyCode == expectedCurrency
}
