package ir.mehrdad.walletfinder.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.snackbar.Snackbar
import ir.mehrdad.walletfinder.R
import ir.mehrdad.walletfinder.crypto.Addresses
import ir.mehrdad.walletfinder.crypto.Bip39
import ir.mehrdad.walletfinder.crypto.DerivationPaths
import ir.mehrdad.walletfinder.crypto.HdNode
import ir.mehrdad.walletfinder.net.BalanceChecker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SeedCheckFragment : Fragment() {

    private lateinit var etPhrase: EditText
    private lateinit var etPassphrase: EditText
    private lateinit var btnCheck: MaterialButton
    private lateinit var tvResult: TextView
    private lateinit var recycler: RecyclerView
    private lateinit var adapter: AddressAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_seed_check, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        etPhrase = view.findViewById(R.id.etPhrase)
        etPassphrase = view.findViewById(R.id.etPassphrase)
        btnCheck = view.findViewById(R.id.btnCheck)
        tvResult = view.findViewById(R.id.tvResult)
        recycler = view.findViewById(R.id.recyclerAddresses)

        adapter = AddressAdapter(::copyAddress, ::checkBalance)
        recycler.layoutManager = LinearLayoutManager(requireContext())
        recycler.adapter = adapter

        btnCheck.setOnClickListener { checkPhrase() }
    }

    private fun checkPhrase() {
        val bip39 = Bip39.get(requireContext())
        val raw = etPhrase.text.toString().trim()
        val words = raw.lowercase().split(Regex("\\s+")).filter { it.isNotEmpty() }
        adapter.submit(emptyList())

        if (words.isEmpty()) {
            tvResult.setText(R.string.seed_enter_first)
            return
        }
        val unknown = bip39.firstUnknownWord(raw)
        if (unknown != null) {
            tvResult.text = getString(R.string.seed_unknown_word, unknown)
            return
        }
        if (words.size !in setOf(12, 15, 18, 21, 24)) {
            tvResult.text = getString(R.string.seed_bad_count, words.size)
            return
        }
        if (!bip39.isValidMnemonic(words)) {
            tvResult.setText(R.string.seed_bad_checksum)
            return
        }

        val passphrase = etPassphrase.text.toString()
        tvResult.text = getString(R.string.seed_valid, words.size)

        viewLifecycleOwner.lifecycleScope.launch {
            val rows = withContext(Dispatchers.Default) {
                buildAddressRows(bip39, words, passphrase)
            }
            adapter.submit(rows)
        }
    }

    private fun buildAddressRows(
        bip39: Bip39,
        words: List<String>,
        passphrase: String
    ): List<AddressAdapter.Row> {
        val mnemonic = words.joinToString(" ")
        val seed = bip39.toSeed(mnemonic, passphrase)
        val root = HdNode.fromSeed(seed)
        val rows = mutableListOf<AddressAdapter.Row>()
        for (i in 0..1) {
            val path = DerivationPaths.receiving(DerivationPaths.BIP84_ACCOUNT, i)
            val pub = root.derivePath(path).publicKey()
            rows += AddressAdapter.Row(
                label = getString(R.string.addr_segwit) + " ${i + 1}",
                path = path,
                address = Addresses.p2wpkh(pub)
            )
        }
        for (i in 0..1) {
            val path = DerivationPaths.receiving(DerivationPaths.BIP44_ACCOUNT, i)
            val pub = root.derivePath(path).publicKey()
            rows += AddressAdapter.Row(
                label = getString(R.string.addr_legacy) + " ${i + 1}",
                path = path,
                address = Addresses.p2pkh(pub)
            )
        }
        return rows
    }

    private fun copyAddress(address: String) {
        val cm = ContextCompat.getSystemService(requireContext(), ClipboardManager::class.java)
        cm?.setPrimaryClip(ClipData.newPlainText("address", address))
        Snackbar.make(requireView(), R.string.copied, Snackbar.LENGTH_SHORT).show()
    }

    private fun checkBalance(holder: AddressAdapter.VH, row: AddressAdapter.Row) {
        holder.setLoading(true)
        viewLifecycleOwner.lifecycleScope.launch {
            val outcome = withContext(Dispatchers.IO) {
                runCatching { BalanceChecker.fetch(row.address) }
            }
            outcome.fold(
                onSuccess = { info ->
                    holder.setBalance(
                        getString(
                            R.string.balance_result,
                            info.balanceSats,
                            String.format("%.8f", info.balanceSats / 100_000_000.0),
                            info.txCount
                        )
                    )
                },
                onFailure = {
                    holder.setBalance(getString(R.string.balance_error))
                }
            )
            holder.setLoading(false)
        }
    }
}
