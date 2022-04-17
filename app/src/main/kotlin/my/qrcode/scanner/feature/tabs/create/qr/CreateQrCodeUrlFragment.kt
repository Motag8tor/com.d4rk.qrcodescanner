package my.qrcode.scanner.feature.tabs.create.qr
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.addTextChangedListener
import my.qrcode.scanner.R
import my.qrcode.scanner.extension.isNotBlank
import my.qrcode.scanner.extension.textString
import my.qrcode.scanner.feature.tabs.create.BaseCreateBarcodeFragment
import my.qrcode.scanner.model.schema.Schema
import my.qrcode.scanner.model.schema.Url
import kotlinx.android.synthetic.main.fragment_create_qr_code_url.edit_text
class CreateQrCodeUrlFragment : BaseCreateBarcodeFragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_create_qr_code_url, container, false)
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        showUrlPrefix()
        handleTextChanged()
    }
    override fun getBarcodeSchema(): Schema {
        return Url(edit_text.textString)
    }
    private fun showUrlPrefix() {
        val prefix = "https://"
        edit_text.apply {
            setText(prefix)
            setSelection(prefix.length)
            requestFocus()
        }
    }
    private fun handleTextChanged() {
        edit_text.addTextChangedListener {
            parentActivity.isCreateBarcodeButtonEnabled = edit_text.isNotBlank()
        }
    }
}