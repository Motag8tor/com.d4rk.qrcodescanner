package my.qrcode.scanner
import androidx.multidex.MultiDexApplication
import my.qrcode.scanner.di.settings

class App : MultiDexApplication() {
    override fun onCreate() {
        applyTheme()
        super.onCreate()
    }
    private fun applyTheme() {
        settings.reapplyTheme()
    }
}