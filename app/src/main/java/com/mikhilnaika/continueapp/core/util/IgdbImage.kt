package com.mikhilnaika.continueapp.core.util

/**
 * Re-writes the size token in an IGDB image URL.
 *
 * IGDB serves every image at a fixed set of named sizes, chosen by one path segment:
 * `…/upload/t_cover_big/co95gk.jpg`. The Worker stores one baseline URL per game, so picking
 * the right size is the *client's* job — it's the only side that knows how big the thing will
 * actually be drawn.
 *
 * This matters more than it sounds. `t_cover_big` is **264x352**. In a 3-column grid that's
 * fine; on the DRAW card it's blown up ~3x, and full-screen it's ~4x — which is exactly the
 * blockiness reported from a real phone. Measured against the live CDN (2026-08-14) for a
 * typical cover:
 *
 * | token | pixels | bytes |
 * |---|---|---|
 * | `t_cover_big` | 264x352 | 22 KB |
 * | `t_cover_big_2x` | 528x704 | 79 KB |
 * | `t_720p` | 540x720 | 83 KB |
 * | `t_1080p` | 810x1080 | 148 KB |
 *
 * Note `t_original` for that cover was only 600x800 — IGDB **upscales** beyond the source
 * rather than erroring, so `t_1080p` on a cover buys layout-correct pixels but no new detail.
 * That's why a full-bleed background should come from `backgroundUrl` (real 1920x1080 artwork)
 * and treat the cover only as a last resort.
 *
 * Unknown/non-IGDB URLs (the offline seed set, or anything without a `/t_…/` segment) are
 * returned untouched.
 */
object IgdbImage {

    /** Matches the single size segment in an IGDB delivery URL. */
    private val SIZE_TOKEN = Regex("/t_[a-z0-9_]+/")

    /** 3-column grid thumbnails — retina-sharp at phone density without 720p's bytes. */
    const val GRID = "t_cover_big_2x"

    /** The DRAW card and other large single-cover surfaces. */
    const val HERO = "t_1080p"

    /** Full-bleed backgrounds. */
    const val FULL_BLEED = "t_1080p"

    fun at(url: String?, size: String): String? {
        if (url == null) return null
        if (!SIZE_TOKEN.containsMatchIn(url)) return url
        return SIZE_TOKEN.replace(url, "/$size/")
    }

    /**
     * Builds a full delivery URL from a bare `image_id` — needed for [OfflineGameRecord]
     * (`com.mikhilnaika.continueapp.core.offline`), which stores just the CDN path segment
     * rather than a whole URL, since that's all the data-dump `covers` table carries.
     */
    fun coverUrl(imageId: String, size: String = GRID): String =
        "https://images.igdb.com/igdb/image/upload/$size/$imageId.jpg"
}
