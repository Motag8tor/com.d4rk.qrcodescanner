package my.qrcode.scanner.feature.tabs.settings.permissions
import android.content.Context
import android.content.Intent
import android.os.Bundle
import my.qrcode.scanner.R
import my.qrcode.scanner.extension.applySystemWindowInsets
import my.qrcode.scanner.feature.BaseActivity
import kotlinx.android.synthetic.main.activity_all_permissions.toolbar
import kotlinx.android.synthetic.main.activity_all_permissions.root_view
class AllPermissionsActivity : BaseActivity() {
    companion object {
        fun start(context: Context) {
            val intent = Intent(context, AllPermissionsActivity::class.java)
            context.startActivity(intent)
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_all_permissions)
        root_view.applySystemWindowInsets(applyTop = true, applyBottom = true)
        toolbar.setNavigationOnClickListener { finish() }
    }
}