package com.nutridiario.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.nutridiario.app.ui.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { NutriTheme { AppRoot() } }
    }
}

private data class Tab(val route: String, val label: String, val icon: ImageVector)

private val tabs = listOf(
    Tab("today", "Hoy", Icons.Filled.Home),
    Tab("add", "Añadir", Icons.Filled.Add),
    Tab("recipes", "Recetas", Icons.AutoMirrored.Filled.List),
    Tab("progress", "Progreso", Icons.Filled.DateRange),
    Tab("profile", "Perfil", Icons.Filled.Person),
)

@Composable
fun AppRoot() {
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val current = backStack?.destination?.route

    Scaffold(
        bottomBar = {
            if (current in tabs.map { it.route }) {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest, tonalElevation = 0.dp) {
                    tabs.forEach { t ->
                        NavigationBarItem(
                            selected = current == t.route,
                            onClick = { goTab(nav, t.route) },
                            icon = { Icon(t.icon, null) },
                            label = { Text(t.label) },
                            colors = NavigationBarItemDefaults.colors(indicatorColor = MaterialTheme.colorScheme.primaryContainer),
                            modifier = Modifier.testTag("tab_${t.route}"),
                        )
                    }
                }
            }
        }
    ) { pad ->
        NavHost(nav, startDestination = "today", modifier = Modifier.padding(pad)) {
            composable("today") {
                // Siempre se empieza en "Hoy"; el primer día se abre el perfil encima para rellenarlo.
                LaunchedEffect(Unit) {
                    if (!Repository.data.value.profile.configured) nav.navigate("profile") { launchSingleTop = true }
                }
                TodayScreen(nav)
            }
            composable("add") { AddScreen(nav) }
            composable("progress") { ProgressScreen() }
            composable("profile") { ProfileScreen(nav) }
            composable("barcode") { BarcodeScreen(nav) }
            composable("photo") { PhotoScreen(nav) }
            composable("search") { SearchScreen(nav) }
            composable("manual") { ManualScreen(nav) }
            composable("custom") { CustomFoodScreen(nav) }
            composable("myfoods") { MyFoodsScreen(nav) }
            composable("recipes") { RecipesScreen(nav) }
            composable("recipe") { RecipeDetailScreen(nav) }
            composable("allrecipes") { AllRecipesScreen(nav) }
            composable("detail") { FoodDetailScreen(nav) }
        }
    }
}
