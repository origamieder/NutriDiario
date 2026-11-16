package com.nutridiario.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.nutridiario.app.Repository
import com.nutridiario.app.Session
import com.nutridiario.app.fmt

@Composable
fun AddScreen(nav: NavController) {
    val recent = Repository.recentFoods()
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        item {
            Text(
                "Añadir a: ${dateLabel(Session.date)}",
                style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 16.dp),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OptionCard("📷", "Código de barras", "Escanea el envase", Modifier.weight(1f)) { start(nav, "barcode") }
                OptionCard("🤖", "Foto con IA", "Reconoce el plato sin internet", Modifier.weight(1f)) { start(nav, "photo") }
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OptionCard("🔎", "Buscar", "Productos online y base local", Modifier.weight(1f)) { start(nav, "search") }
                OptionCard("🗣️", "Decírselo", "\"200 g de arroz\", por voz o texto", Modifier.weight(1f)) {
                    Session.manualText = ""; start(nav, "manual")
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = { start(nav, "custom") }, modifier = Modifier.weight(1f)) {
                    Text("➕ Nuevo alimento\n(datos del envoltorio)", textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                }
                OutlinedButton(onClick = { start(nav, "myfoods") }, modifier = Modifier.weight(1f).height(IntrinsicSize.Max)) {
                    Text("📒 Mis alimentos\n(${Repository.data.value.customFoods.size} guardados)", textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                }
            }
            if (recent.isNotEmpty()) SectionTitle("Recientes", Modifier.padding(top = 12.dp))
        }
        items(recent) { e ->
            FoodRow(e.food, onClick = {
                Session.editingEntry = null
                Session.pendingFood = e.food
                Session.pendingGrams = e.grams
                nav.navigate("detail")
            }, trailing = "${fmt(e.grams, 0)} g")
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
private fun OptionCard(emoji: String, title: String, subtitle: String, modifier: Modifier, onClick: () -> Unit) {
    ElevatedCard(onClick = onClick, modifier = modifier.height(140.dp)) {
        Column(Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            Text(emoji, fontSize = 36.sp)
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        }
    }
}
