package ir.mehrdad.walletfinder.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.progressindicator.CircularProgressIndicator
import ir.mehrdad.walletfinder.R

/** Derived addresses from a validated seed phrase, with copy + optional balance check. */
class AddressAdapter(
    private val onCopy: (String) -> Unit,
    private val onBalance: (VH, Row) -> Unit
) : RecyclerView.Adapter<AddressAdapter.VH>() {

    data class Row(val label: String, val path: String, val address: String)

    private var rows: List<Row> = emptyList()

    fun submit(newRows: List<Row>) {
        rows = newRows
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
        VH(LayoutInflater.from(parent.context).inflate(R.layout.item_address, parent, false))

    override fun getItemCount(): Int = rows.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val row = rows[position]
        holder.bind(row, onCopy, onBalance)
    }

    class VH(view: View) : RecyclerView.ViewHolder(view) {
        private val tvLabel: TextView = view.findViewById(R.id.tvLabel)
        private val tvAddress: TextView = view.findViewById(R.id.tvAddress)
        private val tvBalance: TextView = view.findViewById(R.id.tvBalance)
        private val btnCopy: MaterialButton = view.findViewById(R.id.btnCopy)
        private val btnBalance: MaterialButton = view.findViewById(R.id.btnBalance)
        private val progress: CircularProgressIndicator = view.findViewById(R.id.balanceProgress)

        fun bind(row: Row, onCopy: (String) -> Unit, onBalance: (VH, Row) -> Unit) {
            tvLabel.text = row.label
            tvAddress.text = row.address
            tvBalance.text = ""
            tvBalance.visibility = View.GONE
            progress.visibility = View.GONE
            btnCopy.setOnClickListener { onCopy(row.address) }
            btnBalance.setOnClickListener { onBalance(this, row) }
        }

        fun setLoading(loading: Boolean) {
            progress.visibility = if (loading) View.VISIBLE else View.GONE
            btnBalance.isEnabled = !loading
        }

        fun setBalance(text: String) {
            tvBalance.text = text
            tvBalance.visibility = View.VISIBLE
        }
    }
}
