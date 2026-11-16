package com.nutridiario.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.nutridiario.app.*

@Composable
fun CustomFoodScreen(nav: NavController) {
    val initial = Session.manualText
    val isCode = initial.isNotBlank() && initial.all(Char::isDigit)
    var name by remember { mutableStateOf(if (isCode) "" else initial.replaceFirstChar { it.uppercase() }) }
    var brand by remember { mutableStateOf("") }
    var perPortion by remember { mutableStateOf(false) }
    var portion by remember { mutableStateOf(fmt(Session.pendingGrams ?: 100.0, 0)) }
    val fields = remember { mutableStateMapOf<String, String>() }
    var save by remember { mutableStateOf(true) }
    val keys = listOf(
        "kcal" to "Energía (kcal)", "fat" to "Grasas (g)", "sat" to "  saturadas (g)", "carbs" to "Hidratos (g)",
        "sugar" to "  azúcares (g)", "fiber" to "Fibra (g)", "protein" to "Proteínas (g)", "salt" to "Sal (g)",
    )
    fun v(k: String) = fields[k]?.replace(',', '.')?.toDoubleOrNull() ?: 0.0
    val portionG = portion.replace(',', '.').toDoubleOrNull() ?: 0.0

    Column(Modifier.fillMaxSize()) {
        BackBar("Crear alimento", nav)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
            if (isCode) Text("Código de barras: $initial", style = MaterialTheme.typography.bodySmall)
            OutlinedTextField(name, { name = it }, label = { Text("Nombre") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            OutlinedTextField(brand, { brand = it }, label = { Text("Marca (opcional)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            Spacer(Modifier.height(8.dp))
            Text("Los valores que voy a escribir son:")
            Row(verticalAlignment = Alignment.CenterVertically) {
                FilterChip(!perPortion, { perPortion = false }, label = { Text("Por 100 g") })
                Spacer(Modifier.width(8.dp))
                FilterChip(perPortion, { perPortion = true }, label = { Text("Para lo que he comido") })
            }
            OutlinedTextField(
                portion, { portion = it }, label = { Text("Cantidad que he comido (g)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth(), singleLine = true,
            )
            keys.forEach { (k, label) ->
                OutlinedTextField(
                    fields[k] ?: "", { fields[k] = it }, label = { Text(label) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(save, { save = it }); Text("Guardar en mis alimentos para la próxima vez")
            }
        }
        Button(
            enabled = name.isNotBlank() && portionG > 0 && v("kcal") >= 0,
            onClick = {
                val raw = Nutrients(v("kcal"), v("protein"), v("carbs"), v("sugar"), v("fat"), v("sat"), v("fiber"), v("salt"))
                val per100 = if (perPortion) raw * (100.0 / portionG) else raw
                val food = FoodItem(
                    name = name.trim(), brand = brand.trim().ifBlank { null }, per100 = per100,
                    portionG = portionG, portionName = "mi ración", source = "Creado por ti",
                    barcode = if (isCode) initial else null,
                )
                if (save) Repository.addCustomFood(food)
                Session.pendingFood = food
                Session.pendingGrams = portionG
                nav.navigate("detail") { popUpTo("custom") { inclusive = true } }
            },
            modifier = Modifier.fillMaxWidth().padding(16.dp).height(52.dp),
        ) { Text("Continuar") }
    }
}
