package com.tamaade.ecommerce.data.api

import com.tamaade.ecommerce.BuildConfig
import com.tamaade.ecommerce.data.model.BackgroundMusicConfig
import com.tamaade.ecommerce.data.model.CartLine
import com.tamaade.ecommerce.data.model.HeroBanner
import com.tamaade.ecommerce.data.model.HomeSection
import com.tamaade.ecommerce.data.model.HubtelCheckout
import com.tamaade.ecommerce.data.model.MerchTile
import com.tamaade.ecommerce.data.model.MusicTrack
import com.tamaade.ecommerce.data.model.PaymentStatus
import com.tamaade.ecommerce.data.model.PriceTier
import com.tamaade.ecommerce.data.model.PrivacyContent
import com.tamaade.ecommerce.data.model.Product
import com.tamaade.ecommerce.data.model.ProductCategory
import com.tamaade.ecommerce.data.model.StorefrontPromo
import com.tamaade.ecommerce.data.model.UserSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import java.net.URLEncoder

/** A failed API call. [status] is the HTTP code, or 0 when the server could not be reached. */
class ApiException(message: String, val status: Int) : Exception(message) {
    val isUnauthorized: Boolean
        get() = status == 401
}

data class AuthResult(
    val accessToken: String,
    val user: UserSession
)

/**
 * Minimal JSON client for TamaadeAPI (HttpURLConnection + org.json, no extra dependencies).
 * Every call runs on Dispatchers.IO and throws [ApiException] with a user-facing message.
 */
object TamaadeApi {
    private val baseUrl = BuildConfig.API_BASE_URL.trimEnd('/') + "/"

    suspend fun login(email: String, password: String): AuthResult {
        val json = request(
            method = "POST",
            path = "api/user/login/",
            body = JSONObject()
                .put("email", email)
                .put("password", password)
        )
        val access = json.stringOrNull("access")
            ?: json.stringOrNull("access_token")
            ?: json.stringOrNull("key")
            ?: throw ApiException("Sign in failed. Please try again.", 200)
        val user = json.optJSONObject("user")
        return AuthResult(
            accessToken = access,
            user = UserSession(
                email = user?.stringOrNull("email") ?: email,
                firstName = user?.stringOrNull("first_name").orEmpty(),
                lastName = user?.stringOrNull("last_name").orEmpty(),
                id = user?.intOrNull("pk") ?: user?.intOrNull("id")
            )
        )
    }

    /** Returns the server's confirmation message (the account still has to verify its email). */
    suspend fun register(
        email: String,
        password: String,
        confirmPassword: String,
        firstName: String,
        lastName: String
    ): String {
        val json = request(
            method = "POST",
            path = "api/user/register/",
            body = JSONObject()
                .put("email", email)
                .put("password1", password)
                .put("password2", confirmPassword)
                .put("first_name", firstName)
                .put("last_name", lastName)
        )
        return json.stringOrNull("detail") ?: "Verification e-mail sent."
    }

    suspend fun startHubtelCheckout(token: String, lines: List<CartLine>): HubtelCheckout {
        val items = JSONArray()
        lines.forEach { line ->
            items.put(
                JSONObject()
                    .put("product", line.product.id)
                    .put("name", line.product.name)
                    .put("quantity", line.quantity)
            )
        }
        val json = request(
            method = "POST",
            path = "api/user/payments/hubtel/checkout/",
            body = JSONObject()
                .put("platform", "android")
                .put("items", items),
            token = token
        )
        val directUrl = json.stringOrNull("checkout_direct_url")
        val url = json.stringOrNull("checkout_url") ?: directUrl
        val reference = json.stringOrNull("client_reference")
        if (url == null || reference == null) {
            throw ApiException("Hubtel checkout could not be started. Please try again.", 200)
        }
        return HubtelCheckout(
            checkoutUrl = url,
            checkoutDirectUrl = directUrl,
            checkoutId = json.stringOrNull("checkout_id"),
            clientReference = reference,
            orderId = json.intOrNull("order_id"),
            amount = json.stringOrNull("amount")
        )
    }

    suspend fun hubtelStatus(token: String, reference: String): PaymentStatus {
        val json = request(
            method = "GET",
            path = "api/user/payments/hubtel/status/?ref=" + URLEncoder.encode(reference, "UTF-8"),
            token = token
        )
        return PaymentStatus(
            status = json.stringOrNull("status") ?: "P",
            paid = json.optBoolean("paid", false),
            orderId = json.intOrNull("order_id"),
            clientReference = json.stringOrNull("client_reference") ?: reference
        )
    }

    suspend fun privacy(): PrivacyContent {
        val json = request(method = "GET", path = "api/content/privacy/")
        return PrivacyContent(
            title = json.stringOrNull("title") ?: "Privacy Policy",
            content = json.stringOrNull("content").orEmpty(),
            updatedAt = json.stringOrNull("updated_at")
        )
    }

    suspend fun backgroundMusic(): BackgroundMusicConfig {
        val json = request(method = "GET", path = "api/content/background-music/")
        val track = json.optJSONObject("track")?.let { t ->
            val url = t.stringOrNull("url") ?: return@let null
            MusicTrack(
                id = t.intOrNull("id"),
                title = t.stringOrNull("title").orEmpty(),
                url = url,
                volume = t.optDouble("volume", 1.0).toFloat().coerceIn(0f, 1f)
            )
        }
        return BackgroundMusicConfig(
            enabled = json.optBoolean("enabled", false),
            track = track
        )
    }

    /** Permanently deletes the signed-in account (Google Play account-deletion requirement). */
    suspend fun deleteAccount(token: String, password: String): String {
        val json = request(
            method = "POST",
            path = "api/user/delete-account/",
            body = JSONObject().put("password", password),
            token = token
        )
        return json.stringOrNull("detail") ?: "Your account has been deleted."
    }

    // region Catalog (public, unpaginated JSON arrays)

    suspend fun products(): List<Product> =
        requestList("api/products/").objects().mapNotNull(::parseProduct)

    suspend fun product(id: Int): Product? = parseProduct(request("GET", "api/products/$id/"))

    suspend fun categories(): List<ProductCategory> =
        requestList("api/products/categories/").objects().mapNotNull { c ->
            val id = c.intOrNull("id") ?: return@mapNotNull null
            val name = c.stringOrNull("name") ?: return@mapNotNull null
            ProductCategory(id = id, name = name, icon = mediaUrl(c.stringOrNull("icon")))
        }

    suspend fun banners(): List<HeroBanner> =
        requestList("api/products/banners/").objects()
            .filter { it.optBoolean("is_active", true) }
            .sortedBy { it.optInt("order") }
            .mapNotNull { b ->
                val image = mediaUrl(b.stringOrNull("image") ?: b.stringOrNull("image_url"))
                    ?: return@mapNotNull null
                HeroBanner(
                    id = b.intOrNull("id") ?: return@mapNotNull null,
                    title = b.stringOrNull("title").orEmpty(),
                    image = image,
                    link = b.stringOrNull("link")
                )
            }

    suspend fun priceTiers(): List<PriceTier> =
        requestList("api/products/price-tiers/").objects()
            .filter { it.optBoolean("is_active", true) }
            .sortedWith(compareBy<JSONObject>({ it.optInt("order") }, { it.optDouble("amount") }))
            .mapNotNull { t ->
                val amount = t.optDouble("amount", Double.NaN)
                if (amount.isNaN()) return@mapNotNull null
                PriceTier(id = t.intOrNull("id") ?: return@mapNotNull null, amount = amount)
            }

    suspend fun promos(): List<StorefrontPromo> =
        requestList("api/products/promos/").objects()
            .filter { it.optBoolean("is_active", true) }
            .sortedBy { it.optInt("order") }
            .mapNotNull { p ->
                StorefrontPromo(
                    id = p.intOrNull("id") ?: return@mapNotNull null,
                    key = p.stringOrNull("key").orEmpty(),
                    title = p.stringOrNull("title").orEmpty(),
                    subtitle = p.stringOrNull("subtitle").orEmpty(),
                    highlight = p.stringOrNull("highlight").orEmpty(),
                    ctaLabel = p.stringOrNull("cta_label").orEmpty(),
                    link = p.stringOrNull("link")
                )
            }

    suspend fun merchTiles(placement: String): List<MerchTile> =
        requestList("api/products/merch-tiles/?placement=$placement").objects()
            .filter { it.optBoolean("is_active", true) }
            .sortedBy { it.optInt("order") }
            .mapNotNull { t ->
                MerchTile(
                    id = t.intOrNull("id") ?: return@mapNotNull null,
                    placement = t.stringOrNull("placement") ?: placement,
                    title = t.stringOrNull("title").orEmpty(),
                    image = mediaUrl(t.stringOrNull("image") ?: t.stringOrNull("image_url")),
                    link = t.stringOrNull("link"),
                    badge = t.stringOrNull("badge").orEmpty(),
                    highlight = t.optBoolean("highlight", false)
                )
            }

    suspend fun homeSections(location: String): List<HomeSection> =
        requestList("api/products/home-sections/?location=$location").objects()
            .filter { it.optBoolean("is_active", true) }
            .sortedBy { it.optInt("order") }
            .mapNotNull { h ->
                HomeSection(
                    id = h.intOrNull("id") ?: return@mapNotNull null,
                    key = h.stringOrNull("key").orEmpty(),
                    location = h.stringOrNull("location") ?: location,
                    title = h.stringOrNull("title").orEmpty(),
                    subtitle = h.stringOrNull("subtitle").orEmpty(),
                    ctaLabel = h.stringOrNull("cta_label").orEmpty(),
                    link = h.stringOrNull("link"),
                    productSource = h.stringOrNull("product_source") ?: "featured"
                )
            }

    /** Parses the api/products/ shape (also used for the basket saved on the device). */
    fun parseProduct(p: JSONObject): Product? {
        val id = p.intOrNull("id") ?: return null
        val name = p.stringOrNull("name") ?: return null
        val gallery = p.optJSONArray("images")?.objects().orEmpty()
            .sortedWith(compareBy<JSONObject>({ !it.optBoolean("is_primary", false) }, { it.optInt("order") }))
            .mapNotNull { mediaUrl(it.stringOrNull("url")) }
        return Product(
            id = id,
            name = name,
            desc = p.stringOrNull("desc").orEmpty(),
            category = p.stringOrNull("category").orEmpty(),
            seller = p.stringOrNull("seller").orEmpty(),
            image = gallery.firstOrNull() ?: mediaUrl(p.stringOrNull("image")),
            images = gallery,
            price = p.stringOrNull("price") ?: "0",
            compareAtPrice = p.stringOrNull("compare_at_price"),
            discountPercent = p.intOrNull("discount_percent"),
            promoLabel = p.stringOrNull("promo_label").orEmpty().trim(),
            brand = p.stringOrNull("brand").orEmpty(),
            isNew = p.optBoolean("is_new", false),
            isExpress = p.optBoolean("is_express", false),
            saleEndsAt = p.stringOrNull("sale_ends_at"),
            quantity = p.optInt("quantity", 0),
            createdAt = p.stringOrNull("created_at")
        )
    }

    /** Same field names as the API, so [parseProduct] can read it back. */
    fun productToJson(product: Product): JSONObject {
        val images = JSONArray()
        product.images.forEachIndexed { i, url ->
            images.put(JSONObject().put("url", url).put("is_primary", i == 0).put("order", i))
        }
        return JSONObject()
            .put("id", product.id)
            .put("name", product.name)
            .put("desc", product.desc)
            .put("category", product.category)
            .put("seller", product.seller)
            .put("image", product.image ?: JSONObject.NULL)
            .put("images", images)
            .put("price", product.price)
            .put("compare_at_price", product.compareAtPrice ?: JSONObject.NULL)
            .put("discount_percent", product.discountPercent ?: JSONObject.NULL)
            .put("promo_label", product.promoLabel)
            .put("brand", product.brand)
            .put("is_new", product.isNew)
            .put("is_express", product.isExpress)
            .put("sale_ends_at", product.saleEndsAt ?: JSONObject.NULL)
            .put("quantity", product.quantity)
            .put("created_at", product.createdAt ?: JSONObject.NULL)
    }

    /** API-relative media paths become absolute URLs (same as TamaadeWeb resolveMediaUrl). */
    private fun mediaUrl(url: String?): String? {
        if (url.isNullOrBlank()) return null
        if (url.startsWith("http://") || url.startsWith("https://")) return url
        return baseUrl.trimEnd('/') + (if (url.startsWith("/")) url else "/$url")
    }

    private fun JSONArray.objects(): List<JSONObject> =
        (0 until length()).mapNotNull { optJSONObject(it) }

    // endregion

    private suspend fun request(
        method: String,
        path: String,
        body: JSONObject? = null,
        token: String? = null
    ): JSONObject = when (val json = send(method, path, body, token)) {
        is JSONObject -> json
        else -> JSONObject()
    }

    /** For list endpoints; also accepts a DRF paginated {"results": [...]} body. */
    private suspend fun requestList(path: String): JSONArray =
        when (val json = send("GET", path, null, null)) {
            is JSONArray -> json
            is JSONObject -> json.optJSONArray("results") ?: JSONArray()
            else -> JSONArray()
        }

    private suspend fun send(
        method: String,
        path: String,
        body: JSONObject?,
        token: String?
    ): Any? = withContext(Dispatchers.IO) {
        val connection = try {
            URL(baseUrl + path).openConnection() as HttpURLConnection
        } catch (e: IOException) {
            throw ApiException(OFFLINE_MESSAGE, 0)
        }
        try {
            connection.requestMethod = method
            // The hosted backend can take a while to wake up, so be generous with the read timeout.
            connection.connectTimeout = 20_000
            connection.readTimeout = 60_000
            connection.setRequestProperty("Accept", "application/json")
            if (token != null) connection.setRequestProperty("Authorization", "Bearer $token")
            if (body != null) {
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            }

            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val text = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            val json = parseJson(text)
            if (code !in 200..299) throw ApiException(errorMessage(json, code), code)
            json
        } catch (e: SocketTimeoutException) {
            throw ApiException("Tamaade is taking too long to respond. Please try again.", 0)
        } catch (e: IOException) {
            throw ApiException(OFFLINE_MESSAGE, 0)
        } finally {
            connection.disconnect()
        }
    }

    private fun parseJson(text: String): Any? {
        val trimmed = text.trim()
        return try {
            when {
                trimmed.startsWith("{") -> JSONObject(trimmed)
                trimmed.startsWith("[") -> JSONArray(trimmed)
                else -> null
            }
        } catch (e: JSONException) {
            null
        }
    }

    /** Flattens DRF errors: {"detail"}, {"non_field_errors": [...]} or {"field": ["msg"]}. */
    private fun errorMessage(body: Any?, code: Int): String {
        if (code == 401) return "Your session has expired. Please sign in again."
        if (body is JSONArray) body.joined().let { if (it.isNotBlank()) return it }
        val json = body as? JSONObject
        if (json != null) {
            json.stringOrNull("detail")?.let { if (it.isNotBlank()) return it }
            json.optJSONArray("non_field_errors")?.joined()?.let { if (it.isNotBlank()) return it }
            val messages = json.keys().asSequence().mapNotNull { key ->
                val text = when (val value = json.opt(key)) {
                    is JSONArray -> value.joined()
                    is String -> value
                    else -> null
                }
                if (text.isNullOrBlank()) null else "${fieldLabel(key)}: $text"
            }.toList()
            if (messages.isNotEmpty()) return messages.joinToString("\n")
        }
        return when {
            code == 404 -> "This feature isn't available on the server yet."
            code >= 500 -> "Tamaade server error ($code). Please try again shortly."
            else -> "Request failed ($code). Please try again."
        }
    }

    private fun fieldLabel(key: String): String = when (key) {
        "password1" -> "Password"
        "password2" -> "Confirm password"
        else -> key.replace('_', ' ').replaceFirstChar { it.uppercase() }
    }

    private fun JSONArray.joined(): String =
        (0 until length()).mapNotNull { i -> opt(i)?.toString()?.takeIf { it.isNotBlank() } }
            .joinToString(" ")

    private const val OFFLINE_MESSAGE = "Can't reach Tamaade. Check your internet connection and try again."
}

/** org.json's optString returns "null" for JSON null; this returns a real null instead. */
internal fun JSONObject.stringOrNull(key: String): String? =
    if (!has(key) || isNull(key)) null else optString(key).takeIf { it.isNotEmpty() }

internal fun JSONObject.intOrNull(key: String): Int? =
    if (!has(key) || isNull(key)) null else when (val v = opt(key)) {
        is Number -> v.toInt()
        is String -> v.toIntOrNull()
        else -> null
    }
