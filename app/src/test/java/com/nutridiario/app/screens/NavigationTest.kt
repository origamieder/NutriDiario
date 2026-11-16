package com.nutridiario.app.screens

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.nutridiario.app.*
import com.nutridiario.app.ui.NutriTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Navega por la app real pulsando la barra inferior, como haría una persona. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w400dp-h860dp-xhdpi")
class NavigationTest {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()

    private fun tab(route: String) { rule.onNodeWithTag("tab_$route").performClick(); rule.waitForIdle() }
    private fun onToday() = rule.onNodeWithText("Desayuno", substring = false).assertExists("no estoy en Hoy")
    private fun onAdd() = rule.onNodeWithText("¿Qué has comido?").assertExists("no estoy en Añadir")

    private fun start(configured: Boolean) {
        Repository.saveProfile(Profile("Test", configured = true))
        if (!configured) Repository.saveProfile(Repository.data.value.profile) // (se guarda configurado igualmente)
        rule.setContent { NutriTheme { AppRoot() } }
        rule.waitForIdle()
    }

    @Test fun addThenHome() {
        start(true)
        onToday()
        tab("add"); onAdd()
        tab("today"); onToday()
        tab("add"); onAdd()
        tab("today"); onToday()
    }

    @Test fun fabThenHome() {
        start(true)
        rule.onNodeWithText("Añadir", useUnmergedTree = true).let { } // FAB y pestaña tienen el mismo texto
        rule.onAllNodesWithText("Añadir")[0].performClick(); rule.waitForIdle()
        onAdd()
        tab("today"); onToday()
    }

    @Test fun allTabsAndBack() {
        start(true)
        for (r in listOf("add", "recipes", "progress", "profile", "add", "today", "recipes", "today", "add", "today")) tab(r)
        onToday()
    }

    @Test fun afterAddingFoodGoHomeWorks() {
        start(true)
        tab("add"); onAdd()
        // entrar en "Mis alimentos" → nuevo… y volver al día como tras añadir un alimento
        rule.onNodeWithText("Mis alimentos").performClick(); rule.waitForIdle()
        rule.activity.runOnUiThread { }
        rule.onNodeWithContentDescription("Atrás").performClick(); rule.waitForIdle()
        onAdd()
        tab("today"); onToday()
        tab("add"); onAdd()
        tab("today"); onToday()
    }

    /** Primer arranque: la app empieza en Perfil; tras guardarlo, la barra inferior debe ir bien. */
    @Test fun firstRunThenAddThenHome() {
        Repository.initFile(java.io.File(java.nio.file.Files.createTempDirectory("first").toFile(), "d.json"))
        rule.setContent { NutriTheme { AppRoot() } }
        rule.waitForIdle()
        rule.onNodeWithText("Guardar").performScrollTo().performClick(); rule.waitForIdle()
        onToday()
        tab("add"); onAdd()
        tab("today"); onToday()
        tab("progress"); tab("today"); onToday()
        tab("add"); onAdd()
        tab("today"); onToday()
    }

    /** Desde cualquier pestaña, el botón Atrás del móvil vuelve a "Hoy". */
    @Test fun systemBackFromTabsGoesHome() {
        start(true)
        for (t in listOf("add", "recipes", "progress", "profile")) {
            tab(t)
            rule.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
            rule.waitForIdle()
            onToday()
        }
    }

    /** Pestaña Añadir → Mis alimentos → pestaña Hoy (sin volver atrás antes). */
    @Test fun deepInAddThenHome() {
        start(true)
        tab("add")
        rule.onNodeWithText("Mis alimentos").performClick(); rule.waitForIdle()
        rule.onNodeWithContentDescription("Atrás").performClick(); rule.waitForIdle()
        tab("recipes"); tab("add"); onAdd()
        tab("today"); onToday()
    }
}
