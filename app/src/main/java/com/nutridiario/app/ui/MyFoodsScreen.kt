package com.nutridiario.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import com.nutridiario.app.*

/** "Mis alimentos" en cuadrícula con foto grande. Tocar = añadir al diario; lápiz = editar. */
@Composable
fun MyFoodsScreen(nav: NavController) {
    val data by Repository.data.collectAsStateWithLifecycle()
    var filter by remember { mutableStateOf("") }
    val foods = data.customFoods.filter {
        filter.isBlank() || LocalFoodDb.normalize("${it.name} ${it.brand ?: ""}").contains(LocalFoodDb.normalize(filter))
    }
    fun edit(f: FoodItem?) {
        Session.editorFood = f; Session.editorIsMine = f != null; Session.manualText = ""; Session.pendingGrams = null
        nav.navigate("custom")
    }
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(Modifier.fillMaxSize()) {
            BackBar("Mis alimentos", nav)
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 96.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f),
            ) {
                if (data.customFoods.size > 4) item(span = { GridItemSpan(2) }) {
                    OutlinedTextField(
                        filter, { filter = it }, placeholder = { Text("Buscar en mis alimentos") }, singleLine = true,
                        leadingIcon = { Icon(Icons.Filled.Search, null) }, shape = MaterialTheme.shapes.large,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                if (data.customFoods.isEmpty()) item(span = { GridItemSpan(2) }) {
                    Column(Modifier.padding(vertical = 48.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("📒", fontSize = 56.sp)
                        Text("Aún no tienes alimentos guardados", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Escanea un producto o pulsa «Nuevo alimento». Con una foto de la etiqueta se rellenan los valores solos.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        )
                    }
                }
                items(foods, key = { it.barcode ?: (it.name + it.brand) }) { f ->
                    FoodCard(f, onClick = {
                        Session.editingEntry = null; Session.pendingFood = f; Session.pendingGrams = null
                        nav.navigate("detail")
                    }, onEdit = { edit(f) })
                }
            }
        }
        ExtendedFloatingActionButton(
            onClick = { edit(null) },
            icon = { Icon(Icons.Filled.Add, null) }, text = { Text("Nuevo alimento", fontWeight = FontWeight.Bold) },
            containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
        )
    }
}

@Composable
private fun FoodCard(f: FoodItem, onClick: () -> Unit, onEdit: () -> Unit) {
    Column(
        Modifier.shadow(5.dp, MaterialTheme.shapes.large, ambientColor = Color(0x22000000), spotColor = Color(0x22000000))
            .clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.surfaceContainerLowest)
            .clickable(onClick = onClick),
    ) {
        Box(Modifier.fillMaxWidth().aspectRatio(1.15f).background(foodTint(f.name)), contentAlignment = Alignment.Center) {
            Text(foodEmoji(f.name), fontSize = 54.sp)
            if (f.imageUrl != null) {
                var failed by remember(f.imageUrl) { mutableStateOf(false) }
                if (!failed) AsyncImage(
                    f.imageUrl, f.name, contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().background(Color.White),
                    onState = { if (it is AsyncImagePainter.State.Error) failed = true },
                )
            }
            Box(
                Modifier.align(Alignment.TopEnd).padding(8.dp).size(36.dp).clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.9f)).clickable(onClick = onEdit),
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Filled.Edit, "Editar", tint = Color(0xFF374151), modifier = Modifier.size(18.dp)) }
            if (f.barcode != null) Text(
                "▌▌▌ escaneable", fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.BottomStart).padding(8.dp).clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.55f)).padding(horizontal = 8.dp, vertical = 3.dp),
            )
        }
        Column(Modifier.padding(12.dp)) {
            Text(f.name, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                listOfNotNull(f.brand, f.packageG?.let { "${fmt(it, 0)} ${f.unit}" }).joinToString(" · ").ifBlank { " " },
                style = Caption, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(fmt(f.per100.kcal, 0), fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary, fontSize = 18.sp)
                Text(" kcal/100 ${f.unit}", style = Caption, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 2.dp))
            }
        }
    }
}
