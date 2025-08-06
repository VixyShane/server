import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

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

