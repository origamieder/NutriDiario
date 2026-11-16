package com.nutridiario.app

import kotlinx.serialization.Serializable

@Serializable
data class Nutrients(
    val kcal: Double = 0.0,
    val protein: Double = 0.0,
    val carbs: Double = 0.0,
    val sugar: Double = 0.0,
    val fat: Double = 0.0,
    val satFat: Double = 0.0,
    val fiber: Double = 0.0,
    val salt: Double = 0.0,
) {
    operator fun times(f: Double) = Nutrients(
        kcal * f, protein * f, carbs * f, sugar * f, fat * f, satFat * f, fiber * f, salt * f
    )

    operator fun plus(o: Nutrients) = Nutrients(
        kcal + o.kcal, protein + o.protein, carbs + o.carbs, sugar + o.sugar,
        fat + o.fat, satFat + o.satFat, fiber + o.fiber, salt + o.salt
    )

    /** Valores para una cantidad en gramos, siendo este objeto los valores por 100 g. */
    fun forGrams(g: Double) = this * (g / 100.0)
}

/** Alimento candidato (aún no añadido al diario). Valores siempre por 100 g/ml. */
@Serializable
data class FoodItem(
    val name: String,
    val brand: String? = null,
    val per100: Nutrients,
    val portionG: Double? = null,
    val portionName: String? = null,
    val source: String,
    val barcode: String? = null,
    val imageUrl: String? = null,
)

enum class Meal(val label: String) {
    BREAKFAST("Desayuno"), LUNCH("Comida"), SNACK("Merienda / snack"), DINNER("Cena");

    companion object {
        fun byTime(hour: Int) = when (hour) {
            in 5..10 -> BREAKFAST
            in 11..15 -> LUNCH
            in 16..19 -> SNACK
            else -> DINNER
        }
    }
}

@Serializable
data class FoodEntry(
    val id: Long,
    val date: String, // yyyy-MM-dd
    val meal: Meal,
    val grams: Double,
    val food: FoodItem,
    val timestamp: Long = System.currentTimeMillis(),
) {
    val nutrients: Nutrients get() = food.per100.forGrams(grams)
}

enum class Sex(val label: String) { MALE("Hombre"), FEMALE("Mujer") }

enum class Activity(val label: String, val desc: String, val factor: Double) {
    SEDENTARY("Sedentario", "Trabajo sentado, sin ejercicio", 1.2),
    LIGHT("Ligero", "Ejercicio 1-3 días/semana", 1.375),
    MODERATE("Moderado", "Ejercicio 3-5 días/semana", 1.55),
    ACTIVE("Activo", "Ejercicio intenso 6-7 días/semana", 1.725),
    VERY_ACTIVE("Muy activo", "Trabajo físico + entrenamiento diario", 1.9),
}

enum class Goal(val label: String, val kcalDelta: Int) {
    LOSE("Perder peso (-0,5 kg/sem)", -500),
    LOSE_SLOW("Perder peso suave (-0,25 kg/sem)", -250),
    MAINTAIN("Mantener peso", 0),
    GAIN("Ganar peso / músculo", 300),
}

@Serializable
data class Profile(
    val name: String = "",
    val sex: Sex = Sex.MALE,
    val age: Int = 30,
    val heightCm: Double = 175.0,
    val weightKg: Double = 75.0,
    val activity: Activity = Activity.LIGHT,
    val goal: Goal = Goal.MAINTAIN,
    val configured: Boolean = false,
)

@Serializable
data class WeightEntry(val date: String, val kg: Double)

@Serializable
data class AppData(
    val profile: Profile = Profile(),
    val entries: List<FoodEntry> = emptyList(),
    val weights: List<WeightEntry> = emptyList(),
    val customFoods: List<FoodItem> = emptyList(),
)
