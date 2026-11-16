package com.nutridiario.app.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import com.nutridiario.app.*
import java.time.LocalDate

@Composable
fun RecipeDetailScreen(nav: NavController) {
    val r = Session.selectedRecipe
    if (r == null) { LaunchedEffect(Unit) { nav.popBackStack() }; return }
    val ctx = LocalContext.current
    val data by Repository.data.collectAsStateWithLifecycle()
    val mine = data.pantry.toSet()
    val m = RecipeBook.match(r, mine)
    val n = m.nutrients
    val report = remember(data.profile) { HealthReport(data.profile) }
    val left = report.targets.kcal - data.entries.filter { it.date == LocalDate.now().toString() }.sumOf { it.nutrients.kcal }
    val missingIngs = m.missing.map { RecipeBook.ing(it.id) }

    fun openUrl(url: String) = runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            // Cabecera con degradado
            Box(Modifier.fillMaxWidth().background(recipeBrush(r))) {
                Column(Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 24.dp, start = 20.dp, end = 20.dp)) {
                    IconButton(onClick = { nav.popBackStack() }, modifier = Modifier.offset(x = (-12).dp)) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Atrás", tint = Color.White)
                    }
                    Text(r.emoji, fontSize = 64.sp)
                    Text(r.name, color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, lineHeight = 30.sp)
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        HeroChip("⏱ ${r.minutes} min"); HeroChip("👤 1 ración"); HeroChip("🔥 ${fmt(n.kcal, 0)} kcal")
                    }
                }
            }
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                AppCard(Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Nutrición por ración", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                        Badge2(
                            if (left > 0 && n.kcal <= left) "Encaja: te quedan ${fmt(left, 0)}" else "${fmt(n.kcal / report.targets.kcal * 100, 0)} % de tu día",
                            if (left > 0 && n.kcal <= left) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    MacroSplitBar(n)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Fibra ${fmt(n.fiber)} g · Azúcares ${fmt(n.sugar)} g · Sal ${fmt(n.salt, 1)} g",
                        style = Caption, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                AppCard(Modifier.fillMaxWidth(), padding = 0.dp) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("Ingredientes", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                        Text(
                            if (missingIngs.isEmpty()) "Tienes todo ✓" else "Te faltan ${missingIngs.size}",
                            style = Caption, fontWeight = FontWeight.Bold,
                            color = if (missingIngs.isEmpty()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                        )
                    }
                    r.ingredients.forEach { item ->
                        val ing = RecipeBook.ing(item.id)
                        val coveredBy = RecipeBook.coveredBy(item.id, mine)
                        IngredientRow(ing, item.g, coveredBy, onShop = { openUrl(RecipeBook.eroskiUrl(ing)) })
                    }
                    if (missingIngs.isNotEmpty()) {
                        Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = {
                                val text = "🛒 Para «${r.name}» necesito:\n" + m.missing.joinToString("\n") { "• ${RecipeBook.ing(it.id).name} — ${RecipeBook.amountLabel(RecipeBook.ing(it.id), it.g)}" }
                                runCatching {
                                    ctx.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text), "Lista de la compra"))
                                }
                            }, modifier = Modifier.weight(1f)) { Text("📝 Lista compra") }
                            Button(onClick = {
                                Repository.setPantry(data.pantry + m.missing.map { it.id })
                            }, modifier = Modifier.weight(1f)) { Text("Ya lo tengo") }
                        }
                    } else Spacer(Modifier.height(8.dp))
                }

                AppCard(Modifier.fillMaxWidth()) {
                    Text("Preparación", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    r.steps.forEachIndexed { i, s ->
                        Row(Modifier.padding(vertical = 6.dp)) {
                            Box(Modifier.size(28.dp).clip(CircleShape).background(recipeBrush(r)), contentAlignment = Alignment.Center) {
                                Text("${i + 1}", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                            Spacer(Modifier.width(12.dp))
                            Text(s, modifier = Modifier.padding(top = 3.dp))
                        }
                    }
                }
                Text(
                    "Fotos de productos de Open Food Facts (preferentemente de marca Eroski). El botón 🛒 abre la búsqueda en supermercado.eroski.es.",
                    style = Caption, color = MaterialTheme.colorScheme.outline,
                )
            }
        }
        Button(
            onClick = {
                Session.editingEntry = null
                Session.pendingFood = RecipeBook.toFoodItem(r)
                Session.pendingGrams = null
                nav.navigate("detail")
            },
            shape = MaterialTheme.shapes.large,
            modifier = Modifier.fillMaxWidth().padding(16.dp).height(58.dp),
        ) {
            Icon(Icons.Filled.Add, null); Spacer(Modifier.width(8.dp))
            Text("Me lo he comido · ${fmt(n.kcal, 0)} kcal", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
    }
}

@Composable
private fun HeroChip(text: String) {
    Text(
        text, color = Color.White, style = MaterialTheme.typography.labelLarge,
        modifier = Modifier.clip(CircleShape).background(Color.White.copy(alpha = 0.22f)).padding(horizontal = 12.dp, vertical = 6.dp),
    )
}

@Composable
private fun IngredientRow(ing: Ingredient, grams: Double, coveredBy: String?, onShop: () -> Unit) {
    var photo by remember(ing.id) { mutableStateOf(Repository.data.value.ingredientImages[ing.id]) }
    LaunchedEffect(ing.id) { if (photo == null) photo = IngredientPhotos.get(ing) }
    val have = ing.pantry || coveredBy != null
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onShop).padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(56.dp).clip(MaterialTheme.shapes.medium).background(foodTint(ing.name)), contentAlignment = Alignment.Center) {
            Text(ing.emoji, fontSize = 26.sp)
            photo?.url?.let { url ->
                var failed by remember(url) { mutableStateOf(false) }
                if (!failed) AsyncImage(
                    url, ing.name, contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().background(Color.White),
                    onState = { if (it is AsyncImagePainter.State.Error) failed = true },
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(ing.name, fontWeight = FontWeight.SemiBold)
            Text(RecipeBook.amountLabel(ing, grams), style = Caption, color = MaterialTheme.colorScheme.onSurfaceVariant)
            photo?.title?.let {
                Text(
                    (if (photo?.eroski == true) "Eroski · " else "") + it, style = Caption, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    color = if (photo?.eroski == true) Color(0xFFE30613) else MaterialTheme.colorScheme.outline,
                )
            }
        }
        Spacer(Modifier.width(8.dp))
        when {
            ing.pantry -> Text("despensa", style = Caption, color = MaterialTheme.colorScheme.outline)
            coveredBy != null -> Column(horizontalAlignment = Alignment.End) {
                Badge2("✓ Lo tienes", MaterialTheme.colorScheme.primary)
                if (coveredBy != ing.id) Text("usas ${RecipeBook.ing(coveredBy).name.lowercase()}", style = Caption, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            else -> FilledTonalButton(
                onClick = onShop, contentPadding = PaddingValues(horizontal = 10.dp),
                colors = ButtonDefaults.filledTonalButtonColors(containerColor = Color(0xFFFFE4E6), contentColor = Color(0xFFBE123C)),
            ) { Text("🛒 Eroski", style = MaterialTheme.typography.labelLarge) }
        }
        if (!have) Unit
    }
}
