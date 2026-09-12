package com.mikhilnaika.continueapp.core.share

import android.net.Uri
import com.mikhilnaika.continueapp.BuildConfig

/**
 * The friend loop's link contract — the app half of `worker/src/routes/gameLink.ts`.
 *
 * One game link, two forms, one meaning:
 *   `https://<worker>/g/<igdbId>?c=<campaign>` — what gets shared. Verified App Link, so an
 *     Android device with CONTINUE? installed opens the app; anything else gets the Worker's
 *     landing page, which unfurls with real key art and sells the install.
 *   `continueapp://g/<igdbId>` — the unverifiable fallback the landing page's button fires,
 *     for installs where App Links verification never took.
 *
 * Both are parsed by [parseGameId], which is the only place that decides what a link means.
 * Keep it that way: the https form is exported to the whole internet and the custom-scheme
 * form can be fired by any app on the device, so "what is a valid game link" is a security
 * question, and a second opinion on it elsewhere in the app is a second attack surface.
 */
object ShareLinks {

    /** The Worker host, fed from `WORKER_BASE_URL` — same value the manifest filter uses. */
    private val host: String = BuildConfig.SHARE_LINK_HOST

    /** Public listing. Used wherever there's no specific game to point at. */
    const val PLAY_STORE_URL: String = BuildConfig.PLAY_STORE_URL

    /**
     * IGDB ids are positive and comfortably inside 9 digits; the Worker bounds its own route
     * at `\d{1,9}` for the same reason. An id outside this is not a game we could ever look
     * up, so refusing it here costs a malformed link nothing and saves a pointless round trip.
     */
    private const val MAX_GAME_ID = 999_999_999L

    /**
     * Why the link came from, which only ever changes wording — on the landing page and in the
     * message text. Never carries a person's name: putting one in a URL would mean rendering
     * user text into a public HTML page *and* forwarding a real name around group chats. The
     * personal half of the message is the chat message itself, where the messenger already
     * attaches the sender.
     */
    enum class Campaign(val slug: String) {
        /** "Here, play this." The default. */
        PICK("pick"),

        /** "Bet you don't finish it." */
        DARE("dare"),

        /** "I finished it." Sent from the Credits Roll. */
        CLEARED("cleared"),
    }

    /** The shareable `https://` link for one game. */
    fun gameLink(gameId: Long, campaign: Campaign = Campaign.PICK): String =
        "https://$host/g/$gameId?c=${campaign.slug}"

    /**
     * The IGDB id inside an incoming link, or null if this isn't one of ours.
     *
     * Null is not an error path — [com.mikhilnaika.continueapp.feature.sharetarget.ShareTargetActivity]
     * treats it as "a share we couldn't read" and lands on the manual-entry rung of the usual
     * ladder, so a mangled or truncated link still gives the user somewhere to go.
     */
    fun parseGameId(uri: Uri?): Long? {
        if (uri == null) return null
        return parseGameId(uri.scheme, uri.host, uri.pathSegments.orEmpty())
    }

    /**
     * The actual rule, split out from [Uri] so it can be tested.
     *
     * `android.net.Uri` is a stub that throws in a plain JVM unit test, and this repo has no
     * Robolectric runner wired up — so the alternative to this split is an untested security
     * check, which is not an alternative. Same shape as `TitleParser`: a pure core with a thin
     * Android adapter over it. The adapter above is the only part that isn't covered, and all
     * it does is read three properties.
     */
    internal fun parseGameId(scheme: String?, host: String?, segments: List<String>): Long? {
        val idText = when (scheme?.lowercase()) {
            // `https://<host>/g/<id>` — the host check is the whole point. Without it, any
            // site could publish `https://evil.example/g/1` and, because our intent filter
            // matches on a path prefix, a user tapping it in some contexts would see our
            // sheet vouching for a link we had nothing to do with.
            "https" -> {
                if (!host.equals(this.host, ignoreCase = true)) return null
                if (segments.size != 2 || segments[0] != "g") return null
                segments[1]
            }

            // `continueapp://g/<id>` — the id is the authority's *path*, not a path segment
            // shared with a host, so the shapes genuinely differ and are handled separately
            // rather than being forced into one.
            "continueapp" -> {
                if (!host.equals("g", ignoreCase = true)) return null
                if (segments.size != 1) return null
                segments[0]
            }

            else -> return null
        }

        // `toLongOrNull` rather than a try/catch: a link with a word, a sign, whitespace or
        // 400 digits where the id goes is a link we simply don't recognise.
        val id = idText.toLongOrNull() ?: return null
        return if (id in 1..MAX_GAME_ID) id else null
    }

    /**
     * The message body that travels with a shared card.
     *
     * This is the part the old share had none of. `ACTION_SEND` carried an image and nothing
     * else, so a shared card was a PNG with a link *painted on it* that nobody could tap — and
     * the painted link was `continue.app/pile`, a domain that has never existed. Anything that
     * leaves the app now carries a real, tappable URL.
     */
    fun messageFor(campaign: Campaign, gameName: String, gameId: Long): String {
        val link = gameLink(gameId, campaign)
        val line = when (campaign) {
            Campaign.PICK -> "$gameName. This one's got your name on it."
            Campaign.DARE -> "I dare you to actually finish $gameName."
            Campaign.CLEARED -> "Cleared $gameName. Off the pile, credits rolled."
        }
        return "$line\n\n$link"
    }

    /**
     * The message for a card with no single game behind it — THE PILE, HIGH SCORES.
     *
     * Falls back to the store listing, since there's nothing more specific to deep link to.
     */
    fun messageForPile(totalHours: Int, totalGames: Int): String =
        "$totalHours hours. $totalGames games. Send help.\n\n$PLAY_STORE_URL"

    fun messageForHighScores(topGameName: String?): String {
        val opener = if (topGameName != null) {
            "$topGameName is my #1 of all time. Fight me."
        } else {
            "My all-time top 10. Fight me."
        }
        return "$opener\n\n$PLAY_STORE_URL"
    }
}
