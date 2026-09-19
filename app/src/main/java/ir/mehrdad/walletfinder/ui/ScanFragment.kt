package ir.mehrdad.walletfinder.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import ir.mehrdad.walletfinder.R
import ir.mehrdad.walletfinder.crypto.Bip39
import ir.mehrdad.walletfinder.scan.ScanReport
import ir.mehrdad.walletfinder.scan.SeedHit
import ir.mehrdad.walletfinder.scan.WalletFileHit
import ir.mehrdad.walletfinder.scan.WalletScanner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ScanFragment : Fragment() {

    private lateinit var tvStatus: TextView
    private lateinit var btnPick: MaterialButton
    private lateinit var btnStop: MaterialButton
    private lateinit var recycler: RecyclerView
    private lateinit var btnExport: MaterialButton
    private lateinit var adapter: ResultsAdapter
    private var scanning = false
    private var lastReport: ScanReport? = null

    private val pickFolder =
        registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
            if (uri != null) startScan(uri)
        }

    private val createReport =
        registerForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) { uri ->
            if (uri != null) writeReport(uri)
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_scan, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        tvStatus = view.findViewById(R.id.tvStatus)
        btnPick = view.findViewById(R.id.btnPickFolder)
        btnStop = view.findViewById(R.id.btnStop)
        btnExport = view.findViewById(R.id.btnExport)
        recycler = view.findViewById(R.id.recycler)

        adapter = ResultsAdapter(::onItemClicked)
        recycler.layoutManager = LinearLayoutManager(requireContext())
        recycler.adapter = adapter

        btnPick.setOnClickListener {
            if (!scanning) pickFolder.launch(null)
        }
        btnStop.setOnClickListener { scanJob?.cancel() }
        btnStop.visibility = View.GONE

        btnExport.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.export_confirm_title)
                .setMessage(R.string.export_confirm_body)
                .setPositiveButton(R.string.dialog_ok) { _, _ ->
                    createReport.launch(getString(R.string.export_filename))
                }
                .setNegativeButton(android.R.string.cancel, null)
                .show()
        }
    }

    private var scanJob: kotlinx.coroutines.Job? = null

    private fun startScan(uri: android.net.Uri) {
        try {
            requireContext().contentResolver.takePersistableUriPermission(
                uri,
                android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        } catch (e: Exception) {
            // non-fatal
        }
        scanning = true
        btnPick.isEnabled = false
        btnStop.visibility = View.VISIBLE
        tvStatus.setText(R.string.scan_starting)
        adapter.submit(emptyList())

        val scanner = WalletScanner(requireContext().applicationContext, Bip39.get(requireContext()))
        scanJob = viewLifecycleOwner.lifecycleScope.launch {
            try {
                val report = withContext(Dispatchers.IO) {
                    scanner.scan(uri) { scanned, path ->
                        activity?.runOnUiThread {
                            if (path.isNotEmpty()) {
                                tvStatus.text = getString(R.string.scan_progress, scanned, path)
                            }
                        }
                    }
                }
                showResults(report)
            } catch (e: kotlinx.coroutines.CancellationException) {
                tvStatus.setText(R.string.scan_stopped)
            } catch (e: Exception) {
                tvStatus.setText(R.string.scan_error)
            } finally {
                scanning = false
                btnPick.isEnabled = true
                btnStop.visibility = View.GONE
            }
        }
    }

    private fun showResults(report: ScanReport) {
        lastReport = report
        btnExport.visibility = View.VISIBLE
        val rows = mutableListOf<ResultsAdapter.Row>()
        if (report.walletFiles.isNotEmpty()) {
            rows += ResultsAdapter.Row.Header(getString(R.string.section_wallet_files))
            rows += report.walletFiles.map { ResultsAdapter.Row.File(it) }
        }
        if (report.seeds.isNotEmpty()) {
            rows += ResultsAdapter.Row.Header(getString(R.string.section_seeds))
            rows += report.seeds.map { ResultsAdapter.Row.Seed(it) }
        }
        adapter.submit(rows)

        val suffix = if (report.truncated) getString(R.string.scan_truncated_note) else ""
        tvStatus.text = getString(
            R.string.scan_done,
            report.filesScanned,
            report.walletFiles.size,
            report.seeds.size
        ) + suffix
        if (report.walletFiles.isEmpty() && report.seeds.isEmpty()) {
            tvStatus.append("\n" + getString(R.string.scan_nothing_hint))
        }
    }

    private fun onItemClicked(row: ResultsAdapter.Row) {
        when (row) {
            is ResultsAdapter.Row.File -> showFileDialog(row.hit)
            is ResultsAdapter.Row.Seed -> showSeedDialog(row.hit)
            else -> {}
        }
    }

    private fun showFileDialog(hit: WalletFileHit) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(hit.name)
            .setMessage(
                getString(R.string.dialog_file_body, hit.kind, hit.path, formatSize(hit.sizeBytes), hit.note)
            )
            .setPositiveButton(R.string.dialog_ok, null)
            .setNeutralButton(R.string.dialog_copy_path) { _, _ ->
                copyToClipboard(hit.path)
            }
            .show()
    }

    private fun showSeedDialog(hit: SeedHit) {
        val phrase = hit.words.joinToString(" ")
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.seed_found_title)
            .setMessage(getString(R.string.dialog_seed_body, hit.filePath) + "\n\n" + phrase)
            .setPositiveButton(R.string.dialog_ok, null)
            .setNeutralButton(R.string.dialog_copy_phrase) { _, _ ->
                copyToClipboard(phrase)
                Snackbar.make(requireView(), R.string.copied, Snackbar.LENGTH_SHORT).show()
            }
            .show()
    }

    private fun copyToClipboard(text: String) {
        val cm = ContextCompat.getSystemService(requireContext(), ClipboardManager::class.java)
        cm?.setPrimaryClip(ClipData.newPlainText("wallet", text))
    }

    private fun writeReport(uri: android.net.Uri) {
        val report = lastReport ?: return
        val sb = StringBuilder()
        val date = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.US)
            .format(java.util.Date())
        sb.append(getString(R.string.export_header)).append("\n")
        sb.append(date).append("\n")
        sb.append(getString(
            R.string.scan_done, report.filesScanned, report.walletFiles.size, report.seeds.size
        )).append("\n\n")

        sb.append("== ").append(getString(R.string.section_wallet_files)).append(" ==\n")
        if (report.walletFiles.isEmpty()) sb.append("-\n")
        for (f in report.walletFiles) {
            sb.append("- [").append(f.kind).append("] ").append(f.name)
                .append("  |  ").append(f.path).append("\n")
        }
        sb.append("\n== ").append(getString(R.string.section_seeds)).append(" ==\n")
        if (report.seeds.isEmpty()) sb.append("-\n")
        for (s in report.seeds) {
            sb.append("- ").append(s.filePath).append("\n")
            sb.append("  ").append(s.words.joinToString(" ")).append("\n")
        }
        try {
            requireContext().contentResolver.openOutputStream(uri)?.use { os ->
                os.write(sb.toString().toByteArray(Charsets.UTF_8))
            }
            Snackbar.make(requireView(), R.string.export_saved, Snackbar.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Snackbar.make(requireView(), R.string.export_error, Snackbar.LENGTH_SHORT).show()
        }
    }

    private fun formatSize(bytes: Long): String = when {
        bytes < 0 -> "—"
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        else -> String.format("%.1f MB", bytes / 1024.0 / 1024.0)
    }
}
