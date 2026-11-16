package com.nutridiario.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.nutridiario.app.*
import java.time.LocalTime

fun goToday(nav: NavController) {
    nav.navigate("today") { popUpTo(nav.graph.id) { inclusive = true } }
}

@Composable
fun FoodDetailScreen(nav: NavController) {
    val food = Session.pendingFood
    if (food == null) { LaunchedEffect(Unit) { nav.popBackStack() }; return }
    val editing = Session.editingEntry
    val lastAmount = remember { if (editing == null) Repository.lastAmountFor(food) else null }
    var gramsText by remember {
        mutableStateOf(fmt(Session.pendingGrams ?: food.defaultG ?: lastAmount ?: food.defaultAmount(), 0))
    }
    var meal by remember { mutableStateOf(editing?.meal ?: Session.preferredMeal ?: Meal.byTime(LocalTime.now().hour)) }
    var showPer100 by remember { mutableStateOf(false) }
    val grams = gramsText.replace(',', '.').toDoubleOrNull() ?: 0.0
    val n = food.per100.forGrams(grams)
    val targets = HealthReport(Repository.data.value.profile).targets
    val hint = when {
        Session.pendingGrams == null && food.defaultG != null -> "Tu cantidad por defecto"
        Session.pendingGrams == null && lastAmount != null -> "La misma que la última vez"
        food.packageG != null && food.defaultAmount() == food.packageG -> "Envase entero"
        else -> "Cantidad aproximada"
    }
    val step = if (grams >= 200) 25.0 else 10.0

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        BackBar(if (editing != null) "Editar" else "Añadir alimento", nav)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
            // Cabecera
            Row(verticalAlignment = Alignment.CenterVertically) {
                FoodAvatar(food, 76.dp)
                Spacer(Modifier.width(14.dp))
                Column {
                    Text(food.name, style = MaterialTheme.typography.headlineSmall)
                    food.brand?.let { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        food.source + (food.barcode?.let { " · $it" } ?: ""),
                        style = Caption, color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer).padding(horizontal = 10.dp, vertical = 3.dp),
                    )
                }
            }

            // Calorías y macros
            Spacer(Modifier.height(16.dp))
            AppCard(Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(fmt(n.kcal, 0), fontSize = 40.sp, fontWeight = FontWeight.ExtraBold)
                    Text(" kcal", modifier = Modifier.padding(bottom = 8.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.weight(1f))
                    Text(
                        "${fmt(n.kcal / targets.kcal * 100, 0)} % de tu día",
                        style = Caption, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.padding(bottom = 10.dp).clip(CircleShape)
                            .background(MaterialTheme.colorScheme.secondaryContainer).padding(horizontal = 10.dp, vertical = 4.dp),
                    )
                }
                Spacer(Modifier.height(12.dp))
                MacroSplitBar(n)
            }

            // Cantidad
            Spacer(Modifier.height(16.dp))
            AppCard(Modifier.fillMaxWidth()) {
                Text("Cantidad", style = MaterialTheme.typography.titleMedium)
                Text(hint, style = Caption, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StepButton("−") { gramsText = fmt((grams - step).coerceAtLeast(0.0), 0) }
                    Row(
                        Modifier.weight(1f).padding(horizontal = 12.dp).clip(MaterialTheme.shapes.medium)
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh).padding(vertical = 10.dp),
                        horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.Bottom,
                    ) {
                        BasicTextField(
                            value = gramsText,
                            onValueChange = { gramsText = it.filter { c -> c.isDigit() || c == ',' || c == '.' }.take(6) },
                            textStyle = TextStyle(fontSize = 30.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.End, color = MaterialTheme.colorScheme.onSurface),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                            modifier = Modifier.width((gramsText.length.coerceAtLeast(1) * 19 + 6).dp),
                        )
                        Text(" ${food.unit}", fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 4.dp))
                    }
                    StepButton("+") { gramsText = fmt(grams + step, 0) }
                }
                Spacer(Modifier.height(12.dp))
                val quick = buildList {
                    food.packageG?.let { add("Envase (${fmt(it, 0)} ${food.unit})" to it); add("½ envase" to it / 2) }
                    food.portionG?.let { pg -> if (pg != food.packageG) add("${food.portionName ?: "Ración"} (${fmt(pg, 0)} ${food.unit})" to pg) }
                    listOf(50.0, 100.0, 150.0, 200.0, 250.0).forEach { add("${fmt(it, 0)} ${food.unit}" to it) }
                }
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(quick.size) { i ->
                        val (label, v) = quick[i]
                        val selected = kotlin.math.abs(v - grams) < 0.5
                        FilterChip(selected, { gramsText = fmt(v, 0) }, label = { Text(label) })
                    }
                }
            }

            // Comida
            Spacer(Modifier.height(16.dp))
            Text("¿En qué comida?", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Meal.entries.forEach { m ->
                    val sel = m == meal
                    Column(
                        Modifier.weight(1f).clip(MaterialTheme.shapes.medium)
                            .background(if (sel) m.tint else MaterialTheme.colorScheme.surfaceContainerLowest)
                            .border(2.dp, if (sel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.medium)
                            .clickable { meal = m }.padding(vertical = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(m.emoji, fontSize = 22.sp)
                        Text(m.label.substringBefore(" "), style = Caption, fontWeight = if (sel) FontWeight.Bold else null)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            NutrientTable(if (showPer100) food.per100 else n, if (showPer100) "Por 100 ${food.unit}" else "Para ${fmt(grams, 0)} ${food.unit}")
            TextButton(onClick = { showPer100 = !showPer100 }) {
                Text(if (showPer100) "Ver valores para mi cantidad" else "Ver valores por 100 ${food.unit}")
            }
            val mine = food.source == "Mis alimentos"
            OutlinedButton(
                onClick = {
                    Session.editorFood = food; Session.editorIsMine = mine; Session.manualText = ""
                    nav.navigate("custom")
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text(if (mine) "✏️ Editar en Mis alimentos" else "✏️ Guardar en Mis alimentos / corregir datos") }
            Spacer(Modifier.height(16.dp))
        }
        Button(
            onClick = {
                if (editing != null) Repository.updateEntry(editing.id, meal, grams)
                else Repository.addEntry(Session.date.toString(), meal, grams, food)
                Session.editingEntry = null
                Session.pendingFood = null
                Session.preferredMeal = null
                goToday(nav)
            },
            enabled = grams > 0,
            shape = MaterialTheme.shapes.large,
            modifier = Modifier.fillMaxWidth().padding(16.dp).height(58.dp),
        ) {
            Icon(Icons.Filled.Add, null)
            Spacer(Modifier.width(8.dp))
            Text(
                (if (editing != null) "Guardar" else "Añadir a ${meal.label.substringBefore(" ").lowercase()}") + " · ${fmt(n.kcal, 0)} kcal",
                fontWeight = FontWeight.Bold, fontSize = 16.sp,
            )
        }
    }
}

@Composable
private fun StepButton(label: String, onClick: () -> Unit) {
    Box(
        Modifier.size(52.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Text(label, fontSize = 26.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer) }
}
