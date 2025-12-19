package com.janerli.delishhub.core.share

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider

object ShareHelper {

    fun sharePdf(
        context: Context,
        file: java.io.File,
        chooserTitle: String = "Поделиться"
    ) {
        val uri: Uri = FileProvider.getUriForFile(
            context,
            context.packageName + ".fileprovider",
            file
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        context.startActivity(Intent.createChooser(intent, chooserTitle))
    }
}
