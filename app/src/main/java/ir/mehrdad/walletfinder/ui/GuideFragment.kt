package ir.mehrdad.walletfinder.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.text.HtmlCompat
import androidx.fragment.app.Fragment
import ir.mehrdad.walletfinder.R

class GuideFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_guide, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        view.findViewById<TextView>(R.id.tvGuideBody).text = HtmlCompat.fromHtml(
            getString(R.string.guide_body), HtmlCompat.FROM_HTML_MODE_COMPACT
        )
        view.findViewById<TextView>(R.id.tvSecurityBody).text = HtmlCompat.fromHtml(
            getString(R.string.guide_security_body), HtmlCompat.FROM_HTML_MODE_COMPACT
        )
        view.findViewById<TextView>(R.id.tvHonestBody).text = HtmlCompat.fromHtml(
            getString(R.string.guide_honest_body), HtmlCompat.FROM_HTML_MODE_COMPACT
        )
    }
}
