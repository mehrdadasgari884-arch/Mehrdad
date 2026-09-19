package ir.mehrdad.walletfinder.crypto

import java.math.BigInteger

/** A decoded WIF (Wallet Import Format) private key. */
class WifInfo(
    val version: Int,          // 0x80 mainnet, 0xEF testnet
    val compressed: Boolean,
    val privateKey: ByteArray
) {
    val isMainnet: Boolean get() = version == 0x80

    /** Public key in the form this key was meant to be used with. */
    fun publicKey(): ByteArray {
        val point = Secp256k1.mul(BigInteger(1, privateKey))!!
        return if (compressed) {
            Secp256k1.serializeCompressed(point)
        } else {
            Secp256k1.serializeUncompressed(point)
        }
    }

    /** Compressed public key (needed for SegWit addresses), when allowed. */
    fun compressedPublicKey(): ByteArray =
        Secp256k1.serializeCompressed(Secp256k1.mul(BigInteger(1, privateKey))!!)
}

/**
 * WIF parsing. Validated against the well-known k=1 vectors:
 *   KwDiBf89QgGbjEhKnhXJuH7LrciVrZi3qYjgd9M7rFU73sVHnoWn (compressed)
 *   5HpHagT65TZzG1PH3CSu63k8DbpvD8s5ip4nEB3kEsreAnchuDf (uncompressed)
 */
object Wif {

    fun parse(input: String): WifInfo {
        val payload = Base58.decodeChecked(input.trim())
        val version = payload[0].toInt() and 0xff
        require(version == 0x80 || version == 0xEF) { "bad version" }
        val compressed = payload.size == 34 && payload[33] == 0x01.toByte()
        require(payload.size == if (compressed) 34 else 33) { "bad length" }
        return WifInfo(version, compressed, payload.copyOfRange(1, 33))
    }
}
