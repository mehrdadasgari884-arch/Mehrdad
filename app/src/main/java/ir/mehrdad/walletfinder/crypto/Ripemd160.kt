package ir.mehrdad.walletfinder.crypto

/**
 * Pure-Kotlin RIPEMD-160 (not provided by the Android JCE providers).
 * Ported 1:1 from the verified reference implementation and validated against
 * the official test vectors:
 *   RIPEMD160("")               = 9c1185a5c5e9fc54612808977ee8f548b2258d31
 *   RIPEMD160("abc")            = 8eb208f7e05d987a9b044a8e98c6b087f15a0bfc
 *   RIPEMD160("message digest") = 5d0689ef49d2fae572b881b123a85ffa21595f36
 *   RIPEMD160("a" x 1,000,000)  = 52783243c1697bdbe16d37f97f68f08325dc1528
 */
object Ripemd160 {

    private val R1 = intArrayOf(
        0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15,
        7, 4, 13, 1, 10, 6, 15, 3, 12, 0, 9, 5, 2, 14, 11, 8,
        3, 10, 14, 4, 9, 15, 8, 1, 2, 7, 0, 6, 13, 11, 5, 12,
        1, 9, 11, 10, 0, 8, 12, 4, 13, 3, 7, 15, 14, 5, 6, 2,
        4, 0, 5, 9, 7, 12, 2, 10, 14, 1, 3, 8, 11, 6, 15, 13
    )
    private val R2 = intArrayOf(
        5, 14, 7, 0, 9, 2, 11, 4, 13, 6, 15, 8, 1, 10, 3, 12,
        6, 11, 3, 7, 0, 13, 5, 10, 14, 15, 8, 12, 4, 9, 1, 2,
        15, 5, 1, 3, 7, 14, 6, 9, 11, 8, 12, 2, 10, 0, 4, 13,
        8, 6, 4, 1, 3, 11, 15, 0, 5, 12, 2, 13, 9, 7, 10, 14,
        12, 15, 10, 4, 1, 5, 8, 7, 6, 2, 13, 14, 0, 3, 9, 11
    )
    private val S1 = intArrayOf(
        11, 14, 15, 12, 5, 8, 7, 9, 11, 13, 14, 15, 6, 7, 9, 8,
        7, 6, 8, 13, 11, 9, 7, 15, 7, 12, 15, 9, 11, 7, 13, 12,
        11, 13, 6, 7, 14, 9, 13, 15, 14, 8, 13, 6, 5, 12, 7, 5,
        11, 12, 14, 15, 14, 15, 9, 8, 9, 14, 5, 6, 8, 6, 5, 12,
        9, 15, 5, 11, 6, 8, 13, 12, 5, 12, 13, 14, 11, 8, 5, 6
    )
    private val S2 = intArrayOf(
        8, 9, 9, 11, 13, 15, 15, 5, 7, 7, 8, 11, 14, 14, 12, 6,
        9, 13, 15, 7, 12, 8, 9, 11, 7, 7, 12, 7, 6, 15, 13, 11,
        9, 7, 15, 11, 8, 6, 6, 14, 12, 13, 5, 14, 13, 13, 7, 5,
        15, 5, 8, 11, 14, 14, 6, 14, 6, 9, 12, 9, 12, 5, 15, 8,
        8, 5, 12, 9, 12, 5, 14, 6, 8, 13, 6, 5, 15, 13, 11, 11
    )
    private val KL = intArrayOf(
        0x00000000, 0x5A827999, 0x6ED9EBA1, 0x8F1BBCDC.toInt(), 0xA953FD4E.toInt()
    )
    private val KR = intArrayOf(
        0x50A28BE6, 0x5C4DD124, 0x6D703EF3, 0x7A6D76E9, 0x00000000
    )

    fun digest(msg: ByteArray): ByteArray {
        val bitLen = msg.size.toLong() * 8L
        val padLen = (56 - (msg.size + 1) % 64 + 64) % 64
        val m = ByteArray(msg.size + 1 + padLen + 8)
        msg.copyInto(m)
        m[msg.size] = 0x80.toByte()
        for (i in 0..7) m[m.size - 8 + i] = ((bitLen ushr (8 * i)) and 0xFF).toByte()

        var h0 = 0x67452301
        var h1 = 0xEFCDAB89.toInt()
        var h2 = 0x98BADCFE.toInt()
        var h3 = 0x10325476
        var h4 = 0xC3D2E1F0.toInt()
        val x = IntArray(16)

        var off = 0
        while (off < m.size) {
            for (i in 0..15) {
                x[i] = (m[off + 4 * i].toInt() and 0xff) or
                        ((m[off + 4 * i + 1].toInt() and 0xff) shl 8) or
                        ((m[off + 4 * i + 2].toInt() and 0xff) shl 16) or
                        ((m[off + 4 * i + 3].toInt() and 0xff) shl 24)
            }
            var al = h0; var bl = h1; var cl = h2; var dl = h3; var el = h4
            var ar = h0; var br = h1; var cr = h2; var dr = h3; var er = h4
            for (j in 0..79) {
                val rnd = j shr 4
                var t = al + f(j, bl, cl, dl)
                t += x[R1[j]]
                t += KL[rnd]
                t = rotl(t, S1[j]) + el
                al = el; el = dl; dl = rotl(cl, 10); cl = bl; bl = t

                var u = ar + f(79 - j, br, cr, dr)
                u += x[R2[j]]
                u += KR[rnd]
                u = rotl(u, S2[j]) + er
                ar = er; er = dr; dr = rotl(cr, 10); cr = br; br = u
            }
            val t = h1 + cl + dr
            h1 = h2 + dl + er
            h2 = h3 + el + ar
            h3 = h4 + al + br
            h4 = h0 + bl + cr
            h0 = t
            off += 64
        }

        val out = ByteArray(20)
        val hs = intArrayOf(h0, h1, h2, h3, h4)
        for (i in 0..4) {
            val v = hs[i]
            out[4 * i] = (v and 0xff).toByte()
            out[4 * i + 1] = ((v ushr 8) and 0xff).toByte()
            out[4 * i + 2] = ((v ushr 16) and 0xff).toByte()
            out[4 * i + 3] = ((v ushr 24) and 0xff).toByte()
        }
        return out
    }

    private fun rotl(x: Int, c: Int): Int = (x shl c) or (x ushr (32 - c))

    private fun f(j: Int, x: Int, y: Int, z: Int): Int = when {
        j <= 15 -> x xor y xor z
        j <= 31 -> (x and y) or (x.inv() and z)
        j <= 47 -> (x or y.inv()) xor z
        j <= 63 -> (x and z) or (y and z.inv())
        else -> x xor (y or z.inv())
    }
}
