package com.nutridiario.app.screens

import android.graphics.BitmapFactory
import com.nutridiario.app.FoodClassifier
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/**
 * Herramienta de verificación (no es un test normal): pasa fotos por el preprocesado REAL de la app
 * y guarda los tensores para comprobar con Python que el modelo acierta igual.
 * Solo se ejecuta si se define -Dpreprocess.in y -Dpreprocess.out.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
class PreprocessExport {
    @Test fun export() {
        val inDir = System.getProperty("preprocess.in"); val outDir = System.getProperty("preprocess.out")
        assumeTrue(inDir != null && outDir != null)
        File(inDir!!).listFiles()!!.filter { it.extension == "jpg" }.forEach { f ->
            val bmp = BitmapFactory.decodeFile(f.absolutePath)
            val buf = FoodClassifier.preprocess(bmp)
            val bytes = ByteArray(buf.remaining()); buf.get(bytes)
            File(outDir, f.nameWithoutExtension + ".bin").writeBytes(bytes)
        }
    }
}
