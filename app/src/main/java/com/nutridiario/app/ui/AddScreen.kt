package com.nutridiario.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.nutridiario.app.*

@Composable
fun AddScreen(nav: NavController) {
    val recent = Repository.recentFoods()
    val mine = Repository.data.value.customFoods.size
    val meal = Session.preferredMeal
    LazyColumn(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background), contentPadding = PaddingValues(16.dp)) {
        item {
            Text("¿Qué has comido?", style = MaterialTheme.typography.headlineMedium)
            Text(
                dateLabel(Session.date) + (meal?.let { " · ${it.emoji} ${it.label}" } ?: ""),
                color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 16.dp),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Tile("📷", "Escanear", "Código de barras", listOf(Color(0xFF22C55E), Color(0xFF0D9488)), Modifier.weight(1f)) { start(nav, "barcode") }
                Tile("🤖", "Foto IA", "Reconoce el plato", listOf(Color(0xFF8B5CF6), Color(0xFF6366F1)), Modifier.weight(1f)) { start(nav, "photo") }
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Tile("🔎", "Buscar", "Online y sin conexión", listOf(Color(0xFFF97316), Color(0xFFEF4444)), Modifier.weight(1f)) { start(nav, "search") }
                Tile("🎤", "Decírselo", "\"200 g de arroz\"", listOf(Color(0xFFEC4899), Color(0xFFF43F5E)), Modifier.weight(1f)) { start(nav, "manual") }
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SmallTile("➕", "Nuevo alimento", "Del envoltorio", Modifier.weight(1f)) { start(nav, "custom") }
                SmallTile("📒", "Mis alimentos", "$mine guardado${if (mine == 1) "" else "s"}", Modifier.weight(1f)) { start(nav, "myfoods") }
            }
            if (recent.isNotEmpty()) {
                SectionTitle("Recientes", Modifier.padding(top = 8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(recent) { e ->
                        Column(
                            Modifier.width(118.dp)
                                .shadow(4.dp, MaterialTheme.shapes.medium, ambientColor = Color(0x22000000), spotColor = Color(0x22000000))
                                .clip(MaterialTheme.shapes.medium)
                                .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                                .clickable {
                                    Session.editingEntry = null; Session.pendingFood = e.food; Session.pendingGrams = e.grams
                                    nav.navigate("detail")
                                }
                                .padding(12.dp),
                        ) {
                            FoodAvatar(e.food, 44.dp)
                            Spacer(Modifier.height(8.dp))
                            Text(e.food.name, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.height(40.dp))
                            Text("${fmt(e.nutrients.kcal, 0)} kcal", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, style = Caption)
                            Text("${fmt(e.grams, 0)} ${e.food.unit}", style = Caption, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

private fun start(nav: NavController, route: String) {
    Session.editingEntry = null
    Session.pendingGrams = null
    Session.manualText = ""
    Session.editorFood = null
    Session.editorIsMine = false
    nav.navigate(route)
}

@Composable
private fun Tile(emoji: String, title: String, subtitle: String, colors: List<Color>, modifier: Modifier, onClick: () -> Unit) {
    Column(
        modifier.height(150.dp)
            .shadow(8.dp, MaterialTheme.shapes.large, spotColor = colors.first().copy(alpha = 0.6f))
            .clip(MaterialTheme.shapes.large)
            .background(Brush.linearGradient(colors))
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Box(Modifier.size(48.dp).clip(MaterialTheme.shapes.small).background(Color.White.copy(alpha = 0.22f)), contentAlignment = Alignment.Center) {
            Text(emoji, fontSize = 26.sp)
        }
        Column {
            Text(title, color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
            Text(subtitle, color = Color.White.copy(alpha = 0.88f), style = Caption)
        }
    }
}

@Composable
private fun SmallTile(emoji: String, title: String, subtitle: String, modifier: Modifier, onClick: () -> Unit) {
    Row(
        modifier
            .shadow(4.dp, MaterialTheme.shapes.medium, ambientColor = Color(0x22000000), spotColor = Color(0x22000000))
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceContainerLowest)
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        EmojiBadge(emoji, MaterialTheme.colorScheme.primaryContainer, 38.dp)
        Spacer(Modifier.width(10.dp))
        Column {
            Text(title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
            Text(subtitle, style = Caption, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
