package com.nutridiario.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.nutridiario.app.*
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private val dayFmt = DateTimeFormatter.ofPattern("EEEE d 'de' MMMM", Locale("es", "ES"))

fun dateLabel(d: LocalDate): String = when (d) {
    LocalDate.now() -> "Hoy"
    LocalDate.now().minusDays(1) -> "Ayer"
    else -> d.format(dayFmt).replaceFirstChar { it.uppercase() }
}

private fun greeting(): String = when (LocalTime.now().hour) {
    in 6..13 -> "Buenos días"
    in 14..20 -> "Buenas tardes"
    else -> "Buenas noches"
}

@Composable
fun TodayScreen(nav: NavController) {
    val data by Repository.data.collectAsStateWithLifecycle()
    var date by remember { mutableStateOf(Session.date) }
    LaunchedEffect(date) { Session.date = date }
    val report = remember(data.profile) { HealthReport(data.profile) }
    val t = report.targets
    val entries = data.entries.filter { it.date == date.toString() }
    val total = entries.fold(Nutrients()) { acc, e -> acc + e.nutrients }

    fun addTo(meal: Meal?) {
        Session.date = date; Session.preferredMeal = meal; nav.navigate("add")
    }

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
            // Cabecera
            Row(Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        greeting() + (data.profile.name.takeIf { it.isNotBlank() }?.let { ", $it" } ?: "") + " 👋",
                        style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(dateLabel(date), style = MaterialTheme.typography.headlineMedium)
                }
                Row(
                    Modifier.clip(CircleShape).background(MaterialTheme.colorScheme.surfaceContainerLowest),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = { date = date.minusDays(1) }) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Día anterior") }
                    IconButton(onClick = { date = date.plusDays(1) }, enabled = date < LocalDate.now()) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Día siguiente")
                    }
                }
            }

            // Tarjeta principal con degradado
            GradientCard(modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    KcalRing(total.kcal, t.kcal, 148.dp)
                    Spacer(Modifier.width(18.dp))
                    Column {
                        val left = t.kcal - total.kcal
                        Text(if (left >= 0) "Te quedan" else "Te has pasado", color = Color.White.copy(alpha = 0.9f))
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(fmt(kotlin.math.abs(left), 0), color = Color.White, fontSize = 36.sp, fontWeight = FontWeight.ExtraBold)
                            Text(" kcal", color = Color.White.copy(alpha = 0.9f), modifier = Modifier.padding(bottom = 6.dp))
                        }
                        Spacer(Modifier.height(10.dp))
                        HeroStat("🎯", "Objetivo", "${fmt(t.kcal, 0)} kcal")
                        HeroStat("🔥", "Gasto diario", "${fmt(report.tdee, 0)} kcal")
                    }
                }
            }

            // Macros
            Spacer(Modifier.height(16.dp))
            AppCard(Modifier.fillMaxWidth()) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    MacroRing("Proteínas", total.protein, t.protein, ProteinC)
                    MacroRing("Hidratos", total.carbs, t.carbs, CarbsC)
                    MacroRing("Grasas", total.fat, t.fat, FatC)
                }
                Spacer(Modifier.height(12.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Column(Modifier.weight(1f)) {
                        NutrientBar("Fibra", total.fiber, t.fiber, color = FiberC)
                        NutrientBar("Saturadas", total.satFat, t.satFatMax, isMax = true, color = FatC)
                    }
                    Column(Modifier.weight(1f)) {
                        NutrientBar("Azúcares", total.sugar, t.sugarMax, isMax = true, color = SugarC)
                        NutrientBar("Sal", total.salt, t.saltMax, isMax = true, color = SaltC)
                    }
                }
            }

            // Comidas
            Meal.entries.forEach { meal ->
                Spacer(Modifier.height(16.dp))
                MealCard(
                    meal, entries.filter { it.meal == meal },
                    onAdd = { addTo(meal) },
                    onEdit = { e ->
                        Session.editingEntry = e; Session.pendingFood = e.food; Session.pendingGrams = e.grams
                        nav.navigate("detail")
                    },
                )
            }
            Spacer(Modifier.height(96.dp))
        }
        ExtendedFloatingActionButton(
            onClick = { addTo(null) },
            icon = { Icon(Icons.Filled.Add, null) },
            text = { Text("Añadir", fontWeight = FontWeight.Bold) },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
        )
    }
}

@Composable
private fun HeroStat(emoji: String, label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
        Text(emoji, fontSize = 14.sp)
        Spacer(Modifier.width(6.dp))
        Text("$label ", color = Color.White.copy(alpha = 0.85f), style = Caption)
        Text(value, color = Color.White, fontWeight = FontWeight.Bold, style = Caption)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MealCard(meal: Meal, list: List<FoodEntry>, onAdd: () -> Unit, onEdit: (FoodEntry) -> Unit) {
    val kcal = list.sumOf { it.nutrients.kcal }
    AppCard(Modifier.fillMaxWidth(), padding = 0.dp) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            EmojiBadge(meal.emoji, meal.tint, 44.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(meal.label, style = MaterialTheme.typography.titleMedium)
                Text(
                    if (list.isEmpty()) "Sin registrar" else "${fmt(kcal, 0)} kcal · ${list.size} alimento${if (list.size > 1) "s" else ""}",
                    style = Caption, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            FilledTonalIconButton(onClick = onAdd) { Icon(Icons.Filled.Add, "Añadir a ${meal.label}") }
        }
        list.forEach { e ->
            key(e.id) {
                val state = rememberSwipeToDismissBoxState(confirmValueChange = {
                    if (it == SwipeToDismissBoxValue.EndToStart) { Repository.deleteEntry(e.id); true } else false
                })
                SwipeToDismissBox(
                    state = state,
                    enableDismissFromStartToEnd = false,
                    backgroundContent = {
                        Box(Modifier.fillMaxSize().background(Over).padding(horizontal = 24.dp), contentAlignment = Alignment.CenterEnd) {
                            Icon(Icons.Filled.Delete, "Borrar", tint = Color.White)
                        }
                    },
                ) {
                    Row(
                        Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceContainerLowest)
                            .clickable { onEdit(e) }.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        FoodAvatar(e.food, 40.dp)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(e.food.name, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(
                                "${fmt(e.grams, 0)} ${e.food.unit} · P ${fmt(e.nutrients.protein, 0)} · H ${fmt(e.nutrients.carbs, 0)} · G ${fmt(e.nutrients.fat, 0)}",
                                style = Caption, color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Text("${fmt(e.nutrients.kcal, 0)}", fontWeight = FontWeight.Bold)
                        Text(" kcal", style = Caption, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
        if (list.isNotEmpty()) Spacer(Modifier.height(8.dp))
    }
}
