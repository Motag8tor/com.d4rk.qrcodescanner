package my.qrcode.scanner.feature.tabs.settings.more
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import my.qrcode.scanner.R
class MoreFragment : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_more)
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction().replace(R.id.more, SettingsFragment()).commit()
        }
    }
    class SettingsFragment : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.more, rootKey)
        }
    }
}