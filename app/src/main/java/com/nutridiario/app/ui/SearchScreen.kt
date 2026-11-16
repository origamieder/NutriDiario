package com.nutridiario.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.nutridiario.app.*
import kotlinx.coroutines.launch

@Composable
fun SearchScreen(nav: NavController) {
    val scope = rememberCoroutineScope()
    var query by remember { mutableStateOf(Session.manualText) }
    var online by remember { mutableStateOf<List<FoodItem>?>(null) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val local = remember(query) { LocalFoodDb.search(query, 8) }

    fun searchOnline() {
        if (query.isBlank()) return
        loading = true; error = null
        scope.launch {
            try { online = OpenFoodFacts.search(query) }
            catch (e: Exception) { error = "Sin conexión o error del servidor. Los resultados locales siguen disponibles." }
            finally { loading = false }
        }
    }
    LaunchedEffect(Unit) { if (query.isNotBlank()) searchOnline() }

    fun pick(f: FoodItem) {
        Session.pendingFood = f
        nav.navigate("detail")
    }

    Column(Modifier.fillMaxSize()) {
        BackBar("Buscar alimento o producto", nav)
        OutlinedTextField(
            value = query, onValueChange = { query = it; online = null },
            label = { Text("Ej: yogur griego, Cola Cao, galletas…") },
            singleLine = true,
            trailingIcon = { IconButton(onClick = { searchOnline() }) { Icon(Icons.Filled.Search, "Buscar") } },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { searchOnline() }),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        )
        LazyColumn(Modifier.weight(1f)) {
            if (local.isNotEmpty()) {
                item { SectionTitle("Alimentos genéricos", Modifier.padding(horizontal = 16.dp)) }
                items(local) { f -> FoodRow(f, { pick(f) }) }
            }
            item {
                Row(Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    SectionTitle("Productos online (Open Food Facts)", Modifier.weight(1f))
                    if (loading) CircularProgressIndicator(Modifier.size(20.dp))
                }
                error?.let { Text(it, color = Over, modifier = Modifier.padding(horizontal = 16.dp)) }
                if (online == null && !loading && query.isNotBlank()) {
                    TextButton(onClick = { searchOnline() }, modifier = Modifier.padding(horizontal = 8.dp)) { Text("Buscar «$query» online") }
                }
                if (online?.isEmpty() == true) Text("Sin resultados online", Modifier.padding(horizontal = 16.dp))
            }
            items(online.orEmpty()) { f ->
                ListItem(
                    leadingContent = { f.imageUrl?.let { AsyncImage(it, null, Modifier.size(48.dp)) } },
                    headlineContent = { Text(f.name) },
                    supportingContent = { Text(listOfNotNull(f.brand, "${fmt(f.per100.kcal, 0)} kcal/100 g").joinToString(" · ")) },
                    modifier = Modifier.clickableNoRipple { pick(f) },
                )
            }
        }
    }
}
