package ir.mehrdad.walletfinder.crypto

import java.security.MessageDigest
import javax.crypto.Mac
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/** Hash primitives used across the Bitcoin standards (SHA-256, HMAC-SHA512, PBKDF2, HASH160). */
object Hash {

    fun sha256(data: ByteArray): ByteArray =
        MessageDigest.getInstance("SHA-256").digest(data)

    fun hmacSha512(key: ByteArray, data: ByteArray): ByteArray {
        val mac = Mac.getInstance("HmacSHA512")
        mac.init(SecretKeySpec(key, "HmacSHA512"))
        return mac.doFinal(data)
    }

    /** PBKDF2-HMAC-SHA512 as required by BIP-39 (2048 iterations, 64-byte output). */
    fun pbkdf2Sha512(password: String, salt: String, iterations: Int, lengthBytes: Int): ByteArray {
        val spec = PBEKeySpec(
            password.toCharArray(),
            salt.toByteArray(Charsets.UTF_8),
            iterations,
            lengthBytes * 8
        )
        return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA512")
            .generateSecret(spec).encoded
    }

    /** HASH160 = RIPEMD160(SHA256(x)) — used for Bitcoin addresses. */
    fun hash160(data: ByteArray): ByteArray = Ripemd160.digest(sha256(data))
}
