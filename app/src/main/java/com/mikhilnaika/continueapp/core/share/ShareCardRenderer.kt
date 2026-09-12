package com.mikhilnaika.continueapp.core.share

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import androidx.core.content.res.ResourcesCompat
import coil3.ImageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowHardware
import coil3.toBitmap
import com.mikhilnaika.continueapp.BuildConfig
import com.mikhilnaika.continueapp.R
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 4:5. The most-forwarded aspect ratio there is — it fills a phone screen in a chat thread
 * without being cropped by Instagram, and every messenger renders it whole.
 */
private const val CARD_W = 1080
private const val CARD_H = 1350

private const val MARGIN = 80f

// Straight from ContinueColors. Duplicated as ints because `android.graphics` predates Compose
// by a decade and takes packed ARGB, not `Color`. If the palette moves, move these too.
private const val VOID = 0xFF08090C.toInt()
private const val CABINET = 0xFF101218.toInt()
private const val OUTLINE = 0xFF242833.toInt()
private const val COIN = 0xFFF7C948.toInt()
private const val NEON = 0xFF00E5A0.toInt()
private const val HOT = 0xFFFF3D7F.toInt()
private const val TEXT_PRIMARY = 0xFFF2F4F8.toInt()
private const val TEXT_SECONDARY = 0xFF9AA3B2.toInt()
private const val TEXT_TERTIARY = 0xFF5A6373.toInt()

/**
 * Renders the share cards and hands them to `ACTION_SEND` — docs/02-PRODUCT-SPEC.md §6.
 *
 * Drawn with the platform [Canvas] rather than by capturing live Compose UI: it's the
 * version-stable path (Compose's own layer-capture API is still in flux across BOM releases)
 * and keeps the card's pixels independent of whatever happens to be on screen at share time.
 *
 * A `@Singleton` rather than an `object` because it now needs a [Context] — for the app's real
 * typefaces and for Coil's shared disk cache. That is not an accident of implementation: the
 * previous version was an `object`, and having no Context is precisely why it drew everything
 * in `Typeface.DEFAULT` (Roboto) while the app itself ships Chakra Petch, and why no card
 * could show a single piece of key art. The most-screenshotted artifact the app produces
 * looked like nothing else in the app.
 */
@Singleton
class ShareCardRenderer @Inject constructor(
    @ApplicationContext private val context: Context,
    private val imageLoader: ImageLoader,
    private val launcher: ShareLauncher,
) {

    // Resolved once and reused. `getFont` parses and caches, but these are read dozens of
    // times per card and the lookup is not free.
    private val displayFace: Typeface by lazy { font(R.font.chakrapetch_bold, Typeface.BOLD) }
    private val displayMedium: Typeface by lazy { font(R.font.chakrapetch_semibold, Typeface.NORMAL) }
    private val monoFace: Typeface by lazy { font(R.font.jetbrainsmono_variable, Typeface.NORMAL) }

    private fun font(resId: Int, fallbackStyle: Int): Typeface =
        // A font resource that fails to load throws, and a share card is not worth crashing a
        // share over — the card is still readable in the platform default.
        runCatching { ResourcesCompat.getFont(context, resId) }.getOrNull()
            ?: Typeface.create(Typeface.DEFAULT, fallbackStyle)

    // ---------------------------------------------------------------- cards

    /**
     * CLEARED — docs/02-PRODUCT-SPEC.md §6, "achievement flex". The one people post.
     *
     * Sent from the Credits Roll, which is the entire point: the moment somebody finishes a
     * game is the moment they want to tell someone, and before this change that screen had no
     * share button at all.
     */
    suspend fun renderClearedCard(
        gameName: String,
        keyArtUrl: String?,
        coverUrl: String?,
        hours: Int?,
        allTimeRank: Int?,
    ): Bitmap = withContext(Dispatchers.Default) {
        val art = loadBitmap(keyArtUrl) ?: loadBitmap(coverUrl)
        val cover = loadBitmap(coverUrl)
        val bitmap = newCard()
        val canvas = Canvas(bitmap)

        drawKeyArtHeader(canvas, art)
        drawChrome(canvas)

        var y = 700f
        if (cover != null) {
            drawCover(canvas, cover, left = MARGIN, top = y - 300f, height = 300f)
            y += 40f
        }

        y = drawKicker(canvas, "CLEARED", NEON, y)
        y = drawTitle(canvas, gameName, y)

        val facts = buildList {
            if (hours != null && hours > 0) add("$hours HOURS")
            if (allTimeRank != null) add("#$allTimeRank OF ALL TIME")
        }
        if (facts.isNotEmpty()) drawFacts(canvas, facts.joinToString("   ·   "), y + 14f)

        drawFooter(canvas)
        drawScanlines(canvas)
        bitmap
    }

    /**
     * THE PILE — "412 HOURS. 87 GAMES. SEND HELP."
     *
     * The covers matter more than the number does. A count is abstract; a wall of box art for
     * games you own and have not finished is the joke landing, and it's what makes this the
     * card a stranger understands without reading the text.
     */
    suspend fun renderPileCard(
        totalHours: Int,
        totalGames: Int,
        coverUrls: List<String>,
    ): Bitmap = withContext(Dispatchers.Default) {
        val covers = coverUrls.take(GRID_COLUMNS * GRID_ROWS).mapNotNull { loadBitmap(it) }
        val bitmap = newCard()
        val canvas = Canvas(bitmap)

        drawCoverWall(canvas, covers)
        drawChrome(canvas)

        var y = 780f
        y = drawKicker(canvas, "THE PILE", COIN, y)

        val statPaint = paint(displayFace, 96f, TEXT_PRIMARY)
        canvas.drawText("$totalHours HOURS.", MARGIN, y + 96f, statPaint)
        canvas.drawText("$totalGames GAMES.", MARGIN, y + 200f, statPaint)
        canvas.drawText("SEND HELP.", MARGIN, y + 304f, paint(displayFace, 96f, HOT))

        drawFooter(canvas)
        drawScanlines(canvas)
        bitmap
    }

    /**
     * HIGH SCORES — docs/02-PRODUCT-SPEC.md §6: "extremely arguable → comments → reach".
     *
     * Laid out as an actual arcade high-score table in a monospaced face, because the joke only
     * works if the ranks line up in a column.
     */
    suspend fun renderHighScoresCard(entries: List<HighScoreLine>): Bitmap =
        withContext(Dispatchers.Default) {
            val bitmap = newCard()
            val canvas = Canvas(bitmap)

            // No key art here — a leaderboard belongs to no single game, and picking one
            // game's art to represent the whole list would be a claim the card isn't making.
            drawChrome(canvas)

            var y = drawKicker(canvas, "HIGH SCORES", COIN, 300f)
            y += 40f

            val rankPaint = paint(monoFace, 44f, COIN)
            val namePaint = paint(displayMedium, 44f, TEXT_PRIMARY)
            val hoursPaint = paint(monoFace, 36f, TEXT_TERTIARY).apply { textAlign = Paint.Align.RIGHT }

            entries.take(MAX_HIGH_SCORE_ROWS).forEachIndexed { index, entry ->
                val rank = index + 1
                rankPaint.color = medalColor(rank)
                canvas.drawText(rankLabel(rank), MARGIN, y, rankPaint)

                // Truncated to the column, not to the card — an over-long name would otherwise
                // run under the hours on the right and render as an unreadable overlap.
                val nameLeft = MARGIN + 130f
                val nameRight = CARD_W - MARGIN - 120f
                canvas.drawText(ellipsize(entry.name, namePaint, nameRight - nameLeft), nameLeft, y, namePaint)

                if (entry.hours != null && entry.hours > 0) {
                    canvas.drawText("${entry.hours}h", CARD_W - MARGIN, y, hoursPaint)
                }
                y += 78f
            }

            drawFooter(canvas)
            drawScanlines(canvas)
            bitmap
        }

    // ---------------------------------------------------------------- chrome

    private fun newCard(): Bitmap =
        Bitmap.createBitmap(CARD_W, CARD_H, Bitmap.Config.ARGB_8888).also { Canvas(it).drawColor(VOID) }

    /** Wordmark and the arcade-cabinet corner brackets. Every card gets both. */
    private fun drawChrome(canvas: Canvas) {
        val wordmark = paint(displayFace, 38f, COIN).apply { letterSpacing = 0.2f }
        canvas.drawText("CONTINUE?", MARGIN, 150f, wordmark)

        val bracket = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COIN
            style = Paint.Style.STROKE
            strokeWidth = 5f
            alpha = 110
        }
        val inset = 44f
        val arm = 56f
        val right = CARD_W - inset
        val bottom = CARD_H - inset
        // Four L-shapes rather than a rectangle: a full border reads as a frame around a
        // photo, four corners read as a machine.
        canvas.drawLines(
            floatArrayOf(
                inset, inset, inset + arm, inset, inset, inset, inset, inset + arm,
                right, inset, right - arm, inset, right, inset, right, inset + arm,
                inset, bottom, inset + arm, bottom, inset, bottom, inset, bottom - arm,
                right, bottom, right - arm, bottom, right, bottom, right, bottom - arm,
            ),
            bracket,
        )
    }

    /**
     * The CRT signature — 1px lines every 3px at very low alpha.
     *
     * Drawn **last**, over everything including the key art, because that's what makes it read
     * as a screen the whole card is displayed on rather than a texture layered onto one part
     * of it. Cheap: 450 hairlines in a single `drawLines` call.
     */
    private fun drawScanlines(canvas: Canvas) {
        val scanline = Paint().apply {
            color = Color.BLACK
            alpha = 38
            strokeWidth = 1f
        }
        val points = FloatArray((CARD_H / SCANLINE_PITCH) * 4)
        var i = 0
        var y = 0
        while (y < CARD_H) {
            points[i++] = 0f
            points[i++] = y.toFloat()
            points[i++] = CARD_W.toFloat()
            points[i++] = y.toFloat()
            y += SCANLINE_PITCH
        }
        canvas.drawLines(points, 0, i, scanline)
    }

    /** Key art bleeding from the top edge, dissolved into the background before the text starts. */
    private fun drawKeyArtHeader(canvas: Canvas, art: Bitmap?) {
        val height = 760
        if (art == null) {
            canvas.drawRect(0f, 0f, CARD_W.toFloat(), height.toFloat(), Paint().apply { color = CABINET })
        } else {
            drawCropped(canvas, art, RectF(0f, 0f, CARD_W.toFloat(), height.toFloat()))
        }
        // The fade is the whole trick: without it the art ends on a hard horizontal edge and
        // the card looks like two stacked images instead of one object.
        val fade = Paint().apply {
            shader = LinearGradient(
                0f, height * 0.25f, 0f, height.toFloat(),
                intArrayOf(Color.TRANSPARENT, VOID and 0x00FFFFFF or 0xCC000000.toInt(), VOID),
                floatArrayOf(0f, 0.65f, 1f),
                Shader.TileMode.CLAMP,
            )
        }
        canvas.drawRect(0f, 0f, CARD_W.toFloat(), height.toFloat(), fade)
    }

    /** THE PILE's backdrop: a grid of box art, dimmed hard so the headline stays readable. */
    private fun drawCoverWall(canvas: Canvas, covers: List<Bitmap>) {
        if (covers.isEmpty()) return
        val cellW = CARD_W.toFloat() / GRID_COLUMNS
        val cellH = cellW * 1.4f // box art is roughly 3:4
        covers.forEachIndexed { index, cover ->
            val column = index % GRID_COLUMNS
            val row = index / GRID_COLUMNS
            drawCropped(
                canvas,
                cover,
                RectF(column * cellW, row * cellH, (column + 1) * cellW, (row + 1) * cellH),
            )
        }
        val wash = Paint().apply {
            shader = LinearGradient(
                0f, 0f, 0f, CARD_H.toFloat(),
                intArrayOf(0xCC08090C.toInt(), 0xE608090C.toInt(), VOID),
                floatArrayOf(0f, 0.45f, 0.72f),
                Shader.TileMode.CLAMP,
            )
        }
        canvas.drawRect(0f, 0f, CARD_W.toFloat(), CARD_H.toFloat(), wash)
    }

    private fun drawCover(canvas: Canvas, cover: Bitmap, left: Float, top: Float, height: Float) {
        val width = height * 0.72f
        val target = RectF(left, top, left + width, top + height)
        drawCropped(canvas, cover, target)
        canvas.drawRect(
            target,
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = OUTLINE
                style = Paint.Style.STROKE
                strokeWidth = 3f
            },
        )
    }

    /** Centre-crop [source] into [target], the `ContentScale.Crop` of the Canvas world. */
    private fun drawCropped(canvas: Canvas, source: Bitmap, target: RectF) {
        val scale = maxOf(target.width() / source.width, target.height() / source.height)
        val cropW = (target.width() / scale).toInt().coerceAtLeast(1)
        val cropH = (target.height() / scale).toInt().coerceAtLeast(1)
        val src = Rect(
            ((source.width - cropW) / 2).coerceAtLeast(0),
            ((source.height - cropH) / 2).coerceAtLeast(0),
            0,
            0,
        )
        src.right = (src.left + cropW).coerceAtMost(source.width)
        src.bottom = (src.top + cropH).coerceAtMost(source.height)
        canvas.drawBitmap(source, src, target, Paint(Paint.FILTER_BITMAP_FLAG))
    }

    // ---------------------------------------------------------------- text

    private fun drawKicker(canvas: Canvas, text: String, color: Int, y: Float): Float {
        val kicker = paint(displayFace, 34f, color).apply { letterSpacing = 0.18f }
        canvas.drawText(text, MARGIN, y, kicker)
        return y + 76f
    }

    /** Title, wrapped, and shrunk a step at a time until it fits without wrapping three times. */
    private fun drawTitle(canvas: Canvas, title: String, y: Float): Float {
        val maxWidth = CARD_W - MARGIN * 2
        // A long game name is the normal case, not the exception — "The Legend of Zelda: Tears
        // of the Kingdom" is 45 characters. Shrinking before wrapping keeps the card's rhythm.
        var size = 86f
        var lines: List<String>
        while (true) {
            lines = wrap(title, paint(displayFace, size, TEXT_PRIMARY), maxWidth)
            if (lines.size <= 2 || size <= 52f) break
            size -= 8f
        }
        val titlePaint = paint(displayFace, size, TEXT_PRIMARY)
        var cursor = y
        lines.take(MAX_TITLE_LINES).forEach { line ->
            canvas.drawText(line, MARGIN, cursor, titlePaint)
            cursor += size * 1.12f
        }
        return cursor
    }

    private fun drawFacts(canvas: Canvas, text: String, y: Float) {
        canvas.drawText(text, MARGIN, y, paint(monoFace, 36f, TEXT_SECONDARY).apply { letterSpacing = 0.06f })
    }

    /**
     * The footer carries a **real** URL.
     *
     * The card this replaces printed `continue.app/pile`, a domain that has never been
     * registered by anyone. Every card ever shared from this app carried a dead link.
     */
    private fun drawFooter(canvas: Canvas) {
        val y = CARD_H - 96f
        canvas.drawLine(
            MARGIN, y - 56f, CARD_W - MARGIN, y - 56f,
            Paint().apply { color = OUTLINE; strokeWidth = 2f },
        )
        canvas.drawText(
            "CONTINUE?  ·  ON GOOGLE PLAY",
            MARGIN, y,
            paint(displayMedium, 30f, TEXT_TERTIARY).apply { letterSpacing = 0.1f },
        )
    }

    private fun paint(face: Typeface, size: Float, color: Int): Paint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = face
            textSize = size
            this.color = color
        }

    /** Greedy word wrap. Words longer than the line get their own line and overflow — rare. */
    private fun wrap(text: String, paint: Paint, maxWidth: Float): List<String> {
        val words = text.split(' ').filter { it.isNotBlank() }
        if (words.isEmpty()) return listOf(text)
        val lines = mutableListOf<String>()
        var line = StringBuilder(words.first())
        for (word in words.drop(1)) {
            val candidate = "$line $word"
            if (paint.measureText(candidate) <= maxWidth) {
                line = StringBuilder(candidate)
            } else {
                lines += line.toString()
                line = StringBuilder(word)
            }
        }
        lines += line.toString()
        return lines
    }

    private fun ellipsize(text: String, paint: Paint, maxWidth: Float): String {
        if (paint.measureText(text) <= maxWidth) return text
        var end = text.length
        while (end > 1 && paint.measureText(text.take(end) + "…") > maxWidth) end--
        return text.take(end).trimEnd() + "…"
    }

    private fun rankLabel(rank: Int): String = when (rank) {
        1 -> "1ST"
        2 -> "2ND"
        3 -> "3RD"
        else -> "${rank}TH"
    }

    private fun medalColor(rank: Int): Int = when (rank) {
        1 -> COIN
        2 -> TEXT_PRIMARY
        3 -> HOT
        else -> TEXT_TERTIARY
    }

    // ---------------------------------------------------------------- io

    /**
     * Loads one image through Coil's shared loader, so a cover already on screen is already in
     * the disk cache and costs nothing — which is also what lets these cards render offline.
     *
     * `allowHardware(false)` is not optional: Coil decodes to a `HARDWARE` bitmap by default on
     * API 26+, and a hardware bitmap has no pixel data addressable by the CPU, so drawing one
     * into a software `Canvas` throws. Returns null on any failure; every card is written to
     * look deliberate without its art rather than to fail.
     */
    private suspend fun loadBitmap(url: String?): Bitmap? {
        if (url.isNullOrBlank()) return null
        return runCatching {
            val request = ImageRequest.Builder(context)
                .data(url)
                .allowHardware(false)
                .build()
            (imageLoader.execute(request) as? SuccessResult)?.image?.toBitmap()
        }.getOrNull()
    }

    /**
     * Hands the finished card to [ShareLauncher], which owns every outbound intent.
     *
     * [message] is the change that makes any of this a growth loop. The previous version put
     * `EXTRA_STREAM` in the intent and nothing else — no text, no URL — so a shared card was an
     * image with a fake link painted on it and no way for a recipient to act. Now every share
     * carries a real, tappable link: a `/g/<id>` App Link for a specific game, the Play listing
     * otherwise. See [ShareLinks].
     */
    suspend fun share(bitmap: Bitmap, fileName: String, message: String) =
        launcher.shareImage(bitmap, fileName, message)

    private companion object {
        const val SCANLINE_PITCH = 3
        const val GRID_COLUMNS = 4
        const val GRID_ROWS = 4
        const val MAX_TITLE_LINES = 3
        const val MAX_HIGH_SCORE_ROWS = 10
    }
}

/** One row of the HIGH SCORES card. */
data class HighScoreLine(val name: String, val hours: Int?)
