package com.nutridiario.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.nutridiario.app.*

/** Lista de alimentos guardados por el usuario. Tocar = añadir al diario; lápiz = editar. */
@Composable
fun MyFoodsScreen(nav: NavController) {
    val data by Repository.data.collectAsStateWithLifecycle()
    val foods = data.customFoods
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(Modifier.fillMaxSize()) {
            BackBar("Mis alimentos", nav)
            if (foods.isEmpty()) {
                Text(
                    "Aún no has guardado ningún alimento. Pulsa «Nuevo» y copia los datos del envoltorio: quedarán guardados para la próxima vez.",
                    modifier = Modifier.padding(16.dp),
                )
            }
            LazyColumn(Modifier.weight(1f)) {
                items(foods) { f ->
                    FoodRow(
                        f,
                        onClick = {
                            Session.editingEntry = null; Session.pendingFood = f; Session.pendingGrams = null
                            nav.navigate("detail")
                        },
                        subtitle = listOfNotNull(f.brand, f.packageG?.let { "envase ${fmt(it, 0)} ${f.unit}" }, f.barcode?.let { "📷 escaneable" }).joinToString(" · "),
                        action = {
                            IconButton(onClick = { Session.editorFood = f; Session.editorIsMine = true; Session.manualText = ""; nav.navigate("custom") }) {
                                Icon(Icons.Filled.Edit, "Editar", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        },
                    )
                }
                item { Spacer(Modifier.height(80.dp)) }
            }
        }
        ExtendedFloatingActionButton(
            onClick = { Session.editorFood = null; Session.editorIsMine = false; Session.manualText = ""; Session.pendingGrams = null; nav.navigate("custom") },
            icon = { Icon(Icons.Filled.Add, null) }, text = { Text("Nuevo alimento", fontWeight = FontWeight.Bold) },
            containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
        )
    }
}
