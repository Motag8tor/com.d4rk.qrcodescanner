package my.qrcode.scanner.feature.tabs.settings.search
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import my.qrcode.scanner.R
import my.qrcode.scanner.di.settings
import my.qrcode.scanner.extension.applySystemWindowInsets
import my.qrcode.scanner.extension.unsafeLazy
import my.qrcode.scanner.feature.BaseActivity
import my.qrcode.scanner.feature.common.view.SettingsRadioButton
import my.qrcode.scanner.model.SearchEngine
import kotlinx.android.synthetic.main.activity_choose_search_engine.button_ask_every_time
import kotlinx.android.synthetic.main.activity_choose_search_engine.button_bing
import kotlinx.android.synthetic.main.activity_choose_search_engine.button_duck_duck_go
import kotlinx.android.synthetic.main.activity_choose_search_engine.button_google
import kotlinx.android.synthetic.main.activity_choose_search_engine.button_none
import kotlinx.android.synthetic.main.activity_choose_search_engine.button_qwant
import kotlinx.android.synthetic.main.activity_choose_search_engine.button_yahoo
import kotlinx.android.synthetic.main.activity_choose_search_engine.button_startpage
import kotlinx.android.synthetic.main.activity_choose_search_engine.button_yandex
import kotlinx.android.synthetic.main.activity_choose_search_engine.root_view
import kotlinx.android.synthetic.main.activity_choose_search_engine.toolbar
class ChooseSearchEngineActivity : BaseActivity() {
    companion object {
        fun start(context: Context) {
            val intent = Intent(context, ChooseSearchEngineActivity::class.java)
            context.startActivity(intent)
        }
    }
    private val buttons by unsafeLazy {
        listOf(button_none, button_ask_every_time, button_bing, button_duck_duck_go, button_google, button_qwant, button_startpage, button_yahoo, button_yandex)
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_choose_search_engine)
        supportEdgeToEdge()
        initToolbar()
        showInitialValue()
        handleSettingsChanged()
    }
    private fun supportEdgeToEdge() {
        root_view.applySystemWindowInsets(applyTop = true, applyBottom = true)
    }
    private fun initToolbar() {
        toolbar.setNavigationOnClickListener { finish() }
    }
    private fun showInitialValue() {
        when (settings.searchEngine) {
            SearchEngine.NONE -> button_none.isChecked = true
            SearchEngine.ASK_EVERY_TIME -> button_ask_every_time.isChecked = true
            SearchEngine.STARTPAGE -> button_startpage.isChecked = true
            SearchEngine.BING -> button_bing.isChecked = true
            SearchEngine.DUCK_DUCK_GO -> button_duck_duck_go.isChecked = true
            SearchEngine.GOOGLE -> button_google.isChecked = true
            SearchEngine.QWANT -> button_qwant.isChecked = true
            SearchEngine.YAHOO -> button_yahoo.isChecked = true
            SearchEngine.YANDEX -> button_yandex.isChecked = true
        }
    }
    private fun handleSettingsChanged() {
        button_none.setCheckedChangedListener(SearchEngine.NONE)
        button_ask_every_time.setCheckedChangedListener(SearchEngine.ASK_EVERY_TIME)
        button_bing.setCheckedChangedListener(SearchEngine.BING)
        button_startpage.setCheckedChangedListener(SearchEngine.STARTPAGE)
        button_duck_duck_go.setCheckedChangedListener(SearchEngine.DUCK_DUCK_GO)
        button_google.setCheckedChangedListener(SearchEngine.GOOGLE)
        button_qwant.setCheckedChangedListener(SearchEngine.QWANT)
        button_yahoo.setCheckedChangedListener(SearchEngine.YAHOO)
        button_yandex.setCheckedChangedListener(SearchEngine.YANDEX)
    }
    private fun SettingsRadioButton.setCheckedChangedListener(searchEngine: SearchEngine) {
        setCheckedChangedListener { isChecked ->
            if (isChecked) {
                uncheckOtherButtons(this)
                settings.searchEngine = searchEngine
            }
        }
    }
    private fun uncheckOtherButtons(checkedButton: View) {
        buttons.forEach { button ->
            if (checkedButton !== button) {
                button.isChecked = false
            }
        }
    }
}