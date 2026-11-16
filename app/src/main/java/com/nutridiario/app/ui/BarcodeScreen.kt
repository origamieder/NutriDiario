package com.nutridiario.app.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavController
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.nutridiario.app.OpenFoodFacts
import com.nutridiario.app.Session
import kotlinx.coroutines.launch
import java.util.concurrent.Executors

@Composable
fun BarcodeScreen(nav: NavController) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var hasPerm by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(ctx, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }
    val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { hasPerm = it }
    LaunchedEffect(Unit) { if (!hasPerm) permLauncher.launch(Manifest.permission.CAMERA) }

    var code by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var notFound by remember { mutableStateOf<String?>(null) }
    var manual by remember { mutableStateOf("") }

    fun lookup(c: String) {
        if (loading) return
        code = c; loading = true; error = null
        scope.launch {
            try {
                val food = OpenFoodFacts.byBarcode(c)
                if (food != null) {
                    Session.pendingFood = food
                    Session.pendingGrams = null
                    nav.navigate("detail") { popUpTo("barcode") { inclusive = true } }
                } else notFound = c
            } catch (e: Exception) {
                error = "No se pudo consultar el producto. ¿Tienes internet?\n${e.message ?: ""}"
            } finally {
                loading = false
            }
        }
    }

    Column(Modifier.fillMaxSize()) {
        BackBar("Escanear código de barras", nav)
        Box(Modifier.fillMaxWidth().weight(1f).background(Color.Black), contentAlignment = Alignment.Center) {
            if (hasPerm) {
                CameraBarcodePreview(enabled = !loading && notFound == null && error == null) { lookup(it) }
                Box(Modifier.fillMaxWidth(0.8f).height(160.dp).border(3.dp, Color.White.copy(alpha = 0.8f), MaterialTheme.shapes.medium))
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                    Text("Hace falta permiso de cámara para escanear", color = Color.White)
                    Button(onClick = { permLauncher.launch(Manifest.permission.CAMERA) }) { Text("Dar permiso") }
                }
            }
            if (loading) {
                Card(Modifier.align(Alignment.BottomCenter).padding(16.dp)) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(Modifier.size(24.dp)); Spacer(Modifier.width(12.dp)); Text("Buscando $code…")
                    }
                }
            }
        }
        Column(Modifier.padding(16.dp)) {
            Text("Apunta al código de barras del envase. También puedes escribirlo:", style = MaterialTheme.typography.bodyMedium)
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    manual, { manual = it.filter(Char::isDigit) }, Modifier.weight(1f),
                    label = { Text("Código (EAN)") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
                Spacer(Modifier.width(8.dp))
                Button(onClick = { lookup(manual) }, enabled = manual.length >= 8) { Text("Buscar") }
            }
        }
    }

    error?.let { msg ->
        AlertDialog(
            onDismissRequest = { error = null },
            confirmButton = { TextButton(onClick = { error = null }) { Text("Reintentar") } },
            title = { Text("Error") }, text = { Text(msg) },
        )
    }
    notFound?.let { c ->
        AlertDialog(
            onDismissRequest = { notFound = null },
            title = { Text("Producto no encontrado") },
            text = { Text("El código $c no está en Open Food Facts. Puedes crearlo tú copiando los valores de la etiqueta nutricional, o buscarlo por nombre.") },
            confirmButton = {
                TextButton(onClick = { notFound = null; Session.manualText = c; nav.navigate("custom") { popUpTo("barcode") { inclusive = true } } }) {
                    Text("Crear producto")
                }
            },
            dismissButton = {
                TextButton(onClick = { notFound = null; nav.navigate("search") { popUpTo("barcode") { inclusive = true } } }) { Text("Buscar por nombre") }
            },
        )
    }
}

@OptIn(ExperimentalGetImage::class)
@Composable
private fun CameraBarcodePreview(enabled: Boolean, onCode: (String) -> Unit) {
    val ctx = LocalContext.current
    val owner = LocalLifecycleOwner.current
    val currentEnabled by rememberUpdatedState(enabled)
    val currentOnCode by rememberUpdatedState(onCode)
    val executor = remember { Executors.newSingleThreadExecutor() }
    val scanner: BarcodeScanner = remember {
        BarcodeScanning.getClient(
            BarcodeScannerOptions.Builder().setBarcodeFormats(
                Barcode.FORMAT_EAN_13, Barcode.FORMAT_EAN_8, Barcode.FORMAT_UPC_A, Barcode.FORMAT_UPC_E
            ).build()
        )
    }
    var provider by remember { mutableStateOf<ProcessCameraProvider?>(null) }
    DisposableEffect(Unit) {
        onDispose { provider?.unbindAll(); scanner.close(); executor.shutdown() }
    }
    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { c ->
            val view = PreviewView(c)
            val future = ProcessCameraProvider.getInstance(c)
            future.addListener({
                val p = future.get()
                provider = p
                val preview = Preview.Builder().build().also { it.setSurfaceProvider(view.surfaceProvider) }
                val analysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build()
                analysis.setAnalyzer(executor) { proxy ->
                    val media = proxy.image
                    if (media == null || !currentEnabled) { proxy.close(); return@setAnalyzer }
                    scanner.process(InputImage.fromMediaImage(media, proxy.imageInfo.rotationDegrees))
                        .addOnSuccessListener { codes ->
                            codes.firstOrNull { !it.rawValue.isNullOrBlank() }?.rawValue?.let { v ->
                                if (currentEnabled) ContextCompat.getMainExecutor(ctx).execute { currentOnCode(v) }
                            }
                        }
                        .addOnCompleteListener { proxy.close() }
                }
                p.unbindAll()
                p.bindToLifecycle(owner, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis)
            }, ContextCompat.getMainExecutor(c))
            view
        },
    )
}
