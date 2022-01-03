package com.d4rk.qrcodescanner
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.d4rk.qrcodescanner.feature.tabs.BottomTabsActivity
import com.d4rk.qrcodescanner.di.settings
class SplashScreenActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash_screen)
        Handler(Looper.getMainLooper()).postDelayed({
            val intent = Intent(this, BottomTabsActivity::class.java)
            startActivity(intent)
            finish()
        }, 2000)
    }
    private fun applyTheme() {
        settings.reapplyTheme()
    }
}