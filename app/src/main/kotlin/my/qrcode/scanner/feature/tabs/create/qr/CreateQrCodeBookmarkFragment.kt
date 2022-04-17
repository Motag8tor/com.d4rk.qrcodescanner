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
import my.qrcode.scanner.model.schema.Bookmark
import my.qrcode.scanner.model.schema.Schema
import kotlinx.android.synthetic.main.fragment_create_qr_code_bookmark.edit_text_title
import kotlinx.android.synthetic.main.fragment_create_qr_code_bookmark.edit_text_url
class CreateQrCodeBookmarkFragment : BaseCreateBarcodeFragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_create_qr_code_bookmark, container, false)
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initTitleEditText()
        handleTextChanged()
    }
    override fun getBarcodeSchema(): Schema {
        return Bookmark(
            title = edit_text_title.textString,
            url = edit_text_url.textString
        )
    }
    private fun initTitleEditText() {
        edit_text_title.requestFocus()
    }
    private fun handleTextChanged() {
        edit_text_title.addTextChangedListener { toggleCreateBarcodeButton() }
        edit_text_url.addTextChangedListener { toggleCreateBarcodeButton() }
    }
    private fun toggleCreateBarcodeButton() {
        parentActivity.isCreateBarcodeButtonEnabled = edit_text_title.isNotBlank() || edit_text_url.isNotBlank()
    }
}