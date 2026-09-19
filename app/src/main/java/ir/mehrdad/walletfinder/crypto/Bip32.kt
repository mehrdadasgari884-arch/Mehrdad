package ir.mehrdad.walletfinder.crypto

import java.math.BigInteger

/**
 * BIP-32 hierarchical deterministic key derivation (private-key branch only,
 * which is all a recovery tool needs: seed phrase -> private keys -> addresses).
 *
 * Validated against the official BIP-32 test vector 1 (xpub/xprv at every level)
 * and the BIP-84 test vectors.
 */
class HdNode private constructor(
    val key: ByteArray,
    val chainCode: ByteArray,
    val depth: Int,
    val parentFingerprint: ByteArray,
    val childNumber: Int
) {

    companion object {
        /** 0x80000000 — the hardening bit. Hardened child numbers are negative in signed Int. */
        const val HARDENED_BIT: Int = Int.MIN_VALUE

        const val VERSION_XPRV: Int = 0x0488ADE4
        const val VERSION_XPUB: Int = 0x0488B21E
        const val VERSION_ZPRV: Int = 0x04b2430c
        const val VERSION_ZPUB: Int = 0x04b24746

        fun fromSeed(seed: ByteArray): HdNode {
            val i = Hash.hmacSha512("Bitcoin seed".toByteArray(Charsets.UTF_8), seed)
            return HdNode(i.copyOfRange(0, 32), i.copyOfRange(32, 64), 0, ByteArray(4), 0)
        }
    }

    fun publicKey(): ByteArray = Secp256k1.publicKeyOf(key)

    fun fingerprint(): ByteArray = Hash.hash160(publicKey()).copyOfRange(0, 4)

    /** Derive child at [index]; set the hardening bit (index < 0) for hardened children. */
    fun ckd(index: Int): HdNode {
        val hardened = index < 0
        val data = if (hardened) {
            byteArrayOf(0) + key + ser32(index)
        } else {
            publicKey() + ser32(index)
        }
        val i = Hash.hmacSha512(chainCode, data)
        val il = BigInteger(1, i.copyOfRange(0, 32))
        require(il.compareTo(Secp256k1.N) < 0) { "invalid child key" }
        val childKey = il.add(BigInteger(1, key)).mod(Secp256k1.N)
        return HdNode(
            toBytes32(childKey),
            i.copyOfRange(32, 64),
            depth + 1,
            fingerprint(),
            index
        )
    }

    /** Derive a path such as "m/84'/0'/0'/0/0". */
    fun derivePath(path: String): HdNode {
        var node = this
        val cleaned = path.removePrefix("m/").removePrefix("m")
        for (part in cleaned.split("/")) {
            val p = part.trim()
            if (p.isEmpty()) continue
            val hard = p.endsWith("'") || p.endsWith("h") || p.endsWith("H")
            val num = p.removeSuffix("'").removeSuffix("h").removeSuffix("H").toInt()
            val index = if (hard) num or HARDENED_BIT else num
            node = node.ckd(index)
        }
        return node
    }

    /** Serialize as base58check xprv/zprv (private versions) — used for diagnostics. */
    fun serialize(version: Int): String {
        val buf = ByteArray(78)
        buf[0] = ((version ushr 24) and 0xff).toByte()
        buf[1] = ((version ushr 16) and 0xff).toByte()
        buf[2] = ((version ushr 8) and 0xff).toByte()
        buf[3] = (version and 0xff).toByte()
        buf[4] = depth.toByte()
        parentFingerprint.copyInto(buf, 5)
        ser32(childNumber).copyInto(buf, 9)
        chainCode.copyInto(buf, 13)
        val isPrivateVersion = version == VERSION_XPRV || version == VERSION_ZPRV
        if (isPrivateVersion) {
            buf[45] = 0
            key.copyInto(buf, 46)
        } else {
            publicKey().copyInto(buf, 45)
        }
        return Base58.encodeChecked(buf)
    }

    private fun ser32(v: Int): ByteArray = byteArrayOf(
        ((v ushr 24) and 0xff).toByte(),
        ((v ushr 16) and 0xff).toByte(),
        ((v ushr 8) and 0xff).toByte(),
        (v and 0xff).toByte()
    )

    private fun toBytes32(v: BigInteger): ByteArray {
        val b = v.toByteArray()
        return when {
            b.size == 32 -> b
            b.size > 32 -> b.copyOfRange(b.size - 32, b.size)
            else -> ByteArray(32 - b.size) + b
        }
    }
}
