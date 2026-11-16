package com.nutridiario.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nutridiario.app.*
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun ProgressScreen() {
    val data by Repository.data.collectAsStateWithLifecycle()
    val report = remember(data.profile) { HealthReport(data.profile) }
    val target = report.targets.kcal
    var newWeight by remember { mutableStateOf("") }
    val today = LocalDate.now()
    val days = (6 downTo 0).map { today.minusDays(it.toLong()) }
    val byDay = days.map { d -> d to data.entries.filter { it.date == d.toString() }.fold(Nutrients()) { a, e -> a + e.nutrients } }
    val logged = byDay.filter { it.second.kcal > 0 }

    Column(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).verticalScroll(rememberScrollState()).padding(16.dp)
    ) {
        Text("Progreso", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(bottom = 12.dp))

        // Resumen semanal
        if (logged.isNotEmpty()) {
            val avg = logged.fold(Nutrients()) { a, p -> a + p.second } * (1.0 / logged.size)
            val balance = (avg.kcal - report.tdee) * 7 / 7700
            GradientCard(modifier = Modifier.fillMaxWidth()) {
                Text("Media de los últimos 7 días", color = Color.White.copy(alpha = 0.9f), style = Caption)
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(fmt(avg.kcal, 0), color = Color.White, fontSize = 36.sp, fontWeight = FontWeight.ExtraBold)
                    Text(" kcal/día", color = Color.White.copy(alpha = 0.9f), modifier = Modifier.padding(bottom = 6.dp))
                }
                Text(
                    (if (balance < 0) "📉 A este ritmo bajarías " else "📈 A este ritmo subirías ") + "${fmt(kotlin.math.abs(balance))} kg por semana",
                    color = Color.White, fontWeight = FontWeight.SemiBold,
                )
                Text("${logged.size} de 7 días registrados", color = Color.White.copy(alpha = 0.85f), style = Caption)
            }
            Spacer(Modifier.height(16.dp))
        }

        // Calorías por día
        AppCard(Modifier.fillMaxWidth()) {
            Text("Calorías por día", style = MaterialTheme.typography.titleMedium)
            Text("Objetivo: ${fmt(target, 0)} kcal", style = Caption, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(12.dp))
            val outline = MaterialTheme.colorScheme.outline
            val empty = MaterialTheme.colorScheme.surfaceContainerHighest
            Canvas(Modifier.fillMaxWidth().height(170.dp)) {
                val maxV = maxOf(target * 1.3, byDay.maxOf { it.second.kcal })
                val bw = size.width / byDay.size
                byDay.forEachIndexed { i, (_, n) ->
                    val h = (n.kcal / maxV * size.height).toFloat().coerceAtLeast(6.dp.toPx())
                    val brush = when {
                        n.kcal <= 0 -> Brush.verticalGradient(listOf(empty, empty))
                        n.kcal > target * 1.1 -> Brush.verticalGradient(listOf(Color(0xFFF87171), Over))
                        else -> Brush.verticalGradient(listOf(GreenLight, Teal))
                    }
                    drawRoundRect(brush, Offset(i * bw + bw * 0.18f, size.height - h), Size(bw * 0.64f, h), CornerRadius(10.dp.toPx()))
                }
                val ty = (size.height - target / maxV * size.height).toFloat()
                drawLine(outline, Offset(0f, ty), Offset(size.width, ty), 2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 10f)))
            }
            Row(Modifier.fillMaxWidth()) {
                byDay.forEach { (d, n) ->
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            if (d == today) "Hoy" else d.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale("es", "ES")).replaceFirstChar { it.uppercase() },
                            style = Caption, fontWeight = if (d == today) FontWeight.Bold else null,
                        )
                        Text(if (n.kcal > 0) fmt(n.kcal, 0) else "–", style = Caption, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                    }
                }
            }
        }

        if (logged.isNotEmpty()) {
            val avg = logged.fold(Nutrients()) { a, p -> a + p.second } * (1.0 / logged.size)
            Spacer(Modifier.height(16.dp))
            AppCard(Modifier.fillMaxWidth()) {
                Text("Media diaria frente a tu objetivo", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(4.dp))
                NutrientBar("Proteínas", avg.protein, report.targets.protein, color = ProteinC)
                NutrientBar("Hidratos", avg.carbs, report.targets.carbs, color = CarbsC)
                NutrientBar("Grasas", avg.fat, report.targets.fat, color = FatC)
                NutrientBar("Fibra", avg.fiber, report.targets.fiber, color = FiberC)
                NutrientBar("Azúcares", avg.sugar, report.targets.sugarMax, isMax = true, color = SugarC)
                NutrientBar("Sal", avg.salt, 5.0, isMax = true, color = SaltC)
            }
        }

        // Peso
        Spacer(Modifier.height(16.dp))
        AppCard(Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Mi peso", style = MaterialTheme.typography.titleMedium)
                    Text("Ideal: ${fmt(report.idealWeight)} kg", style = Caption, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text("${fmt(data.profile.weightKg)} kg", fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
            }
            Spacer(Modifier.height(12.dp))
            val weights = data.weights
            if (weights.size >= 2) WeightChart(weights, report.healthyMin, report.healthyMax, report.idealWeight)
            else Text("📊 Apunta tu peso algunos días para ver la evolución.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    newWeight, { newWeight = it.filter { c -> c.isDigit() || c == ',' || c == '.' } },
                    label = { Text("Peso de hoy") }, suffix = { Text("kg") }, singleLine = true, shape = MaterialTheme.shapes.medium,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(8.dp))
                Button(onClick = {
                    newWeight.replace(',', '.').toDoubleOrNull()?.let { Repository.addWeight(today.toString(), it) }
                    newWeight = ""
                }, enabled = (newWeight.replace(',', '.').toDoubleOrNull() ?: 0.0) in 30.0..300.0, modifier = Modifier.height(56.dp)) { Text("Guardar") }
            }
            weights.reversed().take(6).forEachIndexed { i, w ->
                val prev = weights.reversed().getOrNull(i + 1)
                Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(dateLabel(LocalDate.parse(w.date)), Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                    prev?.let {
                        val d = w.kg - it.kg
                        Text(
                            fmtSigned(d), style = Caption, fontWeight = FontWeight.Bold,
                            color = if (d <= 0) Good else Over, modifier = Modifier.padding(end = 8.dp),
                        )
                    }
                    Text("${fmt(w.kg)} kg", fontWeight = FontWeight.Bold)
                    IconButton(onClick = { Repository.deleteWeight(w.date) }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Filled.Close, "Borrar", tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun WeightChart(weights: List<WeightEntry>, hMin: Double, hMax: Double, ideal: Double) {
    val primary = MaterialTheme.colorScheme.primary
    val band = Good.copy(alpha = 0.12f)
    Canvas(Modifier.fillMaxWidth().height(170.dp)) {
        val values = weights.takeLast(30).map { it.kg }
        val lo = minOf(values.min(), ideal) - 2
        val hi = maxOf(values.max(), ideal) + 2
        fun y(v: Double) = (size.height - (v - lo) / (hi - lo) * size.height).toFloat()
        val top = y(hMax.coerceAtMost(hi)); val bottom = y(hMin.coerceAtLeast(lo))
        if (bottom > top) drawRoundRect(band, Offset(0f, top), Size(size.width, bottom - top), CornerRadius(12.dp.toPx()))
        drawLine(Good, Offset(0f, y(ideal)), Offset(size.width, y(ideal)), 2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 10f)))
        val step = size.width / (values.size - 1)
        val line = Path(); val fill = Path()
        values.forEachIndexed { i, v ->
            val px = i * step; val py = y(v)
            if (i == 0) { line.moveTo(px, py); fill.moveTo(px, size.height); fill.lineTo(px, py) } else { line.lineTo(px, py); fill.lineTo(px, py) }
        }
        fill.lineTo(size.width, size.height); fill.close()
        drawPath(fill, Brush.verticalGradient(listOf(primary.copy(alpha = 0.3f), Color.Transparent)))
        drawPath(line, primary, style = Stroke(3.dp.toPx()))
        values.forEachIndexed { i, v ->
            drawCircle(Color.White, 6.dp.toPx(), Offset(i * step, y(v)))
            drawCircle(primary, 4.dp.toPx(), Offset(i * step, y(v)))
        }
    }
    Text("Zona verde: rango saludable · Línea discontinua: peso ideal", style = Caption, color = MaterialTheme.colorScheme.onSurfaceVariant)
}
