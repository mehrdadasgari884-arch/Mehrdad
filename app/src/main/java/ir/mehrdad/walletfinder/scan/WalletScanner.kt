package ir.mehrdad.walletfinder.scan

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import ir.mehrdad.walletfinder.crypto.Bip39
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** A wallet-like file found on the device. */
data class WalletFileHit(
    val name: String,
    val path: String,
    val kind: String,
    val note: String,
    val sizeBytes: Long
)

/** A BIP-39 phrase (with valid checksum) found inside a text file. */
data class SeedHit(
    val words: List<String>,
    val filePath: String
)

data class ScanReport(
    val filesScanned: Int,
    val walletFiles: List<WalletFileHit>,
    val seeds: List<SeedHit>,
    val truncated: Boolean
)

/**
 * Recursively scans a user-selected folder (via Storage Access Framework, so no
 * storage permission is required) looking for:
 *
 *  1. Wallet files — Bitcoin Core wallet.dat (name or Berkeley-DB magic),
 *     Electrum wallets, MultiBit files, generic *.wallet / *.key files,
 *     JSON keystores and files containing extended keys (xprv/zprv/xpub).
 *  2. BIP-39 seed phrases — sequences of 12/15/18/21/24 wordlist words with a
 *     valid checksum, inside text-like files.
 *
 * Everything runs locally on the device; nothing leaves it.
 */
class WalletScanner(
    private val context: Context,
    private val bip39: Bip39
) {

    private val resolver = context.contentResolver

    suspend fun scan(
        rootUri: Uri,
        onProgress: (scanned: Int, currentPath: String) -> Unit
    ): ScanReport = withContext(Dispatchers.IO) {
        val walletFiles = mutableListOf<WalletFileHit>()
        val seeds = mutableListOf<SeedHit>()
        val seenSeeds = HashSet<String>()
        var scanned = 0
        var truncated = false

        val root = DocumentFile.fromTreeUri(context, rootUri)
        if (root == null || !root.exists()) {
            return@withContext ScanReport(0, emptyList(), emptyList(), false)
        }

        // stack of (directory, depth, relative path for display)
        val stack = ArrayDeque<Triple<DocumentFile, Int, String>>()
        stack.addLast(Triple(root, 0, root.name ?: "…"))

        outer@ while (stack.isNotEmpty()) {
            val (dir, depth, dirPath) = stack.removeLast()
            val children: Array<DocumentFile> = try {
                dir.listFiles()
            } catch (e: Exception) {
                emptyArray()
            }
            for (child in children) {
                if (scanned >= MAX_FILES || seeds.size >= MAX_SEEDS) {
                    truncated = true
                    break@outer
                }
                val name = child.name ?: continue
                val childPath = "$dirPath/$name"
                if (child.isDirectory) {
                    if (depth + 1 <= MAX_DEPTH) stack.addLast(Triple(child, depth + 1, childPath))
                    continue
                }
                val length = try { child.length() } catch (e: Exception) { -1L }
                if (length > MAX_TOTAL_FILE_SIZE) continue // skip huge files entirely
                scanned++
                if (scanned % 25 == 0) onProgress(scanned, childPath)
                try {
                    processFile(child, name, childPath, length, walletFiles, seeds, seenSeeds)
                } catch (e: Exception) {
                    // unreadable file — ignore and move on
                }
            }
        }
        onProgress(scanned, "")
        ScanReport(scanned, walletFiles, seeds, truncated)
    }

    // ------------------------------------------------------------------

    private fun processFile(
        doc: DocumentFile,
        name: String,
        path: String,
        size: Long,
        walletFiles: MutableList<WalletFileHit>,
        seeds: MutableList<SeedHit>,
        seenSeeds: HashSet<String>
    ) {
        val lower = name.lowercase()
        val ext = lower.substringAfterLast('.', "")

        // --- binary-ish wallet file detection (needs only the first 64 KB) ---
        var isWalletFile = false
        if (lower == "wallet.dat") {
            walletFiles += WalletFileHit(name, path, KIND_CORE, NOTE_CORE, size)
            isWalletFile = true
        } else if (lower.contains("multibit")) {
            walletFiles += WalletFileHit(name, path, KIND_MULTIBIT, NOTE_MULTIBIT, size)
            isWalletFile = true
        } else {
            val head = readBytes(doc, HEAD_BYTES)
            if (head != null) {
                when {
                    containsBdbMagic(head) -> {
                        walletFiles += WalletFileHit(name, path, KIND_CORE_MAYBE, NOTE_CORE, size)
                        isWalletFile = true
                    }
                    ext == "wallet" -> {
                        val asText = head.toString(Charsets.UTF_8)
                        if (asText.contains("\"keystore\"") || asText.contains("wallet_type")) {
                            walletFiles += WalletFileHit(name, path, KIND_ELECTRUM, NOTE_ELECTRUM, size)
                        } else {
                            walletFiles += WalletFileHit(name, path, KIND_GENERIC_WALLET, NOTE_GENERIC, size)
                        }
                        isWalletFile = true
                    }
                    ext == "key" -> {
                        walletFiles += WalletFileHit(name, path, KIND_KEY_FILE, NOTE_KEY, size)
                        isWalletFile = true
                    }
                    ext == "json" -> {
                        val asText = head.toString(Charsets.UTF_8)
                        when {
                            asText.contains("\"crypto\"") && asText.contains("\"kdf\"") -> {
                                walletFiles += WalletFileHit(name, path, KIND_JSON_KEYSTORE, NOTE_KEYSTORE, size)
                                isWalletFile = true
                            }
                            asText.contains("\"keystore\"") || asText.contains("wallet_type") -> {
                                walletFiles += WalletFileHit(name, path, KIND_ELECTRUM, NOTE_ELECTRUM, size)
                                isWalletFile = true
                            }
                            asText.contains("xprv") || asText.contains("zprv") -> {
                                walletFiles += WalletFileHit(name, path, KIND_XPRV, NOTE_XPRV, size)
                                isWalletFile = true
                            }
                        }
                    }
                    (ext == "txt" || ext == "md" || ext == "csv" || ext == "log") &&
                        head.toString(Charsets.UTF_8).let { it.contains("xprv") || it.contains("zprv") } -> {
                        walletFiles += WalletFileHit(name, path, KIND_XPRV, NOTE_XPRV, size)
                        isWalletFile = true
                    }
                }
            }
        }

        // --- seed-phrase scan in text-like files ---
        if (!isWalletFile && (ext in TEXT_EXTS || ext.isEmpty()) && size <= MAX_TEXT_BYTES) {
            val text = readText(doc, MAX_TEXT_BYTES) ?: return
            extractSeeds(text, path, seeds, seenSeeds)
        }
    }

    private fun extractSeeds(
        text: String,
        path: String,
        seeds: MutableList<SeedHit>,
        seenSeeds: HashSet<String>
    ) {
        val tokens = text.lowercase().split(NON_LETTER).filter { it.isNotEmpty() }
        val run = ArrayList<String>(24)
        for (tok in tokens) {
            if (bip39.containsWord(tok)) {
                run.add(tok)
                if (run.size > 24) run.removeAt(0)
                for (len in WINDOW_LENGTHS) {
                    if (run.size >= len) {
                        val window = run.subList(run.size - len, run.size)
                        if (bip39.isValidMnemonic(window)) {
                            val key = window.joinToString(" ")
                            if (seenSeeds.add(key) && seeds.size < MAX_SEEDS) {
                                seeds += SeedHit(window.toList(), path)
                            }
                        }
                    }
                }
            } else {
                run.clear()
            }
        }
    }

    private fun containsBdbMagic(b: ByteArray): Boolean {
        // Berkeley DB hash-metric magic "b1 05 00" appears in Bitcoin Core wallet.dat
        for (i in 0..b.size - 4) {
            if (b[i] == 0x62.toByte() && b[i + 1] == 0x31.toByte() &&
                b[i + 2] == 0x05.toByte() && b[i + 3] == 0x00.toByte()
            ) return true
        }
        return false
    }

    private fun readBytes(doc: DocumentFile, max: Int): ByteArray? = try {
        resolver.openInputStream(doc.uri)?.use { input ->
            val buf = ByteArray(max)
            var read = 0
            while (read < max) {
                val n = input.read(buf, read, max - read)
                if (n <= 0) break
                read += n
            }
            buf.copyOfRange(0, read)
        }
    } catch (e: Exception) {
        null
    }

    private fun readText(doc: DocumentFile, max: Int): String? {
        val bytes = readBytes(doc, max) ?: return null
        return String(bytes, Charsets.UTF_8)
    }

    private companion object {
        const val MAX_FILES = 20_000
        const val MAX_DEPTH = 8
        const val MAX_TEXT_BYTES = 512 * 1024
        const val MAX_TOTAL_FILE_SIZE = 300L * 1024 * 1024
        const val HEAD_BYTES = 64 * 1024
        const val MAX_SEEDS = 200
        val WINDOW_LENGTHS = intArrayOf(24, 21, 18, 15, 12)
        val TEXT_EXTS = setOf("txt", "md", "csv", "log", "json", "xml", "yml", "yaml", "cfg", "ini")
        val NON_LETTER = Regex("[^a-z]+")

        const val KIND_CORE = "Bitcoin Core — wallet.dat"
        const val KIND_CORE_MAYBE = "Bitcoin Core (احتمالی) — ساختار Berkeley DB"
        const val KIND_ELECTRUM = "کیف پول Electrum"
        const val KIND_MULTIBIT = "کیف پول MultiBit"
        const val KIND_GENERIC_WALLET = "فایل کیف پول (.wallet)"
        const val KIND_KEY_FILE = "فایل کلید (.key)"
        const val KIND_JSON_KEYSTORE = "Keystore رمزنگاری‌شده (JSON)"
        const val KIND_XPRV = "فایل حاوی کلید خصوصی توسعه‌یافته (xprv/zprv)"

        const val NOTE_CORE = "با نسخهٔ دسکتاپ Bitcoin Core یا ابزارهای پشتیبان‌گیری قابل بازیابی است."
        const val NOTE_ELECTRUM = "در اپ دسکتاپ/اندروید Electrum با منوی File → Open باز می‌شود."
        const val NOTE_MULTIBIT = "نرم‌افزار قدیمی موبایل/دسکتاپ؛ با عبارت بازیابی آن بهتر است بازیابی کنید."
        const val NOTE_GENERIC = "احتمالاً فایل کیف پول یک اپ است؛ با همان اپ اصلی بازش کنید."
        const val NOTE_KEY = "فایل کلید خصوصی؛ ممکن است به MultiBit یا اپ‌های قدیمی تعلق داشته باشد."
        const val NOTE_KEYSTORE = "کی‌استور رمزنگاری‌شده (معمولاً اتریومی)؛ با رمز عبورش در همان اپ باز می‌شود."
        const val NOTE_XPRV = "حاوی کلید اصلی کیف پول است! هرگز آن را با کسی به اشتراک نگذارید."
    }
}
