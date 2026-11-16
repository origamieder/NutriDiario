package com.nutridiario.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LabelParserTest {
    private fun line(text: String, row: Int, col: Int = 0) =
        OcrLine(text, left = 40 + col * 400, top = 100 + row * 50, right = 40 + col * 400 + 300, bottom = 140 + row * 50)

    /** Caso 1: el OCR devuelve cada fila completa en una línea. */
    @Test fun oneLinePerRow() {
        val d = LabelParser.parse(
            listOf(
                "INFORMACIÓN NUTRICIONAL Por 100 g", "Valor energético 2098 kJ / 500 kcal",
                "Grasas 24 g", "de las cuales saturadas 11 g", "Hidratos de carbono 63 g",
                "de los cuales azúcares 33 g", "Fibra alimentaria 2,5 g", "Proteínas 6,0 g", "Sal 0,40 g", "Peso neto: 100 g",
            ).mapIndexed { i, t -> line(t, i) }
        )
        assertEquals(500.0, d.kcal!!, 0.01); assertEquals(24.0, d.fat!!, 0.01); assertEquals(11.0, d.satFat!!, 0.01)
        assertEquals(63.0, d.carbs!!, 0.01); assertEquals(33.0, d.sugar!!, 0.01); assertEquals(2.5, d.fiber!!, 0.01)
        assertEquals(6.0, d.protein!!, 0.01); assertEquals(0.4, d.salt!!, 0.01); assertEquals(100.0, d.packageAmount!!, 0.01)
        assertEquals(8, d.nutrientCount)
    }

    /** Caso 2: nombres en una columna y valores en otras (por 100 g | por ración | %IR). */
    @Test fun columnsPer100AndServing() {
        val names = listOf("Valor energético", "Grasas", "de las cuales saturadas", "Hidratos de carbono", "de los cuales azúcares", "Proteínas", "Sal")
        val per100 = listOf("1975 kJ / 471 kcal", "19 g", "9,8 g", "66 g", "36 g", "6,5 g", "0,53 g")
        val perServing = listOf("494 kJ / 118 kcal", "4,8 g", "2,5 g", "17 g", "9,0 g", "1,6 g", "0,13 g")
        val ri = listOf("6%", "7%", "12%", "6%", "10%", "3%", "2%")
        val lines = names.mapIndexed { i, t -> line(t, i + 1, 0) } +
            listOf(line("Por 100 g", 0, 1), line("Por ración (25 g)", 0, 2)) +
            per100.mapIndexed { i, t -> line(t, i + 1, 1) } + perServing.mapIndexed { i, t -> line(t, i + 1, 2) } +
            ri.mapIndexed { i, t -> line(t, i + 1, 3) }
        val d = LabelParser.parse(lines.shuffled(kotlin.random.Random(3))) // el orden de lectura no importa
        assertEquals(471.0, d.kcal!!, 0.01); assertEquals(19.0, d.fat!!, 0.01); assertEquals(9.8, d.satFat!!, 0.01)
        assertEquals(66.0, d.carbs!!, 0.01); assertEquals(36.0, d.sugar!!, 0.01); assertEquals(6.5, d.protein!!, 0.01)
        assertEquals(0.53, d.salt!!, 0.001); assertEquals(25.0, d.servingAmount!!, 0.01)
        assertNull(d.fiber)
    }

    /** Caso 3: bebida, energía en dos líneas, "<0,5 g", sodio en mg, errores de OCR (O por 0). */
    @Test fun drinkWithOcrNoise() {
        val d = LabelParser.parse(
            listOf(
                "Valores medios por 100 ml", "Energía 197 kJ", "47 kcal", "Grasas: O g", "de las cuales saturadas: 0 g",
                "Hidratos de carbono: 11 g", "de los cuales azúcares: 11 g", "Proteínas: <0,5 g", "Sodio 80 mg", "500 ml ℮",
            ).mapIndexed { i, t -> line(t, i) }
        )
        assertEquals(47.0, d.kcal!!, 0.01); assertEquals(0.0, d.fat!!, 0.01); assertEquals(11.0, d.sugar!!, 0.01)
        assertEquals(0.5, d.protein!!, 0.01); assertEquals(0.2, d.salt!!, 0.001)
        assertEquals("ml", d.unit); assertEquals(500.0, d.packageAmount!!, 0.01)
    }

    @Test fun onlyKj() {
        val d = LabelParser.parse(listOf(line("Energía 2092 kJ", 0), line("Grasas 10 g", 1)))
        assertEquals(500.0, d.kcal!!, 0.1)
    }
}
