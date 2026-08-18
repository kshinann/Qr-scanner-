package com.qrtoolkit.app.data.lookup

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException

data class ProductInfo(val name: String?, val brand: String?, val imageUrl: String?)

/**
 * Looks up a scanned retail barcode (EAN/UPC) against Open Food Facts' free, keyless API --
 * the one genuinely public product database that doesn't require signing up for an API key.
 * Coverage is food/grocery-focused, so a non-food barcode will often come back with no match;
 * that's a data-coverage gap, not a bug.
 */
class ProductLookupService(private val client: OkHttpClient = OkHttpClient()) {

    suspend fun lookup(barcode: String): ProductInfo? = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("https://world.openfoodfacts.org/api/v2/product/$barcode.json")
            .build()
        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                val body = response.body?.string() ?: return@withContext null
                parse(body)
            }
        } catch (e: IOException) {
            null
        }
    }

    private fun parse(body: String): ProductInfo? = try {
        val json = JSONObject(body)
        if (json.optInt("status", 0) != 1) {
            null
        } else {
            val product = json.optJSONObject("product")
            if (product == null) {
                null
            } else {
                ProductInfo(
                    name = product.optString("product_name").takeIf { it.isNotBlank() },
                    brand = product.optString("brands").takeIf { it.isNotBlank() },
                    imageUrl = product.optString("image_front_url").takeIf { it.isNotBlank() }
                        ?: product.optString("image_url").takeIf { it.isNotBlank() },
                )
            }
        }
    } catch (e: JSONException) {
        null
    }
}
