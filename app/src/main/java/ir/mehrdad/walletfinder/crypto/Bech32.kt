package ir.mehrdad.walletfinder.crypto

/** Bech32 encoding for native SegWit (P2WPKH) addresses per BIP-173. */
object Bech32 {

    private const val CHARSET = "qpzry9x8gf2tvdw0s3jn54khce6mua7l"
    private val GEN = intArrayOf(
        0x3b6a57b2, 0x26508e6d, 0x1ea119fa, 0x3d4233dd, 0x2a1462b3
    )

    private fun polymod(values: IntArray): Int {
        var chk = 1
        for (v in values) {
            val b = chk ushr 25
            chk = ((chk and 0x1ffffff) shl 5) xor v
            for (i in 0..4) {
                if (((b ushr i) and 1) == 1) chk = chk xor GEN[i]
            }
        }
        return chk
    }

    private fun hrpExpand(hrp: String): IntArray {
        val out = IntArray(hrp.length * 2 + 1)
        for (i in hrp.indices) out[i] = hrp[i].code shr 5
        out[hrp.length] = 0
        for (i in hrp.indices) out[hrp.length + 1 + i] = hrp[i].code and 31
        return out
    }

    private fun convertBits(data: ByteArray, from: Int, to: Int, pad: Boolean): IntArray {
        var acc = 0
        var bits = 0
        val ret = ArrayList<Int>()
        val maxv = (1 shl to) - 1
        for (value in data) {
            acc = (acc shl from) or (value.toInt() and 0xff)
            bits += from
            while (bits >= to) {
                bits -= to
                ret.add((acc ushr bits) and maxv)
            }
        }
        if (pad) {
            while (bits > 0) {
                ret.add((acc shl (to - bits)) and maxv)
                bits = 0
            }
        } else {
            require(!(bits >= from || ((acc shl (to - bits)) and maxv) != 0)) { "bad convert" }
        }
        return ret.toIntArray()
    }

    /** Encode a witness-v0 P2WPKH address such as bc1q... */
    fun encodeSegwitAddress(hrp: String, witver: Int, program: ByteArray): String {
        val data = intArrayOf(witver) + convertBits(program, 8, 5, true)
        val mod = polymod(hrpExpand(hrp) + data + IntArray(6)) xor 1
        val sb = StringBuilder(hrp).append('1')
        for (d in data) sb.append(CHARSET[d])
        for (p in 0..5) sb.append(CHARSET[(mod ushr (5 * (5 - p))) and 31])
        return sb.toString()
    }
}
