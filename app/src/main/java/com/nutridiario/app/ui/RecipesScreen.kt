package com.nutridiario.app.ui

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.nutridiario.app.*
import java.time.LocalDate
import java.time.LocalTime

private val quickIngredients = listOf(
    "huevo", "pollo", "arroz", "pasta", "patata", "tomate", "cebolla", "pimiento", "calabacin", "atun",
    "carne_picada", "salmon", "queso", "jamon_york", "champinones", "garbanzos", "lentejas", "leche",
    "yogur", "platano", "avena", "lechuga", "tortilla_trigo", "pan", "nata", "espinacas", "zanahoria", "gambas",
)

val RecipeGradients = listOf(
    listOf(Color(0xFFF97316), Color(0xFFEF4444)), listOf(Color(0xFF22C55E), Color(0xFF0D9488)),
    listOf(Color(0xFF8B5CF6), Color(0xFF6366F1)), listOf(Color(0xFFEC4899), Color(0xFFF43F5E)),
    listOf(Color(0xFF0EA5E9), Color(0xFF6366F1)), listOf(Color(0xFFF59E0B), Color(0xFFF97316)),
)

fun recipeBrush(r: Recipe) = Brush.linearGradient(RecipeGradients[r.id % RecipeGradients.size])

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RecipesScreen(nav: NavController) {
    val ctx = LocalContext.current
    val data by Repository.data.collectAsStateWithLifecycle()
    val mine = data.pantry.toSet()
    var input by remember { mutableStateOf("") }
    var unknown by remember { mutableStateOf<List<String>>(emptyList()) }

    fun addText(text: String) {
        val (found, unk) = RecipeBook.parseList(text)
        Repository.setPantry(data.pantry + found.map { it.id })
        unknown = unk
        input = ""
    }
    val speech = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { r ->
        if (r.resultCode == Activity.RESULT_OK) r.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()?.let { addText(it) }
    }

    val report = remember(data.profile) { HealthReport(data.profile) }
    val eatenToday = data.entries.filter { it.date == LocalDate.now().toString() }.sumOf { it.nutrients.kcal }
    val kcalLeft = report.targets.kcal - eatenToday
    val meal = Meal.byTime(LocalTime.now().hour)
    val matches = remember(mine) { if (mine.isEmpty()) emptyList() else RecipeBook.suggest(mine) }
    val ready = matches.filter { it.missing.isEmpty() }
    val almost = matches.filter { it.missing.isNotEmpty() }
    val ideas = remember(kcalLeft.toInt() / 50, meal) { RecipeBook.ideas(meal, kcalLeft, report.targets.kcal).take(6) }

    LazyColumn(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text("Recetas", style = MaterialTheme.typography.headlineMedium)
            Text("Dime qué tienes y te propongo un plato para ti (1 ración)", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item {
            AppCard(Modifier.fillMaxWidth()) {
                Text("🧺 Lo que tengo en casa", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        input, { input = it }, Modifier.weight(1f),
                        placeholder = { Text("pollo, arroz, tomate…") }, singleLine = true, shape = MaterialTheme.shapes.large,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { if (input.isNotBlank()) addText(input) }),
                        trailingIcon = {
                            if (input.isNotBlank()) TextButton(onClick = { addText(input) }) { Text("Añadir") }
                        },
                    )
                    Spacer(Modifier.width(8.dp))
                    FilledTonalButton(onClick = {
                        val i = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
                            .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                            .putExtra(RecognizerIntent.EXTRA_LANGUAGE, "es-ES")
                            .putExtra(RecognizerIntent.EXTRA_PROMPT, "¿Qué ingredientes tienes?")
                        try { speech.launch(i) } catch (e: ActivityNotFoundException) {
                            Toast.makeText(ctx, "Tu móvil no tiene reconocimiento de voz", Toast.LENGTH_SHORT).show()
                        }
                    }, modifier = Modifier.height(56.dp)) { Text("🎤", fontSize = 20.sp) }
                }
                if (unknown.isNotEmpty()) {
                    Text("No conozco: ${unknown.joinToString()}", style = Caption, color = MaterialTheme.colorScheme.secondary, modifier = Modifier.padding(top = 4.dp))
                }
                if (mine.isNotEmpty()) {
                    Spacer(Modifier.height(10.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        data.pantry.mapNotNull { RecipeBook.ingredients[it] }.forEach { i ->
                            Row(
                                Modifier.clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer)
                                    .clickable { Repository.setPantry(data.pantry - i.id) }
                                    .padding(start = 10.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text("${i.emoji} ${i.name}", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                Spacer(Modifier.width(4.dp))
                                Icon(Icons.Filled.Close, "Quitar", Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
                            }
                        }
                        TextButton(onClick = { Repository.setPantry(emptyList()) }) { Text("Vaciar") }
                    }
                }
                Spacer(Modifier.height(10.dp))
                Text("Toca para añadir:", style = Caption, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(6.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    quickIngredients.filter { it !in mine }.take(if (mine.isEmpty()) 18 else 8).mapNotNull { RecipeBook.ingredients[it] }.forEach { i ->
                        Text(
                            "${i.emoji} ${i.name.substringBefore(" (")}", style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.clip(CircleShape).background(MaterialTheme.colorScheme.surfaceContainerHigh)
                                .clickable { Repository.setPantry(data.pantry + i.id) }.padding(horizontal = 10.dp, vertical = 6.dp),
                        )
                    }
                }
                Text(
                    "No hace falta añadir aceite, sal, ajo ni especias: doy por hecho que los tienes.",
                    style = Caption, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 10.dp),
                )
            }
        }

        if (mine.isNotEmpty()) {
            item { SectionHeader("✅ Puedes cocinarlo ya", ready.size) }
            if (ready.isEmpty()) item {
                Text("Con eso solo no me sale ningún plato completo. Mira abajo los que casi tienes 👇", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            items(ready, key = { "r" + it.recipe.id }) { m -> RecipeCard(m, kcalLeft) { open(nav, m.recipe) } }
            if (almost.isNotEmpty()) {
                item { SectionHeader("🛒 Te falta poco", almost.size) }
                items(almost.take(12), key = { "a" + it.recipe.id }) { m -> RecipeCard(m, kcalLeft) { open(nav, m.recipe) } }
            }
        }
        item {
            SectionHeader("💡 Ideas para ${meal.label.substringBefore(" ").lowercase()}", null)
            Text(
                if (kcalLeft > 0) "Te quedan ${fmt(kcalLeft, 0)} kcal hoy" else "Hoy ya has llegado a tu objetivo: mejor algo ligero",
                style = Caption, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        items(ideas, key = { "i" + it.recipe.id }) { m -> RecipeCard(RecipeBook.match(m.recipe, mine), kcalLeft, showMissing = mine.isNotEmpty()) { open(nav, m.recipe) } }
        item {
            TextButton(onClick = { nav.navigate("allrecipes") }, modifier = Modifier.fillMaxWidth()) {
                Text("Ver las ${RecipeBook.recipes.size} recetas")
            }
        }
    }
}

private fun open(nav: NavController, r: Recipe) {
    Session.selectedRecipe = r
    nav.navigate("recipe")
}

@Composable
private fun SectionHeader(title: String, count: Int?) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
        Text(title, style = MaterialTheme.typography.titleLarge)
        count?.let {
            Spacer(Modifier.width(8.dp))
            Text(
                "$it", style = Caption, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.clip(CircleShape).background(MaterialTheme.colorScheme.primary).padding(horizontal = 8.dp, vertical = 2.dp),
            )
        }
    }
}

@Composable
fun RecipeCard(m: RecipeMatch, kcalLeft: Double, showMissing: Boolean = true, onClick: () -> Unit) {
    val r = m.recipe
    val n = m.nutrients
    Row(
        Modifier.fillMaxWidth()
            .shadow(5.dp, MaterialTheme.shapes.large, ambientColor = Color(0x22000000), spotColor = Color(0x22000000))
            .clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.surfaceContainerLowest)
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(100.dp).background(recipeBrush(r)), contentAlignment = Alignment.Center) {
            Text(r.emoji, fontSize = 46.sp)
        }
        Column(Modifier.weight(1f).padding(horizontal = 14.dp, vertical = 10.dp)) {
            Text(r.name, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(
                "⏱ ${r.minutes} min · 🔥 ${fmt(n.kcal, 0)} kcal · 💪 ${fmt(n.protein, 0)} g",
                style = Caption, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 3.dp),
            )
            when {
                !showMissing -> if (kcalLeft > 0 && n.kcal <= kcalLeft) Badge2("Encaja en tu día", MaterialTheme.colorScheme.primary)
                m.missing.isEmpty() -> Badge2("✓ Tienes todo", MaterialTheme.colorScheme.primary)
                else -> Text(
                    "Falta: " + m.missing.joinToString { RecipeBook.ing(it.id).name.substringBefore(" (").lowercase() },
                    style = Caption, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.secondary, maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
fun Badge2(text: String, color: Color) {
    Text(
        text, style = Caption, fontWeight = FontWeight.Bold, color = color,
        modifier = Modifier.clip(CircleShape).background(color.copy(alpha = 0.12f)).padding(horizontal = 8.dp, vertical = 2.dp),
    )
}

/** Todas las recetas, con filtro por comida. */
@Composable
fun AllRecipesScreen(nav: NavController) {
    val data by Repository.data.collectAsStateWithLifecycle()
    val mine = data.pantry.toSet()
    var filter by remember { mutableStateOf<String?>(null) }
    val tags = listOf("desayuno" to "☀️ Desayuno", "comida" to "🍽️ Comida", "merienda" to "🍎 Merienda", "cena" to "🌙 Cena")
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        BackBar("Todas las recetas", nav)
        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    tags.forEach { (t, label) -> FilterChip(filter == t, { filter = if (filter == t) null else t }, label = { Text(label, style = Caption) }) }
                }
            }
            items(RecipeBook.recipes.filter { filter == null || filter in it.meals }, key = { it.id }) { r ->
                RecipeCard(RecipeBook.match(r, mine), 0.0, showMissing = mine.isNotEmpty()) { open(nav, r) }
            }
        }
    }
}
