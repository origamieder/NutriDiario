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
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
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
import com.nutridiario.app.*
import kotlinx.coroutines.launch
import java.time.LocalTime
import java.util.concurrent.Executors

/**
 * Cantidad con la que se puede añadir un producto sin preguntar, o null si no hay forma fiable
 * de saberla (entonces se abre la ficha para que la indiques).
 */
fun confidentAmount(food: FoodItem): Double? =
    food.defaultG ?: Repository.lastAmountFor(food) ?: food.defaultAmount().takeIf {
        (food.packageG != null && it == food.packageG) || (food.portionG != null && it == food.portionG)
    }

@Composable
fun BarcodeScreen(nav: NavController) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var hasPerm by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(ctx, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }
    val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { hasPerm = it }
    LaunchedEffect(Unit) { if (!hasPerm) permLauncher.launch(Manifest.permission.CAMERA) }

    var quickAdd by remember { mutableStateOf(true) }
    var code by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var notFound by remember { mutableStateOf<FoodItem?>(null) }
    var manual by remember { mutableStateOf("") }
    var added by remember { mutableStateOf<FoodEntry?>(null) }
    var addedCount by remember { mutableIntStateOf(0) }
    var lastCode by remember { mutableStateOf("") }
    var lastTime by remember { mutableLongStateOf(0L) }
    val meal = Session.preferredMeal ?: Meal.byTime(LocalTime.now().hour)

    fun openDetail(food: FoodItem) {
        Session.editingEntry = null; Session.pendingFood = food; Session.pendingGrams = null
        nav.navigate("detail")
    }

    fun handle(food: FoodItem) {
        val amount = confidentAmount(food)
        if (quickAdd && amount != null) {
            Repository.addEntry(Session.date.toString(), meal, amount, food)
            added = Repository.data.value.entries.maxByOrNull { it.id }
            addedCount++
        } else openDetail(food)
    }

    fun lookup(c: String) {
        if (loading) return
        val now = System.currentTimeMillis()
        if (c == lastCode && now - lastTime < 4000) return // evita añadir dos veces el mismo producto
        lastCode = c; lastTime = now
        code = c; loading = true; error = null
        scope.launch {
            try {
                val mine = Repository.customByBarcode(c)
                if (mine != null) { handle(mine); return@launch }
                when (val r = OpenFoodFacts.byBarcode(c)) {
                    is OpenFoodFacts.BarcodeResult.Found -> handle(r.food)
                    is OpenFoodFacts.BarcodeResult.Incomplete -> notFound = r.partial
                    OpenFoodFacts.BarcodeResult.NotFound ->
                        notFound = FoodItem(name = "", per100 = Nutrients(), source = "Mis alimentos", barcode = c)
                }
            } catch (e: Exception) {
                error = "No se pudo consultar el producto. ¿Tienes internet?\n${e.message ?: ""}"
            } finally {
                loading = false
            }
        }
    }

    Column(Modifier.fillMaxSize().background(Color.Black)) {
        Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
            if (hasPerm) {
                CameraBarcodePreview(enabled = !loading && notFound == null && error == null) { lookup(it) }
                ScanFrame()
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                    Text("📷", style = MaterialTheme.typography.displayMedium)
                    Text("Hace falta permiso de cámara para escanear", color = Color.White)
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = { permLauncher.launch(Manifest.permission.CAMERA) }) { Text("Dar permiso") }
                }
            }
            // barra superior
            Row(
                Modifier.align(Alignment.TopCenter).fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.6f), Color.Transparent)))
                    .padding(horizontal = 8.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = { if (addedCount > 0) goToday(nav) else nav.popBackStack() }, colors = ButtonDefaults.textButtonColors(contentColor = Color.White)) {
                    Text(if (addedCount > 0) "✓ Listo ($addedCount)" else "✕ Cerrar", fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.weight(1f))
                Text("${meal.emoji} ${meal.label}", color = Color.White, modifier = Modifier.padding(end = 12.dp))
            }
            if (loading) {
                Row(
                    Modifier.align(Alignment.BottomCenter).padding(16.dp).clip(MaterialTheme.shapes.large)
                        .background(MaterialTheme.colorScheme.surfaceContainerLowest).padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) { CircularProgressIndicator(Modifier.size(22.dp)); Spacer(Modifier.width(12.dp)); Text("Buscando $code…") }
            }
            androidx.compose.animation.AnimatedVisibility(visible = added != null && !loading, modifier = Modifier.align(Alignment.BottomCenter), enter = slideInVertically { it }) {
                added?.let { e ->
                    AddedCard(
                        e,
                        onUndo = { Repository.deleteEntry(e.id); added = null; addedCount--; lastCode = "" },
                        onChange = {
                            Session.editingEntry = e; Session.pendingFood = e.food; Session.pendingGrams = e.grams
                            nav.navigate("detail")
                        },
                    )
                }
            }
        }
        // panel inferior
        Column(
            Modifier.fillMaxWidth().clip(MaterialTheme.shapes.extraLarge.copy(bottomStart = androidx.compose.foundation.shape.CornerSize(0), bottomEnd = androidx.compose.foundation.shape.CornerSize(0)))
                .background(MaterialTheme.colorScheme.surfaceContainerLowest).padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("⚡ Añadir al instante", fontWeight = FontWeight.Bold)
                    Text(
                        if (quickAdd) "Se apunta solo con la cantidad del envase. Puedes escanear varios seguidos."
                        else "Al escanear se abre la ficha para elegir la cantidad.",
                        style = Caption, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(quickAdd, { quickAdd = it })
            }
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    manual, { manual = it.filter(Char::isDigit) }, Modifier.weight(1f),
                    placeholder = { Text("…o escribe el código") }, singleLine = true, shape = MaterialTheme.shapes.medium,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
                Spacer(Modifier.width(8.dp))
                Button(onClick = { lastCode = ""; lookup(manual) }, enabled = manual.length >= 8, modifier = Modifier.height(56.dp)) { Text("Buscar") }
            }
        }
    }

    error?.let { msg ->
        AlertDialog(
            onDismissRequest = { error = null; lastCode = "" },
            confirmButton = { TextButton(onClick = { error = null; lastCode = "" }) { Text("Reintentar") } },
            title = { Text("Sin conexión") }, text = { Text(msg) },
        )
    }
    notFound?.let { partial ->
        AlertDialog(
            onDismissRequest = { notFound = null; lastCode = "" },
            icon = { Text("📦", style = MaterialTheme.typography.headlineMedium) },
            title = { Text(if (partial.name.isNotBlank()) "Faltan los valores nutricionales" else "Producto no encontrado") },
            text = {
                Text(
                    if (partial.name.isNotBlank())
                        "«${partial.name}» está en Open Food Facts pero sin valores nutricionales. Haz una foto a la tabla nutricional y los relleno yo. Se guardará y la próxima vez que lo escanees se añadirá directamente."
                    else
                        "El código ${partial.barcode} no está en Open Food Facts. Haz una foto a la tabla nutricional y relleno los valores yo; se guardará para la próxima vez."
                )
            },
            confirmButton = {
                Button(onClick = {
                    notFound = null
                    Session.editorFood = partial; Session.editorIsMine = false; Session.editorAutoLabel = true
                    nav.navigate("custom")
                }) { Text("📸 Foto a la etiqueta") }
            },
            dismissButton = {
                TextButton(onClick = { notFound = null; nav.navigate("search") }) { Text("Buscar por nombre") }
            },
        )
    }
}

@Composable
private fun AddedCard(e: FoodEntry, onUndo: () -> Unit, onChange: () -> Unit) {
    Column(
        Modifier.padding(16.dp).fillMaxWidth().clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.surfaceContainerLowest).padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            FoodAvatar(e.food, 52.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("✅ Añadido a ${e.meal.label.lowercase()}", style = Caption, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                Text(e.food.name, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${fmt(e.grams, 0)} ${e.food.unit} · ${fmt(e.nutrients.kcal, 0)} kcal", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onUndo, modifier = Modifier.weight(1f)) { Text("Deshacer") }
            FilledTonalButton(onClick = onChange, modifier = Modifier.weight(1f)) { Text("Cambiar cantidad") }
        }
    }
}

/** Marco del visor con esquinas y una línea de escaneo animada. */
@Composable
private fun ScanFrame() {
    val t = rememberInfiniteTransition(label = "scan")
    val pos by t.animateFloat(0.1f, 0.9f, infiniteRepeatable(tween(1600, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "line")
    Canvas(Modifier.fillMaxWidth(0.82f).height(190.dp)) {
        val c = 30.dp.toPx(); val w = 5.dp.toPx(); val col = Color.White
        fun corner(x: Float, y: Float, dx: Float, dy: Float) {
            drawLine(col, Offset(x, y), Offset(x + dx * c, y), w, StrokeCap.Round)
            drawLine(col, Offset(x, y), Offset(x, y + dy * c), w, StrokeCap.Round)
        }
        corner(0f, 0f, 1f, 1f); corner(size.width, 0f, -1f, 1f)
        corner(0f, size.height, 1f, -1f); corner(size.width, size.height, -1f, -1f)
        val y = size.height * pos
        drawLine(
            Brush.horizontalGradient(listOf(Color.Transparent, GreenLight, Color.Transparent)),
            Offset(12.dp.toPx(), y), Offset(size.width - 12.dp.toPx(), y), 3.dp.toPx(),
        )
    }
}

@OptIn(ExperimentalGetImage::class)
@Composable
fun CameraBarcodePreview(enabled: Boolean, onCode: (String) -> Unit) {
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
