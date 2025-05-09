package cn.hellokk.ble.ui

import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.viewpager.widget.ViewPager
import cn.hellokk.ble.R
import cn.hellokk.ble.databinding.ActivityViewPagerBinding
import cn.hellokk.ble.ui.adapter.SectionsPagerAdapter

class ViewPagerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityViewPagerBinding

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityViewPagerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setStatusBarColor(R.color.color_3a89ff, R.color.white, false)
        binding.title.setOnClickListener { onBackPressed() }
        val sectionsPagerAdapter = SectionsPagerAdapter(this, supportFragmentManager)
        val viewPager: ViewPager = binding.viewPager
        viewPager.adapter = sectionsPagerAdapter
        viewPager.addOnPageChangeListener(object : ViewPager.SimpleOnPageChangeListener() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                when(position) {
                    0 -> setStatusBarColor(R.color.color_3a89ff, R.color.white, false)
                    1 -> setStatusBarColor(R.color.white, R.color.white, true)
                    2 -> setStatusBarColor(R.color.f6f6f6, R.color.f6f6f6, true)
                }
            }
        })
        viewPager.currentItem = 0
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun setStatusBarColor(statusBarColor: Int, navigationBarColor: Int, lightStatusBars: Boolean){
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        window.statusBarColor = resources.getColor(statusBarColor, theme)
        window.navigationBarColor = resources.getColor(navigationBarColor, theme)
        ViewCompat.getWindowInsetsController(binding.root)?.isAppearanceLightStatusBars = lightStatusBars
    }

    override fun onBackPressed() {
        super.onBackPressed()
    }
}