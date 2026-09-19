package ir.mehrdad.walletfinder.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.progressindicator.CircularProgressIndicator
import ir.mehrdad.walletfinder.R

/**
 * Derived addresses (from a seed phrase or a WIF key) with copy + balance check.
 * Balance/loading states are tracked per address so they survive recycling.
 */
class AddressAdapter(
    private val onCopy: (String) -> Unit,
    private val onBalance: (Row) -> Unit
) : RecyclerView.Adapter<AddressAdapter.VH>() {

    data class Row(val label: String, val path: String, val address: String)

    private var rows: List<Row> = emptyList()
    private val balances = HashMap<String, String>()
    private val loading = HashSet<String>()

    fun submit(newRows: List<Row>) {
        rows = newRows
        balances.clear()
        loading.clear()
        notifyDataSetChanged()
    }

    fun setLoading(address: String, isLoading: Boolean) {
        if (isLoading) loading.add(address) else loading.remove(address)
        refresh(address)
    }

    fun setBalance(address: String, text: String) {
        balances[address] = text
        loading.remove(address)
        refresh(address)
    }

    private fun refresh(address: String) {
        val i = rows.indexOfFirst { it.address == address }
        if (i >= 0) notifyItemChanged(i)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
        VH(LayoutInflater.from(parent.context).inflate(R.layout.item_address, parent, false))

    override fun getItemCount(): Int = rows.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val row = rows[position]
        holder.tvLabel.text = row.label
        holder.tvAddress.text = row.address
        val balance = balances[row.address]
        if (balance != null) {
            holder.tvBalance.text = balance
            holder.tvBalance.visibility = View.VISIBLE
        } else {
            holder.tvBalance.visibility = View.GONE
        }
        holder.progress.visibility = if (loading.contains(row.address)) View.VISIBLE else View.GONE
        holder.btnBalance.isEnabled = !loading.contains(row.address)
        holder.btnCopy.setOnClickListener { onCopy(row.address) }
        holder.btnBalance.setOnClickListener { onBalance(row) }
    }

    class VH(view: View) : RecyclerView.ViewHolder(view) {
        val tvLabel: TextView = view.findViewById(R.id.tvLabel)
        val tvAddress: TextView = view.findViewById(R.id.tvAddress)
        val tvBalance: TextView = view.findViewById(R.id.tvBalance)
        val btnCopy: MaterialButton = view.findViewById(R.id.btnCopy)
        val btnBalance: MaterialButton = view.findViewById(R.id.btnBalance)
        val progress: CircularProgressIndicator = view.findViewById(R.id.balanceProgress)
    }
}
