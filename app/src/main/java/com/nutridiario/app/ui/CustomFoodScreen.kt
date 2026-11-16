package com.nutridiario.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.core.content.FileProvider
import kotlinx.coroutines.launch
import androidx.navigation.NavController
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.background
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import android.Manifest
import android.content.pm.PackageManager
import com.nutridiario.app.*

private enum class DefaultQty(val label: String) { PACKAGE("Envase entero"), SERVING("Una ración"), CUSTOM("Otra cantidad") }

/**
 * Editor de "Mis alimentos": se rellena con los datos del envoltorio y se guarda para siempre
 * (con su código de barras, si lo tiene, para que al escanearlo salga directamente).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CustomFoodScreen(nav: NavController) {
    val original = remember { Session.editorFood }
    val isMine = remember { Session.editorIsMine }
    val manual = remember { Session.manualText }
    val prefillCode = original?.barcode ?: manual.takeIf { it.isNotBlank() && it.all(Char::isDigit) }
    fun d(v: Double?) = v?.takeIf { it > 0 }?.let { fmt(it, if (it % 1.0 == 0.0) 0 else 1) } ?: ""

    var name by remember { mutableStateOf(original?.name ?: if (prefillCode == null) manual.replaceFirstChar { it.uppercase() } else "") }
    var brand by remember { mutableStateOf(original?.brand ?: "") }
    var barcode by remember { mutableStateOf(prefillCode ?: "") }
    var unit by remember { mutableStateOf(original?.unit ?: "g") }
    var pkg by remember { mutableStateOf(d(original?.packageG)) }
    var serving by remember { mutableStateOf(d(original?.portionG)) }
    var perServing by remember { mutableStateOf(false) }
    val p = original?.per100
    val fields = remember {
        mutableStateMapOf(
            "kcal" to d(p?.kcal), "fat" to d(p?.fat), "sat" to d(p?.satFat), "carbs" to d(p?.carbs),
            "sugar" to d(p?.sugar), "fiber" to d(p?.fiber), "protein" to d(p?.protein), "salt" to d(p?.salt),
        )
    }
    var defaultMode by remember {
        mutableStateOf(
            when {
                original?.defaultG != null && original.defaultG == original.packageG -> DefaultQty.PACKAGE
                original?.defaultG != null && original.defaultG == original.portionG -> DefaultQty.SERVING
                original?.defaultG != null -> DefaultQty.CUSTOM
                else -> DefaultQty.PACKAGE
            }
        )
    }
    var customQty by remember { mutableStateOf(d(original?.defaultG ?: Session.pendingGrams)) }
    var confirmDelete by remember { mutableStateOf(false) }
    var scanning by remember { mutableStateOf(false) }
    val ctx = LocalContext.current
    val camPerm = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { if (it) scanning = true }
    fun startScan() {
        if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) scanning = true
        else camPerm.launch(Manifest.permission.CAMERA)
    }

    // --- Lectura automática de la tabla nutricional con la cámara (OCR en el móvil)
    val autoLabel = remember { Session.editorAutoLabel.also { Session.editorAutoLabel = false } }
    var reading by remember { mutableStateOf(false) }
    var labelMsg by remember { mutableStateOf<String?>(null) }
    var labelOk by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val labelUri = remember {
        val dir = java.io.File(ctx.cacheDir, "photos").apply { mkdirs() }
        FileProvider.getUriForFile(ctx, "${ctx.packageName}.fileprovider", java.io.File(dir, "etiqueta.jpg"))
    }
    fun fv(v: Double) = fmt(v, if (v % 1.0 == 0.0) 0 else if (v < 1) 2 else 1)
    fun applyLabel(l: LabelData) {
        val names = mutableListOf<String>()
        fun put(k: String, label: String, v: Double?) { if (v != null) fields[k] = fv(v) else names += label }
        put("kcal", "energía", l.kcal); put("fat", "grasas", l.fat); put("sat", "saturadas", l.satFat)
        put("carbs", "hidratos", l.carbs); put("sugar", "azúcares", l.sugar); put("fiber", "fibra", l.fiber)
        put("protein", "proteínas", l.protein); put("salt", "sal", l.salt)
        perServing = false
        unit = l.unit
        l.packageAmount?.let { if (pkg.isBlank()) pkg = fv(it) }
        l.servingAmount?.let { if (serving.isBlank()) serving = fv(it) }
        labelOk = l.nutrientCount >= 4
        labelMsg = when {
            l.nutrientCount == 0 -> "No he podido leer la tabla. Prueba con la foto más cerca, recta y con buena luz."
            names.isEmpty() -> "✨ He leído los 8 valores de la etiqueta. Échales un vistazo por si acaso."
            else -> "✨ He leído ${l.nutrientCount} de 8 valores. Falta: ${names.joinToString()}. Revísalos."
        }
    }
    fun readLabel(uri: android.net.Uri) {
        reading = true; labelMsg = null
        scope.launch {
            try { applyLabel(LabelReader.read(ctx, uri)) }
            catch (e: Exception) { labelMsg = "No se pudo leer la foto: ${e.message}"; labelOk = false }
            finally { reading = false }
        }
    }
    val takeLabel = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok -> if (ok) readLabel(labelUri) }
    val pickLabel = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { it?.let { u -> readLabel(u) } }
    val labelPerm = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { if (it) takeLabel.launch(labelUri) }
    fun shootLabel() {
        if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) takeLabel.launch(labelUri)
        else labelPerm.launch(Manifest.permission.CAMERA)
    }
    LaunchedEffect(Unit) { if (autoLabel) shootLabel() }

    // --- Foto del producto
    var imageUrl by remember { mutableStateOf(original?.imageUrl) }
    var imageBusy by remember { mutableStateOf(false) }
    var showImageSearch by remember { mutableStateOf(false) }
    val photoUri = remember {
        val dir = java.io.File(ctx.cacheDir, "photos").apply { mkdirs() }
        FileProvider.getUriForFile(ctx, "${ctx.packageName}.fileprovider", java.io.File(dir, "producto.jpg"))
    }
    fun setImageFrom(uri: android.net.Uri) {
        imageBusy = true
        scope.launch {
            try { imageUrl = ImageStore.saveFromUri(ctx, uri) } catch (_: Exception) { }
            finally { imageBusy = false }
        }
    }
    val takePhoto = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok -> if (ok) setImageFrom(photoUri) }
    val pickPhoto = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { it?.let { u -> setImageFrom(u) } }
    val photoPerm = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { if (it) takePhoto.launch(photoUri) }
    fun shootPhoto() {
        if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) takePhoto.launch(photoUri)
        else photoPerm.launch(Manifest.permission.CAMERA)
    }

    fun num(s: String?) = s?.replace(',', '.')?.toDoubleOrNull()
    val pkgG = num(pkg)?.takeIf { it > 0 }
    val servingG = num(serving)?.takeIf { it > 0 }
    val defaultG = when (defaultMode) {
        DefaultQty.PACKAGE -> pkgG
        DefaultQty.SERVING -> servingG
        DefaultQty.CUSTOM -> num(customQty)?.takeIf { it > 0 }
    }
    val kcalOk = num(fields["kcal"]) != null
    val valid = name.isNotBlank() && kcalOk && (!perServing || servingG != null)
    val problem = when {
        name.isBlank() -> "Falta el nombre"
        !kcalOk -> "Falta la energía (kcal)"
        perServing && servingG == null -> "Indica el tamaño de la ración"
        else -> null
    }

    fun build(): FoodItem {
        val raw = Nutrients(
            num(fields["kcal"]) ?: 0.0, num(fields["protein"]) ?: 0.0, num(fields["carbs"]) ?: 0.0, num(fields["sugar"]) ?: 0.0,
            num(fields["fat"]) ?: 0.0, num(fields["sat"]) ?: 0.0, num(fields["fiber"]) ?: 0.0, num(fields["salt"]) ?: 0.0,
        )
        return FoodItem(
            name = name.trim(),
            brand = brand.trim().ifBlank { null },
            per100 = if (perServing && servingG != null) raw * (100.0 / servingG) else raw,
            portionG = servingG,
            portionName = servingG?.let { "1 ración" },
            source = "Mis alimentos",
            barcode = barcode.trim().ifBlank { null },
            imageUrl = imageUrl,
            packageG = pkgG,
            unit = unit,
            defaultG = defaultG,
        )
    }

    fun save(): FoodItem {
        val f = build()
        Repository.saveCustomFood(if (isMine) original else null, f)
        Session.editorFood = null; Session.editorIsMine = false
        return f
    }

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        BackBar(if (isMine) "Editar mi alimento" else "Nuevo alimento", nav) {
            if (isMine) IconButton(onClick = { confirmDelete = true }) { Icon(Icons.Filled.Delete, "Borrar") }
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
            // Botón grande: leer la etiqueta automáticamente
            Column(
                Modifier.fillMaxWidth()
                    .shadow(8.dp, MaterialTheme.shapes.large, spotColor = ProteinC.copy(alpha = 0.6f))
                    .clip(MaterialTheme.shapes.large)
                    .background(Brush.linearGradient(listOf(Color(0xFF8B5CF6), Color(0xFF6366F1))))
                    .clickable(enabled = !reading) { shootLabel() }
                    .padding(16.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(48.dp).clip(MaterialTheme.shapes.small).background(Color.White.copy(alpha = 0.22f)), contentAlignment = Alignment.Center) {
                        if (reading) CircularProgressIndicator(Modifier.size(26.dp), color = Color.White, strokeWidth = 3.dp)
                        else Text("📸", fontSize = 26.sp)
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(if (reading) "Leyendo la etiqueta…" else "Rellenar con una foto", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                        Text("Haz una foto a la tabla nutricional y relleno los valores yo", color = Color.White.copy(alpha = 0.9f), style = Caption)
                    }
                }
            }
            TextButton(onClick = { pickLabel.launch(androidx.activity.result.PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }, enabled = !reading) {
                Text("…o elegir una foto de la galería")
            }
            labelMsg?.let { msg ->
                Text(
                    msg,
                    color = if (labelOk) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSecondaryContainer,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.fillMaxWidth().clip(MaterialTheme.shapes.medium)
                        .background(if (labelOk) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer)
                        .padding(12.dp),
                )
            }
            Spacer(Modifier.height(12.dp))
            AppCard(Modifier.fillMaxWidth()) {
            Text("Producto", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(contentAlignment = Alignment.Center) {
                    FoodAvatar(name.ifBlank { "?" }, imageUrl, 96.dp)
                    if (imageBusy) CircularProgressIndicator(Modifier.size(32.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilledTonalButton(onClick = { shootPhoto() }, Modifier.weight(1f), contentPadding = PaddingValues(horizontal = 8.dp)) { Text("📷 Foto") }
                        FilledTonalButton(
                            onClick = { pickPhoto.launch(androidx.activity.result.PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                            Modifier.weight(1f), contentPadding = PaddingValues(horizontal = 8.dp),
                        ) { Text("🖼️ Galería") }
                    }
                    Button(
                        onClick = { showImageSearch = true },
                        enabled = name.isNotBlank() || barcode.isNotBlank(),
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("🌐 Buscar foto online") }
                    if (imageUrl != null) TextButton(onClick = { imageUrl = null }, contentPadding = PaddingValues(0.dp)) { Text("Quitar foto") }
                }
            }
            Spacer(Modifier.height(4.dp))
            OutlinedTextField(name, { name = it }, label = { Text("Nombre *") }, modifier = Modifier.fillMaxWidth(), singleLine = true, shape = MaterialTheme.shapes.medium)
            OutlinedTextField(brand, { brand = it }, label = { Text("Marca") }, modifier = Modifier.fillMaxWidth(), singleLine = true, shape = MaterialTheme.shapes.medium)
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    barcode, { barcode = it.filter(Char::isDigit) }, label = { Text("Código de barras") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f), singleLine = true, shape = MaterialTheme.shapes.medium,
                )
                Spacer(Modifier.width(8.dp))
                FilledTonalButton(onClick = { startScan() }, modifier = Modifier.padding(top = 8.dp).height(56.dp)) { Text("📷 Escanear") }
            }
            }
            Spacer(Modifier.height(12.dp))
            AppCard(Modifier.fillMaxWidth()) {
            Text("Envase", style = MaterialTheme.typography.titleMedium)
            Row {
                FilterChip(unit == "g", { unit = "g" }, label = { Text("Sólido (g)") }); Spacer(Modifier.width(8.dp))
                FilterChip(unit == "ml", { unit = "ml" }, label = { Text("Bebida (ml)") })
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Num("Envase", pkg, { pkg = it }, unit, Modifier.weight(1f))
                Num("Ración", serving, { serving = it }, unit, Modifier.weight(1f))
            }
            Text("Ej.: paquete de 100 g, lata de 500 ml. La ración es opcional (\"por galleta: 12 g\").", style = Caption, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(12.dp))
            AppCard(Modifier.fillMaxWidth()) {
            Text("Información nutricional", style = MaterialTheme.typography.titleMedium)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Valores: ", style = MaterialTheme.typography.bodyMedium)
                FilterChip(!perServing, { perServing = false }, label = { Text("por 100 $unit") }); Spacer(Modifier.width(8.dp))
                FilterChip(perServing, { perServing = true }, label = { Text("por ración") })
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Num("Energía *", fields["kcal"], { fields["kcal"] = it }, "kcal", Modifier.weight(1f))
                Num("Proteínas", fields["protein"], { fields["protein"] = it }, "g", Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Num("Grasas", fields["fat"], { fields["fat"] = it }, "g", Modifier.weight(1f))
                Num("de ellas saturadas", fields["sat"], { fields["sat"] = it }, "g", Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Num("Hidratos", fields["carbs"], { fields["carbs"] = it }, "g", Modifier.weight(1f))
                Num("de ellos azúcares", fields["sugar"], { fields["sugar"] = it }, "g", Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Num("Fibra", fields["fiber"], { fields["fiber"] = it }, "g", Modifier.weight(1f))
                Num("Sal", fields["salt"], { fields["salt"] = it }, "g", Modifier.weight(1f))
            }
            Text("Si la etiqueta solo da kJ, divide entre 4,184 para obtener kcal.", style = Caption, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(12.dp))
            AppCard(Modifier.fillMaxWidth()) {
            Text("Al añadirlo, poner por defecto", style = MaterialTheme.typography.titleMedium)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DefaultQty.entries.forEach { m ->
                    val extra = when (m) {
                        DefaultQty.PACKAGE -> pkgG?.let { " (${fmt(it, 0)} $unit)" } ?: ""
                        DefaultQty.SERVING -> servingG?.let { " (${fmt(it, 0)} $unit)" } ?: ""
                        DefaultQty.CUSTOM -> ""
                    }
                    FilterChip(defaultMode == m, { defaultMode = m }, label = { Text(m.label + extra) })
                }
            }
            if (defaultMode == DefaultQty.CUSTOM) Num("Cantidad por defecto", customQty, { customQty = it }, unit, Modifier.fillMaxWidth())
            }
            Spacer(Modifier.height(16.dp))
        }
        problem?.let { Text(it, color = Over, modifier = Modifier.padding(horizontal = 16.dp)) }
        Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = { save(); nav.popBackStack() }, enabled = valid, modifier = Modifier.weight(1f).height(52.dp)) {
                Text("Guardar")
            }
            Button(
                onClick = {
                    val f = save()
                    Session.editingEntry = null
                    Session.pendingFood = f
                    Session.pendingGrams = null
                    nav.navigate("detail") { popUpTo("custom") { inclusive = true } }
                },
                enabled = valid, modifier = Modifier.weight(1f).height(52.dp),
            ) { Text("Guardar y añadir", fontWeight = FontWeight.Bold) }
        }
    }

    if (showImageSearch) {
        ImageSearchDialog(
            name = name, brand = brand.ifBlank { null }, barcode = barcode.ifBlank { null },
            onDismiss = { showImageSearch = false },
            onPick = { url ->
                showImageSearch = false
                imageUrl = url // se ve al momento; en segundo plano se guarda una copia en el móvil
                imageBusy = true
                scope.launch { try { imageUrl = ImageStore.download(ctx, url) } finally { imageBusy = false } }
            },
        )
    }

    if (scanning) {
        Dialog(onDismissRequest = { scanning = false }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
            Column(Modifier.fillMaxWidth(0.92f).clip(MaterialTheme.shapes.large).background(MaterialTheme.colorScheme.surfaceContainerLowest)) {
                Box(Modifier.fillMaxWidth().height(280.dp).background(Color.Black)) {
                    CameraBarcodePreview(enabled = true) { c -> barcode = c; scanning = false }
                }
                Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Apunta al código de barras", Modifier.weight(1f))
                    TextButton(onClick = { scanning = false }) { Text("Cancelar") }
                }
            }
        }
    }

    if (confirmDelete && original != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("¿Borrar «${original.name}»?") },
            text = { Text("Se quitará de Mis alimentos. Lo que ya tengas apuntado en el diario no cambia.") },
            confirmButton = {
                TextButton(onClick = {
                    Repository.deleteCustomFood(original); Session.editorFood = null; Session.editorIsMine = false
                    confirmDelete = false; nav.popBackStack()
                }) { Text("Borrar") }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancelar") } },
        )
    }
}

@Composable
private fun Num(label: String, value: String?, onChange: (String) -> Unit, suffix: String, modifier: Modifier) {
    OutlinedTextField(
        value ?: "", { onChange(it.filter { c -> c.isDigit() || c == ',' || c == '.' }) },
        label = { Text(label) }, suffix = { Text(suffix) }, singleLine = true, shape = MaterialTheme.shapes.medium,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = modifier,
    )
}

/** Cuadrícula de fotos de producto encontradas en Open Food Facts para elegir una. */
@Composable
private fun ImageSearchDialog(name: String, brand: String?, barcode: String?, onDismiss: () -> Unit, onPick: (String) -> Unit) {
    var results by remember { mutableStateOf<List<OpenFoodFacts.ImageCandidate>?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) {
        try { results = OpenFoodFacts.imageCandidates(name, brand, barcode) }
        catch (e: Exception) { error = "No se pudo buscar. ¿Tienes internet?" }
    }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            Modifier.fillMaxWidth(0.94f).fillMaxHeight(0.8f).clip(MaterialTheme.shapes.large)
                .background(MaterialTheme.colorScheme.surfaceContainerLowest).padding(16.dp)
        ) {
            Text("Elige la foto", style = MaterialTheme.typography.titleLarge)
            Text(
                "Fotos de Open Food Facts para «${listOfNotNull(name.ifBlank { null }, brand).joinToString(" ")}»",
                style = Caption, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                val list = results
                when {
                    error != null -> Text(error!!, color = Over)
                    list == null -> CircularProgressIndicator()
                    list.isEmpty() -> Text("No he encontrado fotos. Prueba con otro nombre o haz tú la foto.", textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    else -> androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
                        columns = androidx.compose.foundation.lazy.grid.GridCells.Fixed(3),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        items(list.size) { i ->
                            val c = list[i]
                            Column(Modifier.clip(MaterialTheme.shapes.medium).clickable { onPick(c.url) }) {
                                coil.compose.AsyncImage(
                                    c.url, c.title,
                                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                    modifier = Modifier.fillMaxWidth().aspectRatio(1f).clip(MaterialTheme.shapes.medium).background(Color.White),
                                )
                                Text(c.title, style = Caption, maxLines = 2, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
            }
            TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) { Text("Cerrar") }
        }
    }
}
