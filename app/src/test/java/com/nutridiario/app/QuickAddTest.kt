package com.nutridiario.app

import com.nutridiario.app.ui.confidentAmount
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.nio.file.Files

class QuickAddTest {
    @Test fun amountsForInstantAdd() {
        Repository.initFile(File(Files.createTempDirectory("qa").toFile(), "d.json"))
        val monster = FoodItem("Monster Energy", "Monster", Nutrients(47.0), source = "x", barcode = "1", packageG = 500.0, unit = "ml")
        assertEquals(500.0, confidentAmount(monster)!!, 0.01)                      // lata entera
        val bigBox = FoodItem("Digestive", null, Nutrients(480.0), portionG = 14.7, source = "x", barcode = "2", packageG = 425.0)
        assertEquals(14.7, confidentAmount(bigBox)!!, 0.01)                        // 1 galleta
        val unknown = FoodItem("Sin datos", null, Nutrients(300.0), source = "x", barcode = "3")
        assertNull(confidentAmount(unknown))                                        // se abre la ficha para preguntar
        Repository.addEntry("2026-11-16", Meal.SNACK, 60.0, unknown)
        assertEquals(60.0, confidentAmount(unknown)!!, 0.01)                       // la próxima vez, lo de la última vez
        val mine = FoodItem("Chocobom", null, Nutrients(500.0), source = "Mis alimentos", barcode = "4", packageG = 100.0, defaultG = 100.0)
        assertEquals(100.0, confidentAmount(mine)!!, 0.01)
    }
}
