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
import androidx.compose.foundation.verticalScroll
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
    val dislikes = data.dislikes.toSet()
    val sort = RecipeSort.of(data.recipeSort)
    val matches = remember(mine, dislikes) { if (mine.isEmpty()) emptyList() else RecipeBook.suggest(mine, dislikes = dislikes) }
    val ready = matches.filter { it.missing.isEmpty() }.sortedBy(sort)
    val almost = matches.filter { it.missing.isNotEmpty() }.sortedBy(sort)
    val ideas = remember(kcalLeft.toInt() / 50, meal, dislikes, sort) {
        RecipeBook.ideas(meal, kcalLeft, report.targets.kcal, dislikes).take(6).map { RecipeBook.match(it.recipe, mine) }.sortedBy(sort)
    }

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

        item { PreferencesCard(data.dislikes) }
        item { SortBar(sort) }
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
        items(ideas, key = { "i" + it.recipe.id }) { m -> RecipeCard(m, kcalLeft, showMissing = mine.isNotEmpty()) { open(nav, m.recipe) } }
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
                "⏱ ${r.minutes} min · 🔥 ${fmt(n.kcal, 0)} kcal",
                style = Caption, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 3.dp),
            )
            Row(Modifier.padding(top = 2.dp, bottom = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MacroDot("P", n.protein, ProteinC); MacroDot("H", n.carbs, CarbsC); MacroDot("G", n.fat, FatC)
            }
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
    val dislikes = data.dislikes.toSet()
    val sort = RecipeSort.of(data.recipeSort)
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
            item { SortBar(sort) }
            val list = RecipeBook.recipes.filter { (filter == null || filter in it.meals) && RecipeBook.allowed(it, dislikes) }
                .map { RecipeBook.match(it, mine) }.sortedBy(sort)
            if (dislikes.isNotEmpty()) item {
                Text("Ocultas ${RecipeBook.recipes.size - RecipeBook.recipes.count { RecipeBook.allowed(it, dislikes) }} recetas con ingredientes que has excluido", style = Caption, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            items(list, key = { it.recipe.id }) { m ->
                RecipeCard(m, 0.0, showMissing = mine.isNotEmpty()) { open(nav, m.recipe) }
            }
        }
    }
}

@Composable
private fun MacroDot(label: String, grams: Double, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(7.dp).clip(CircleShape).background(color)); Spacer(Modifier.width(3.dp))
        Text("$label ${fmt(grams, 0)} g", style = Caption, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** Barra para ordenar las recetas; la elección se guarda. */
@Composable
fun SortBar(sort: RecipeSort) {
    Column {
        Text("Ordenar por", style = Caption, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(4.dp))
        val state = androidx.compose.foundation.lazy.rememberLazyListState(initialFirstVisibleItemIndex = sort.ordinal)
        androidx.compose.foundation.lazy.LazyRow(state = state, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(RecipeSort.entries) { s ->
                FilterChip(sort == s, { Repository.setRecipeSort(s.name) }, label = { Text(s.label) })
            }
        }
    }
}

/** Ingredientes que no quieres: grupos de un toque + ingredientes sueltos. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PreferencesCard(dislikes: List<String>) {
    val set = dislikes.toSet()
    var picking by remember { mutableStateOf(false) }
    fun groupOn(g: DietGroup) = g.ids.all { it in set }
    AppCard(Modifier.fillMaxWidth()) {
        Text("🚫 No quiero en mis recetas", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            DietGroup.entries.forEach { g ->
                FilterChip(groupOn(g), {
                    if (groupOn(g)) {
                        // quitar el grupo, pero sin quitar lo que siga cubierto por otro grupo activo
                        val keep = DietGroup.entries.filter { it != g && groupOn(it) }.flatMap { it.ids }.toSet()
                        Repository.setDislikes(dislikes.filterNot { it in g.ids && it !in keep })
                    } else Repository.setDislikes(dislikes + g.ids)
                }, label = { Text(g.label) })
            }
        }
        val loose = dislikes.filter { id -> DietGroup.entries.none { g -> groupOn(g) && id in g.ids } }.mapNotNull { RecipeBook.ingredients[it] }
        if (loose.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                loose.forEach { i ->
                    Row(
                        Modifier.clip(CircleShape).background(MaterialTheme.colorScheme.errorContainer)
                            .clickable { Repository.setDislikes(dislikes - i.id) }.padding(start = 10.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("${i.emoji} ${i.name}", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onErrorContainer)
                        Spacer(Modifier.width(4.dp))
                        Icon(Icons.Filled.Close, "Quitar", Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onErrorContainer)
                    }
                }
            }
        }
        TextButton(onClick = { picking = true }) { Text("＋ Excluir otro ingrediente") }
    }
    if (picking) {
        AlertDialog(
            onDismissRequest = { picking = false },
            title = { Text("¿Qué no quieres?") },
            text = {
                Column(Modifier.heightIn(max = 420.dp).verticalScrollCompat()) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        RecipeBook.ingredients.values.filterNot { it.pantry }.sortedBy { it.name }.forEach { i ->
                            val on = i.id in set
                            FilterChip(on, {
                                Repository.setDislikes(if (on) dislikes - i.id else dislikes + i.id)
                            }, label = { Text("${i.emoji} ${i.name.substringBefore(" (")}") })
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { picking = false }) { Text("Listo") } },
        )
    }
}

@Composable
private fun Modifier.verticalScrollCompat() = this.verticalScroll(androidx.compose.foundation.rememberScrollState())
