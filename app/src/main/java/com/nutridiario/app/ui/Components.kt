package com.nutridiario.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.nutridiario.app.FoodItem
import com.nutridiario.app.Nutrients
import com.nutridiario.app.fmt

val Over = Color(0xFFE53935)
val Good = Color(0xFF43A047)
val Warn = Color(0xFFFB8C00)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackBar(title: String, nav: NavController, actions: @Composable RowScope.() -> Unit = {}) {
    TopAppBar(
        title = { Text(title) },
        navigationIcon = {
            IconButton(onClick = { nav.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Atrás") }
        },
        actions = actions,
    )
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = modifier.padding(vertical = 8.dp))
}

@Composable
fun KcalRing(consumed: Double, target: Double, size: Dp = 150.dp) {
    val frac = if (target > 0) (consumed / target).toFloat() else 0f
    val track = MaterialTheme.colorScheme.surfaceVariant
    val color = when {
        frac > 1.1f -> Over
        frac > 1f -> Warn
        else -> MaterialTheme.colorScheme.primary
    }
    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(size)) {
        Canvas(Modifier.fillMaxSize().padding(8.dp)) {
            val stroke = Stroke(width = 16.dp.toPx(), cap = StrokeCap.Round)
            drawArc(track, 135f, 270f, false, style = stroke)
            drawArc(color, 135f, 270f * frac.coerceIn(0f, 1f), false, style = stroke)
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(fmt(consumed, 0), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("de ${fmt(target, 0)} kcal", style = MaterialTheme.typography.bodySmall)
        }
    }
}

/** Barra de progreso de un nutriente. Si isMax=true, pasarse es malo (azúcar, sal…). */
@Composable
fun NutrientBar(label: String, value: Double, target: Double, unit: String = "g", isMax: Boolean = false) {
    val frac = if (target > 0) (value / target).toFloat() else 0f
    val color = when {
        isMax && frac > 1f -> Over
        isMax && frac > 0.8f -> Warn
        !isMax && frac > 1.15f -> Warn
        !isMax && frac >= 0.85f -> Good
        else -> MaterialTheme.colorScheme.primary
    }
    Column(Modifier.padding(vertical = 4.dp)) {
        Row(Modifier.fillMaxWidth()) {
            Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            Text(
                "${fmt(value)} / ${if (isMax) "máx. " else ""}${fmt(target, 0)} $unit",
                style = MaterialTheme.typography.bodyMedium,
                color = if (isMax && frac > 1f) Over else MaterialTheme.colorScheme.onSurface,
            )
        }
        LinearProgressIndicator(
            progress = { frac.coerceIn(0f, 1f) },
            color = color,
            modifier = Modifier.fillMaxWidth().height(8.dp).padding(top = 2.dp),
            drawStopIndicator = {},
        )
    }
}

@Composable
fun NutrientTable(n: Nutrients, header: String) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(header, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Row2("Energía", "${fmt(n.kcal, 0)} kcal  (${fmt(n.kcal * 4.184, 0)} kJ)", bold = true)
            HorizontalDivider()
            Row2("Grasas", "${fmt(n.fat)} g")
            Row2("   de las cuales saturadas", "${fmt(n.satFat)} g")
            Row2("Hidratos de carbono", "${fmt(n.carbs)} g")
            Row2("   de los cuales azúcares", "${fmt(n.sugar)} g")
            Row2("Fibra", "${fmt(n.fiber)} g")
            Row2("Proteínas", "${fmt(n.protein)} g")
            Row2("Sal", "${fmt(n.salt, 2)} g")
        }
    }
}

@Composable
fun Row2(a: String, b: String, bold: Boolean = false) {
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
        Text(a, Modifier.weight(1f), fontWeight = if (bold) FontWeight.Bold else null)
        Text(b, fontWeight = if (bold) FontWeight.Bold else null)
    }
}

@Composable
fun FoodRow(food: FoodItem, onClick: () -> Unit, trailing: String? = null) {
    ListItem(
        headlineContent = { Text(food.name) },
        supportingContent = {
            Text(
                listOfNotNull(food.brand, "${fmt(food.per100.kcal, 0)} kcal/100 g", "P ${fmt(food.per100.protein)} · HC ${fmt(food.per100.carbs)} · G ${fmt(food.per100.fat)}")
                    .joinToString(" · ")
            )
        },
        trailingContent = trailing?.let { { Text(it, style = MaterialTheme.typography.labelSmall) } },
        modifier = Modifier.clickableNoRipple(onClick),
    )
}

fun Modifier.clickableNoRipple(onClick: () -> Unit) = this.clickable(onClick = onClick)
