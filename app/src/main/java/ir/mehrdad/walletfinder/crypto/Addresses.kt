package ir.mehrdad.walletfinder.crypto

/** Bitcoin address encodings from a compressed public key. */
object Addresses {

    /** Legacy P2PKH address ("1..."), BIP-44 style. */
    fun p2pkh(pubKey: ByteArray): String =
        Base58.encodeChecked(byteArrayOf(0x00) + Hash.hash160(pubKey))

    /** Native SegWit P2WPKH bech32 address ("bc1q..."), BIP-84 style. */
    fun p2wpkh(pubKey: ByteArray): String =
        Bech32.encodeSegwitAddress("bc", 0, Hash.hash160(pubKey))
}

/** Standard derivation paths used by the app when expanding a seed phrase. */
object DerivationPaths {
    const val BIP44_ACCOUNT = "m/44'/0'/0'"
    const val BIP84_ACCOUNT = "m/84'/0'/0'"

    fun receiving(pathAccount: String, i: Int) = "$pathAccount/0/$i"
}
