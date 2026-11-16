package com.nutridiario.app

import android.content.Context
import android.graphics.Bitmap
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.imageclassifier.ImageClassifier
import kotlinx.serialization.json.Json
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel

data class Prediction(val label: String, val score: Float, val food: FoodItem?)

/**
 * Reconocimiento de alimentos en fotos con dos modelos de IA que van DENTRO de la app y se
 * ejecutan en el móvil, sin conexión:
 *  - food101.tflite: EfficientNet-B0 entrenado con Food-101 (101 platos preparados).
 *  - food_model.tflite: EfficientNet-Lite2 general (ImageNet), bueno con fruta y verdura.
 */
object FoodClassifier {
    private var general: ImageClassifier? = null
    private var dishes: Interpreter? = null
    private var dishLabels: List<String> = emptyList()
    private const val SIZE = 128

    private fun general(context: Context): ImageClassifier = general ?: ImageClassifier.createFromOptions(
        context,
        ImageClassifier.ImageClassifierOptions.builder()
            .setBaseOptions(BaseOptions.builder().setModelAssetPath("food_model.tflite").build())
            .setRunningMode(RunningMode.IMAGE)
            .setMaxResults(25)
            .build()
    ).also { general = it }

    private fun dishes(context: Context): Interpreter = dishes ?: run {
        val fd = context.assets.openFd("food101.tflite")
        val buf = FileInputStream(fd.fileDescriptor).channel.map(FileChannel.MapMode.READ_ONLY, fd.startOffset, fd.declaredLength)
        dishLabels = Json.decodeFromString(context.assets.open("food101_labels.json").bufferedReader().use { it.readText() })
        Interpreter(buf, Interpreter.Options().setNumThreads(4))
    }.also { dishes = it }

    /** Igual que en el entrenamiento: lado corto a 160 px, recorte central de 128 px, RGB en 0..1. */
    fun preprocess(bitmap: Bitmap): ByteBuffer {
        // reducir a la mitad varias veces evita perder detalle (como el "antialias" del entrenamiento)
        var src = bitmap
        while (minOf(src.width, src.height) >= 160 * 2) src = Bitmap.createScaledBitmap(src, src.width / 2, src.height / 2, true)
        val k = 160f / minOf(src.width, src.height)
        val scaled = Bitmap.createScaledBitmap(src, Math.round(src.width * k).coerceAtLeast(SIZE), Math.round(src.height * k).coerceAtLeast(SIZE), true)
        val crop = Bitmap.createBitmap(scaled, (scaled.width - SIZE) / 2, (scaled.height - SIZE) / 2, SIZE, SIZE)
        val px = IntArray(SIZE * SIZE); crop.getPixels(px, 0, SIZE, 0, 0, SIZE, SIZE)
        val buf = ByteBuffer.allocateDirect(SIZE * SIZE * 3 * 4).order(ByteOrder.nativeOrder())
        for (p in px) {
            buf.putFloat(((p shr 16) and 0xFF) / 255f); buf.putFloat(((p shr 8) and 0xFF) / 255f); buf.putFloat((p and 0xFF) / 255f)
        }
        buf.rewind()
        return buf
    }

    /** Devuelve primero las predicciones que son comida conocida (de los dos modelos), luego el resto. */
    @Synchronized
    fun classify(context: Context, bitmap: Bitmap): List<Prediction> {
        val bmp = if (bitmap.config == Bitmap.Config.ARGB_8888) bitmap else bitmap.copy(Bitmap.Config.ARGB_8888, false)

        // 1) platos (Food-101)
        val out = Array(1) { FloatArray(101) }
        runCatching { dishes(context).run(preprocess(bmp), out) }
        val dishPreds = out[0].withIndex().sortedByDescending { it.value }.take(8).mapNotNull { (i, p) ->
            dishLabels.getOrNull(i)?.let { Prediction(it.replace('_', ' '), p, LocalFoodDb.byAlias(it.replace('_', ' '))) }
        }

        // 2) modelo general (fruta, verdura, bebidas…)
        val cats = runCatching {
            general(context).classify(BitmapImageBuilder(bmp).build()).classificationResult().classifications().firstOrNull()?.categories().orEmpty()
        }.getOrDefault(emptyList())
        val generalPreds = cats.map { c -> Prediction(c.categoryName(), c.score(), LocalFoodDb.byAlias(c.categoryName())) }

        return merge(dishPreds, generalPreds)
    }

    /** Une las dos listas: por cada alimento se queda la mayor confianza; descarta propuestas muy improbables. */
    fun merge(dishPreds: List<Prediction>, generalPreds: List<Prediction>): List<Prediction> {
        val foods = (dishPreds + generalPreds).filter { it.food != null && it.score >= 0.02f }
            .groupBy { it.food!!.name }.map { (_, ps) -> ps.maxBy { it.score } }
            .sortedByDescending { it.score }.take(8)
        return foods + generalPreds.filter { it.food == null }.take(5)
    }
}
