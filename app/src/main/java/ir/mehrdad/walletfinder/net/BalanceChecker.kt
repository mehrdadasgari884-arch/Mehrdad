package ir.mehrdad.walletfinder.net

import org.json.JSONObject
import java.io.IOException
import java.net.URI
import java.net.URL
import javax.net.ssl.HttpsURLConnection

/**
 * Optional, explicit balance lookup over the public mempool.space API.
 * Only the *public address* is sent — never a seed phrase or private key.
 */
object BalanceChecker {

    data class Info(val balanceSats: Long, val txCount: Int)

    fun fetch(address: String): Info {
        val url: URL = URI("https://mempool.space/api/address/$address").toURL()
        val conn = url.openConnection() as HttpsURLConnection
        conn.connectTimeout = 12_000
        conn.readTimeout = 12_000
        conn.setRequestProperty("User-Agent", "WalletFinder/1.0")
        try {
            if (conn.responseCode != 200) throw IOException("HTTP ${conn.responseCode}")
            val body = conn.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(body)
            val chain = json.getJSONObject("chain_stats")
            val mem = json.optJSONObject("mempool_stats")
            val balance = chain.getLong("funded_txo_sum") - chain.getLong("spent_txo_sum") +
                (mem?.let { it.optLong("funded_txo_sum", 0) - it.optLong("spent_txo_sum", 0) } ?: 0L)
            val txCount = chain.getInt("tx_count") + (mem?.optInt("tx_count", 0) ?: 0)
            return Info(balance, txCount)
        } finally {
            conn.disconnect()
        }
    }
}
