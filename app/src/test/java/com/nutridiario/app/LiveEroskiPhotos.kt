package com.nutridiario.app

import kotlinx.coroutines.runBlocking
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/** Prueba con internet (solo con -Dlive=true): fotos Eroski reales para ingredientes de las recetas. */
class LiveEroskiPhotos {
    @Test fun realPhotos() = runBlocking {
        assumeTrue(System.getProperty("live") == "true")
        LocalFoodDb.initFromJson(File("src/main/assets/foods.json").readText())
        RecipeBook.initFromJson(File("src/main/assets/ingredients.json").readText(), File("src/main/assets/recipes.json").readText())
        var eroski = 0; var any = 0; var downloadable = 0
        val ids = (System.getProperty("ids") ?: "nata,huevo,pasta").split(",")
        for (id in ids) {
            val ing = RecipeBook.ing(id)
            val img = runCatching { OpenFoodFacts.ingredientImage(ing.shop) }.getOrNull()
            var ok = false
            img?.url?.let { u ->
                ok = runCatching {
                    val c = URL(u).openConnection() as HttpURLConnection
                    (c.responseCode == 200 && (c.contentType ?: "").startsWith("image")).also { c.disconnect() }
                }.getOrDefault(false)
            }
            if (img != null) any++; if (img?.eroski == true) eroski++; if (ok) downloadable++
            println("${ing.name.padEnd(28)} → ${if (img?.eroski == true) "EROSKI" else "otra  "} ${img?.title} ${if (ok) "✓ foto descargable" else "✗"}")
        }
        println("Resumen: $any/${ids.size} con foto, $eroski de marca Eroski, $downloadable descargables")
    }
}
