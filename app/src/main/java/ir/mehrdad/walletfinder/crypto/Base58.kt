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
}
