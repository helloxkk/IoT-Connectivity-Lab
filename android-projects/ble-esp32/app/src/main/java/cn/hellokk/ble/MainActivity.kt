package cn.hellokk.ble

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.app.Activity
import android.os.Bundle
import android.view.WindowManager
import androidx.core.view.ViewCompat
import cn.hellokk.ble.R
import cn.hellokk.ble.databinding.ActivityMainBinding


class MainActivity : Activity() {

    private lateinit var animatorSetsuofang: AnimatorSet
    private lateinit var binding: ActivityMainBinding

    @SuppressLint("InlinedApi", "SimpleDateFormat")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        window.statusBarColor = resources.getColor(R.color.f6f6f6, baseContext.theme)
        window.navigationBarColor = resources.getColor(R.color.f6f6f6, baseContext.theme)
        ViewCompat.getWindowInsetsController(binding.root)?.isAppearanceLightStatusBars = true

        binding.title.setOnClickListener { onBackPressed() }
    }

    override fun onResume() {
        super.onResume()
    }

    override fun onPause() {
        super.onPause()
    }

    override fun onBackPressed() {
        super.onBackPressed()
    }

}