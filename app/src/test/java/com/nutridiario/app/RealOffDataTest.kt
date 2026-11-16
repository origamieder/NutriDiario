package com.nutridiario.app

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test

/** Pruebas con respuestas reales de Open Food Facts (guardadas en src/test/resources). */
class RealOffDataTest {
    private fun products(name: String): List<FoodItem> {
        val j = JSONObject(javaClass.classLoader!!.getResource("off_$name.json").readText())
        val arr = j.getJSONArray("products")
        return (0 until arr.length()).mapNotNull { OpenFoodFacts.parse(arr.getJSONObject(it), "test") }
    }
    private fun List<FoodItem>.named(n: String) = first { it.name == n }

    @Test fun monsterCansAre500ml() {
        val m = products("monster")
        assertEquals(500.0, m.named("Monster Energy").defaultAmount(), 0.1)
        assertEquals("ml", m.named("Monster Energy").unit)
        assertEquals(500.0, m.named("Monster Mango loco").defaultAmount(), 0.1)
        assertEquals(473.0, m.named("Monster Energy Zero Ultra").defaultAmount(), 0.1)
    }

    @Test fun cookies() {
        val g = products("galletas")
        assertEquals(14.7, g.named("Digestive Avena Choco").defaultAmount(), 0.1)          // caja de 425 g → 1 ración
        assertEquals(45.0, g.named("SiN GLuTEN Sandwich Chocolate").defaultAmount(), 0.1)   // "225 g (5 x 45g)" → 1 paquetito
        assertEquals(41.4, g.named("Nutella Biscuits").defaultAmount(), 0.1) // paquete individual
        assertEquals(60.0, g.named("Barquillos de choco").defaultAmount(), 0.1)
        assertEquals(150.0, g.named("Zero Chip Choco").defaultAmount(), 0.1)               // "Peso neto 150g"
    }
}
