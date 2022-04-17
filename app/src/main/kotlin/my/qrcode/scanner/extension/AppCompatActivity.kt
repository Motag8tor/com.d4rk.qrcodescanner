package my.qrcode.scanner.extension
import androidx.appcompat.app.AppCompatActivity
import my.qrcode.scanner.feature.common.dialog.ErrorDialogFragment
fun AppCompatActivity.showError(error: Throwable?) {
    val errorDialog =
        ErrorDialogFragment.newInstance(
            this,
            error
        )
    errorDialog.show(supportFragmentManager, "")
}