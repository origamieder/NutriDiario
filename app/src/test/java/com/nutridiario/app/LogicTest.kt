package com.nutridiario.app

import org.junit.Assert.*
import org.junit.BeforeClass
import org.junit.Test
import java.io.File

class LogicTest {
    companion object {
        @BeforeClass @JvmStatic
        fun setup() = LocalFoodDb.initFromJson(File("src/main/assets/foods.json").readText())
    }

    @Test fun parserGrams() {
        val p = QuantityParser.parse("150 g de pollo a la plancha")
        assertEquals(150.0, p.grams!!, 0.01); assertEquals("pollo a la plancha", p.query)
        assertEquals(500.0, QuantityParser.parse("medio kilo de fresas").grams!!, 0.01)
        assertEquals(1500.0, QuantityParser.parse("1,5 litros de agua").grams!!, 0.01)
        assertEquals(200.0, QuantityParser.parse("200gr arroz").grams!!, 0.01)
    }

    @Test fun parserUnitsAndContainers() {
        val h = QuantityParser.parse("2 huevos")
        assertEquals(2.0, h.units!!, 0.01); assertEquals("huevos", h.query)
        val v = QuantityParser.parse("un vaso de leche")
        assertEquals(250.0, v.grams!!, 0.01); assertEquals("leche", v.query)
        assertEquals(30.0, QuantityParser.parse("2 cucharadas de aceite").grams!!, 0.01)
    }

    @Test fun localSearch() {
        assertEquals("Pechuga de pollo a la plancha", LocalFoodDb.search("pollo a la plancha").first().name)
        assertEquals("Huevo cocido", LocalFoodDb.search("huevos").first().name)
        assertTrue(LocalFoodDb.search("platano").first().name.startsWith("Plátano"))
        assertEquals("Leche entera", LocalFoodDb.search("leche").first().name)
        assertEquals("Arroz blanco cocido", LocalFoodDb.search("arroz").first().name)
        println(LocalFoodDb.search("tortilla").map { it.name })
        println(LocalFoodDb.search("macarrones").map { it.name })
    }

    @Test fun aiLabelsMapToFoods() {
        listOf("pizza", "cheeseburger", "Granny Smith", "banana", "carbonara", "hotdog", "broccoli", "ice cream", "French loaf", "espresso", "mushroom", "burrito")
            .forEach { assertNotNull("sin mapear: $it", LocalFoodDb.byAlias(it)) }
    }

    @Test fun health() {
        val r = HealthReport(Profile(sex = Sex.MALE, age = 35, heightCm = 178.0, weightKg = 90.0, activity = Activity.LIGHT, goal = Goal.LOSE))
        assertEquals(28.4, r.bmi, 0.1)
        assertEquals("Sobrepeso", r.bmiCategory)
        assertEquals(1842.5, r.bmr, 0.1)
        assertTrue(r.idealWeight in r.healthyMin..r.healthyMax)
        println("BMI ${r.bmi} ideal ${r.idealWeight} rango ${r.healthyMin}-${r.healthyMax} BMR ${r.bmr} TDEE ${r.tdee} targets ${r.targets} semanas ${r.weeksToIdeal}")
        println(r.advice)
        val f = HealthReport(Profile(sex = Sex.FEMALE, age = 28, heightCm = 162.0, weightKg = 55.0))
        println("F: BMI ${f.bmi} ideal ${f.idealWeight} ${f.idealFormulas} fat ${f.bodyFatPct} targets ${f.targets}")
        println(f.advice)
    }
}
