package com.nutridiario.app.ui

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.nutridiario.app.*

@Composable
fun ManualScreen(nav: NavController) {
    val ctx = LocalContext.current
    var text by remember { mutableStateOf(Session.manualText) }
    val parsed = remember(text) { QuantityParser.parse(text) }
    val results = remember(parsed.query) { LocalFoodDb.search(parsed.query, 15) }

    val speech = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { r ->
        if (r.resultCode == Activity.RESULT_OK) {
            r.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()?.let { text = it }
        }
    }

    fun gramsFor(f: FoodItem): Double? = parsed.grams ?: parsed.units?.let { u -> u * (f.portionG ?: 100.0) }

    Column(Modifier.fillMaxSize()) {
        BackBar("Dime qué has comido", nav)
        Column(Modifier.padding(horizontal = 16.dp)) {
            Row {
                OutlinedTextField(
                    value = text, onValueChange = { text = it },
                    label = { Text("Ej: 150 g de pollo, 2 huevos, un vaso de leche") },
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(8.dp))
                FilledTonalButton(onClick = {
                    val i = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
                        .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                        .putExtra(RecognizerIntent.EXTRA_LANGUAGE, "es-ES")
                        .putExtra(RecognizerIntent.EXTRA_PROMPT, "¿Qué has comido y cuánto?")
                    try { speech.launch(i) } catch (e: ActivityNotFoundException) {
                        Toast.makeText(ctx, "Tu móvil no tiene reconocimiento de voz", Toast.LENGTH_SHORT).show()
                    }
                }, modifier = Modifier.height(56.dp)) { Text("🎤", fontSize = 20.sp) }
            }
            if (text.isNotBlank()) {
                val qty = parsed.grams?.let { "${fmt(it, 0)} g" } ?: parsed.units?.let { "${fmt(it)} unidad(es)/ración(es)" } ?: "no indicada (se usará una ración típica)"
                Text("Alimento: «${parsed.query}» · Cantidad: $qty", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp))
            }
        }
        LazyColumn(Modifier.weight(1f)) {
            items(results) { f ->
                val g = gramsFor(f) ?: f.portionG ?: 100.0
                ListItem(
                    headlineContent = { Text(f.name, fontWeight = FontWeight.Medium) },
                    supportingContent = { Text("${fmt(g, 0)} g → ${fmt(f.per100.kcal * g / 100, 0)} kcal · P ${fmt(f.per100.protein * g / 100)} · HC ${fmt(f.per100.carbs * g / 100)} · G ${fmt(f.per100.fat * g / 100)}") },
                    modifier = Modifier.clickableNoRipple {
                        Session.pendingFood = f.copy(source = if (f.source == "Base de datos local") "Descrito por ti" else f.source)
                        Session.pendingGrams = g
                        nav.navigate("detail")
                    },
                )
            }
            item {
                Column(Modifier.padding(16.dp)) {
                    if (text.isNotBlank() && results.isEmpty()) Text("No lo encuentro en la base local.")
                    OutlinedButton(onClick = {
                        Session.manualText = parsed.query; Session.pendingGrams = parsed.grams; nav.navigate("search")
                    }, enabled = parsed.query.isNotBlank(), modifier = Modifier.fillMaxWidth()) { Text("Buscar «${parsed.query}» online") }
                    OutlinedButton(onClick = {
                        Session.manualText = parsed.query; Session.pendingGrams = parsed.grams; nav.navigate("custom")
                    }, modifier = Modifier.fillMaxWidth()) { Text("Crear alimento con mis propios valores") }
                }
            }
        }
    }
}
