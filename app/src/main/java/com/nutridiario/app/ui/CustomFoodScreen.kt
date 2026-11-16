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
            imageUrl = original?.imageUrl,
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
            Row(
                Modifier.fillMaxWidth().clip(MaterialTheme.shapes.medium).background(MaterialTheme.colorScheme.primaryContainer).padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("📦", fontSize = 26.sp); Spacer(Modifier.width(12.dp))
                Text(
                    "Copia los datos del envoltorio. Se guardará en Mis alimentos y la próxima vez lo encontrarás buscándolo o escaneándolo.",
                    style = Caption, color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
            Spacer(Modifier.height(12.dp))
            AppCard(Modifier.fillMaxWidth()) {
            Text("Producto", style = MaterialTheme.typography.titleMedium)
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
