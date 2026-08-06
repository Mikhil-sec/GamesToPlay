package com.mikhilnaika.continueapp.core.share

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import androidx.core.content.FileProvider
import com.mikhilnaika.continueapp.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

private const val CARD_SIZE_PX = 1080

/**
 * Renders share cards to a PNG and fires `ACTION_SEND` — docs/02-PRODUCT-SPEC.md §6 "SHARE".
 * Drawn with the platform `Canvas` directly rather than capturing live Compose UI: it's the
 * version-stable path (Compose's own layer-capture API is still in flux across BOM releases)
 * and keeps the card's exact pixels independent of whatever's on screen at share time.
 */
object ShareCardRenderer {

    /** "THE PILE" card — docs/02-PRODUCT-SPEC.md §6: "412 HOURS. 87 GAMES. SEND HELP." */
    fun renderPileCard(totalHours: Int, totalGames: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(CARD_SIZE_PX, CARD_SIZE_PX, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.parseColor("#101218")) // ContinueColors.SurfaceCabinet

        val wordmarkPaint = textPaint(size = 36f, color = "#F7C948", bold = true)
        canvas.drawText("CONTINUE?", 80f, 140f, wordmarkPaint)

        val statPaint = textPaint(size = 92f, color = "#F2F4F8", bold = true)
        canvas.drawText("$totalHours HOURS.", 80f, 480f, statPaint)
        canvas.drawText("$totalGames GAMES.", 80f, 590f, statPaint)

        val sendHelpPaint = textPaint(size = 72f, color = "#FF3D7F", bold = true)
        canvas.drawText("SEND HELP.", 80f, 720f, sendHelpPaint)

        val linkPaint = textPaint(size = 28f, color = "#5A6373", bold = false)
        canvas.drawText("continue.app/pile", 80f, CARD_SIZE_PX - 80f, linkPaint)

        return bitmap
    }

    private fun textPaint(size: Float, color: String, bold: Boolean): Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = size
        this.color = Color.parseColor(color)
        typeface = Typeface.create(Typeface.DEFAULT, if (bold) Typeface.BOLD else Typeface.NORMAL)
    }

    suspend fun share(context: Context, bitmap: Bitmap, fileName: String) {
        val file = withContext(Dispatchers.IO) {
            File(context.cacheDir, "share").apply { mkdirs() }.resolve("$fileName.png").also { f ->
                FileOutputStream(f).use { out -> bitmap.compress(Bitmap.CompressFormat.PNG, 100, out) }
            }
        }

        val uri = FileProvider.getUriForFile(context, "${BuildConfig.APPLICATION_ID}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(intent, "Share").apply {
            if (context !is android.app.Activity) addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }
}
