package com.nutridiario.app.screens

import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import android.graphics.Canvas
import androidx.compose.ui.test.onRoot
import androidx.navigation.compose.rememberNavController
import com.nutridiario.app.*
import com.nutridiario.app.ui.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.time.LocalDate

/** Genera capturas en app/build/screens/ con: ./gradlew testDebugUnitTest --tests '*Screenshots*' */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w400dp-h860dp-xhdpi")
class Screenshots {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()

    @Before fun seed() {
        val p = Profile("Ander", Sex.MALE, 32, 178.0, 84.0, Activity.LIGHT, Goal.LOSE, true)
        Repository.saveProfile(p)
        Repository.data.value.entries.forEach { Repository.deleteEntry(it.id) }
        val today = LocalDate.now().toString()
        fun local(n: String) = LocalFoodDb.search(n).first()
        Repository.addEntry(today, Meal.BREAKFAST, 250.0, local("cafe con leche"))
        Repository.addEntry(today, Meal.BREAKFAST, 90.0, local("tostada"))
        Repository.addEntry(today, Meal.LUNCH, 300.0, local("lentejas con chorizo"))
        Repository.addEntry(today, Meal.LUNCH, 120.0, local("pan"))
        Repository.addEntry(today, Meal.LUNCH, 180.0, local("manzana"))
        Repository.addEntry(
            today, Meal.SNACK, 500.0,
            FoodItem("Monster Energy", "Monster", Nutrients(47.0, 0.0, 11.0, 11.0, 0.0, 0.0, 0.0, 0.19), 500.0, "1 lata", "Código de barras", "5060337500401", packageG = 500.0, unit = "ml"),
        )
        Session.date = LocalDate.now()
    }

    private fun shot(name: String, content: @Composable () -> Unit) {
        rule.setContent { NutriTheme { content() } }
        rule.waitForIdle()
        Thread.sleep(300); rule.waitForIdle()
        val view = rule.activity.window.decorView
        val bmp = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        view.draw(Canvas(bmp))
        val f = File("build/screens/$name.png").apply { parentFile!!.mkdirs() }
        f.outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    @Test fun today() = shot("1_hoy") { TodayScreen(rememberNavController()) }
    @Test fun add() = shot("2_anadir") { AddScreen(rememberNavController()) }
    @Test fun detail() {
        Session.pendingFood = LocalFoodDb.search("lentejas con chorizo").first(); Session.pendingGrams = 300.0; Session.editingEntry = null
        shot("3_detalle") { FoodDetailScreen(rememberNavController()) }
    }
    @Test fun profile() = shot("4_perfil") { ProfileScreen(rememberNavController()) }
    @Test fun progress() = shot("5_progreso") { ProgressScreen() }
    @Test fun myFoods() {
        Repository.saveCustomFood(null, FoodItem("Chocobom", "Salica", Nutrients(500.0, 6.0, 63.0, 33.0, 25.0, 11.0), 25.0, "1 ración", "Mis alimentos", "8410368033824", packageG = 100.0, defaultG = 100.0))
        val img = File("build/screens-tmp/photo.png").apply { parentFile!!.mkdirs() }
        val b = Bitmap.createBitmap(300, 300, Bitmap.Config.ARGB_8888)
        android.graphics.Canvas(b).apply {
            drawColor(android.graphics.Color.rgb(250, 204, 21))
            drawCircle(150f, 150f, 110f, android.graphics.Paint().apply { color = android.graphics.Color.rgb(220, 38, 38) })
        }
        img.outputStream().use { b.compress(Bitmap.CompressFormat.PNG, 100, it) }
        Repository.saveCustomFood(null, FoodItem("Galletas rellenas", "Hacendado", Nutrients(480.0, 6.0, 66.0, 36.0, 19.0, 9.8), 26.0, "1 ración", "Mis alimentos", "8480000111111", imageUrl = "file://" + img.absolutePath, packageG = 500.0))
        Repository.saveCustomFood(null, FoodItem("Monster Mango Loco", "Monster", Nutrients(47.0, 0.0, 12.0, 12.0), null, null, "Mis alimentos", "5060337500999", packageG = 500.0, unit = "ml"))
        Repository.saveCustomFood(null, FoodItem("Yogur proteico", "Danone", Nutrients(60.0, 10.0, 4.0, 4.0, 0.2), null, null, "Mis alimentos", packageG = 160.0))
        shot("7_mis_alimentos") { MyFoodsScreen(rememberNavController()) }
    }
    private fun noNetworkPhotos() =
        RecipeBook.ingredients.keys.forEach { Repository.cacheIngredientImage(it, IngredientImage(null, null, false)) }

    @Config(qualifiers = "w400dp-h2600dp-xhdpi")
    @Test fun recipesTall() {
        noNetworkPhotos()
        Repository.setPantry(listOf("pollo", "arroz", "pimiento", "cebolla", "tomate", "huevo", "patata"))
        Repository.setDislikes(DietGroup.FISH.ids.toList()); Repository.setRecipeSort("PROTEIN_DESC")
        shot("8b_recetas_resultados") { RecipesScreen(rememberNavController()) }
    }
    @Test fun recipes() {
        noNetworkPhotos()
        Repository.setPantry(listOf("pollo", "arroz", "pimiento", "cebolla", "tomate", "huevo", "patata"))
        shot("8_recetas") { RecipesScreen(rememberNavController()) }
    }
    @Test fun recipeDetail() {
        noNetworkPhotos()
        Repository.setPantry(listOf("huevo", "patata"))
        Session.selectedRecipe = RecipeBook.recipes.first { it.name == "Tortilla de patatas" }
        shot("9_receta") { RecipeDetailScreen(rememberNavController()) }
    }
    @Test fun editor() {
        Session.editorFood = null; Session.manualText = ""
        shot("6_nuevo_alimento") { CustomFoodScreen(rememberNavController()) }
    }
}
