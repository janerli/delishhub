package com.janerli.delishhub.core.media

import android.content.ContentResolver
import android.graphics.Bitmap
import android.net.Uri
import java.io.File
import java.io.FileOutputStream

object ImageStorage {

    /**
     * Копирует Uri (content://...) из галереи в filesDir приложения.
     * Возвращает file:// Uri, который удобно хранить в Room.
     */
    fun copyFromUriToInternal(
        filesDir: File,
        contentResolver: ContentResolver,
        sourceUri: Uri,
        recipeId: String
    ): Uri? {
        return runCatching {
            val dir = File(filesDir, "recipe_images").apply { mkdirs() }
            val file = File(dir, "recipe_${recipeId}_main.jpg")

            contentResolver.openInputStream(sourceUri)?.use { input ->
                FileOutputStream(file).use { output ->
                    input.copyTo(output)
                }
            } ?: return@runCatching null

            Uri.fromFile(file)
        }.getOrNull()
    }

    /**
     * Сохраняет Bitmap (с камеры через TakePicturePreview) в filesDir приложения.
     * Возвращает file:// Uri.
     */
    fun saveBitmapToInternal(
        filesDir: File,
        bitmap: Bitmap,
        recipeId: String
    ): Uri? {
        return runCatching {
            val dir = File(filesDir, "recipe_images").apply { mkdirs() }
            val file = File(dir, "recipe_${recipeId}_main.jpg")

            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
            }

            Uri.fromFile(file)
        }.getOrNull()
    }
}
