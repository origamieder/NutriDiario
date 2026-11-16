package com.nutridiario.app

import org.junit.Assert.*
import org.junit.BeforeClass
import org.junit.Test
import java.io.File

class RecipeTest {
    companion object {
        @BeforeClass @JvmStatic fun setup() {
            LocalFoodDb.initFromJson(File("src/main/assets/foods.json").readText())
            RecipeBook.initFromJson(File("src/main/assets/ingredients.json").readText(), File("src/main/assets/recipes.json").readText())
        }
    }

    @Test fun understandsWhatYouHave() {
        val (found, unknown) = RecipeBook.parseList("Tengo pechugas de pollo, arroz, 2 tomates y una cebolla, tomate frito y caviar")
        assertEquals(listOf("pollo", "arroz", "tomate", "cebolla", "tomate_frito"), found.map { it.id })
        assertEquals(listOf("caviar"), unknown)
        assertEquals("huevo", RecipeBook.findIngredient("huevos")!!.id)
        assertEquals("champinones", RecipeBook.findIngredient("champiñones")!!.id)
        assertEquals("pasta", RecipeBook.findIngredient("unos macarrones")!!.id)
    }

    @Test fun recommendsWithWhatYouHave() {
        val mine = setOf("pollo", "arroz", "pimiento", "cebolla", "tomate", "caldo")
        val res = RecipeBook.suggest(mine)
        val first = res.first()
        assertTrue(first.missing.isEmpty())
        assertEquals("Arroz con pollo y verduras", first.recipe.name) // tomate cubre el tomate triturado
        res.take(6).forEach { println("${it.recipe.name}: falta ${it.missing.map { m -> m.id }}") }
        // la sal, el aceite, el ajo y las especias no hace falta decirlos
        assertTrue(first.recipe.ingredients.any { it.id == "aceite" })
    }

    @Test fun eggsAndPotatoes() {
        val res = RecipeBook.suggest(setOf("huevo", "patata"))
        val tortilla = res.first { it.recipe.name == "Tortilla de patatas" }
        assertEquals(listOf("cebolla"), tortilla.missing.map { it.id })   // te falta poco: solo la cebolla
        assertTrue(res.indexOf(tortilla) < 3)
    }

    @Test fun labelsAndNutrition() {
        assertEquals("2 huevos (120 g)", RecipeBook.amountLabel(RecipeBook.ing("huevo"), 120.0))
        assertEquals("1 lata (60 g)", RecipeBook.amountLabel(RecipeBook.ing("atun"), 60.0))
        assertEquals("150 g", RecipeBook.amountLabel(RecipeBook.ing("pollo"), 150.0))
        val r = RecipeBook.recipes.first { it.name == "Tortilla de patatas" }
        val f = RecipeBook.toFoodItem(r)
        assertEquals(RecipeBook.nutrients(r).kcal, f.per100.forGrams(f.defaultG!!).kcal, 0.5)
        assertTrue(RecipeBook.recipes.all { RecipeBook.nutrients(it).kcal in 150.0..800.0 })
        assertTrue(RecipeBook.eroskiUrl(RecipeBook.ing("nata")).endsWith("q=nata+cocinar"))
    }

    @Test fun ideasForDinner() {
        val ideas = RecipeBook.ideas(Meal.DINNER, 600.0, 2000.0)
        assertTrue(ideas.isNotEmpty()); assertTrue(ideas.first().nutrients.kcal < 750)
        println("Ideas cena: " + ideas.take(4).map { it.recipe.name + " " + fmt(it.nutrients.kcal, 0) })
    }

    @Test fun noFishAndSorting() {
        val noFish = DietGroup.FISH.ids
        val all = RecipeBook.suggest(setOf("arroz", "tomate", "patata", "huevo", "salmon", "atun"), maxMissing = 9, dislikes = noFish)
        assertTrue(all.isNotEmpty())
        assertTrue(all.none { m -> m.recipe.ingredients.any { it.id in noFish } })
        val ideas = RecipeBook.ideas(Meal.DINNER, 700.0, 2000.0, noFish)
        assertTrue(ideas.none { m -> m.recipe.ingredients.any { it.id in noFish } })
        val byKcal = all.sortedBy(RecipeSort.KCAL_ASC).map { it.nutrients.kcal }
        assertEquals(byKcal.sorted(), byKcal)
        val byProt = all.sortedBy(RecipeSort.PROTEIN_DESC).map { it.nutrients.protein }
        assertEquals(byProt.sortedDescending(), byProt)
        // el atún en lata sí se permite
        assertTrue(RecipeBook.allowed(RecipeBook.recipes.first { it.name == "Macarrones con tomate y atún" }, noFish))
        assertFalse(RecipeBook.allowed(RecipeBook.recipes.first { it.name == "Salmón al horno con patatas" }, noFish))
        println("Sin pescado se ocultan: " + RecipeBook.recipes.filterNot { RecipeBook.allowed(it, noFish) }.map { it.name })
    }

    @Test fun oldNoFishSettingKeepsTuna() {
        val f = java.io.File(java.nio.file.Files.createTempDirectory("mig").toFile(), "d.json")
        f.writeText("""{"dislikes":["salmon","merluza","atun","gambas","champinones"]}""")
        Repository.initFile(f)
        assertEquals(listOf("salmon", "merluza", "gambas", "champinones"), Repository.data.value.dislikes)
        assertEquals(1, Repository.data.value.dataVersion)
    }
}
