package com.nutridiario.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.nutridiario.app.*
import java.time.LocalTime

fun goToday(nav: NavController) {
    nav.navigate("today") { popUpTo(nav.graph.id) { inclusive = true } }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FoodDetailScreen(nav: NavController) {
    val food = Session.pendingFood
    if (food == null) { LaunchedEffect(Unit) { nav.popBackStack() }; return }
    val editing = Session.editingEntry
    var gramsText by remember { mutableStateOf(fmt(Session.pendingGrams ?: food.defaultAmount(), 0)) }
    var meal by remember { mutableStateOf(editing?.meal ?: Meal.byTime(LocalTime.now().hour)) }
    val grams = gramsText.replace(',', '.').toDoubleOrNull() ?: 0.0
    val n = food.per100.forGrams(grams)
    val targets = HealthReport(Repository.data.value.profile).targets

    Column(Modifier.fillMaxSize()) {
        BackBar(if (editing != null) "Editar" else "Añadir alimento", nav)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (food.imageUrl != null) {
                    AsyncImage(food.imageUrl, null, Modifier.size(72.dp).clip(MaterialTheme.shapes.medium))
                    Spacer(Modifier.width(12.dp))
                }
                Column {
                    Text(food.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    food.brand?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
                    Text("Fuente: ${food.source}" + (food.barcode?.let { " · $it" } ?: ""), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                }
            }
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = gramsText,
                onValueChange = { gramsText = it.filter { c -> c.isDigit() || c == ',' || c == '.' } },
                label = { Text(if (food.packageG != null) "Cantidad (puesta según el envase)" else "Cantidad (aproximada)") },
                suffix = { Text(food.unit) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                food.packageG?.let { pk ->
                    AssistChip(onClick = { gramsText = fmt(pk, 0) }, label = { Text("Envase entero (${fmt(pk, 0)} ${food.unit})") })
                    AssistChip(onClick = { gramsText = fmt(pk / 2, 0) }, label = { Text("½ envase") })
                }
                food.portionG?.let { pg ->
                    AssistChip(onClick = { gramsText = fmt(pg, 0) }, label = { Text("${food.portionName ?: "1 ración"} (${fmt(pg, 0)} ${food.unit})") })
                    AssistChip(onClick = { gramsText = fmt(pg * 2, 0) }, label = { Text("x2") })
                    AssistChip(onClick = { gramsText = fmt(pg / 2, 0) }, label = { Text("½") })
                }
                listOf(50, 100, 150, 200, 250, 330).forEach { g ->
                    AssistChip(onClick = { gramsText = g.toString() }, label = { Text("$g g") })
                }
            }
            SectionTitle("Comida")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Meal.entries.forEach { m ->
                    FilterChip(selected = meal == m, onClick = { meal = m }, label = { Text(m.label) })
                }
            }
            Spacer(Modifier.height(16.dp))
            Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("${fmt(n.kcal, 0)} kcal", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                        Text("${fmt(n.kcal / targets.kcal * 100, 0)} % de tu objetivo diario", style = MaterialTheme.typography.bodySmall)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Proteínas ${fmt(n.protein)} g")
                        Text("Hidratos ${fmt(n.carbs)} g")
                        Text("Grasas ${fmt(n.fat)} g")
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            NutrientTable(n, "Valores para ${fmt(grams, 0)} ${food.unit}")
            Spacer(Modifier.height(12.dp))
            NutrientTable(food.per100, "Valores por 100 ${food.unit}")
            Spacer(Modifier.height(16.dp))
        }
        Button(
            onClick = {
                if (editing != null) Repository.updateEntry(editing.id, meal, grams)
                else Repository.addEntry(Session.date.toString(), meal, grams, food)
                Session.editingEntry = null
                Session.pendingFood = null
                goToday(nav)
            },
            enabled = grams > 0,
            modifier = Modifier.fillMaxWidth().padding(16.dp).height(52.dp),
        ) { Text(if (editing != null) "Guardar cambios" else "Añadir a ${Meal.entries.first { it == meal }.label.lowercase()}") }
    }
}
