package com.nutridiario.app.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.exifinterface.media.ExifInterface
import androidx.navigation.NavController
import com.nutridiario.app.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun PhotoScreen(nav: NavController) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    var preds by remember { mutableStateOf<List<Prediction>?>(null) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val photoUri = remember {
        val dir = File(ctx.cacheDir, "photos").apply { mkdirs() }
        FileProvider.getUriForFile(ctx, "${ctx.packageName}.fileprovider", File(dir, "comida.jpg"))
    }

    fun analyze(uri: Uri) {
        busy = true; preds = null; error = null
        scope.launch {
            try {
                val bmp = withContext(Dispatchers.IO) { loadBitmap(ctx, uri) }
                bitmap = bmp
                preds = withContext(Dispatchers.Default) { FoodClassifier.classify(ctx, bmp) }
            } catch (e: Exception) {
                error = "No se pudo analizar la imagen: ${e.message}"
            } finally { busy = false }
        }
    }

    val takePicture = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok -> if (ok) analyze(photoUri) }
    val pickImage = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri -> uri?.let { analyze(it) } }
    val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) takePicture.launch(photoUri) else error = "Sin permiso de cámara. Puedes elegir una foto de la galería."
    }
    fun shoot() {
        if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) takePicture.launch(photoUri)
        else permLauncher.launch(Manifest.permission.CAMERA)
    }
    LaunchedEffect(Unit) { if (bitmap == null) shoot() }

    Column(Modifier.fillMaxSize()) {
        BackBar("Reconocer con foto", nav)
        LazyColumn(Modifier.weight(1f).padding(horizontal = 16.dp)) {
            item {
                bitmap?.let {
                    Image(
                        it.asImageBitmap(), null, contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxWidth().height(240.dp).clip(MaterialTheme.shapes.large),
                    )
                }
                Row(Modifier.padding(vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(onClick = { shoot() }, Modifier.weight(1f)) { Text("📷 Hacer foto") }
                    OutlinedButton(onClick = {
                        pickImage.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    }, Modifier.weight(1f)) { Text("🖼️ Galería") }
                }
                if (busy) Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(Modifier.size(24.dp)); Spacer(Modifier.width(12.dp)); Text("Analizando en el móvil…")
                }
                error?.let { Text(it, color = Over) }
            }
            preds?.let { list ->
                val foods = list.filter { it.food != null }
                val others = list.filter { it.food == null }
                item {
                    if (foods.isEmpty()) {
                        Text(
                            "La IA no ha identificado ningún alimento con seguridad. Prueba con una foto más cercana y con buena luz, o dime qué es.",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    } else SectionTitle("¿Qué es? Toca la opción correcta")
                }
                items(foods) { p ->
                    val f = p.food!!
                    FoodRow(
                        f, onClick = {
                            Session.pendingFood = f.copy(source = "Foto (IA local)")
                            Session.pendingGrams = f.portionG
                            nav.navigate("detail")
                        },
                        subtitle = "${fmt(f.per100.kcal, 0)} kcal/100 g · ración ${fmt(f.portionG ?: 100.0, 0)} g",
                        value = "${fmt(p.score * 100.0, 0)} %", trailing = "seguro",
                    )
                }
                item {
                    if (others.isNotEmpty()) {
                        Text(
                            "Otras cosas detectadas: " + others.joinToString { "${it.label} (${fmt(it.score * 100.0, 0)} %)" },
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.padding(vertical = 8.dp),
                        )
                    }
                    OutlinedButton(onClick = { Session.manualText = ""; nav.navigate("manual") }, Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                        Text("No es ninguno: decir qué es y cuánto pesa")
                    }
                    Text(
                        "El modelo de IA (EfficientNet-Lite, Google) va dentro de la app y funciona sin internet. Reconoce bien platos y alimentos comunes (pizza, hamburguesa, fruta, verdura, pasta…); para platos caseros complejos es más fiable decírselo.",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.padding(bottom = 16.dp),
                    )
                }
            }
        }
    }
}

private fun loadBitmap(ctx: Context, uri: Uri, maxSide: Int = 1024): Bitmap {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    ctx.contentResolver.openInputStream(uri).use { BitmapFactory.decodeStream(it, null, bounds) }
    var sample = 1
    while (bounds.outWidth / (sample * 2) >= maxSide && bounds.outHeight / (sample * 2) >= maxSide) sample *= 2
    val opts = BitmapFactory.Options().apply { inSampleSize = sample; inPreferredConfig = Bitmap.Config.ARGB_8888 }
    val bmp = ctx.contentResolver.openInputStream(uri).use { BitmapFactory.decodeStream(it, null, opts) }
        ?: throw IllegalStateException("imagen no válida")
    val rotation = runCatching {
        ctx.contentResolver.openInputStream(uri).use { s ->
            when (ExifInterface(s!!).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else -> 0f
            }
        }
    }.getOrDefault(0f)
    return if (rotation == 0f) bmp
    else Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, Matrix().apply { postRotate(rotation) }, true)
}
