package ir.mehrdad.walletfinder.crypto

/** Bitcoin address encodings from a compressed public key. */
object Addresses {

    /** Legacy P2PKH address ("1..."), BIP-44 style. */
    fun p2pkh(pubKey: ByteArray): String =
        Base58.encodeChecked(byteArrayOf(0x00) + Hash.hash160(pubKey))

    /** Native SegWit P2WPKH bech32 address ("bc1q..."), BIP-84 style. */
    fun p2wpkh(pubKey: ByteArray): String =
        Bech32.encodeSegwitAddress("bc", 0, Hash.hash160(pubKey))

    /** Nested SegWit P2SH-P2WPKH address ("3..."), BIP-49 style. */
    fun p2shP2wpkh(pubKey: ByteArray): String {
        val redeemScript = byteArrayOf(0x00, 0x14) + Hash.hash160(pubKey)
        return Base58.encodeChecked(byteArrayOf(0x05) + Hash.hash160(redeemScript))
    }

    /** Lightweight sanity check before querying a balance. */
    fun looksValid(address: String): Boolean {
        val a = address.trim()
        return when {
            a.startsWith("bc1q") || a.startsWith("bc1p") ->
                a.length in 40..64 && a.all { it in BECH32_CHARS }
            a.startsWith("1") || a.startsWith("3") -> try {
                val payload = Base58.decodeChecked(a)
                payload.isNotEmpty() && (payload[0] == 0x00.toByte() || payload[0] == 0x05.toByte())
            } catch (e: Exception) {
                false
            }
            else -> false
        }
    }

    private const val BECH32_CHARS = "qpzry9x8gf2tvdw0s3jn54khce6mua7l"
}

/** Standard derivation paths used by the app when expanding a seed phrase. */
object DerivationPaths {
    const val BIP44_ACCOUNT = "m/44'/0'/0'"
    const val BIP49_ACCOUNT = "m/49'/0'/0'"
    const val BIP84_ACCOUNT = "m/84'/0'/0'"

    fun receiving(pathAccount: String, i: Int) = "$pathAccount/0/$i"
}
