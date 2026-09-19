package ir.mehrdad.walletfinder

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.tabs.TabLayout
import ir.mehrdad.walletfinder.ui.GuideFragment
import ir.mehrdad.walletfinder.ui.ScanFragment
import ir.mehrdad.walletfinder.ui.SeedCheckFragment

class MainActivity : AppCompatActivity() {

    private lateinit var fragments: List<Fragment>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        fragments = listOf(ScanFragment(), SeedCheckFragment(), GuideFragment())
        supportFragmentManager.beginTransaction()
            .add(R.id.fragmentContainer, fragments[2], "guide").hide(fragments[2])
            .add(R.id.fragmentContainer, fragments[1], "seed").hide(fragments[1])
            .add(R.id.fragmentContainer, fragments[0], "scan")
            .commit()

        val tabs = findViewById<TabLayout>(R.id.tabs)
        tabs.addTab(tabs.newTab().setText(R.string.tab_scan))
        tabs.addTab(tabs.newTab().setText(R.string.tab_seed))
        tabs.addTab(tabs.newTab().setText(R.string.tab_guide))
        tabs.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) = showFragment(tab.position)
            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })
    }

    private fun showFragment(position: Int) {
        val tx = supportFragmentManager.beginTransaction()
        fragments.forEachIndexed { i, f -> if (i == position) tx.show(f) else tx.hide(f) }
        tx.commit()
    }
}
