package com.mikhilnaika.continueapp.core.friends

import java.math.BigInteger
import java.security.KeyFactory
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.PrivateKey
import java.security.Signature
import java.security.spec.ECGenParameterSpec
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec

/** Something that can sign a snapshot as its owner. [PileIdentity] in the app, a bare key pair in tests. */
interface PileSigner {
    /** Raw uncompressed P-256 point, 65 bytes. */
    val publicKey: ByteArray
    fun sign(message: ByteArray): ByteArray
}

/**
 * The ECDSA P-256 plumbing behind signed pile links, kept free of Android types so the whole
 * signing and verification path runs in a plain JVM test.
 *
 * P-256 rather than Ed25519 because it's the curve every Android version this app supports
 * (minSdk 26) can sign and verify with the platform provider; Ed25519 only arrived in API 33.
 */
object PileKeys {

    const val RAW_PUBLIC_KEY_BYTES = 65

    private const val ALGORITHM = "EC"
    private const val CURVE = "secp256r1"
    private const val SIGNATURE = "SHA256withECDSA"

    /**
     * The fixed DER header of an X.509 SubjectPublicKeyInfo for an uncompressed P-256 key.
     *
     * Links carry only the 65-byte point; this prefix is glued back on locally to hand the key to
     * `KeyFactory`. That's 26 bytes (35 URL characters) not spent in every shared link, and a
     * standard encoding every JCA provider accepts, so there's no provider-specific curve
     * parameter handling to get wrong.
     */
    private val SPKI_PREFIX: ByteArray = hex("3059301306072a8648ce3d020106082a8648ce3d030107034200")

    // P-256 domain parameters (FIPS 186-4 D.1.2.3), for the on-curve check below.
    private val P = BigInteger("ffffffff00000001000000000000000000000000ffffffffffffffffffffffff", 16)
    private val A = P - BigInteger.valueOf(3)
    private val B = BigInteger("5ac635d8aa3a93e7b3ebbd55769886bc651d06b0cc53b0f63bce3c3e27d2604b", 16)

    fun generate(): KeyPair =
        KeyPairGenerator.getInstance(ALGORITHM).apply { initialize(ECGenParameterSpec(CURVE)) }.generateKeyPair()

    /** The 65-byte point out of a JCA public key, or null if it isn't an uncompressed P-256 key. */
    fun rawPublicKey(x509: ByteArray): ByteArray? {
        if (x509.size != SPKI_PREFIX.size + RAW_PUBLIC_KEY_BYTES) return null
        if (!x509.copyOfRange(0, SPKI_PREFIX.size).contentEquals(SPKI_PREFIX)) return null
        return x509.copyOfRange(SPKI_PREFIX.size, x509.size)
    }

    fun privateKey(pkcs8: ByteArray): PrivateKey =
        KeyFactory.getInstance(ALGORITHM).generatePrivate(PKCS8EncodedKeySpec(pkcs8))

    fun sign(privateKey: PrivateKey, message: ByteArray): ByteArray =
        Signature.getInstance(SIGNATURE).run {
            initSign(privateKey)
            update(message)
            sign()
        }

    /**
     * True only for a well-formed point on P-256 and a signature that verifies against it.
     *
     * Every failure — a malformed key, a point off the curve, a garbage DER signature, a provider
     * exception — is a plain `false`. The input came off the internet; nothing about it is worth
     * crashing the import sheet for.
     *
     * The explicit on-curve check is belt and braces: providers do validate, but not all of them
     * historically did, and a link is exactly the attacker-shaped input that check exists for.
     */
    fun verify(rawPublicKey: ByteArray, message: ByteArray, signature: ByteArray): Boolean {
        if (!isOnCurve(rawPublicKey)) return false
        return try {
            val key = KeyFactory.getInstance(ALGORITHM)
                .generatePublic(X509EncodedKeySpec(SPKI_PREFIX + rawPublicKey))
            Signature.getInstance(SIGNATURE).run {
                initVerify(key)
                update(message)
                verify(signature)
            }
        } catch (_: Exception) {
            false
        }
    }

    internal fun isOnCurve(raw: ByteArray): Boolean {
        if (raw.size != RAW_PUBLIC_KEY_BYTES || raw[0] != 0x04.toByte()) return false
        val x = BigInteger(1, raw.copyOfRange(1, 33))
        val y = BigInteger(1, raw.copyOfRange(33, 65))
        if (x >= P || y >= P) return false
        val lhs = y.multiply(y).mod(P)
        val rhs = x.multiply(x).multiply(x).add(A.multiply(x)).add(B).mod(P)
        return lhs == rhs
    }

    private fun hex(text: String): ByteArray =
        ByteArray(text.length / 2) { i -> text.substring(i * 2, i * 2 + 2).toInt(16).toByte() }
}

/** A signer over an in-memory key pair. */
class KeyPairSigner(private val keyPair: KeyPair) : PileSigner {
    override val publicKey: ByteArray = requireNotNull(PileKeys.rawPublicKey(keyPair.public.encoded)) {
        "Not an uncompressed P-256 public key"
    }

    override fun sign(message: ByteArray): ByteArray = PileKeys.sign(keyPair.private, message)
}
