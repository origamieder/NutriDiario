package com.nutridiario.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.nutridiario.app.FoodItem
import com.nutridiario.app.Nutrients
import com.nutridiario.app.fmt

fun Modifier.clickableNoRipple(onClick: () -> Unit) = this.clickable(onClick = onClick)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackBar(title: String, nav: NavController, actions: @Composable RowScope.() -> Unit = {}) {
    CenterAlignedTopAppBar(
        title = { Text(title, fontWeight = FontWeight.Bold) },
        navigationIcon = {
            IconButton(onClick = { nav.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Atrás") }
        },
        actions = actions,
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = MaterialTheme.colorScheme.background),
    )
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(text, style = MaterialTheme.typography.titleMedium, modifier = modifier.padding(top = 16.dp, bottom = 8.dp))
}

/** Tarjeta blanca redondeada con sombra suave. */
@Composable
fun AppCard(modifier: Modifier = Modifier, padding: Dp = 16.dp, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier
            .shadow(6.dp, MaterialTheme.shapes.large, ambientColor = Color(0x22000000), spotColor = Color(0x22000000))
            .clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.surfaceContainerLowest)
            .padding(padding),
        content = content,
    )
}

@Composable
fun GradientCard(brush: Brush = HeroGradient, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier
            .shadow(10.dp, MaterialTheme.shapes.extraLarge, spotColor = Green.copy(alpha = 0.5f))
            .clip(MaterialTheme.shapes.extraLarge)
            .background(brush)
            .padding(20.dp),
        content = content,
    )
}

/** Foto del producto o, si no hay, un emoji sobre un círculo de color. */
@Composable
fun FoodAvatar(food: FoodItem, size: Dp = 48.dp) = FoodAvatar(food.name, food.imageUrl, size)

@Composable
fun FoodAvatar(name: String, imageUrl: String?, size: Dp = 48.dp) {
    Box(
        Modifier.size(size).clip(RoundedCornerShape(size * 0.32f)).background(foodTint(name)),
        contentAlignment = Alignment.Center,
    ) {
        Text(foodEmoji(name), fontSize = (size.value * 0.5f).sp)
        if (imageUrl != null) {
            AsyncImage(imageUrl, null, Modifier.fillMaxSize().background(Color.White), contentScale = ContentScale.Crop)
        }
    }
}

@Composable
fun EmojiBadge(emoji: String, tint: Color, size: Dp = 40.dp) {
    Box(Modifier.size(size).clip(CircleShape).background(tint), contentAlignment = Alignment.Center) {
        Text(emoji, fontSize = (size.value * 0.48f).sp)
    }
}

/** Anillo grande de calorías, pensado para ir sobre el degradado. */
@Composable
fun KcalRing(consumed: Double, target: Double, size: Dp = 150.dp) {
    val frac = if (target > 0) (consumed / target).toFloat() else 0f
    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(size)) {
        Canvas(Modifier.fillMaxSize().padding(6.dp)) {
            val stroke = Stroke(width = 14.dp.toPx(), cap = StrokeCap.Round)
            drawArc(Color.White.copy(alpha = 0.25f), 0f, 360f, false, style = stroke)
            drawArc(
                if (frac > 1.05f) Color(0xFFFFD2D2) else Color.White,
                -90f, 360f * frac.coerceIn(0f, 1f), false, style = stroke,
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(fmt(consumed, 0), color = Color.White, fontSize = 34.sp, fontWeight = FontWeight.ExtraBold)
            Text("de ${fmt(target, 0)} kcal", color = Color.White.copy(alpha = 0.85f), style = Caption)
        }
    }
}

/** Anillo pequeño de un macronutriente. */
@Composable
fun MacroRing(label: String, value: Double, target: Double, color: Color, modifier: Modifier = Modifier) {
    val frac = if (target > 0) (value / target).toFloat() else 0f
    val track = color.copy(alpha = 0.15f)
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(78.dp)) {
            Canvas(Modifier.fillMaxSize().padding(4.dp)) {
                val stroke = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
                drawArc(track, 0f, 360f, false, style = stroke)
                drawArc(if (frac > 1.15f) Over else color, -90f, 360f * frac.coerceIn(0f, 1f), false, style = stroke)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(fmt(value, 0), fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                Text("/${fmt(target, 0)} g", style = Caption, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Spacer(Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(8.dp).clip(CircleShape).background(color))
            Spacer(Modifier.width(4.dp))
            Text(label, style = MaterialTheme.typography.labelMedium)
        }
    }
}

/** Barra horizontal fina con color propio. Si isMax=true, pasarse es malo. */
@Composable
fun NutrientBar(label: String, value: Double, target: Double, unit: String = "g", isMax: Boolean = false, color: Color = Green) {
    val frac = if (target > 0) (value / target).toFloat() else 0f
    val barColor = if (isMax && frac > 1f) Over else color
    Column(Modifier.padding(vertical = 5.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
            Text(label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
            Text(fmt(value, if (unit == "g" && value < 10) 1 else 0), fontWeight = FontWeight.Bold, color = if (isMax && frac > 1f) Over else MaterialTheme.colorScheme.onSurface)
            Text(" / ${if (isMax) "máx " else ""}${fmt(target, 0)} $unit", style = Caption, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.height(4.dp))
        Box(Modifier.fillMaxWidth().height(8.dp).clip(CircleShape).background(barColor.copy(alpha = 0.15f))) {
            Box(Modifier.fillMaxWidth(frac.coerceIn(0f, 1f)).fillMaxHeight().clip(CircleShape).background(barColor))
        }
    }
}

/** Barra apilada con el reparto de calorías entre proteínas, hidratos y grasas. */
@Composable
fun MacroSplitBar(n: Nutrients) {
    val p = n.protein * 4; val c = n.carbs * 4; val f = n.fat * 9
    val total = (p + c + f).takeIf { it > 0 } ?: 1.0
    Column {
        Row(Modifier.fillMaxWidth().height(12.dp).clip(CircleShape)) {
            if (p > 0) Box(Modifier.weight((p / total).toFloat()).fillMaxHeight().background(ProteinC))
            if (c > 0) Box(Modifier.weight((c / total).toFloat()).fillMaxHeight().background(CarbsC))
            if (f > 0) Box(Modifier.weight((f / total).toFloat()).fillMaxHeight().background(FatC))
            if (p + c + f <= 0) Box(Modifier.weight(1f).fillMaxHeight().background(MaterialTheme.colorScheme.surfaceVariant))
        }
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth()) {
            MacroStat("Proteínas", n.protein, ProteinC, Modifier.weight(1f))
            MacroStat("Hidratos", n.carbs, CarbsC, Modifier.weight(1f))
            MacroStat("Grasas", n.fat, FatC, Modifier.weight(1f))
        }
    }
}

@Composable
fun MacroStat(label: String, grams: Double, color: Color, modifier: Modifier = Modifier) {
    Column(modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(8.dp).clip(CircleShape).background(color)); Spacer(Modifier.width(6.dp))
            Text(label, style = Caption, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text("${fmt(grams)} g", fontWeight = FontWeight.Bold, fontSize = 18.sp)
    }
}

@Composable
fun NutrientTable(n: Nutrients, header: String) {
    AppCard(Modifier.fillMaxWidth()) {
        Text(header, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Row2("Energía", "${fmt(n.kcal, 0)} kcal · ${fmt(n.kcal * 4.184, 0)} kJ", bold = true)
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Row2("Grasas", "${fmt(n.fat)} g")
        Row2("   saturadas", "${fmt(n.satFat)} g", dim = true)
        Row2("Hidratos de carbono", "${fmt(n.carbs)} g")
        Row2("   azúcares", "${fmt(n.sugar)} g", dim = true)
        Row2("Fibra", "${fmt(n.fiber)} g")
        Row2("Proteínas", "${fmt(n.protein)} g")
        Row2("Sal", "${fmt(n.salt, 2)} g")
    }
}

@Composable
fun Row2(a: String, b: String, bold: Boolean = false, dim: Boolean = false) {
    val c = if (dim) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(a, Modifier.weight(1f), fontWeight = if (bold) FontWeight.Bold else null, color = c)
        Text(b, fontWeight = if (bold) FontWeight.Bold else FontWeight.Medium, color = c)
    }
}

/** Fila de alimento con avatar, para listas de búsqueda/recientes. */
@Composable
fun FoodRow(
    food: FoodItem, onClick: () -> Unit, trailing: String? = null,
    subtitle: String? = null, value: String? = null, action: (@Composable () -> Unit)? = null,
) {
    Row(
        Modifier.fillMaxWidth().clip(MaterialTheme.shapes.medium).clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FoodAvatar(food, 48.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(food.name, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                subtitle ?: listOfNotNull(food.brand, food.packageG?.let { "${fmt(it, 0)} ${food.unit}" }, "P ${fmt(food.per100.protein)} · H ${fmt(food.per100.carbs)} · G ${fmt(food.per100.fat)}")
                    .joinToString(" · "),
                style = Caption, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(8.dp))
        Column(horizontalAlignment = Alignment.End) {
            Text(value ?: "${fmt(food.per100.kcal, 0)}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Text(trailing ?: "kcal/100${food.unit}", style = Caption, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        action?.invoke()
    }
}
