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
    private lateinit var btnCheckAll: MaterialButton
    private lateinit var tvResult: TextView
    private lateinit var recycler: RecyclerView
    private lateinit var adapter: AddressAdapter

    private var currentRows: List<AddressAdapter.Row> = emptyList()
    private var checkingAll = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_seed_check, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        etPhrase = view.findViewById(R.id.etPhrase)
        etPassphrase = view.findViewById(R.id.etPassphrase)
        btnCheck = view.findViewById(R.id.btnCheck)
        btnCheckAll = view.findViewById(R.id.btnCheckAll)
        tvResult = view.findViewById(R.id.tvResult)
        recycler = view.findViewById(R.id.recyclerAddresses)

        adapter = AddressAdapter(::copyAddress, ::checkBalance)
        recycler.layoutManager = LinearLayoutManager(requireContext())
        recycler.adapter = adapter

        btnCheck.setOnClickListener { checkPhrase() }
        btnCheckAll.setOnClickListener { checkAllBalances() }
        btnCheckAll.visibility = View.GONE
    }

    private fun checkPhrase() {
        val bip39 = Bip39.get(requireContext())
        val raw = etPhrase.text.toString().trim()
        val words = raw.lowercase().split(Regex("\\s+")).filter { it.isNotEmpty() }
        currentRows = emptyList()
        adapter.submit(emptyList())
        btnCheckAll.visibility = View.GONE

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
            currentRows = rows
            adapter.submit(rows)
            btnCheckAll.visibility = View.VISIBLE
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

        val types: List<Triple<String, Int, (ByteArray) -> String>> = listOf(
            Triple(DerivationPaths.BIP84_ACCOUNT, R.string.addr_segwit, { pub: ByteArray -> Addresses.p2wpkh(pub) }),
            Triple(DerivationPaths.BIP49_ACCOUNT, R.string.addr_p2sh, { pub: ByteArray -> Addresses.p2shP2wpkh(pub) }),
            Triple(DerivationPaths.BIP44_ACCOUNT, R.string.addr_legacy, { pub: ByteArray -> Addresses.p2pkh(pub) })
        )
        for ((account, labelRes, encode) in types) {
            for (i in 0..1) {
                val path = DerivationPaths.receiving(account, i)
                val pub = root.derivePath(path).publicKey()
                rows += AddressAdapter.Row(
                    label = getString(labelRes) + " ${i + 1}",
                    path = path,
                    address = encode(pub)
                )
            }
        }
        return rows
    }

    private fun copyAddress(address: String) {
        val cm = ContextCompat.getSystemService(requireContext(), ClipboardManager::class.java)
        cm?.setPrimaryClip(ClipData.newPlainText("address", address))
        Snackbar.make(requireView(), R.string.copied, Snackbar.LENGTH_SHORT).show()
    }

    private fun checkBalance(row: AddressAdapter.Row) {
        adapter.setLoading(row.address, true)
        viewLifecycleOwner.lifecycleScope.launch {
            val outcome = withContext(Dispatchers.IO) {
                runCatching { BalanceChecker.fetch(row.address) }
            }
            adapter.setBalance(row.address, balanceText(outcome))
        }
    }

    private fun checkAllBalances() {
        if (checkingAll || currentRows.isEmpty()) return
        checkingAll = true
        btnCheckAll.isEnabled = false
        viewLifecycleOwner.lifecycleScope.launch {
            for (row in currentRows) {
                adapter.setLoading(row.address, true)
                val outcome = withContext(Dispatchers.IO) {
                    runCatching { BalanceChecker.fetch(row.address) }
                }
                adapter.setBalance(row.address, balanceText(outcome))
            }
            checkingAll = false
            btnCheckAll.isEnabled = true
        }
    }

    private fun balanceText(outcome: Result<BalanceChecker.Info>): String = outcome.fold(
        onSuccess = { info ->
            getString(
                R.string.balance_result,
                info.balanceSats,
                String.format("%.8f", info.balanceSats / 100_000_000.0),
                info.txCount
            )
        },
        onFailure = { getString(R.string.balance_error) }
    )
}
