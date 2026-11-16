package com.nutridiario.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.nutridiario.app.*

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProfileScreen(nav: NavController) {
    val data by Repository.data.collectAsStateWithLifecycle()
    val saved = data.profile
    var name by remember { mutableStateOf(saved.name) }
    var sex by remember { mutableStateOf(saved.sex) }
    var age by remember { mutableStateOf(saved.age.toString()) }
    var height by remember { mutableStateOf(fmt(saved.heightCm, 0)) }
    var weight by remember { mutableStateOf(fmt(saved.weightKg)) }
    var activity by remember { mutableStateOf(saved.activity) }
    var goal by remember { mutableStateOf(saved.goal) }

    fun num(s: String) = s.replace(',', '.').toDoubleOrNull()
    val valid = (num(age) ?: 0.0) in 14.0..110.0 && (num(height) ?: 0.0) in 120.0..230.0 && (num(weight) ?: 0.0) in 30.0..300.0
    val draft = if (valid) Profile(name, sex, num(age)!!.toInt(), num(height)!!, num(weight)!!, activity, goal, true) else null
    val report = draft?.let { HealthReport(it) }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        Text("Mi perfil", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        if (!saved.configured) {
            Text("¡Bienvenido/a! Rellena tus datos para calcular tus necesidades y tu peso ideal.", modifier = Modifier.padding(vertical = 8.dp))
        }
        OutlinedTextField(name, { name = it }, label = { Text("Nombre (opcional)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        Row {
            Sex.entries.forEach { s ->
                FilterChip(sex == s, { sex = s }, label = { Text(s.label) }, modifier = Modifier.padding(end = 8.dp))
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NumField("Edad", age, { age = it }, "años", Modifier.weight(1f))
            NumField("Altura", height, { height = it }, "cm", Modifier.weight(1f))
            NumField("Peso", weight, { weight = it }, "kg", Modifier.weight(1f))
        }
        SectionTitle("Actividad física")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Activity.entries.forEach { a -> FilterChip(activity == a, { activity = a }, label = { Text(a.label) }) }
        }
        Text(activity.desc, style = MaterialTheme.typography.bodySmall)
        SectionTitle("Objetivo")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Goal.entries.forEach { g -> FilterChip(goal == g, { goal = g }, label = { Text(g.label) }) }
        }
        Button(
            onClick = {
                val first = !saved.configured
                Repository.saveProfile(draft!!)
                if (first) goToday(nav)
            },
            enabled = valid,
            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp).height(52.dp),
        ) { Text("Guardar perfil") }

        if (report != null) ReportView(report)
    }
}

@Composable
private fun NumField(label: String, value: String, onChange: (String) -> Unit, suffix: String, modifier: Modifier) {
    OutlinedTextField(
        value, { onChange(it.filter { c -> c.isDigit() || c == ',' || c == '.' }) },
        label = { Text(label) }, suffix = { Text(suffix) }, singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = modifier,
    )
}

@Composable
fun ReportView(r: HealthReport) {
    val p = r.p
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text("Tu peso frente al ideal", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Row {
                Big("IMC", fmt(r.bmi), r.bmiCategory, Modifier.weight(1f))
                Big("Peso ideal", "${fmt(r.idealWeight)} kg", "actual ${fmt(p.weightKg)} kg", Modifier.weight(1f))
                Big("Diferencia", "${fmtSigned(r.diffToIdeal)} kg", if (r.diffToIdeal > 0) "por encima" else "por debajo", Modifier.weight(1f))
            }
            Spacer(Modifier.height(12.dp))
            BmiGauge(r.bmi)
            Spacer(Modifier.height(8.dp))
            Text("Rango de peso saludable para ${fmt(p.heightCm, 0)} cm: ${fmt(r.healthyMin)} – ${fmt(r.healthyMax)} kg (IMC 18,5–24,9)")
            Spacer(Modifier.height(8.dp))
            Text(r.advice, fontWeight = FontWeight.Medium)
            r.weeksToIdeal?.let { Text("Al ritmo de tu objetivo llegarías en unas $it semanas.", style = MaterialTheme.typography.bodySmall) }
        }
    }
    Spacer(Modifier.height(12.dp))
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text("Peso ideal según distintas fórmulas", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            r.idealFormulas.forEach { Row2(it.formula, "${fmt(it.kg)} kg") }
            HorizontalDivider()
            Row2("Grasa corporal estimada", "${fmt(r.bodyFatPct)} % (${r.bodyFatCategory})")
            Text("Estimación a partir del IMC (Deurenberg). Para un dato real usa una báscula de bioimpedancia o plicómetro.", style = MaterialTheme.typography.bodySmall)
        }
    }
    Spacer(Modifier.height(12.dp))
    val t = r.targets
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text("Tus necesidades diarias", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Row2("Metabolismo basal (Mifflin-St Jeor)", "${fmt(r.bmr, 0)} kcal")
            Row2("Gasto total con tu actividad", "${fmt(r.tdee, 0)} kcal")
            Row2("Objetivo calórico", "${fmt(t.kcal, 0)} kcal", bold = true)
            HorizontalDivider()
            Row2("Proteínas", "${fmt(t.protein, 0)} g")
            Row2("Hidratos de carbono", "${fmt(t.carbs, 0)} g")
            Row2("Grasas", "${fmt(t.fat, 0)} g")
            Row2("Fibra (mínimo)", "${fmt(t.fiber, 0)} g")
            Row2("Azúcares (máximo)", "${fmt(t.sugarMax, 0)} g")
            Row2("Grasas saturadas (máximo)", "${fmt(t.satFatMax, 0)} g")
            Row2("Sal (máximo, OMS)", "${fmt(t.saltMax, 0)} g")
            Row2("Agua", "${fmt(t.waterMl / 1000)} L")
        }
    }
    Spacer(Modifier.height(8.dp))
    Text(
        "Valores orientativos basados en fórmulas estándar; no sustituyen el consejo de un profesional sanitario.",
        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline,
    )
    Spacer(Modifier.height(24.dp))
}

@Composable
private fun Big(label: String, value: String, sub: String, modifier: Modifier) {
    Column(modifier) {
        Text(label, style = MaterialTheme.typography.labelMedium)
        Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(sub, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun BmiGauge(bmi: Double) {
    val segments = listOf(15.0 to 18.5 to Color(0xFF64B5F6), 18.5 to 25.0 to Good, 25.0 to 30.0 to Warn, 30.0 to 40.0 to Over)
    val marker = MaterialTheme.colorScheme.onSurface
    Column {
        Canvas(Modifier.fillMaxWidth().height(22.dp)) {
            val min = 15.0; val max = 40.0
            fun x(v: Double) = ((v.coerceIn(min, max) - min) / (max - min) * size.width).toFloat()
            segments.forEach { (range, color) ->
                drawRect(color, Offset(x(range.first), 6.dp.toPx()), Size(x(range.second) - x(range.first), 10.dp.toPx()))
            }
            val mx = x(bmi)
            drawRect(marker, Offset(mx - 2.dp.toPx(), 0f), Size(4.dp.toPx(), size.height))
        }
        Row(Modifier.fillMaxWidth()) {
            Text("Bajo", Modifier.weight(14f), style = MaterialTheme.typography.labelSmall)
            Text("Normal", Modifier.weight(26f), style = MaterialTheme.typography.labelSmall)
            Text("Sobrepeso", Modifier.weight(20f), style = MaterialTheme.typography.labelSmall)
            Text("Obesidad", Modifier.weight(40f), style = MaterialTheme.typography.labelSmall)
        }
    }
}
