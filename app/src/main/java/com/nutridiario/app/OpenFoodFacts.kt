package com.nutridiario.app

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/** Cliente de Open Food Facts (base de datos abierta y gratuita de productos envasados). */
object OpenFoodFacts {
    private const val FIELDS =
        "code,product_name,product_name_es,generic_name_es,brands,nutriments,serving_quantity,serving_size,image_front_small_url,quantity,product_quantity,product_quantity_unit"

    /** Open Food Facts a veces responde 503 cuando está saturado: se reintenta un par de veces. */
    private fun get(url: String): JSONObject {
        var last: Exception? = null
        repeat(3) { attempt ->
            try {
                return getOnce(url)
            } catch (e: ServerBusy) {
                last = e
                Thread.sleep(1500L * (attempt + 1))
            }
        }
        throw Exception("Open Food Facts está saturado ahora mismo, inténtalo en un momento", last)
    }

    private class ServerBusy(code: Int) : Exception("HTTP $code")

    private fun getOnce(url: String): JSONObject {
        val c = URL(url).openConnection() as HttpURLConnection
        c.connectTimeout = 15000
        c.readTimeout = 20000
        c.setRequestProperty("User-Agent", "NutriDiario/1.0 (Android)")
        try {
            if (c.responseCode == 404) return JSONObject()
            if (c.responseCode >= 500 || c.responseCode == 429) throw ServerBusy(c.responseCode)
            if (c.responseCode !in 200..299) throw Exception("Error del servidor (${c.responseCode})")
            return JSONObject(c.inputStream.bufferedReader().use { it.readText() })
        } finally {
            c.disconnect()
        }
    }

    /** Devuelve null si el producto no existe en la base de datos. */
    suspend fun byBarcode(code: String): FoodItem? = withContext(Dispatchers.IO) {
        val j = get("https://world.openfoodfacts.org/api/v2/product/$code.json?fields=$FIELDS")
        if (j.optInt("status", 0) != 1) return@withContext null
        parse(j.getJSONObject("product"), "Código de barras")?.copy(barcode = code)
    }

    suspend fun search(query: String): List<FoodItem> = withContext(Dispatchers.IO) {
        val q = URLEncoder.encode(query, "UTF-8")
        val j = get(
            "https://es.openfoodfacts.org/cgi/search.pl?search_terms=$q&search_simple=1&action=process" +
                "&json=1&page_size=30&sort_by=unique_scans_n&fields=$FIELDS"
        )
        val arr = j.optJSONArray("products") ?: return@withContext emptyList()
        (0 until arr.length()).mapNotNull { parse(arr.getJSONObject(it), "Open Food Facts") }
    }

    internal fun parse(p: JSONObject, source: String): FoodItem? {
        val n = p.optJSONObject("nutriments") ?: return null
        fun v(key: String): Double = n.optDouble("${key}_100g", Double.NaN).let { if (it.isNaN()) 0.0 else it }
        var kcal = n.optDouble("energy-kcal_100g", Double.NaN)
        if (kcal.isNaN()) {
            val kj = n.optDouble("energy_100g", Double.NaN)
            if (kj.isNaN()) return null
            kcal = kj / 4.184
        }
        val name = listOf("product_name_es", "product_name", "generic_name_es")
            .map { p.optString(it).trim() }.firstOrNull { it.isNotBlank() } ?: return null
        val salt = v("salt").takeIf { it > 0 } ?: (v("sodium") * 2.5)
        val serving = p.optDouble("serving_quantity", Double.NaN).takeIf { !it.isNaN() && it > 0 }
        val pkg = PackageQuantity.parse(
            p.optString("quantity"),
            p.optDouble("product_quantity", Double.NaN).takeIf { !it.isNaN() && it > 0 },
            p.optString("product_quantity_unit"),
        )
        return FoodItem(
            name = name,
            brand = p.optString("brands").split(",").firstOrNull()?.trim()?.takeIf { it.isNotBlank() },
            per100 = Nutrients(kcal, v("proteins"), v("carbohydrates"), v("sugars"), v("fat"), v("saturated-fat"), v("fiber"), salt),
            portionG = serving,
            portionName = p.optString("serving_size").takeIf { it.isNotBlank() && serving != null }?.let { "1 ración ($it)" },
            source = source,
            barcode = p.optString("code").takeIf { it.isNotBlank() },
            imageUrl = p.optString("image_front_small_url").takeIf { it.isNotBlank() },
            packageG = pkg?.amount,
            unit = pkg?.unit ?: "g",
        )
    }
}
