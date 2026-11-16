package com.nutridiario.app

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test

class PackageTest {
    private fun product(quantity: String, pq: Double? = null, unit: String = "", serving: Double? = null) = JSONObject(
        """{"product_name":"X","code":"1","quantity":"$quantity",
           ${pq?.let { "\"product_quantity\":$it," } ?: ""} "product_quantity_unit":"$unit",
           ${serving?.let { "\"serving_quantity\":$it,\"serving_size\":\"$it g\"," } ?: ""}
           "nutriments":{"energy-kcal_100g":47,"sugars_100g":11}}"""
    )

    @Test fun monsterCan500ml() {
        val f = OpenFoodFacts.parse(product("500 ml", 500.0, "ml", serving = 250.0), "test")!!
        assertEquals(500.0, f.packageG!!, 0.1); assertEquals("ml", f.unit)
        assertEquals(500.0, f.defaultAmount(), 0.1)   // lata entera, no la "ración" de 250
    }

    @Test fun variants() {
        assertEquals(500.0, PackageQuantity.parse("50 cl", null, null)!!.amount, 0.1)
        assertEquals(500.0, PackageQuantity.parse("0,5 L", null, null)!!.amount, 0.1)
        assertEquals(25.0, PackageQuantity.parse("6 x 25 g", 150.0, "g")!!.amount, 0.1)
        assertEquals(330.0, PackageQuantity.parse("4x330ml", null, null)!!.amount, 0.1)
        assertEquals(160.0, PackageQuantity.parse("", 160.0, "g")!!.amount, 0.1)
    }

    @Test fun cookiePackAndBigPackage() {
        assertEquals(160.0, OpenFoodFacts.parse(product("160 g"), "t")!!.defaultAmount(), 0.1)
        // paquete grande de 1 kg: usa la ración, no el kilo entero
        assertEquals(30.0, OpenFoodFacts.parse(product("1 kg", serving = 30.0), "t")!!.defaultAmount(), 0.1)
        assertEquals(100.0, OpenFoodFacts.parse(product("1 kg"), "t")!!.defaultAmount(), 0.1)
    }
}
