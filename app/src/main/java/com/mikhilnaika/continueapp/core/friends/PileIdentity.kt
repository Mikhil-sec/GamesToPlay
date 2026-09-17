package com.mikhilnaika.continueapp.core.friends

import android.content.Context
import android.util.Base64
import androidx.core.util.AtomicFile
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.security.KeyFactory
import java.security.KeyPair
import java.security.spec.X509EncodedKeySpec
import javax.inject.Inject
import javax.inject.Singleton

/**
 * This device's pile key — the random key pair that signs the links this user shares, so their
 * friends can tell a real update from someone else's forgery. See [PileSnapshot].
 *
 * **What it is not.** It is not an account, and it is not derived from anything: no device id,
 * no advertising id, no Google account. It exists only on this phone and inside links the user
 * chose to send. It is never sent to our server.
 *
 * **Where it lives.** A private file in `noBackupFilesDir`, which Android never backs up or
 * transfers. That is deliberate: a restored or transferred install gets a fresh key rather than
 * a copy, so one key never ends up signing for two phones. The cost is that friends see a
 * restored install's next share as a new person — and the import sheet offers "this is someone I
 * already follow" for exactly that case.
 *
 * A software key rather than AndroidKeyStore on purpose. What it protects is a read-only list of
 * games; the keystore's hardware guarantees buy nothing here, and its device-specific failure
 * modes would turn into "sharing is broken on some Samsungs".
 */
@Singleton
class PileIdentity @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val file = AtomicFile(File(context.noBackupFilesDir, FILE_NAME))
    private val mutex = Mutex()
    private var cached: Stored? = null

    private class Stored(val keyPair: KeyPair, val sequence: Long) {
        val signer = KeyPairSigner(keyPair)
    }

    /** This device's public key id, as friends store it — lets the import sheet spot "that's you". */
    suspend fun publicKeyId(): String = mutex.withLock {
        PileSnapshotCodec.base64(load().signer.publicKey)
    }

    /**
     * Hands out the signer and the next sequence number, persisting the increment first.
     *
     * Persisted *before* the link is built, so a crash between the two can only skip a number,
     * never reuse one — a reused number would make a friend's phone treat a genuinely newer
     * share as "already up to date".
     */
    suspend fun <T> nextShare(block: (signer: PileSigner, sequence: Long) -> T): T = mutex.withLock {
        val current = load()
        val next = Stored(current.keyPair, current.sequence + 1)
        save(next)
        cached = next
        block(next.signer, next.sequence)
    }

    /**
     * Throws the key away. The next share is signed by a new one, so everyone who followed the
     * old one keeps a frozen copy and nobody can connect the two.
     */
    suspend fun reset() = mutex.withLock {
        withContext(Dispatchers.IO) { file.delete() }
        cached = null
    }

    private suspend fun load(): Stored {
        cached?.let { return it }
        val stored = withContext(Dispatchers.IO) { read() } ?: Stored(PileKeys.generate(), 0L).also { save(it) }
        cached = stored
        return stored
    }

    /** Null for a missing or unreadable file — either way the answer is a fresh key. */
    private fun read(): Stored? = runCatching {
        val lines = String(file.readFully(), Charsets.UTF_8).lines()
        val privateKey = PileKeys.privateKey(Base64.decode(lines[0], Base64.NO_WRAP))
        val publicKey = KeyFactory.getInstance("EC")
            .generatePublic(X509EncodedKeySpec(Base64.decode(lines[1], Base64.NO_WRAP)))
        Stored(KeyPair(publicKey, privateKey), lines[2].toLong())
    }.getOrNull()

    private suspend fun save(stored: Stored) = withContext(Dispatchers.IO) {
        val text = listOf(
            Base64.encodeToString(stored.keyPair.private.encoded, Base64.NO_WRAP),
            Base64.encodeToString(stored.keyPair.public.encoded, Base64.NO_WRAP),
            stored.sequence.toString(),
        ).joinToString("\n")
        val out = file.startWrite()
        try {
            out.write(text.toByteArray(Charsets.UTF_8))
            file.finishWrite(out)
        } catch (e: Exception) {
            file.failWrite(out)
            throw e
        }
    }

    private companion object {
        const val FILE_NAME = "pile_identity"
    }
}
