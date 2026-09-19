package com.mikhilnaika.continueapp.core.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.SoundPool
import androidx.annotation.RawRes
import androidx.core.content.getSystemService
import com.mikhilnaika.continueapp.R
import com.mikhilnaika.continueapp.core.data.UserPreferencesRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * One-shot sound effects — docs/03-DESIGN-SYSTEM.md §6. Every file is synthesised by
 * `tools/make_audio.py` and dedicated to the public domain; see `docs/AUDIO-LICENSE.md`.
 *
 * [volume] is a per-sound trim so the mix is decided here once, not at forty call sites: the
 * button blip plays more than anything else in the app and has to sit well under the payoff
 * sounds, or the app sounds like a keyboard. The whole set was cut ~5 dB after the first device
 * test, where effects at 50% phone volume "sounded like 75%".
 */
enum class Sfx(@RawRes val res: Int, val volume: Float) {
    COIN(R.raw.sfx_coin, 0.3f),
    COIN_DROP(R.raw.sfx_coin_drop, 0.33f),
    LEVER(R.raw.sfx_lever, 0.45f),
    DEAL(R.raw.sfx_deal, 0.28f),
    FLIP(R.raw.sfx_flip, 0.25f),
    WHOOSH(R.raw.sfx_whoosh, 0.28f),
    SELECT(R.raw.sfx_select, 0.28f),
    BLIP(R.raw.sfx_blip, 0.14f),
    TICK(R.raw.sfx_tick, 0.17f),
    ERROR(R.raw.sfx_error, 0.25f),
    ADD(R.raw.sfx_add, 0.28f),
    FRIEND(R.raw.sfx_friend, 0.33f),
    POWER_UP(R.raw.sfx_powerup, 0.4f),
    CRT_ON(R.raw.sfx_crt_on, 0.33f),
}

/**
 * The four places the app has a soundtrack; which *recording* plays there is the user's
 * [MusicPack]. Music is deliberately not everywhere: THE PILE is a place you visit for two
 * minutes to decide something, and a loop under that is something people mute and never unmute.
 * It plays where the arcade metaphor is loudest — the cold open, the CONTINUE? countdown, the
 * shop counter, and the credits.
 *
 * Every track is mastered to the same loudness (tools/make_audio.py `master`), so these levels
 * hold for every pack. They were cut ~10 dB after the first device test ("50% phone volume sounds
 * like 90%"): music is a bed under the app, not the app.
 */
enum class MusicTrack(val volume: Float, val loop: Boolean) {
    TITLE(0.26f, loop = true),
    CONTINUE(0.22f, loop = true),
    SHOP(0.2f, loop = true),
    VICTORY(0.32f, loop = false),
}

/**
 * The app's only way to make a sound.
 *
 * **The SOUND and MUSIC toggles in YOU are enforced here, and only here** — the same shape as
 * [com.mikhilnaika.continueapp.core.util.Haptics], and for the same reason: HAPTICS spent weeks
 * as a switch that controlled nothing because it was "enforced at the call site". There is no
 * way to play a sound that skips these checks, because there is no other way to play one.
 *
 * Three courtesies on top of the toggles, all checked at play time:
 * - **Silent mode mutes everything; vibrate does not.** The first build muted on vibrate too,
 *   and on the first device test it was simply silent — plenty of people leave a phone on
 *   vibrate permanently and expect the media volume to govern game sound, which is the Android
 *   convention. Full silent is a deliberate "no sound, anywhere", and is honoured.
 * - **Music never talks over the user's own audio.** If a podcast or playlist is already
 *   playing, the soundtrack simply doesn't start; effects still play, briefly, on top.
 * - **No audio focus is taken**, so nothing else is ever paused on our account.
 *
 * Everything is on `USAGE_GAME`, which rides the media volume and — importantly for the demo
 * video — is captured by Android's built-in screen recorder.
 */
@Singleton
class ArcadeAudio @Inject constructor(
    @ApplicationContext private val context: Context,
    userPreferencesRepository: UserPreferencesRepository,
) {
    private val audioManager: AudioManager? = context.getSystemService()
    private val attributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_GAME)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .build()

    private val soundPool: SoundPool = SoundPool.Builder()
        .setMaxStreams(MAX_STREAMS)
        .setAudioAttributes(attributes)
        .build()

    /** Sound id per effect, filled as SoundPool finishes decoding (it's asynchronous). */
    private val loaded = HashMap<Sfx, Int>()
    private val ready = HashSet<Int>()

    /**
     * Effects asked for before their file finished decoding. The first device test lost the
     * coin-drop sound this way: the banner fires within a second of launch, SoundPool was still
     * loading, and the request was dropped. Now it plays the moment the file is ready.
     */
    private val pending = HashMap<Int, Float>()

    @Volatile private var sfxEnabled = true
    @Volatile private var musicEnabled = true

    /** Main-thread only: MediaPlayer calls and [current] are touched from composition and here. */
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var player: MediaPlayer? = null
    private var current: MusicTrack? = null
    private var currentPack: MusicPack? = null
    private var previewing = false
    private var fadeJob: Job? = null

    @Volatile private var selectedPack: MusicPack = MusicPack.DEFAULT

    init {
        soundPool.setOnLoadCompleteListener { pool, sampleId, status ->
            if (status != 0) return@setOnLoadCompleteListener
            val waiting = synchronized(ready) {
                ready += sampleId
                pending.remove(sampleId)
            }
            if (waiting != null) pool.play(sampleId, waiting, waiting, 1, 0, 1f)
        }
        // ~150 KB of decoded effects in total, so loading all of them up front costs nothing and
        // means the very first coin of a session isn't the one that plays late.
        Sfx.entries.forEach { loaded[it] = soundPool.load(context, it.res, 1) }

        userPreferencesRepository.isSoundEffectsEnabled
            .onEach { sfxEnabled = it }
            .launchIn(scope)
        userPreferencesRepository.isMusicEnabled
            .onEach { enabled ->
                musicEnabled = enabled
                // Switching MUSIC off has to be instant, not "from the next screen on".
                if (!enabled && !previewing) stopNow()
            }
            .launchIn(scope)
        userPreferencesRepository.musicPackId
            .onEach { id ->
                selectedPack = MusicPack.fromId(id)
                // Changing pack mid-song swaps the song, so the choice is heard at once.
                val playing = current
                if (playing != null && !previewing && currentPack != selectedPack) {
                    stopNow()
                    startMusic(playing)
                }
            }
            .launchIn(scope)
    }

    fun play(sfx: Sfx) {
        if (!sfxEnabled || isSilenced()) return
        val id = loaded[sfx] ?: return
        val isReady = synchronized(ready) {
            (id in ready).also { if (!it) pending[id] = sfx.volume }
        }
        if (isReady) soundPool.play(id, sfx.volume, sfx.volume, 1, 0, 1f)
    }

    /**
     * Starts [track] from the user's pack unless it is already playing. Called by [MusicCue]
     * when its screen comes to the foreground; the caller never needs to know whether music is
     * allowed.
     *
     * A non-null [preview] plays that pack instead — YOU's PREVIEW button. A preview is an
     * explicit tap, so it plays even with MUSIC off and over other audio; only silent mode
     * stops it.
     */
    fun startMusic(track: MusicTrack, preview: MusicPack? = null) {
        val pack = preview ?: selectedPack
        if (isSilenced()) return
        if (preview == null && !musicEnabled) return
        if (current == track && currentPack == pack && player != null) return
        // Someone else's music is playing and it isn't ours — leave their evening alone.
        if (preview == null && player == null && audioManager?.isMusicActive == true) return
        stopNow()
        current = track
        currentPack = pack
        previewing = preview != null
        val mp = MediaPlayer()
        player = mp
        try {
            mp.setAudioAttributes(attributes)
            context.resources.openRawResourceFd(pack.res(track)).use { fd ->
                mp.setDataSource(fd.fileDescriptor, fd.startOffset, fd.length)
            }
            mp.isLooping = track.loop
            mp.setVolume(0f, 0f)
            mp.setOnPreparedListener {
                // A later start/stop may have replaced us while the file was preparing.
                if (player !== it) return@setOnPreparedListener
                it.start()
                fadeTo(it, track.volume, FADE_IN_MS)
            }
            mp.setOnCompletionListener { if (!track.loop && player === it) stopNow() }
            mp.setOnErrorListener { failed, _, _ ->
                if (player === failed) stopNow()
                true
            }
            mp.prepareAsync()
        } catch (e: Exception) {
            // Music is decoration. A device that can't play it gets a quiet app, not a crash.
            stopNow()
        }
    }

    /** Fades out and releases [track] (of [preview]'s pack, if given), if it is still playing. */
    fun stopMusic(track: MusicTrack, preview: MusicPack? = null) {
        if (current != track) return
        if (preview != null && currentPack != preview) return
        val mp = player ?: return
        player = null
        current = null
        currentPack = null
        previewing = false
        fadeJob?.cancel()
        // Deliberately not stored in [fadeJob]: the next screen's track starting mid-fade must
        // not cancel this, or the old player would be left half-loud and never released.
        scope.launch {
            ramp(mp, from = track.volume, to = 0f, durationMs = FADE_OUT_MS)
            runCatching { mp.stop() }
            mp.release()
        }
    }

    private fun stopNow() {
        fadeJob?.cancel()
        player?.let { runCatching { it.stop() }; it.release() }
        player = null
        current = null
        currentPack = null
        previewing = false
    }

    private fun fadeTo(mp: MediaPlayer, target: Float, durationMs: Long) {
        fadeJob?.cancel()
        fadeJob = scope.launch { ramp(mp, 0f, target, durationMs) }
    }

    private suspend fun ramp(mp: MediaPlayer, from: Float, to: Float, durationMs: Long) {
        val steps = 10
        for (i in 1..steps) {
            val v = from + (to - from) * i / steps
            runCatching { mp.setVolume(v, v) }
            delay(durationMs / steps)
        }
    }

    private fun isSilenced(): Boolean =
        audioManager?.ringerMode == AudioManager.RINGER_MODE_SILENT

    private companion object {
        /** Enough for a coin, a deal and a blip to overlap without one stealing another's voice. */
        const val MAX_STREAMS = 6
        const val FADE_IN_MS = 400L
        const val FADE_OUT_MS = 300L
    }
}
