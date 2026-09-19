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
import ir.mehrdad.walletfinder.crypto.Wif
import ir.mehrdad.walletfinder.crypto.WifInfo
import ir.mehrdad.walletfinder.net.BalanceChecker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Tools for single keys/addresses: WIF → addresses, and any-address balance lookup. */
class KeyToolFragment : Fragment() {

    private lateinit var etWif: EditText
    private lateinit var btnWif: MaterialButton
    private lateinit var tvWifResult: TextView
    private lateinit var rvWifAddresses: RecyclerView
    private lateinit var wifAdapter: AddressAdapter

    private lateinit var etAddress: EditText
    private lateinit var btnAddress: MaterialButton
    private lateinit var tvAddressResult: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_key_tool, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        etWif = view.findViewById(R.id.etWif)
        btnWif = view.findViewById(R.id.btnWif)
        tvWifResult = view.findViewById(R.id.tvWifResult)
        rvWifAddresses = view.findViewById(R.id.rvWifAddresses)

        wifAdapter = AddressAdapter(::copyText, ::checkBalance)
        rvWifAddresses.layoutManager = LinearLayoutManager(requireContext())
        rvWifAddresses.adapter = wifAdapter

        etAddress = view.findViewById(R.id.etAddress)
        btnAddress = view.findViewById(R.id.btnAddress)
        tvAddressResult = view.findViewById(R.id.tvAddressResult)

        btnWif.setOnClickListener { convertWif() }
        btnAddress.setOnClickListener { checkAddress() }
    }

    // ---------------- WIF ----------------

    private fun convertWif() {
        val input = etWif.text.toString().trim()
        wifAdapter.submit(emptyList())
        if (input.isEmpty()) {
            tvWifResult.setText(R.string.wif_enter_first)
            return
        }
        val info: WifInfo = try {
            Wif.parse(input)
        } catch (e: IllegalArgumentException) {
            tvWifResult.setText(R.string.wif_invalid)
            return
        } catch (e: Exception) {
            tvWifResult.setText(R.string.wif_invalid)
            return
        }

        if (!info.isMainnet) {
            tvWifResult.setText(R.string.wif_testnet)
            return
        }

        val rows = mutableListOf<AddressAdapter.Row>()
        val legacyPub = info.publicKey()
        rows += AddressAdapter.Row(
            getString(R.string.addr_legacy), "WIF", Addresses.p2pkh(legacyPub)
        )
        if (info.compressed) {
            val compPub = info.compressedPublicKey()
            rows += AddressAdapter.Row(
                getString(R.string.addr_p2sh), "WIF (compressed)", Addresses.p2shP2wpkh(compPub)
            )
            rows += AddressAdapter.Row(
                getString(R.string.addr_segwit), "WIF (compressed)", Addresses.p2wpkh(compPub)
            )
            tvWifResult.setText(R.string.wif_ok_compressed)
        } else {
            tvWifResult.setText(R.string.wif_ok_uncompressed)
        }
        wifAdapter.submit(rows)
    }

    // ---------------- address balance ----------------

    private fun checkAddress() {
        val address = etAddress.text.toString().trim()
        if (address.isEmpty()) {
            tvAddressResult.setText(R.string.addr_enter_first)
            return
        }
        if (!Addresses.looksValid(address)) {
            tvAddressResult.setText(R.string.addr_invalid)
            return
        }
        tvAddressResult.setText(R.string.addr_checking)
        btnAddress.isEnabled = false
        viewLifecycleOwner.lifecycleScope.launch {
            val outcome = withContext(Dispatchers.IO) {
                runCatching { BalanceChecker.fetch(address) }
            }
            tvAddressResult.text = outcome.fold(
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
            btnAddress.isEnabled = true
        }
    }

    private fun checkBalance(row: AddressAdapter.Row) {
        wifAdapter.setLoading(row.address, true)
        viewLifecycleOwner.lifecycleScope.launch {
            val outcome = withContext(Dispatchers.IO) {
                runCatching { BalanceChecker.fetch(row.address) }
            }
            wifAdapter.setBalance(
                row.address,
                outcome.fold(
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
            )
        }
    }

    private fun copyText(text: String) {
        val cm = ContextCompat.getSystemService(requireContext(), ClipboardManager::class.java)
        cm?.setPrimaryClip(ClipData.newPlainText("wallet", text))
        Snackbar.make(requireView(), R.string.copied, Snackbar.LENGTH_SHORT).show()
    }
}
