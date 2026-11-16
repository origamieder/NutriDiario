package com.nutridiario.app

import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.nio.file.Files

class MyFoodsTest {
    @Test fun saveFindEditDelete() {
        val dir = Files.createTempDirectory("nd").toFile()
        Repository.initFile(File(dir, "data.json"))
        LocalFoodDb.initFromJson(File("src/main/assets/foods.json").readText())

        val choco = FoodItem(
            name = "Chocobom", brand = "Salica", barcode = "8410368033824", source = "Mis alimentos",
            per100 = Nutrients(kcal = 500.0, fat = 24.0, carbs = 64.0), packageG = 100.0, defaultG = 100.0,
        )
        Repository.saveCustomFood(null, choco)
        assertEquals(choco, Repository.customByBarcode("8410368033824"))   // al escanear sale directo
        assertEquals(100.0, choco.defaultAmount(), 0.01)
        assertEquals("Chocobom", LocalFoodDb.search("chocobom").first().name) // sale primero al buscar

        val edited = choco.copy(per100 = choco.per100.copy(kcal = 510.0))
        Repository.saveCustomFood(choco, edited)
        assertEquals(1, Repository.data.value.customFoods.size)              // no se duplica
        assertEquals(510.0, Repository.customByBarcode("8410368033824")!!.per100.kcal, 0.01)

        // se conserva tras reiniciar la app
        Repository.initFile(File(dir, "data.json"))
        assertEquals(1, Repository.data.value.customFoods.size)

        Repository.deleteCustomFood(Repository.data.value.customFoods.first())
        assertNull(Repository.customByBarcode("8410368033824"))
    }
}
