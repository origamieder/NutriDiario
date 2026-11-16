package com.nutridiario.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nutridiario.app.*
import java.time.LocalDate

@Composable
fun ProgressScreen() {
    val data by Repository.data.collectAsStateWithLifecycle()
    val report = remember(data.profile) { HealthReport(data.profile) }
    val target = report.targets.kcal
    var newWeight by remember { mutableStateOf("") }
    val today = LocalDate.now()
    val days = (13 downTo 0).map { today.minusDays(it.toLong()) }
    val byDay = days.map { d -> d to data.entries.filter { it.date == d.toString() }.fold(Nutrients()) { a, e -> a + e.nutrients } }
    val logged = byDay.takeLast(7).filter { it.second.kcal > 0 }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        Text("Progreso", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)

        SectionTitle("Calorías últimos 14 días")
        val primary = MaterialTheme.colorScheme.primary
        val outline = MaterialTheme.colorScheme.outline
        Canvas(Modifier.fillMaxWidth().height(160.dp)) {
            val maxV = maxOf(target * 1.4, byDay.maxOf { it.second.kcal })
            val bw = size.width / byDay.size
            byDay.forEachIndexed { i, (_, n) ->
                val h = (n.kcal / maxV * size.height).toFloat()
                val c = if (n.kcal > target * 1.1) Over else if (n.kcal > 0) primary else outline
                drawRect(c, Offset(i * bw + bw * 0.15f, size.height - h), Size(bw * 0.7f, h))
            }
            val ty = (size.height - target / maxV * size.height).toFloat()
            drawLine(outline, Offset(0f, ty), Offset(size.width, ty), 2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f)))
        }
        Text("Línea discontinua: tu objetivo (${fmt(target, 0)} kcal)", style = MaterialTheme.typography.bodySmall)

        if (logged.isNotEmpty()) {
            val avg = logged.fold(Nutrients()) { a, p -> a + p.second } * (1.0 / logged.size)
            Card(Modifier.fillMaxWidth().padding(top = 12.dp)) {
                Column(Modifier.padding(16.dp)) {
                    Text("Media diaria (últimos 7 días con registros: ${logged.size})", fontWeight = FontWeight.Bold)
                    Row2("Calorías", "${fmt(avg.kcal, 0)} / ${fmt(target, 0)} kcal")
                    Row2("Proteínas", "${fmt(avg.protein, 0)} / ${fmt(report.targets.protein, 0)} g")
                    Row2("Hidratos", "${fmt(avg.carbs, 0)} / ${fmt(report.targets.carbs, 0)} g")
                    Row2("Grasas", "${fmt(avg.fat, 0)} / ${fmt(report.targets.fat, 0)} g")
                    Row2("Fibra", "${fmt(avg.fiber, 0)} / ${fmt(report.targets.fiber, 0)} g")
                    Row2("Azúcares", "${fmt(avg.sugar, 0)} / máx. ${fmt(report.targets.sugarMax, 0)} g")
                    Row2("Sal", "${fmt(avg.salt)} / máx. 5 g")
                    val balance = (avg.kcal - report.tdee) * 7 / 7700
                    Text(
                        "Con esta media, tu peso cambiaría unos ${fmtSigned(balance)} kg por semana.",
                        style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }
        }

        SectionTitle("Evolución del peso")
        val weights = data.weights
        if (weights.size >= 2) WeightChart(weights, report.healthyMin, report.healthyMax, report.idealWeight)
        else Text("Registra tu peso varios días para ver la gráfica.", style = MaterialTheme.typography.bodySmall)

        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
            OutlinedTextField(
                newWeight, { newWeight = it.filter { c -> c.isDigit() || c == ',' || c == '.' } },
                label = { Text("Peso de hoy") }, suffix = { Text("kg") }, singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(8.dp))
            Button(onClick = {
                newWeight.replace(',', '.').toDoubleOrNull()?.let { Repository.addWeight(today.toString(), it) }
                newWeight = ""
            }, enabled = (newWeight.replace(',', '.').toDoubleOrNull() ?: 0.0) in 30.0..300.0) { Text("Guardar") }
        }
        weights.reversed().take(30).forEach { w ->
            ListItem(
                headlineContent = { Text("${fmt(w.kg)} kg") },
                supportingContent = { Text(dateLabel(LocalDate.parse(w.date))) },
                trailingContent = { IconButton(onClick = { Repository.deleteWeight(w.date) }) { Icon(Icons.Filled.Delete, "Borrar") } },
            )
        }
    }
}

@Composable
private fun WeightChart(weights: List<WeightEntry>, hMin: Double, hMax: Double, ideal: Double) {
    val primary = MaterialTheme.colorScheme.primary
    val band = Good.copy(alpha = 0.15f)
    val idealC = Good
    Canvas(Modifier.fillMaxWidth().height(180.dp)) {
        val values = weights.map { it.kg }
        val lo = minOf(values.min(), ideal) - 2
        val hi = maxOf(values.max(), ideal) + 2
        fun y(v: Double) = (size.height - (v - lo) / (hi - lo) * size.height).toFloat()
        val top = y(hMax.coerceAtMost(hi)); val bottom = y(hMin.coerceAtLeast(lo))
        if (bottom > top) drawRect(band, Offset(0f, top), Size(size.width, bottom - top))
        drawLine(idealC, Offset(0f, y(ideal)), Offset(size.width, y(ideal)), 2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f)))
        val step = size.width / (values.size - 1)
        val path = Path()
        values.forEachIndexed { i, v -> if (i == 0) path.moveTo(0f, y(v)) else path.lineTo(i * step, y(v)) }
        drawPath(path, primary, style = Stroke(3.dp.toPx()))
        values.forEachIndexed { i, v -> drawCircle(primary, 4.dp.toPx(), Offset(i * step, y(v))) }
    }
    Text("Zona verde: rango saludable · Línea verde: peso ideal (${fmt(ideal)} kg)", style = MaterialTheme.typography.bodySmall)
}
