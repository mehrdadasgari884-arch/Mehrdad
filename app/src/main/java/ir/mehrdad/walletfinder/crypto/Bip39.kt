package ir.mehrdad.walletfinder.crypto

import android.content.Context
import java.text.Normalizer

/**
 * BIP-39 mnemonic phrases: validation (wordlist + checksum) and seed derivation.
 * Validated against all 24 official English test vectors (Trezor reference).
 */
class Bip39(private val words: List<String>) {

    private val index: Map<String, Int> = words.withIndex().associate { (i, w) -> w to i }

    val size: Int get() = words.size

    fun containsWord(word: String): Boolean = index.containsKey(word)

    fun isValidMnemonic(mnemonic: String): Boolean =
        isValidMnemonic(mnemonic.trim().lowercase().split(Regex("\\s+")).filter { it.isNotEmpty() })

    fun isValidMnemonic(wordList: List<String>): Boolean {
        if (wordList.size !in VALID_LENGTHS) return false
        val idxs = IntArray(wordList.size)
        for (i in wordList.indices) {
            idxs[i] = index[wordList[i]] ?: return false
        }
        val bits = StringBuilder(wordList.size * 11)
        for (i in idxs) bits.append(i.toString(2).padStart(11, '0'))
        val csLen = wordList.size / 3
        val entLen = wordList.size * 11 - csLen
        val entBytes = ByteArray(entLen / 8)
        for (b in entBytes.indices) {
            var v = 0
            for (k in 0..7) v = (v shl 1) or bits[b * 8 + k].digitToInt()
            entBytes[b] = v.toByte()
        }
        val hashByte = Hash.sha256(entBytes)[0].toInt() and 0xff
        val csBits = (0 until csLen).joinToString("") { i ->
            ((hashByte ushr (7 - i)) and 1).toString()
        }
        return csBits == bits.substring(entLen)
    }

    /** First word that is not in the BIP-39 wordlist (for helpful error messages). */
    fun firstUnknownWord(mnemonic: String): String? {
        for (w in mnemonic.trim().lowercase().split(Regex("\\s+"))) {
            if (w.isNotEmpty() && !index.containsKey(w)) return w
        }
        return null
    }

    /** BIP-39 seed: PBKDF2-HMAC-SHA512, 2048 iterations, salt = "mnemonic" + passphrase. */
    fun toSeed(mnemonic: String, passphrase: String = ""): ByteArray {
        val mn = Normalizer.normalize(mnemonic, Normalizer.Form.NFKD)
        val salt = Normalizer.normalize("mnemonic$passphrase", Normalizer.Form.NFKD)
        return Hash.pbkdf2Sha512(mn, salt, 2048, 64)
    }

    companion object {
        private val VALID_LENGTHS = setOf(12, 15, 18, 21, 24)

        @Volatile
        private var instance: Bip39? = null

        /** Loads the 2048-word English wordlist from assets once per process. */
        fun get(context: Context): Bip39 {
            return instance ?: synchronized(this) {
                instance ?: run {
                    val text = context.applicationContext.assets
                        .open("bip39_english.txt")
                        .bufferedReader()
                        .use { it.readText() }
                    Bip39(text.trim().split(Regex("\\s+"))).also { instance = it }
                }
            }
        }
    }
}
