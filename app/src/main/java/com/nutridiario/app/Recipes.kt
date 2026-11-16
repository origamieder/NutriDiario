package com.nutridiario.app

import android.content.Context
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.net.URLEncoder

@Serializable
data class Ingredient(
    val id: String, val name: String, val emoji: String, val aliases: List<String>,
    val kcal: Double, val protein: Double, val carbs: Double, val sugar: Double,
    val fat: Double, val satFat: Double, val fiber: Double, val salt: Double,
    /** Cosas que casi todo el mundo tiene en casa (aceite, sal, ajo, especias…): no hace falta decirlas. */
    val pantry: Boolean,
    /** Texto para buscarlo en el supermercado. */
    val shop: String,
    val unitG: Double? = null, val unitName: String? = null,
) {
    val per100 get() = Nutrients(kcal, protein, carbs, sugar, fat, satFat, fiber, salt)
}

@Serializable data class RecipeItem(val id: String, val g: Double)

@Serializable
data class Recipe(
    val id: Int, val name: String, val emoji: String, val meals: List<String>, val minutes: Int,
    val ingredients: List<RecipeItem>, val steps: List<String>,
)

/** Resultado de comparar una receta con lo que tienes. */
data class RecipeMatch(val recipe: Recipe, val have: List<RecipeItem>, val missing: List<RecipeItem>, val nutrients: Nutrients) {
    val required get() = have.size + missing.size
}

/** Grupos de ingredientes que se pueden excluir de un toque. */
enum class DietGroup(val label: String, val ids: Set<String>) {
    FISH("🐟 Sin pescado ni marisco", setOf("salmon", "merluza", "atun", "gambas")),
    PORK("🐷 Sin cerdo", setOf("cerdo", "chorizo", "jamon", "jamon_york", "bacon")),
    MEAT("🥦 Vegetariano", setOf("pollo", "muslo_pollo", "pavo", "ternera", "carne_picada", "cerdo", "chorizo", "jamon", "jamon_york", "bacon", "salmon", "merluza", "atun", "gambas")),
    DAIRY("🥛 Sin lácteos", setOf("leche", "nata", "queso", "queso_fresco", "mozzarella", "yogur", "mantequilla")),
    EGG("🥚 Sin huevo", setOf("huevo")),
    GLUTEN("🌾 Sin gluten", setOf("pasta", "pan", "tortilla_trigo", "harina", "pan_rallado", "cuscus")),
}

/** Formas de ordenar las recetas. */
enum class RecipeSort(val label: String) {
    RECOMMENDED("⭐ Recomendado"),
    KCAL_ASC("🔥 Menos calorías"),
    KCAL_DESC("🔥 Más calorías"),
    PROTEIN_DESC("💪 Más proteína"),
    FAT_ASC("🧈 Menos grasa"),
    CARBS_ASC("🍞 Menos hidratos"),
    SUGAR_ASC("🍬 Menos azúcar"),
    FIBER_DESC("🌾 Más fibra"),
    TIME_ASC("⏱ Más rápidas");

    /** null = mantener el orden recomendado. */
    val key: ((RecipeMatch) -> Double)?
        get() = when (this) {
            RECOMMENDED -> null
            KCAL_ASC -> { m -> m.nutrients.kcal }
            KCAL_DESC -> { m -> -m.nutrients.kcal }
            PROTEIN_DESC -> { m -> -m.nutrients.protein }
            FAT_ASC -> { m -> m.nutrients.fat }
            CARBS_ASC -> { m -> m.nutrients.carbs }
            SUGAR_ASC -> { m -> m.nutrients.sugar }
            FIBER_DESC -> { m -> -m.nutrients.fiber }
            TIME_ASC -> { m -> m.recipe.minutes.toDouble() }
        }

    companion object {
        fun of(name: String) = entries.firstOrNull { it.name == name } ?: RECOMMENDED
    }
}

fun List<RecipeMatch>.sortedBy(sort: RecipeSort): List<RecipeMatch> = sort.key?.let { k -> sortedBy(k) } ?: this

/** Recetas de una ración incluidas en la app (funcionan sin internet). */
object RecipeBook {
    private val json = Json { ignoreUnknownKeys = true }
    var ingredients: Map<String, Ingredient> = emptyMap(); private set
    var recipes: List<Recipe> = emptyList(); private set

    fun init(ctx: Context) = initFromJson(
        ctx.assets.open("ingredients.json").bufferedReader().use { it.readText() },
        ctx.assets.open("recipes.json").bufferedReader().use { it.readText() },
    )

    fun initFromJson(ingJson: String, recJson: String) {
        ingredients = json.decodeFromString<List<Ingredient>>(ingJson).associateBy { it.id }
        recipes = json.decodeFromString(recJson)
    }

    fun ing(id: String): Ingredient = ingredients.getValue(id)

    /** Ingredientes que valen como sustitutos (si tienes pollo, sirve para una receta con muslos…). */
    private val substitutes = mapOf(
        "muslo_pollo" to listOf("pollo", "pavo"), "pollo" to listOf("pavo", "muslo_pollo"), "pavo" to listOf("pollo"),
        "tomate_triturado" to listOf("tomate", "tomate_frito"), "tomate_frito" to listOf("tomate_triturado"),
        "mozzarella" to listOf("queso"), "queso" to listOf("mozzarella"),
        "fresas" to listOf("frutos_rojos"), "frutos_rojos" to listOf("fresas"),
        "jamon_york" to listOf("pavo"), "merluza" to listOf("salmon"),
    )

    /** Qué ingrediente tuyo cubre el de la receta (él mismo o un sustituto), o null. */
    fun coveredBy(id: String, mine: Set<String>): String? =
        if (id in mine) id else substitutes[id]?.firstOrNull { it in mine }

    fun nutrients(r: Recipe): Nutrients = r.ingredients.fold(Nutrients()) { acc, it -> acc + ing(it.id).per100.forGrams(it.g) }
    fun grams(r: Recipe): Double = r.ingredients.sumOf { it.g }

    /** La receta no lleva nada de lo que has excluido. */
    fun allowed(r: Recipe, dislikes: Set<String>) = r.ingredients.none { it.id in dislikes }

    fun match(r: Recipe, mine: Set<String>): RecipeMatch {
        val req = r.ingredients.filterNot { ing(it.id).pantry }
        val (have, missing) = req.partition { coveredBy(it.id, mine) != null }
        return RecipeMatch(r, have, missing, nutrients(r))
    }

    /** Recetas ordenadas: primero las que puedes hacer ya, luego a las que les falta menos. */
    fun suggest(mine: Set<String>, maxMissing: Int = 2, dislikes: Set<String> = emptySet()): List<RecipeMatch> =
        recipes.filter { allowed(it, dislikes) }.map { match(it, mine) }
            .filter { it.have.isNotEmpty() && it.missing.size <= maxMissing }
            .sortedWith(compareBy<RecipeMatch> { it.missing.size }.thenByDescending { it.have.size }.thenBy { it.recipe.minutes })

    /** Ideas para la comida actual que encajen con las calorías que te quedan hoy. */
    fun ideas(meal: Meal, kcalLeft: Double, dailyTarget: Double, dislikes: Set<String> = emptySet()): List<RecipeMatch> {
        val tag = when (meal) { Meal.BREAKFAST -> "desayuno"; Meal.LUNCH -> "comida"; Meal.SNACK -> "merienda"; Meal.DINNER -> "cena" }
        val goal = minOf(kcalLeft.coerceAtLeast(200.0), dailyTarget * if (meal == Meal.SNACK || meal == Meal.BREAKFAST) 0.25 else 0.35)
        return recipes.filter { tag in it.meals && allowed(it, dislikes) }.map { match(it, emptySet()) }
            .sortedBy { kotlin.math.abs(it.nutrients.kcal - goal) - it.nutrients.protein * 2 }
    }

    /** Reconoce un ingrediente escrito o dicho ("pechugas", "tomates", "un poco de nata"). */
    fun findIngredient(text: String): Ingredient? {
        val n = LocalFoodDb.normalize(text)
            .replace(Regex("^(unos?|unas?|un poco de|algo de|mucho|mucha|tengo|hay|de|del|la|el|los|las)\\s+"), "")
            .replace(Regex("^\\d+\\s*(g|gr|gramos|kg|ml|l)?\\s*(de\\s+)?"), "").trim()
        if (n.isBlank()) return null
        val all = ingredients.values
        all.firstOrNull { i -> i.aliases.any { LocalFoodDb.normalize(it) == n } || LocalFoodDb.normalize(i.name) == n }?.let { return it }
        val singular = n.removeSuffix("es").removeSuffix("s")
        all.firstOrNull { i -> i.aliases.any { a -> LocalFoodDb.normalize(a).let { it == singular || it.removeSuffix("s") == singular } } }?.let { return it }
        // la frase contiene un alias completo (el más largo gana: "tomate frito" antes que "tomate")
        return all.flatMap { i -> i.aliases.map { LocalFoodDb.normalize(it) to i } }
            .filter { (a, _) -> Regex("\\b${Regex.escape(a)}s?\\b").containsMatchIn(n) }
            .maxByOrNull { it.first.length }?.second
    }

    /** "pollo, arroz y 2 tomates" → [pollo, arroz, tomate] + palabras no reconocidas. */
    fun parseList(text: String): Pair<List<Ingredient>, List<String>> {
        val parts = text.split(Regex("[,;\\n.]|\\s+y\\s+|\\s+e\\s+|\\s+con\\s+", RegexOption.IGNORE_CASE))
            .map { it.trim() }.filter { it.isNotBlank() }
        val found = mutableListOf<Ingredient>(); val unknown = mutableListOf<String>()
        parts.forEach { p -> findIngredient(p)?.let { if (it !in found) found += it } ?: run { unknown += p } }
        return found to unknown
    }

    fun amountLabel(i: Ingredient, g: Double): String {
        val u = i.unitG
        if (u != null && i.unitName != null) {
            val n = g / u
            val rounded = Math.round(n * 2) / 2.0
            if (rounded >= 0.5 && kotlin.math.abs(n - rounded) < 0.2) {
                val count = if (rounded % 1.0 == 0.0) rounded.toInt().toString() else fmt(rounded)
                val name = if (rounded > 1) plural(i.unitName) else i.unitName
                return "$count $name (${fmt(g, 0)} g)"
            }
        }
        return when {
            i.id == "sal" || i.id == "especias" -> "al gusto"
            i.id == "aceite" -> "${fmt(g, 0)} g (${fmt(g / 10, 0).let { if (it == "0") "un chorrito" else "$it cda." }})"
            i.id == "caldo" || i.id == "leche" -> "${fmt(g, 0)} ml"
            else -> "${fmt(g, 0)} g"
        }
    }

    private fun plural(s: String) = when {
        s.endsWith("z") -> s.dropLast(1) + "ces"
        s.last() in "aeiouáéó" -> s + "s"
        else -> s + "es"
    }

    /** Búsqueda del producto en la web del supermercado Eroski. */
    fun eroskiUrl(i: Ingredient) = "https://supermercado.eroski.es/es/search/results/?q=" + URLEncoder.encode(i.shop, "UTF-8")

    /** La receta entera como un alimento, para apuntarla en el diario. */
    fun toFoodItem(r: Recipe): FoodItem {
        val g = grams(r)
        return FoodItem(
            name = r.name, per100 = nutrients(r) * (100.0 / g), portionG = g, portionName = "1 ración",
            source = "Receta", defaultG = g,
        )
    }
}
