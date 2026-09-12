package com.mikhilnaika.continueapp.core.share

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.core.content.FileProvider
import com.mikhilnaika.continueapp.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Every outbound `ACTION_SEND` in the app, in one place.
 *
 * One owner rather than an `Intent` built at each call site, for the reason the HAPTICS bug
 * taught this codebase the hard way: a rule that lives at the call sites is a rule that holds
 * only at the call sites somebody remembered. The rule here is **every share carries a real,
 * tappable link** — the old share sent an image and nothing else, with a dead
 * `continue.app/pile` painted onto the pixels. Routing all of it through here makes a
 * link-less share something you'd have to go out of your way to write.
 */
@Singleton
class ShareLauncher @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    /** Text-only — a game recommendation, where the link's own unfurl supplies the artwork. */
    fun shareText(message: String) {
        launch(
            Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, message)
            }
        )
    }

    /** A rendered card, plus the message that makes it actionable. */
    suspend fun shareImage(bitmap: Bitmap, fileName: String, message: String) {
        val file = withContext(Dispatchers.IO) {
            File(context.cacheDir, "share").apply { mkdirs() }.resolve("$fileName.png").also { f ->
                FileOutputStream(f).use { out -> bitmap.compress(Bitmap.CompressFormat.PNG, 100, out) }
            }
        }
        val uri = FileProvider.getUriForFile(context, "${BuildConfig.APPLICATION_ID}.fileprovider", file)

        launch(
            Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_TEXT, message)
                // Some targets read the URI off the ClipData rather than the extra, and the
                // read grant only reliably covers a URI the system can actually see — which it
                // can't while the URI is buried inside an extra. Without this a share arrives
                // with no image at all in a handful of apps, silently.
                clipData = ClipData.newUri(context.contentResolver, "CONTINUE? card", uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        )
    }

    private fun launch(intent: Intent) {
        // The application Context is not an Activity, so the chooser has no task to be pushed
        // onto and the platform refuses to start it without this.
        context.startActivity(Intent.createChooser(intent, "Share").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    }
}
