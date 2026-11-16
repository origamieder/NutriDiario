package com.nutridiario.app

import org.junit.Assert.*
import org.junit.Test
import java.io.File

class ClassifierMergeTest {
    @Test fun food101LabelsAllMapToFoods() {
        LocalFoodDb.initFromJson(File("src/main/assets/foods.json").readText())
        val labels = kotlinx.serialization.json.Json.decodeFromString<List<String>>(File("src/main/assets/food101_labels.json").readText())
        assertEquals(101, labels.size)
        labels.forEach { assertNotNull("sin alimento: $it", LocalFoodDb.byAlias(it.replace('_', ' '))) }
        assertEquals("Paella", LocalFoodDb.byAlias("paella")!!.name)
        assertEquals("Tortilla francesa", LocalFoodDb.byAlias("omelette")!!.name)
        assertEquals("Ensalada César", LocalFoodDb.byAlias("caesar salad")!!.name)
    }

    @Test fun mergeKeepsBestOfBothModels() {
        LocalFoodDb.initFromJson(File("src/main/assets/foods.json").readText())
        fun p(l: String, s: Float) = Prediction(l, s, LocalFoodDb.byAlias(l))
        val dishes = listOf(p("apple pie", 0.30f), p("paella", 0.25f), p("omelette", 0.01f))
        val general = listOf(p("Granny Smith", 0.70f), p("plate", 0.4f), p("pizza", 0.05f))
        val r = FoodClassifier.merge(dishes, general)
        assertEquals(listOf("Manzana", "Tarta de manzana", "Paella", "Pizza"), r.filter { it.food != null }.map { it.food!!.name })
        assertTrue(r.none { it.label == "omelette" })   // < 2 %: descartado
        assertEquals("plate", r.last().label)            // lo que no es comida, al final
    }
}
