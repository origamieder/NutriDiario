package com.nutridiario.app

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json
import java.io.File
import java.time.LocalDate

/** Almacenamiento local en un fichero JSON privado de la app. */
object Repository {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private lateinit var file: File
    private val _data = MutableStateFlow(AppData())
    val data: StateFlow<AppData> = _data.asStateFlow()

    fun init(context: Context) = initFile(File(context.filesDir, "nutridiario.json"))

    fun initFile(f: File) {
        file = f
        _data.value = if (file.exists()) {
            runCatching { json.decodeFromString(AppData.serializer(), file.readText()) }.getOrDefault(AppData())
        } else AppData()
    }

    @Synchronized
    private fun update(block: (AppData) -> AppData) {
        val new = block(_data.value)
        _data.value = new
        val tmp = File(file.parentFile, file.name + ".tmp")
        tmp.writeText(json.encodeToString(AppData.serializer(), new))
        tmp.renameTo(file)
    }

    fun saveProfile(p: Profile) = update { d ->
        val today = LocalDate.now().toString()
        val weights = d.weights.filterNot { it.date == today } + WeightEntry(today, p.weightKg)
        d.copy(profile = p.copy(configured = true), weights = weights.sortedBy { it.date })
    }

    fun addWeight(date: String, kg: Double) = update { d ->
        val weights = (d.weights.filterNot { it.date == date } + WeightEntry(date, kg)).sortedBy { it.date }
        val latest = weights.last()
        d.copy(weights = weights, profile = if (latest.date == date) d.profile.copy(weightKg = kg) else d.profile)
    }

    fun deleteWeight(date: String) = update { d -> d.copy(weights = d.weights.filterNot { it.date == date }) }

    fun addEntry(date: String, meal: Meal, grams: Double, food: FoodItem) = update { d ->
        val id = (d.entries.maxOfOrNull { it.id } ?: 0L) + 1
        d.copy(entries = d.entries + FoodEntry(id, date, meal, grams, food))
    }

    fun updateEntry(id: Long, meal: Meal, grams: Double) = update { d ->
        d.copy(entries = d.entries.map { if (it.id == id) it.copy(meal = meal, grams = grams) else it })
    }

    fun deleteEntry(id: Long) = update { d -> d.copy(entries = d.entries.filterNot { it.id == id }) }

    fun addCustomFood(f: FoodItem) = saveCustomFood(null, f)

    /** Guarda (o reemplaza) un alimento en "Mis alimentos". */
    fun saveCustomFood(original: FoodItem?, f: FoodItem) = update { d ->
        val rest = d.customFoods.filterNot { it == original || sameProduct(it, f) }
        d.copy(customFoods = (rest + f).sortedBy { it.name.lowercase() })
    }

    fun deleteCustomFood(f: FoodItem) = update { d -> d.copy(customFoods = d.customFoods.filterNot { it == f }) }

    fun customByBarcode(code: String): FoodItem? = _data.value.customFoods.firstOrNull { it.barcode == code }

    private fun sameProduct(a: FoodItem, b: FoodItem) =
        if (a.barcode != null && b.barcode != null) a.barcode == b.barcode
        else a.name.equals(b.name, true) && (a.brand ?: "").equals(b.brand ?: "", true)

    /** Cantidad que usaste la última vez con este mismo producto (por código de barras o nombre+marca). */
    fun lastAmountFor(food: FoodItem): Double? = _data.value.entries
        .filter { e ->
            if (food.barcode != null && e.food.barcode != null) e.food.barcode == food.barcode
            else e.food.name.equals(food.name, true) && (e.food.brand ?: "").equals(food.brand ?: "", true)
        }
        .maxByOrNull { it.timestamp }?.grams

    fun setPantry(ids: List<String>) = update { d -> d.copy(pantry = ids.distinct()) }

    fun cacheIngredientImage(id: String, img: IngredientImage) =
        update { d -> d.copy(ingredientImages = d.ingredientImages + (id to img)) }

    fun entriesFor(date: String) = _data.value.entries.filter { it.date == date }

    /** Alimentos usados recientemente (sin repetir), para añadirlos rápido. */
    fun recentFoods(limit: Int = 15): List<FoodEntry> =
        _data.value.entries.sortedByDescending { it.timestamp }
            .distinctBy { it.food.name.lowercase() + "|" + (it.food.brand ?: "") }
            .take(limit)
}

/** Estado temporal compartido entre pantallas durante el flujo de "añadir alimento". */
object Session {
    var date: LocalDate = LocalDate.now()
    var pendingFood: FoodItem? = null
    var pendingGrams: Double? = null
    var editingEntry: FoodEntry? = null
    var manualText: String = ""
    /** Comida a la que se añadirá (si se pulsó el "+" de una comida concreta). */
    var preferredMeal: Meal? = null
    /** Alimento con el que se abre el editor de "Mis alimentos" (para corregir/editar). */
    var editorFood: FoodItem? = null
    /** true si editorFood ya está guardado en "Mis alimentos" (se reemplaza al guardar). */
    var editorIsMine: Boolean = false
    /** Abrir el editor directamente con la cámara para leer la tabla nutricional. */
    var editorAutoLabel: Boolean = false
    var selectedRecipe: Recipe? = null
}
