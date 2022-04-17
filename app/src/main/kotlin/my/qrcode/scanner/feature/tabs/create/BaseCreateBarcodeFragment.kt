package my.qrcode.scanner.feature.tabs.create
import androidx.fragment.app.Fragment
import my.qrcode.scanner.extension.unsafeLazy
import my.qrcode.scanner.model.Contact
import my.qrcode.scanner.model.schema.Other
import my.qrcode.scanner.model.schema.Schema
abstract class BaseCreateBarcodeFragment : Fragment() {
    protected val parentActivity by unsafeLazy { requireActivity() as CreateBarcodeActivity }
    open val latitude: Double? = null
    open val longitude: Double? = null
    open fun getBarcodeSchema(): Schema = Other("")
    open fun showPhone(phone: String) {}
    open fun showContact(contact: Contact) {}
    open fun showLocation(latitude: Double?, longitude: Double?) {}
}