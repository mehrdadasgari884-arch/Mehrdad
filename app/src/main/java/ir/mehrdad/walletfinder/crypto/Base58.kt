package ir.mehrdad.walletfinder.crypto

import java.math.BigInteger

/** Base58 and Base58Check encoding (used for legacy Bitcoin addresses and xprv/xpub). */
object Base58 {

    private const val ALPHABET = "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz"
    private val FIFTY_EIGHT = BigInteger.valueOf(58)

    fun encode(bytes: ByteArray): String {
        var zeros = 0
        while (zeros < bytes.size && bytes[zeros].toInt() == 0) zeros++
        var x = BigInteger(1, bytes)
        val sb = StringBuilder()
        while (x.signum() > 0) {
            val divRem = x.divideAndRemainder(FIFTY_EIGHT)
            sb.append(ALPHABET[divRem[1].toInt()])
            x = divRem[0]
        }
        repeat(zeros) { sb.append('1') }
        return sb.reverse().toString()
    }

    fun encodeChecked(payload: ByteArray): String {
        val checksum = Hash.sha256(Hash.sha256(payload))
        return encode(payload + checksum.copyOfRange(0, 4))
    }

    /** Decode a Base58 string back to bytes (leading "1"s become zero bytes). */
    fun decode(input: String): ByteArray {
        var x = BigInteger.ZERO
        for (c in input) {
            val idx = ALPHABET.indexOf(c)
            require(idx >= 0) { "invalid character: $c" }
            x = x.multiply(FIFTY_EIGHT).add(BigInteger.valueOf(idx.toLong()))
        }
        var zeros = 0
        while (zeros < input.length && input[zeros] == '1') zeros++
        val raw = x.toByteArray()
        val stripped = if (raw.isNotEmpty() && raw[0] == 0.toByte()) {
            raw.copyOfRange(1, raw.size)
        } else {
            raw
        }
        return ByteArray(zeros) + stripped
    }

    /** Decode and verify the 4-byte checksum. Throws on invalid input. */
    fun decodeChecked(input: String): ByteArray {
        val full = decode(input)
        require(full.size >= 5) { "too short" }
        val payload = full.copyOfRange(0, full.size - 4)
        val checksum = full.copyOfRange(full.size - 4, full.size)
        val expect = Hash.sha256(Hash.sha256(payload)).copyOfRange(0, 4)
        require(checksum.contentEquals(expect)) { "bad checksum" }
        return payload
    }
}
