package com.nutridiario.app

import android.content.Context
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.text.Normalizer

@Serializable
private data class RawFood(
    val name: String, val aliases: List<String>,
    val kcal: Double, val protein: Double, val carbs: Double, val sugar: Double,
    val fat: Double, val satFat: Double, val fiber: Double, val salt: Double,
    val portionG: Double, val portionName: String,
)

class LocalFood(val item: FoodItem, val keys: List<String>)

/** Base de datos de alimentos genéricos incluida en la app (funciona sin internet). */
object LocalFoodDb {
    private var foods: List<LocalFood> = emptyList()

    fun init(context: Context) =
        initFromJson(context.assets.open("foods.json").bufferedReader().use { it.readText() })

    fun initFromJson(text: String) {
        foods = Json.decodeFromString<List<RawFood>>(text).map { r ->
            LocalFood(
                FoodItem(
                    name = r.name,
                    per100 = Nutrients(r.kcal, r.protein, r.carbs, r.sugar, r.fat, r.satFat, r.fiber, r.salt),
                    portionG = r.portionG, portionName = r.portionName, source = "Base de datos local",
                ),
                (listOf(r.name) + r.aliases).map { normalize(it) }
            )
        }
    }

    /** Busca por una etiqueta exacta (usado para las etiquetas del modelo de IA). */
    fun byAlias(label: String): FoodItem? {
        val n = normalize(label)
        return foods.firstOrNull { f -> f.keys.any { it == n } }?.item
    }

    fun search(query: String, limit: Int = 20): List<FoodItem> {
        val q = normalize(query)
        if (q.isBlank()) return emptyList()
        val tokens = q.split(" ").filter { it.length > 1 && it !in STOP }
        val custom = Repository.data.value.customFoods.map { f ->
            LocalFood(f, listOfNotNull(normalize(f.name), f.brand?.let { normalize("${f.name} ${it}") }, f.brand?.let { normalize(it) }))
        }
        return (custom + foods).mapNotNull { f ->
            var best = 0
            for (k in f.keys) {
                val s = when {
                    k == q -> 100
                    k.startsWith(q) -> 80
                    k.contains(q) -> 60
                    tokens.isNotEmpty() && tokens.all { t -> k.contains(t) || k.contains(t.trimEnd('s')) } -> 50
                    else -> tokens.count { t -> k.split(" ").any { w -> w.startsWith(t.trimEnd('s')) } } * 15
                }
                if (s > best) best = s
            }
            if (best > 0) f.item to best - f.keys.first().length / 10 else null
        }.sortedByDescending { it.second }.take(limit).map { it.first }
    }

    private val STOP = setOf("de", "del", "la", "el", "los", "las", "con", "y", "a", "al", "en", "un", "una")

    fun normalize(s: String): String =
        Normalizer.normalize(s.lowercase().trim(), Normalizer.Form.NFD)
            .replace(Regex("\\p{Mn}+"), "")
            .replace(Regex("[^a-z0-9ñ ]"), " ")
            .replace(Regex("\\s+"), " ").trim()
}
