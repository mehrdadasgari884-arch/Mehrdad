package ir.mehrdad.walletfinder.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import ir.mehrdad.walletfinder.R
import ir.mehrdad.walletfinder.scan.SeedHit
import ir.mehrdad.walletfinder.scan.WalletFileHit

/** Mixed list: section headers, wallet files and found seed phrases. */
class ResultsAdapter(
    private val onClick: (Row) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    sealed class Row {
        data class Header(val title: String) : Row()
        data class File(val hit: WalletFileHit) : Row()
        data class Seed(val hit: SeedHit) : Row()
    }

    private var rows: List<Row> = emptyList()

    fun submit(newRows: List<Row>) {
        rows = newRows
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int = when (rows[position]) {
        is Row.Header -> 0
        is Row.File -> 1
        is Row.Seed -> 2
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            0 -> HeaderVH(inflater.inflate(R.layout.item_header, parent, false))
            1 -> FileVH(inflater.inflate(R.layout.item_wallet_file, parent, false))
            else -> SeedVH(inflater.inflate(R.layout.item_seed, parent, false))
        }
    }

    override fun getItemCount(): Int = rows.size

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val row = rows[position]) {
            is Row.Header -> (holder as HeaderVH).bind(row.title)
            is Row.File -> (holder as FileVH).bind(row.hit) { onClick(row) }
            is Row.Seed -> (holder as SeedVH).bind(row.hit) { onClick(row) }
        }
    }

    class HeaderVH(view: View) : RecyclerView.ViewHolder(view) {
        private val tv: TextView = view.findViewById(R.id.tvHeader)
        fun bind(title: String) { tv.text = title }
    }

    class FileVH(view: View) : RecyclerView.ViewHolder(view) {
        private val tvTitle: TextView = view.findViewById(R.id.tvTitle)
        private val tvSubtitle: TextView = view.findViewById(R.id.tvSubtitle)
        private val tvKind: TextView = view.findViewById(R.id.tvKind)
        fun bind(hit: WalletFileHit, click: () -> Unit) {
            tvTitle.text = hit.name
            tvSubtitle.text = hit.path
            tvKind.text = hit.kind
            itemView.setOnClickListener { click() }
        }
    }

    class SeedVH(view: View) : RecyclerView.ViewHolder(view) {
        private val tvTitle: TextView = view.findViewById(R.id.tvTitle)
        private val tvSubtitle: TextView = view.findViewById(R.id.tvSubtitle)
        fun bind(hit: SeedHit, click: () -> Unit) {
            val masked = hit.words.take(2).joinToString(" ") +
                "  ••••  " + hit.words.last()
            tvTitle.text = itemView.context.getString(R.string.seed_item_title, hit.words.size, masked)
            tvSubtitle.text = hit.filePath
            itemView.setOnClickListener { click() }
        }
    }
}
