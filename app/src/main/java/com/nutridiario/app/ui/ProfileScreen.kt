package com.nutridiario.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.nutridiario.app.*

private val Activity.emoji get() = when (this) {
    Activity.SEDENTARY -> "🛋️"; Activity.LIGHT -> "🚶"; Activity.MODERATE -> "🏃"; Activity.ACTIVE -> "🏋️"; Activity.VERY_ACTIVE -> "🔥"
}
private val Goal.emoji get() = when (this) {
    Goal.LOSE -> "📉"; Goal.LOSE_SLOW -> "🐢"; Goal.MAINTAIN -> "⚖️"; Goal.GAIN -> "💪"
}
private val Goal.short get() = when (this) {
    Goal.LOSE -> "Perder peso"; Goal.LOSE_SLOW -> "Perder suave"; Goal.MAINTAIN -> "Mantener"; Goal.GAIN -> "Ganar peso"
}
private val Goal.sub get() = when (this) {
    Goal.LOSE -> "-0,5 kg/semana"; Goal.LOSE_SLOW -> "-0,25 kg/semana"; Goal.MAINTAIN -> "Mismo peso"; Goal.GAIN -> "+ músculo"
}

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
    var editing by remember { mutableStateOf(!saved.configured) }

    fun num(s: String) = s.replace(',', '.').toDoubleOrNull()
    val valid = (num(age) ?: 0.0) in 14.0..110.0 && (num(height) ?: 0.0) in 120.0..230.0 && (num(weight) ?: 0.0) in 30.0..300.0
    val draft = if (valid) Profile(name, sex, num(age)!!.toInt(), num(height)!!, num(weight)!!, activity, goal, true) else null
    val report = (if (editing) draft else saved.takeIf { it.configured })?.let { HealthReport(it) }

    Column(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).verticalScroll(rememberScrollState()).padding(16.dp)
    ) {
        if (!saved.configured) {
            Text("¡Bienvenido/a! 👋", style = MaterialTheme.typography.headlineMedium)
            Text("Cuéntame un poco sobre ti para calcular tus calorías y tu peso ideal.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(16.dp))
        } else {
            Text("Mi perfil", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(bottom = 12.dp))
        }

        if (report != null && !editing) {
            ProfileHero(report) { editing = true }
            Spacer(Modifier.height(16.dp))
        }

        if (editing) {
            AppCard(Modifier.fillMaxWidth()) {
                Text("Mis datos", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Sex.entries.forEach { s ->
                        ChoiceTile(if (s == Sex.MALE) "👨" else "👩", s.label, null, sex == s, Modifier.weight(1f)) { sex = s }
                    }
                }
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(name, { name = it }, label = { Text("Nombre (opcional)") }, singleLine = true, modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium)
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NumField("Edad", age, { age = it }, "años", Modifier.weight(1f))
                    NumField("Altura", height, { height = it }, "cm", Modifier.weight(1f))
                    NumField("Peso", weight, { weight = it }, "kg", Modifier.weight(1f))
                }
                SectionTitle("Actividad física")
                Activity.entries.forEach { a ->
                    ChoiceRow(a.emoji, a.label, a.desc, activity == a) { activity = a }
                }
                SectionTitle("Objetivo")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Goal.entries.take(2).forEach { g -> ChoiceTile(g.emoji, g.short, g.sub, goal == g, Modifier.weight(1f)) { goal = g } }
                }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Goal.entries.drop(2).forEach { g -> ChoiceTile(g.emoji, g.short, g.sub, goal == g, Modifier.weight(1f)) { goal = g } }
                }
                Button(
                    onClick = {
                        val first = !saved.configured
                        Repository.saveProfile(draft!!)
                        editing = false
                        if (first) goToday(nav)
                    },
                    enabled = valid, shape = MaterialTheme.shapes.large,
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp).height(54.dp),
                ) { Text("Guardar", fontWeight = FontWeight.Bold, fontSize = 16.sp) }
            }
            Spacer(Modifier.height(16.dp))
        }

        if (report != null) ReportView(report)
    }
}

@Composable
private fun ProfileHero(r: HealthReport, onEdit: () -> Unit) {
    val p = r.p
    GradientCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(56.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.25f)), contentAlignment = Alignment.Center) {
                Text(if (p.sex == Sex.MALE) "👨" else "👩", fontSize = 30.sp)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(p.name.ifBlank { "Tú" }, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
                Text("${p.age} años · ${fmt(p.heightCm, 0)} cm · ${fmt(p.weightKg)} kg", color = Color.White.copy(alpha = 0.9f), style = Caption)
            }
            TextButton(onClick = onEdit, colors = ButtonDefaults.textButtonColors(contentColor = Color.White)) { Text("Editar") }
        }
        Spacer(Modifier.height(16.dp))
        Row {
            HeroNumber("IMC", fmt(r.bmi), r.bmiCategory, Modifier.weight(1f))
            HeroNumber("Peso ideal", "${fmt(r.idealWeight, 0)} kg", "rango ${fmt(r.healthyMin, 0)}–${fmt(r.healthyMax, 0)}", Modifier.weight(1f))
            HeroNumber(
                if (r.diffToIdeal >= 0) "Te sobran" else "Te faltan", "${fmt(kotlin.math.abs(r.diffToIdeal))} kg",
                "${p.goal.emoji} ${p.goal.short}", Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun HeroNumber(label: String, value: String, sub: String, modifier: Modifier) {
    Column(modifier) {
        Text(label, color = Color.White.copy(alpha = 0.85f), style = Caption)
        Text(value, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
        Text(sub, color = Color.White.copy(alpha = 0.85f), style = Caption)
    }
}

@Composable
private fun ChoiceTile(emoji: String, title: String, sub: String?, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Column(
        modifier.clip(MaterialTheme.shapes.medium)
            .background(if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh)
            .border(2.dp, if (selected) MaterialTheme.colorScheme.primary else Color.Transparent, MaterialTheme.shapes.medium)
            .clickable(onClick = onClick).padding(vertical = 12.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(emoji, fontSize = 26.sp)
        Text(title, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        sub?.let { Text(it, style = Caption, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center) }
    }
}

@Composable
private fun ChoiceRow(emoji: String, title: String, sub: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 3.dp).clip(MaterialTheme.shapes.medium)
            .background(if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh)
            .border(2.dp, if (selected) MaterialTheme.colorScheme.primary else Color.Transparent, MaterialTheme.shapes.medium)
            .clickable(onClick = onClick).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(emoji, fontSize = 22.sp)
        Spacer(Modifier.width(12.dp))
        Column {
            Text(title, fontWeight = FontWeight.Bold)
            Text(sub, style = Caption, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun NumField(label: String, value: String, onChange: (String) -> Unit, suffix: String, modifier: Modifier) {
    OutlinedTextField(
        value, { onChange(it.filter { c -> c.isDigit() || c == ',' || c == '.' }) },
        label = { Text(label) }, suffix = { Text(suffix) }, singleLine = true, shape = MaterialTheme.shapes.medium,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = modifier,
    )
}

@Composable
fun ReportView(r: HealthReport) {
    val p = r.p
    AppCard(Modifier.fillMaxWidth()) {
        Text("Tu peso frente al ideal", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(16.dp))
        BmiGauge(r.bmi)
        Spacer(Modifier.height(16.dp))
        WeightJourney(p.weightKg, r.idealWeight, r.healthyMin, r.healthyMax)
        Spacer(Modifier.height(16.dp))
        Row(
            Modifier.fillMaxWidth().clip(MaterialTheme.shapes.medium).background(MaterialTheme.colorScheme.primaryContainer).padding(12.dp),
        ) {
            Text("💡", fontSize = 20.sp)
            Spacer(Modifier.width(10.dp))
            Column {
                Text(r.advice, color = MaterialTheme.colorScheme.onPrimaryContainer)
                r.weeksToIdeal?.let {
                    Text("Al ritmo de tu objetivo: unas $it semanas.", style = Caption, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }
        }
    }
    Spacer(Modifier.height(16.dp))
    val t = r.targets
    AppCard(Modifier.fillMaxWidth()) {
        Text("Tus necesidades diarias", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatTile("😴", "Basal", "${fmt(r.bmr, 0)}", "kcal", Modifier.weight(1f))
            StatTile("🔥", "Gasto total", "${fmt(r.tdee, 0)}", "kcal", Modifier.weight(1f))
            StatTile("🎯", "Objetivo", "${fmt(t.kcal, 0)}", "kcal", Modifier.weight(1f), highlight = true)
        }
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth()) {
            MacroStat("Proteínas", t.protein, ProteinC, Modifier.weight(1f))
            MacroStat("Hidratos", t.carbs, CarbsC, Modifier.weight(1f))
            MacroStat("Grasas", t.fat, FatC, Modifier.weight(1f))
        }
        Spacer(Modifier.height(12.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Row2("🌾 Fibra (mínimo)", "${fmt(t.fiber, 0)} g")
        Row2("🍬 Azúcares (máximo)", "${fmt(t.sugarMax, 0)} g")
        Row2("🧈 Grasas saturadas (máximo)", "${fmt(t.satFatMax, 0)} g")
        Row2("🧂 Sal (máximo, OMS)", "${fmt(t.saltMax, 0)} g")
        Row2("💧 Agua", "${fmt(t.waterMl / 1000)} L")
    }
    Spacer(Modifier.height(16.dp))
    AppCard(Modifier.fillMaxWidth()) {
        Text("Peso ideal según cada fórmula", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(4.dp))
        r.idealFormulas.forEach { Row2(it.formula, "${fmt(it.kg)} kg") }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Row2("Grasa corporal estimada", "${fmt(r.bodyFatPct)} % · ${r.bodyFatCategory}", bold = true)
        Text("Estimación a partir del IMC (Deurenberg). Para un dato real, usa una báscula de bioimpedancia.", style = Caption, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    Spacer(Modifier.height(12.dp))
    Text(
        "Valores orientativos basados en fórmulas estándar; no sustituyen el consejo de un profesional sanitario.",
        style = Caption, color = MaterialTheme.colorScheme.outline,
    )
    Spacer(Modifier.height(24.dp))
}

@Composable
private fun StatTile(emoji: String, label: String, value: String, unit: String, modifier: Modifier, highlight: Boolean = false) {
    Column(
        modifier.clip(MaterialTheme.shapes.medium)
            .background(if (highlight) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(12.dp),
    ) {
        val c = if (highlight) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
        Text(emoji, fontSize = 18.sp)
        Text(label, style = Caption, color = c.copy(alpha = 0.8f))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(value, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = c)
            Text(" $unit", style = Caption, color = c.copy(alpha = 0.8f), modifier = Modifier.padding(bottom = 3.dp))
        }
    }
}

/** Escala de IMC con un marcador en tu valor. */
@Composable
private fun BmiGauge(bmi: Double) {
    val segments = listOf(Triple(15.0, 18.5, Color(0xFF60A5FA)), Triple(18.5, 25.0, Good), Triple(25.0, 30.0, Warn), Triple(30.0, 40.0, Over))
    val marker = MaterialTheme.colorScheme.onSurface
    Column {
        Row(verticalAlignment = Alignment.Bottom) {
            Text("IMC ", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(fmt(bmi), fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
        }
        Spacer(Modifier.height(6.dp))
        Canvas(Modifier.fillMaxWidth().height(30.dp)) {
            val min = 15.0; val max = 40.0
            fun x(v: Double) = ((v.coerceIn(min, max) - min) / (max - min) * size.width).toFloat()
            val gap = 3.dp.toPx(); val h = 12.dp.toPx(); val top = 14.dp.toPx()
            segments.forEach { (a, b, color) ->
                drawRoundRect(color, Offset(x(a) + gap / 2, top), Size(x(b) - x(a) - gap, h), CornerRadius(h / 2))
            }
            val mx = x(bmi)
            val tri = Path().apply {
                moveTo(mx - 8.dp.toPx(), 0f); lineTo(mx + 8.dp.toPx(), 0f); lineTo(mx, 11.dp.toPx()); close()
            }
            drawPath(tri, marker)
        }
        Row(Modifier.fillMaxWidth()) {
            Text("Bajo", Modifier.weight(14f), style = Caption, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("Normal", Modifier.weight(26f), style = Caption, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("Sobrepeso", Modifier.weight(20f), style = Caption, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("Obesidad", Modifier.weight(40f), style = Caption, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** Línea de peso: rango saludable en verde, tu peso actual y tu peso ideal. */
@Composable
private fun WeightJourney(current: Double, ideal: Double, hMin: Double, hMax: Double) {
    val lo = minOf(current, hMin) - 5
    val hi = maxOf(current, hMax) + 5
    val primary = MaterialTheme.colorScheme.primary
    val track = MaterialTheme.colorScheme.surfaceContainerHighest
    val onSurface = MaterialTheme.colorScheme.onSurface
    Column {
        Canvas(Modifier.fillMaxWidth().height(36.dp)) {
            fun x(v: Double) = ((v - lo) / (hi - lo) * size.width).toFloat()
            val y = size.height / 2; val h = 10.dp.toPx()
            drawRoundRect(track, Offset(0f, y - h / 2), Size(size.width, h), CornerRadius(h / 2))
            drawRoundRect(Good.copy(alpha = 0.45f), Offset(x(hMin), y - h / 2), Size(x(hMax) - x(hMin), h), CornerRadius(h / 2))
            drawLine(primary, Offset(x(ideal), y), Offset(x(current), y), 4.dp.toPx())
            drawCircle(Good, 9.dp.toPx(), Offset(x(ideal), y))
            drawCircle(Color.White, 4.dp.toPx(), Offset(x(ideal), y))
            drawCircle(onSurface, 10.dp.toPx(), Offset(x(current), y))
            drawCircle(Color.White, 4.dp.toPx(), Offset(x(current), y))
        }
        Row(Modifier.fillMaxWidth()) {
            Legend(Good, "Ideal ${fmt(ideal)} kg", Modifier.weight(1f))
            Legend(onSurface, "Actual ${fmt(current)} kg", Modifier.weight(1f))
            Legend(Good.copy(alpha = 0.45f), "Saludable", Modifier.weight(1f))
        }
    }
}

@Composable
private fun Legend(color: Color, text: String, modifier: Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(10.dp).clip(CircleShape).background(color)); Spacer(Modifier.width(6.dp))
        Text(text, style = Caption)
    }
}
