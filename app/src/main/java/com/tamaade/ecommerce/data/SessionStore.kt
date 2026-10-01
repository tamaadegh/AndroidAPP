package com.tamaade.ecommerce.data

import android.content.Context
import androidx.core.content.edit
import com.tamaade.ecommerce.data.api.TamaadeApi
import com.tamaade.ecommerce.data.api.intOrNull
import com.tamaade.ecommerce.data.api.stringOrNull
import com.tamaade.ecommerce.data.model.CartLine
import com.tamaade.ecommerce.data.model.UserSession
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/**
 * SharedPreferences-backed storage for everything that must survive an app restart:
 * the signed-in session, the basket, an in-flight Hubtel checkout and user settings.
 */
class SessionStore(context: Context) {
    private val prefs = context.applicationContext
        .getSharedPreferences("tamaade_session", Context.MODE_PRIVATE)

    val accessToken: String?
        get() = prefs.getString(KEY_ACCESS, null)

    fun loadUser(): UserSession? {
        if (accessToken == null) return null
        val raw = prefs.getString(KEY_USER, null) ?: return null
        return try {
            val json = JSONObject(raw)
            UserSession(
                email = json.stringOrNull("email") ?: return null,
                firstName = json.stringOrNull("first_name").orEmpty(),
                lastName = json.stringOrNull("last_name").orEmpty(),
                id = json.intOrNull("id")
            )
        } catch (e: JSONException) {
            null
        }
    }

    fun saveSession(accessToken: String, user: UserSession) {
        val json = JSONObject()
            .put("email", user.email)
            .put("first_name", user.firstName)
            .put("last_name", user.lastName)
            .put("id", user.id ?: JSONObject.NULL)
        prefs.edit {
            putString(KEY_ACCESS, accessToken)
            putString(KEY_USER, json.toString())
        }
    }

    fun clearSession() {
        prefs.edit {
            remove(KEY_ACCESS)
            remove(KEY_USER)
            remove(KEY_PENDING_REF)
        }
    }

    /** client_reference of a Hubtel checkout opened in the browser but not yet confirmed. */
    var pendingCheckoutRef: String?
        get() = prefs.getString(KEY_PENDING_REF, null)
        set(value) = prefs.edit {
            if (value == null) remove(KEY_PENDING_REF) else putString(KEY_PENDING_REF, value)
        }

    var musicEnabled: Boolean
        get() = prefs.getBoolean(KEY_MUSIC, true)
        set(value) = prefs.edit { putBoolean(KEY_MUSIC, value) }

    /**
     * Basket with a snapshot of each product (API field names), so it shows offline;
     * it is re-synced with the live catalog once products load.
     */
    fun saveCart(lines: List<CartLine>) {
        val array = JSONArray()
        lines.forEach {
            array.put(JSONObject().put("product", TamaadeApi.productToJson(it.product)).put("qty", it.quantity))
        }
        prefs.edit { putString(KEY_CART, array.toString()) }
    }

    fun loadCart(): List<CartLine> {
        val raw = prefs.getString(KEY_CART, null) ?: return emptyList()
        return try {
            val array = JSONArray(raw)
            (0 until array.length()).mapNotNull { i ->
                val item = array.optJSONObject(i) ?: return@mapNotNull null
                val product = item.optJSONObject("product")?.let(TamaadeApi::parseProduct)
                    ?: return@mapNotNull null
                val qty = item.optInt("qty", 0)
                if (qty > 0) CartLine(product, qty) else null
            }
        } catch (e: JSONException) {
            emptyList()
        }
    }

    private companion object {
        const val KEY_ACCESS = "access_token"
        const val KEY_USER = "user"
        const val KEY_PENDING_REF = "pending_checkout_ref"
        const val KEY_MUSIC = "background_music_enabled"
        const val KEY_CART = "cart"
    }
}
