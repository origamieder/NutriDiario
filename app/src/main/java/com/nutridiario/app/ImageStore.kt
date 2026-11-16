package com.nutridiario.app

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

/** Fotos de "Mis alimentos" guardadas dentro de la app (se ven sin internet). */
object ImageStore {
    private fun dir(ctx: Context) = File(ctx.filesDir, "food_images").apply { mkdirs() }

    /** Guarda una foto de la cámara o la galería, reducida, y devuelve su dirección "file://…". */
    suspend fun saveFromUri(ctx: Context, uri: Uri): String = withContext(Dispatchers.IO) {
        save(ctx, loadBitmap(ctx, uri, 720))
    }

    /** Descarga una foto de internet y la guarda en el móvil. Si falla, devuelve la URL tal cual. */
    suspend fun download(ctx: Context, url: String): String = withContext(Dispatchers.IO) {
        runCatching {
            val c = URL(url).openConnection() as HttpURLConnection
            c.connectTimeout = 15000; c.readTimeout = 20000
            c.setRequestProperty("User-Agent", "NutriDiario/1.0 (Android)")
            val bytes = c.inputStream.use { it.readBytes() }
            c.disconnect()
            val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: error("imagen no válida")
            save(ctx, scale(bmp, 720))
        }.getOrDefault(url)
    }

    /** Borra la foto si es un fichero propio de la app. */
    fun deleteIfLocal(ctx: Context, imageUrl: String?) {
        val path = imageUrl?.removePrefix("file://") ?: return
        val f = File(path)
        if (f.parentFile == dir(ctx)) f.delete()
    }

    private fun save(ctx: Context, bmp: Bitmap): String {
        val f = File(dir(ctx), UUID.randomUUID().toString() + ".jpg")
        f.outputStream().use { bmp.compress(Bitmap.CompressFormat.JPEG, 85, it) }
        return "file://" + f.absolutePath
    }

    private fun scale(bmp: Bitmap, maxSide: Int): Bitmap {
        val m = maxOf(bmp.width, bmp.height)
        if (m <= maxSide) return bmp
        val k = maxSide.toFloat() / m
        return Bitmap.createScaledBitmap(bmp, (bmp.width * k).toInt(), (bmp.height * k).toInt(), true)
    }

    /** Carga una imagen reducida y girada según su EXIF. */
    fun loadBitmap(ctx: Context, uri: Uri, maxSide: Int = 1024): Bitmap {
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
        val rotated = if (rotation == 0f) bmp
        else Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, Matrix().apply { postRotate(rotation) }, true)
        return scale(rotated, maxSide)
    }
}
