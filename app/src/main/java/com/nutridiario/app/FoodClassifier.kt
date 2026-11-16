package com.nutridiario.app

import android.content.Context
import android.graphics.Bitmap
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.imageclassifier.ImageClassifier

data class Prediction(val label: String, val score: Float, val food: FoodItem?)

/**
 * Reconocimiento de alimentos en fotos con un modelo de IA que va DENTRO de la app
 * (assets/food_model.tflite) y se ejecuta en el móvil, sin conexión.
 */
object FoodClassifier {
    private var classifier: ImageClassifier? = null

    private fun get(context: Context): ImageClassifier = classifier ?: ImageClassifier.createFromOptions(
        context,
        ImageClassifier.ImageClassifierOptions.builder()
            .setBaseOptions(BaseOptions.builder().setModelAssetPath("food_model.tflite").build())
            .setRunningMode(RunningMode.IMAGE)
            .setMaxResults(25)
            .build()
    ).also { classifier = it }

    /** Devuelve primero las predicciones que son comida conocida, luego el resto. */
    @Synchronized
    fun classify(context: Context, bitmap: Bitmap): List<Prediction> {
        val bmp = if (bitmap.config == Bitmap.Config.ARGB_8888) bitmap else bitmap.copy(Bitmap.Config.ARGB_8888, false)
        val result = get(context).classify(BitmapImageBuilder(bmp).build())
        val cats = result.classificationResult().classifications().firstOrNull()?.categories().orEmpty()
        val preds = cats.map { c ->
            Prediction(c.categoryName(), c.score(), LocalFoodDb.byAlias(c.categoryName()))
        }
        val foods = preds.filter { it.food != null }.distinctBy { it.food!!.name }
        return foods + preds.filter { it.food == null }.take(5)
    }
}
