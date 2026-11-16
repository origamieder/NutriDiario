package com.nutridiario.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.nutridiario.app.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val dayFmt = DateTimeFormatter.ofPattern("EEEE d 'de' MMMM", Locale("es", "ES"))

fun dateLabel(d: LocalDate): String = when (d) {
    LocalDate.now() -> "Hoy"
    LocalDate.now().minusDays(1) -> "Ayer"
    else -> d.format(dayFmt).replaceFirstChar { it.uppercase() }
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

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { date = date.minusDays(1) }) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Día anterior") }
                Text(dateLabel(date), style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold)
                IconButton(onClick = { date = date.plusDays(1) }, enabled = date < LocalDate.now()) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Día siguiente")
                }
            }

            Card(Modifier.fillMaxWidth()) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    KcalRing(total.kcal, t.kcal)
                    Spacer(Modifier.width(16.dp))
                    Column {
                        val left = t.kcal - total.kcal
                        Text(if (left >= 0) "Te quedan" else "Te has pasado", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            "${fmt(kotlin.math.abs(left), 0)} kcal",
                            style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold,
                            color = if (left >= 0) MaterialTheme.colorScheme.primary else Over,
                        )
                        Spacer(Modifier.height(8.dp))
                        Text("Objetivo: ${data.profile.goal.label}", style = MaterialTheme.typography.bodySmall)
                        Text("Gasto diario estimado: ${fmt(report.tdee, 0)} kcal", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("Nutrientes", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    NutrientBar("Proteínas", total.protein, t.protein)
                    NutrientBar("Hidratos de carbono", total.carbs, t.carbs)
                    NutrientBar("Grasas", total.fat, t.fat)
                    NutrientBar("Fibra", total.fiber, t.fiber)
                    NutrientBar("Azúcares", total.sugar, t.sugarMax, isMax = true)
                    NutrientBar("Grasas saturadas", total.satFat, t.satFatMax, isMax = true)
                    NutrientBar("Sal", total.salt, t.saltMax, isMax = true)
                }
            }

            Meal.entries.forEach { meal ->
                val list = entries.filter { it.meal == meal }
                val mealKcal = list.sumOf { it.nutrients.kcal }
                Row(Modifier.fillMaxWidth().padding(top = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(meal.label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    Text("${fmt(mealKcal, 0)} kcal", style = MaterialTheme.typography.bodyMedium)
                }
                if (list.isEmpty()) {
                    Text("Nada registrado", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                }
                list.forEach { e ->
                    ListItem(
                        headlineContent = { Text(e.food.name) },
                        supportingContent = {
                            Text("${fmt(e.grams, 0)} g · P ${fmt(e.nutrients.protein)} · HC ${fmt(e.nutrients.carbs)} · G ${fmt(e.nutrients.fat)}")
                        },
                        trailingContent = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("${fmt(e.nutrients.kcal, 0)} kcal", fontWeight = FontWeight.Bold)
                                IconButton(onClick = { Repository.deleteEntry(e.id) }) { Icon(Icons.Filled.Delete, "Borrar") }
                            }
                        },
                        modifier = Modifier.clickableNoRipple {
                            Session.editingEntry = e
                            Session.pendingFood = e.food
                            Session.pendingGrams = e.grams
                            nav.navigate("detail")
                        },
                    )
                }
            }
            Spacer(Modifier.height(80.dp))
        }
        ExtendedFloatingActionButton(
            onClick = { Session.date = date; nav.navigate("add") },
            icon = { Icon(Icons.Filled.Add, null) },
            text = { Text("Añadir comida") },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
        )
    }
}
