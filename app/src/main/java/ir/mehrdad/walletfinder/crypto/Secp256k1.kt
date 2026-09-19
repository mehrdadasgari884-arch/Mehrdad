package ir.mehrdad.walletfinder.crypto

import java.math.BigInteger

/** A point on the secp256k1 curve. */
data class EcPoint(val x: BigInteger, val y: BigInteger)

/**
 * Minimal secp256k1 group operations needed for BIP-32 public keys.
 * Uses affine coordinates with BigInteger.modInverse — more than fast enough
 * for deriving a handful of addresses from a seed phrase.
 */
object Secp256k1 {

    /** Field prime p */
    val P: BigInteger = BigInteger("FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFEFFFFFC2F", 16)

    /** Group order n */
    val N: BigInteger = BigInteger("FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFEBAAEDCE6AF48A03BBFD25E8CD0364141", 16)

    private val GX = BigInteger("79BE667EF9DCBBAC55A06295CE870B07029BFCDB2DCE28D959F2815B16F81798", 16)
    private val GY = BigInteger("483ADA7726A3C4655DA4FBFC0E1108A8FD17B448A68554199C47D08FFB10D4B8", 16)
    val G = EcPoint(GX, GY)

    private val TWO = BigInteger.valueOf(2)
    private val THREE = BigInteger.valueOf(3)

    /** Point addition (handles doubling and the point at infinity). */
    fun add(p: EcPoint?, q: EcPoint?): EcPoint? {
        if (p == null) return q
        if (q == null) return p
        if (p.x == q.x && (p.y + q.y).mod(P).signum() == 0) return null
        val lambda = if (p.x == q.x && p.y == q.y) {
            THREE.multiply(p.x).multiply(p.x)
                .multiply(TWO.multiply(p.y).mod(P).modInverse(P))
                .mod(P)
        } else {
            q.y.subtract(p.y).multiply(q.x.subtract(p.x).mod(P).modInverse(P)).mod(P)
        }
        val x = lambda.multiply(lambda).subtract(p.x).subtract(q.x).mod(P)
        val y = lambda.multiply(p.x.subtract(x)).subtract(p.y).mod(P)
        return EcPoint(x, y)
    }

    /** Scalar multiplication with double-and-add. */
    fun mul(k: BigInteger, point: EcPoint = G): EcPoint? {
        var kk = k.mod(N)
        var result: EcPoint? = null
        var addend: EcPoint? = point
        while (kk.signum() > 0) {
            if (kk.testBit(0)) result = add(result, addend)
            addend = add(addend, addend)
            kk = kk.shiftRight(1)
        }
        return result
    }

    /** SEC1 compressed serialization (33 bytes: 0x02/0x03 prefix + x). */
    fun serializeCompressed(pt: EcPoint): ByteArray {
        val out = ByteArray(33)
        out[0] = if (pt.y.testBit(0)) 0x03 else 0x02
        val x = pt.x.toByteArray()
        val srcOff = maxOf(0, x.size - 32)
        val dstOff = 1 + maxOf(0, 32 - x.size)
        System.arraycopy(x, srcOff, out, dstOff, minOf(32, x.size))
        return out
    }

    /** Compressed public key for private key scalar k. */
    fun publicKeyOf(privateKey: ByteArray): ByteArray =
        serializeCompressed(mul(BigInteger(1, privateKey))!!)
}
