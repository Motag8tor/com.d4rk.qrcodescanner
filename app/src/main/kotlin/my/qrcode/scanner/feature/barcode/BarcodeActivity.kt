package my.qrcode.scanner.feature.barcode
import android.app.SearchManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.CalendarContract
import android.provider.ContactsContract
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.print.PrintHelper
import com.chaquo.python.PyObject
import com.chaquo.python.Python
import my.qrcode.scanner.R
import my.qrcode.scanner.di.settings
import my.qrcode.scanner.di.barcodeDatabase
import my.qrcode.scanner.di.wifiConnector
import my.qrcode.scanner.di.barcodeImageGenerator
import my.qrcode.scanner.di.barcodeImageSaver
import my.qrcode.scanner.extension.unsafeLazy
import my.qrcode.scanner.extension.orFalse
import my.qrcode.scanner.extension.showError
import my.qrcode.scanner.extension.toPhoneType
import my.qrcode.scanner.extension.toEmailType
import my.qrcode.scanner.extension.currentLocale
import my.qrcode.scanner.extension.applySystemWindowInsets
import my.qrcode.scanner.extension.toStringId
import my.qrcode.scanner.extension.toCountryEmoji
import my.qrcode.scanner.feature.BaseActivity
import my.qrcode.scanner.feature.barcode.otp.OtpActivity
import my.qrcode.scanner.feature.barcode.save.SaveBarcodeAsImageActivity
import my.qrcode.scanner.feature.barcode.save.SaveBarcodeAsTextActivity
import my.qrcode.scanner.feature.common.dialog.ChooseSearchEngineDialogFragment
import my.qrcode.scanner.feature.common.dialog.DeleteConfirmationDialogFragment
import my.qrcode.scanner.feature.common.dialog.EditBarcodeNameDialogFragment
import my.qrcode.scanner.model.Barcode
import my.qrcode.scanner.model.ParsedBarcode
import my.qrcode.scanner.model.SearchEngine
import my.qrcode.scanner.model.schema.BarcodeSchema
import my.qrcode.scanner.model.schema.OtpAuth
import my.qrcode.scanner.usecase.save
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_barcode.*
import kotlinx.android.synthetic.main.activity_barcode.scroll_view
import kotlinx.android.synthetic.main.activity_barcode.toolbar
import kotlinx.android.synthetic.main.fragment_settings.*
import kotlinx.coroutines.*
import java.text.SimpleDateFormat
import java.util.Locale
class BarcodeActivity : BaseActivity(), DeleteConfirmationDialogFragment.Listener, ChooseSearchEngineDialogFragment.Listener, EditBarcodeNameDialogFragment.Listener {
    companion object {
        private const val BARCODE_KEY = "BARCODE_KEY"
        private const val IS_CREATED = "IS_CREATED"
        private var reportContent = ""
        fun start(context: Context, barcode: Barcode, isCreated: Boolean = false) {
            val intent = Intent(context, BarcodeActivity::class.java).apply {
                putExtra(BARCODE_KEY, barcode)
                putExtra(IS_CREATED, isCreated)
            }
            context.startActivity(intent)
        }
    }
    private val disposable = CompositeDisposable()
    private val dateFormatter = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.ENGLISH)
    private val originalBarcode by unsafeLazy {
        intent?.getSerializableExtra(BARCODE_KEY) as? Barcode ?: throw IllegalArgumentException("No barcode passed")
    }
    private val isCreated by unsafeLazy {
        intent?.getBooleanExtra(IS_CREATED, false).orFalse()
    }
    private val barcode by unsafeLazy {
        ParsedBarcode(originalBarcode)
    }
    private val clipboardManager by unsafeLazy {
        getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
    }
    private var originalBrightness: Float = 0.5f
    @OptIn(DelicateCoroutinesApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_barcode)
        supportEdgeToEdge()
        saveOriginalBrightness()
        applySettings()
        handleToolbarBackPressed()
        handleToolbarMenuClicked()
        handleButtonsClicked()
        
        // This is the important one
        showBarcode()

        GlobalScope.launch(Dispatchers.Main) { // launch the coroutine immediately
            generateReport()
        }

        showButtonText()
    }

    private suspend fun generateReport() {
        // Introduce Python
        val py = Python.getInstance()

        // Retrieve the analyser script
        val module = py.getModule("analyser")

        val content = originalBarcode.rawBytes

        // If no result then return 0"
        val result = module.callAttr("analyser", content).toString()
        Log.d("Result", result)

        if (result == "url" || result == "file") {
            var retries = 3
            val delay: Long = 10000

            while (retries >= 0) { // More than to account for weird scenarios
                var value = 0
                if (result == "url") {
                    value = checkURL(module)
                } else if (result == "file") {
                    value = checkFile(module)
                }

                if (value !=0) {
                    when (value) {
                        1 -> text_view_barcode_report.setTextColor(Color.GREEN)
                        2 -> text_view_barcode_report.setTextColor(Color.YELLOW)
                        3 -> text_view_barcode_report.setTextColor(Color.RED)
                    }
                    Log.d("Colour", "Colour changed to $value")
                    break
                }

                if (retries == 0) {
                    text_view_barcode_report.setTextColor(Color.RED)
                    reportContent = "Scan timed out. Please try again later."
                    showReport()
                } else {
                    Log.d("Test", "$retries")
                    retries--
                    delay(delay)
                }
            }
        } else if (result == "wifi") {
            checkWifi(module) // Check WiFi network's safety
        }
        showWarning()
    }

    private fun checkWifi(module: PyObject) {
        val report = module.callAttr("get_wifi_analysis").toString()
        Log.d("Wi-Fi Result", report)

        var unsafe = false
        when {
            report.contains("hidden") -> reportContent += "This network is hidden.\n"
        }
        when {
            report.contains("nopass") -> {
                reportContent += "This network does not require a password!\n"
                unsafe = true
            }
        }
        when {
            report.contains("noauth") -> {
                reportContent += "This network is not encrypted!\n"
                unsafe = true
            }
            report.contains("authWEP") -> {
                reportContent += "This network uses the outdated WEP encryption!\n"
                unsafe = true
            }
        }
        if (unsafe) {
            text_view_barcode_report.setTextColor(Color.RED)
            reportContent += "This network is not safe to join."
        } else {
            text_view_barcode_report.setTextColor(Color.GREEN)
            reportContent += "This network appears to be safe."
        }
        showReport()
    }

    private fun checkURL(module: PyObject): Int {
        var value = 0
        val (Green, Yellow, Red) = listOf(1, 2, 3)
        val report = module.callAttr("get_url_analysis").toString()
        Log.d("URL Report Value", report)
        when (report) {
            "1" -> {
                value = if (value < Red) Red else value
                reportContent =
                    "Unable to generate a report. Please scan again or proceed with caution."
            }
            "2" -> {
                text_view_barcode_report.setTextColor(Color.YELLOW)
                reportContent = "Report is not ready yet. Please wait..."
            }
            "3" -> {
                value = if (value < Green) Green else value
                reportContent = "There was an error with the request. Aborting..."
            }
            else -> {
                val conclusion = module.callAttr("get_url_conclusion").toString()
                when (conclusion) {
                    "malicious" -> {
                        value = if (value < Red) Red else value
                        reportContent =
                            "This domain appears to be malicious. Avoiding this website is recommended.\n"
                    }
                    "suspicious" -> {
                        value = if (value < Yellow) Yellow else value
                        reportContent =
                            "This domain appears to be suspicious. Proceed with caution.\n"
                    }
                    "harmless" -> {
                        value = if (value < Green) Green else value
                        reportContent = "This domain appears to be safe.\n"
                    }
                }

                val creationDate = module.callAttr("get_url_creation_date").toInt()
                if (creationDate == 0) {
                    Log.d("Creation Date", "Unable to retrieve creation date")
                } else if (creationDate < 31) {
                    value = if (value < Yellow) Yellow else value
                    reportContent += "This URL was registered in the last month. Proceed with caution.\n"
                } else {
                    value = if (value < Green) Green else value
                    reportContent += "This URL was registered over a month ago.\n"
                }

                val downloadable = module.callAttr("get_url_downloadable").toString()
                when (downloadable) {
                    "True" -> {
                        value = if (value < Yellow) Yellow else value
                        reportContent += "This URL attempts to download a file. Proceed with caution."
                    }
                }
            }
        }
        showReport()
        return value
    }

    private suspend fun checkFile(module: PyObject): Int {
        var value = 0
        val (Green, Yellow, Red) = listOf(1, 2, 3)
        delay(2000)
        val report = module.callAttr("get_file_analysis").toString()
        Log.d("File Report Value", report)
        when (report) {
            "1" -> {
                value = if (value < Red) Red else value
                reportContent =
                    "Unable to generate a report. Please scan again or proceed with caution."
            }
            "2" -> {
                text_view_barcode_report.setTextColor(Color.YELLOW)
                reportContent = "Report is not ready yet. Please wait..."
            }
            "3" -> {
                value = if (value < Red) Red else value
                reportContent = "There was an error with the request. Aborting..."
            }
            else -> {
                when (report) {
                    "malicious" -> {
                        value = if (value < Red) Red else value
                        reportContent =
                            "This QRCode appears to be malicious.\n"
                    }
                    "suspicious" -> {
                        value = if (value < Yellow) Yellow else value
                        reportContent =
                            "This QRCode appears to be suspicious.\n"
                    }
                    "harmless" -> {
                        value = if (value < Green) Green else value
                        reportContent = "This QRCode appears to be safe.\n"
                    }
                }
            }
        }
        showReport()
        return value
    }

    override fun onDeleteConfirmed() {
        deleteBarcode()
    }
    override fun onNameConfirmed(name: String) {
        updateBarcodeName(name)
    }
    override fun onSearchEngineSelected(searchEngine: SearchEngine) {
        performWebSearchUsingSearchEngine(searchEngine)
    }
    override fun onDestroy() {
        super.onDestroy()
        disposable.clear()
    }
    private fun supportEdgeToEdge() {
        root_view.applySystemWindowInsets(applyTop = true, applyBottom = true)
    }
    private fun saveOriginalBrightness() {
        originalBrightness = window.attributes.screenBrightness
    }
    private fun applySettings() {
        if (settings.copyToClipboard) {
            copyToClipboard(barcode.text)
        }
        if (settings.openLinksAutomatically.not() || isCreated) {
            return
        }
        when (barcode.schema) {
            BarcodeSchema.APP -> openInAppMarket()
            BarcodeSchema.BOOKMARK -> saveBookmark()
            BarcodeSchema.CRYPTOCURRENCY -> openBitcoinUrl()
            BarcodeSchema.EMAIL -> sendEmail(barcode.email)
            BarcodeSchema.GEO -> showLocation()
            BarcodeSchema.GOOGLE_MAPS -> showLocation()
            BarcodeSchema.MMS -> sendSmsOrMms(barcode.phone)
            BarcodeSchema.MECARD -> addToContacts()
            BarcodeSchema.OTP_AUTH -> openOtpInOtherApp()
            BarcodeSchema.PHONE -> callPhone(barcode.phone)
            BarcodeSchema.SMS -> sendSmsOrMms(barcode.phone)
            BarcodeSchema.URL -> openLink()
            BarcodeSchema.VEVENT -> addToCalendar()
            BarcodeSchema.VCARD -> addToContacts()
            BarcodeSchema.WIFI -> connectToWifi()
            BarcodeSchema.YOUTUBE -> openInYoutube()
            BarcodeSchema.NZCOVIDTRACER -> openLink()
            BarcodeSchema.BOARDINGPASS -> return
            else -> return
        }
    }
    private fun handleToolbarBackPressed() {
        toolbar.setNavigationOnClickListener {
            finish()
        }
    }
    private fun handleToolbarMenuClicked() {
        toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.item_increase_brightness -> {
                    increaseBrightnessToMax()
                    toolbar.menu.findItem(R.id.item_increase_brightness).isVisible = false
                    toolbar.menu.findItem(R.id.item_decrease_brightness).isVisible = true
                }
                R.id.item_decrease_brightness -> {
                    restoreOriginalBrightness()
                    toolbar.menu.findItem(R.id.item_increase_brightness).isVisible = true
                    toolbar.menu.findItem(R.id.item_decrease_brightness).isVisible = false
                }
                R.id.item_add_to_favorites -> toggleIsFavorite()
                R.id.item_show_barcode_image -> navigateToBarcodeImageActivity()
                R.id.item_save -> saveBarcode()
                R.id.item_delete -> showDeleteBarcodeConfirmationDialog()
            }
            return@setOnMenuItemClickListener true
        }
    }








    private fun showWarning() {
        button_warning.setOnClickListener { showWarningDialog() }
        button_warning.isVisible = true
    }
    private fun showWarningDialog() {
        button_warning.isVisible = false
        showOrHideButtons()
    }
    private fun handleButtonsClicked() {
        button_edit_name.setOnClickListener { showEditBarcodeNameDialog() }
        button_search_on_web.setOnClickListener { searchBarcodeTextOnInternet() }
        button_add_to_calendar.setOnClickListener { addToCalendar() }
        button_add_to_contacts.setOnClickListener { addToContacts() }
        button_show_location.setOnClickListener { showLocation() }
        button_connect_to_wifi.setOnClickListener { connectToWifi() }
        button_open_wifi_settings.setOnClickListener { openWifiSettings() }
        button_copy_network_name.setOnClickListener { copyNetworkNameToClipboard() }
        button_copy_network_password.setOnClickListener { copyNetworkPasswordToClipboard() }
        button_open_app.setOnClickListener { openApp() }
        button_open_in_app_market.setOnClickListener { openInAppMarket() }
        button_open_in_youtube.setOnClickListener { openInYoutube() }
        button_show_otp.setOnClickListener { showOtp() }
        button_open_otp.setOnClickListener { openOtpInOtherApp() }
        button_open_bitcoin_uri.setOnClickListener { openBitcoinUrl() }
        button_open_link.setOnClickListener { openLink() }
        button_save_bookmark.setOnClickListener { saveBookmark() }
        button_call_phone_1.setOnClickListener { callPhone(barcode.phone) }
        button_call_phone_2.setOnClickListener { callPhone(barcode.secondaryPhone) }
        button_call_phone_3.setOnClickListener { callPhone(barcode.tertiaryPhone) }
        button_send_sms_or_mms_1.setOnClickListener { sendSmsOrMms(barcode.phone) }
        button_send_sms_or_mms_2.setOnClickListener { sendSmsOrMms(barcode.secondaryPhone) }
        button_send_sms_or_mms_3.setOnClickListener { sendSmsOrMms(barcode.tertiaryPhone) }
        button_send_email_1.setOnClickListener { sendEmail(barcode.email) }
        button_send_email_2.setOnClickListener { sendEmail(barcode.secondaryEmail) }
        button_send_email_3.setOnClickListener { sendEmail(barcode.tertiaryEmail) }
        button_share_as_text.setOnClickListener { shareBarcodeAsText() }
        button_copy.setOnClickListener { copyBarcodeTextToClipboard() }
        button_search.setOnClickListener { searchBarcodeTextOnInternet() }
        button_save_as_text.setOnClickListener { navigateToSaveBarcodeAsTextActivity() }
        button_share_as_image.setOnClickListener { shareBarcodeAsImage() }
        button_save_as_image.setOnClickListener { navigateToSaveBarcodeAsImageActivity() }
        button_print.setOnClickListener { printBarcode() }
    }
    private fun toggleIsFavorite() {
        val newBarcode = originalBarcode.copy(isFavorite = barcode.isFavorite.not())
        barcodeDatabase.save(newBarcode)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                {
                    barcode.isFavorite = newBarcode.isFavorite
                    showBarcodeIsFavorite(newBarcode.isFavorite)
                },
                {}
            )
            .addTo(disposable)
    }
    private fun updateBarcodeName(name: String) {
        if (name.isBlank()) {
            return
        }
        val newBarcode = originalBarcode.copy(
            id = barcode.id,
            name = name
        )
        barcodeDatabase.save(newBarcode)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                {
                    barcode.name = name
                    showBarcodeName(name)
                },
                ::showError
            )
            .addTo(disposable)
    }
    private fun saveBarcode() {
        toolbar?.menu?.findItem(R.id.item_save)?.isVisible = false
        barcodeDatabase.save(originalBarcode, settings.doNotSaveDuplicates)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { id ->
                    barcode.id = id
                    button_edit_name.isVisible = true
                    toolbar?.menu?.findItem(R.id.item_delete)?.isVisible = true
                },
                { error ->
                    toolbar?.menu?.findItem(R.id.item_save)?.isVisible = true
                    showError(error)
                }
            )
            .addTo(disposable)
    }
    private fun deleteBarcode() {
        showLoading(true)
        barcodeDatabase.delete(barcode.id)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { finish() },
                { error ->
                    showLoading(false)
                    showError(error)
                }
            )
            .addTo(disposable)
    }
    private fun addToCalendar() {
        val intent = Intent(Intent.ACTION_INSERT).apply {
            data = CalendarContract.Events.CONTENT_URI
            putExtra(CalendarContract.Events.TITLE, barcode.eventSummary)
            putExtra(CalendarContract.Events.DESCRIPTION, barcode.eventDescription)
            putExtra(CalendarContract.Events.EVENT_LOCATION, barcode.eventLocation)
            putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, barcode.eventStartDate)
            putExtra(CalendarContract.EXTRA_EVENT_END_TIME, barcode.eventEndDate)
        }
        startActivityIfExists(intent)
    }
    private fun addToContacts() {
        val intent = Intent(ContactsContract.Intents.Insert.ACTION).apply {
            type = ContactsContract.Contacts.CONTENT_TYPE
            val fullName = "${barcode.firstName.orEmpty()} ${barcode.lastName.orEmpty()}"
            putExtra(ContactsContract.Intents.Insert.NAME, fullName)
            putExtra(ContactsContract.Intents.Insert.COMPANY, barcode.organization.orEmpty())
            putExtra(ContactsContract.Intents.Insert.JOB_TITLE, barcode.jobTitle.orEmpty())
            putExtra(ContactsContract.Intents.Insert.PHONE, barcode.phone.orEmpty())
            putExtra(ContactsContract.Intents.Insert.PHONE_TYPE, barcode.phoneType.orEmpty().toPhoneType())
            putExtra(ContactsContract.Intents.Insert.SECONDARY_PHONE, barcode.secondaryPhone.orEmpty())
            putExtra(ContactsContract.Intents.Insert.SECONDARY_PHONE_TYPE, barcode.secondaryPhoneType.orEmpty().toPhoneType())
            putExtra(ContactsContract.Intents.Insert.TERTIARY_PHONE, barcode.tertiaryPhone.orEmpty())
            putExtra(ContactsContract.Intents.Insert.TERTIARY_PHONE_TYPE, barcode.tertiaryPhoneType.orEmpty().toPhoneType())
            putExtra(ContactsContract.Intents.Insert.EMAIL, barcode.email.orEmpty())
            putExtra(ContactsContract.Intents.Insert.EMAIL_TYPE, barcode.emailType.orEmpty().toEmailType())
            putExtra(ContactsContract.Intents.Insert.SECONDARY_EMAIL, barcode.secondaryEmail.orEmpty())
            putExtra(ContactsContract.Intents.Insert.SECONDARY_EMAIL_TYPE, barcode.secondaryEmailType.orEmpty().toEmailType())
            putExtra(ContactsContract.Intents.Insert.TERTIARY_EMAIL, barcode.tertiaryEmail.orEmpty())
            putExtra(ContactsContract.Intents.Insert.TERTIARY_EMAIL_TYPE, barcode.tertiaryEmailType.orEmpty().toEmailType())
            putExtra(ContactsContract.Intents.Insert.NOTES, barcode.note.orEmpty())
        }
        startActivityIfExists(intent)
    }
    private fun callPhone(phone: String?) {
        val phoneUri = "tel:${phone.orEmpty()}"
        startActivityIfExists(Intent.ACTION_DIAL, phoneUri)
    }
    private fun sendSmsOrMms(phone: String?) {
        val uri = Uri.parse("sms:${phone.orEmpty()}")
        val intent = Intent(Intent.ACTION_SENDTO, uri).apply {
            putExtra("sms_body", barcode.smsBody.orEmpty())
        }
        startActivityIfExists(intent)
    }
    private fun sendEmail(email: String?) {
        val uri = Uri.parse("mailto:${email.orEmpty()}")
        val intent = Intent(Intent.ACTION_SEND, uri).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_EMAIL, arrayOf(email.orEmpty()))
            putExtra(Intent.EXTRA_SUBJECT, barcode.emailSubject.orEmpty())
            putExtra(Intent.EXTRA_TEXT, barcode.emailBody.orEmpty())
        }
        startActivityIfExists(intent)
    }
    private fun showLocation() {
        startActivityIfExists(Intent.ACTION_VIEW, barcode.geoUri.orEmpty())
    }
    private fun connectToWifi() {
        showConnectToWifiButtonEnabled(false)
        wifiConnector
            .connect(
                    this,
                    barcode.networkAuthType.orEmpty(),
                    barcode.networkName.orEmpty(),
                    barcode.networkPassword.orEmpty(),
                    barcode.isHidden.orFalse(),
                    barcode.anonymousIdentity.orEmpty(),
                    barcode.identity.orEmpty(),
                    barcode.eapMethod.orEmpty(),
                    barcode.phase2Method.orEmpty()
            )
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                {
                    showConnectToWifiButtonEnabled(true)
                    showToast(R.string.activity_barcode_connecting_to_wifi)
                },
                { error ->
                    showConnectToWifiButtonEnabled(true)
                    showError(error)
                }
            )
            .addTo(disposable)
    }
    private fun openWifiSettings() {
        val intent = Intent(Settings.ACTION_WIFI_SETTINGS)
        startActivityIfExists(intent)
    }
    private fun copyNetworkNameToClipboard() {
        copyToClipboard(barcode.networkName.orEmpty())
        showToast(R.string.activity_barcode_copied)
    }
    private fun copyNetworkPasswordToClipboard() {
        copyToClipboard(barcode.networkPassword.orEmpty())
        showToast(R.string.activity_barcode_copied)
    }
    private fun openApp() {
        val intent = packageManager?.getLaunchIntentForPackage(barcode.appPackage.orEmpty())
        if (intent != null) {
            startActivityIfExists(intent)
        }
    }
    private fun openInAppMarket() {
        startActivityIfExists(Intent.ACTION_VIEW, barcode.appMarketUrl.orEmpty())
    }
    private fun openInYoutube() {
        startActivityIfExists(Intent.ACTION_VIEW, barcode.youtubeUrl.orEmpty())
    }
    private fun showOtp() {
        val otp = OtpAuth.parse(barcode.otpUrl.orEmpty()) ?: return
        OtpActivity.start(this, otp)
    }
    private fun openOtpInOtherApp() {
        startActivityIfExists(Intent.ACTION_VIEW, barcode.otpUrl.orEmpty())
    }
    private fun openBitcoinUrl() {
        startActivityIfExists(Intent.ACTION_VIEW, barcode.bitcoinUri.orEmpty())
    }
    private fun openLink() {
        startActivityIfExists(Intent.ACTION_VIEW, barcode.url.orEmpty())
    }
    private fun saveBookmark() {
        val intent = Intent(Intent.ACTION_INSERT, Uri.parse("content://browser/bookmarks")).apply {
            putExtra("title", barcode.bookmarkTitle.orEmpty())
            putExtra("url", barcode.url.orEmpty())
        }
        startActivityIfExists(intent)
    }
    private fun shareBarcodeAsText() {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, barcode.text)
        }
        startActivityIfExists(intent)
    }
    private fun copyBarcodeTextToClipboard() {
        copyToClipboard(barcode.text)
        showToast(R.string.activity_barcode_copied)
    }
    private fun searchBarcodeTextOnInternet() {
        when (val searchEngine = settings.searchEngine) {
           SearchEngine.NONE -> performWebSearch()
           SearchEngine.ASK_EVERY_TIME -> showSearchEnginesDialog()
           else -> performWebSearchUsingSearchEngine(searchEngine)
        }
    }
    private fun performWebSearch() {
        val intent = Intent(Intent.ACTION_WEB_SEARCH).apply {
            putExtra(SearchManager.QUERY, barcode.text)
        }
        startActivityIfExists(intent)
    }
    private fun performWebSearchUsingSearchEngine(searchEngine: SearchEngine) {
        val url = searchEngine.templateUrl + barcode.text
        startActivityIfExists(Intent.ACTION_VIEW, url)
    }
    private fun shareBarcodeAsImage() {
        val imageUri = try {
            val image = barcodeImageGenerator.generateBitmap(originalBarcode, 200, 200, 1)
            barcodeImageSaver.saveImageToCache(this, image, barcode)
        } catch (ex: Exception) {
            showError(ex)
            return
        }
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, imageUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivityIfExists(intent)
    }
    private fun printBarcode() {
        val barcodeImage = try {
            barcodeImageGenerator.generateBitmap(originalBarcode, 1000, 1000, 3)
        } catch (ex: Exception) {
            showError(ex)
            return
        }
        PrintHelper(this).apply {
            scaleMode = PrintHelper.SCALE_MODE_FIT
            printBitmap("${barcode.format}_${barcode.schema}_${barcode.date}", barcodeImage)
        }
    }
    private fun navigateToBarcodeImageActivity() {
        BarcodeImageActivity.start(this, originalBarcode)
    }
    private fun navigateToSaveBarcodeAsTextActivity() {
        SaveBarcodeAsTextActivity.start(this, originalBarcode)
    }
    private fun navigateToSaveBarcodeAsImageActivity() {
        SaveBarcodeAsImageActivity.start(this, originalBarcode)
    }





















    
    // SHOW THE ACTUAL BARCODE
    private fun showBarcode() {
        showBarcodeMenuIfNeeded()
        showBarcodeIsFavorite()
        showBarcodeImageIfNeeded()
        showBarcodeDate()
        showBarcodeFormat()
        showBarcodeName()




        // BARCODE TEXT
        showBarcodeText()
        showBarcodeCountry()
    }
    private fun showBarcodeMenuIfNeeded() {
        toolbar.inflateMenu(R.menu.menu_barcode)
        toolbar.menu.apply {
            findItem(R.id.item_increase_brightness).isVisible = isCreated
            findItem(R.id.item_add_to_favorites)?.isVisible = barcode.isInDb
            findItem(R.id.item_show_barcode_image)?.isVisible = isCreated.not()
            findItem(R.id.item_save)?.isVisible = barcode.isInDb.not()
            findItem(R.id.item_delete)?.isVisible = barcode.isInDb
        }
    }
    private fun showBarcodeIsFavorite() {
        showBarcodeIsFavorite(barcode.isFavorite)
    }
    private fun showBarcodeIsFavorite(isFavorite: Boolean) {
        val iconId = if (isFavorite) {
            R.drawable.ic_favorite_checked
        } else {
            R.drawable.ic_favorite_unchecked
        }
        toolbar.menu?.findItem(R.id.item_add_to_favorites)?.icon = ContextCompat.getDrawable(this, iconId)
    }
    private fun showBarcodeImageIfNeeded() {
        if (isCreated) {
            showBarcodeImage()
        }
    }
    private fun showBarcodeImage() {
        try {
            val bitmap = barcodeImageGenerator.generateBitmap(originalBarcode, 2000, 2000, 0, settings.barcodeContentColor, settings.barcodeBackgroundColor)
            layout_barcode_image_background.isVisible = true
            image_view_barcode.isVisible = true
            image_view_barcode.setImageBitmap(bitmap)
            image_view_barcode.setBackgroundColor(settings.barcodeBackgroundColor)
            layout_barcode_image_background.setBackgroundColor(settings.barcodeBackgroundColor)
            if (settings.isDarkTheme.not() || settings.areBarcodeColorsInversed) {
                layout_barcode_image_background.setPadding(0, 0, 0, 0)
            }
        } catch (ex: Exception) {
            image_view_barcode.isVisible = false
        }
    }
    private fun showBarcodeDate() {
        text_view_date.text = dateFormatter.format(barcode.date)
    }
    private fun showBarcodeFormat() {
        val format = barcode.format.toStringId()
        toolbar.setTitle(format)
    }
    private fun showBarcodeName() {
        showBarcodeName(barcode.name)
    }
    private fun showBarcodeName(name: String?) {
        text_view_barcode_name.isVisible = name.isNullOrBlank().not()
        text_view_barcode_name.text = name.orEmpty()
    }















    // ACTUAL BARCODE MESSAGE
    private fun showBarcodeText() {
        text_view_barcode_text.text = if (isCreated) {
            barcode.text
        } else {
            barcode.formattedText
        }
    }



    private fun showReport() {
        text_view_barcode_report.text = ""
        if (reportContent.isEmpty()) {
            text_view_barcode_report.text = ""
        } else {
            text_view_barcode_report.text = reportContent
            text_view_barcode_hex.text = "Hexdump:\n" + convert(barcode.rawBytes)
        }
        reportContent = ""
    }

    private fun convert(yes: ByteArray) : String {
        return yes.joinToString(" ") { "%02x".format(it) }
    }



    private fun showBarcodeCountry() {
        val country = barcode.country ?: return
        when (country.contains('/')) {
            false -> showOneBarcodeCountry(country)
            true -> showTwoBarcodeCountries(country.split('/'))
        }
    }
    private fun showOneBarcodeCountry(country: String) {
        val fullCountryName = buildFullCountryName(country)
        showFullCountryName(fullCountryName)
    }
    private fun showTwoBarcodeCountries(countries: List<String>) {
        val firstFullCountryName = buildFullCountryName(countries[0])
        val secondFullCountryName = buildFullCountryName(countries[1])
        val fullCountryName = "$firstFullCountryName / $secondFullCountryName"
        showFullCountryName(fullCountryName)
    }
    private fun buildFullCountryName(country: String): String {
        val currentLocale = currentLocale ?: return ""
        val countryName = Locale("", country).getDisplayName(currentLocale)
        val countryEmoji = country.toCountryEmoji()
        return "$countryEmoji $countryName"
    }
    private fun showFullCountryName(fullCountryName: String) {
        text_view_country.apply {
            text = fullCountryName
            isVisible = fullCountryName.isBlank().not()
        }
    }
    private fun showOrHideButtons() {
        button_search.isVisible = isCreated.not()
        button_edit_name.isVisible = barcode.isInDb
        if (isCreated) {
            return
        }


        button_share_as_text.isVisible = true
        button_copy.isVisible = true
        button_search_on_web.isVisible = barcode.isProductBarcode
        button_save_as_text.isVisible = true
        button_share_as_image.isVisible = true
        button_save_as_image.isVisible = true


        button_search.isVisible = barcode.isProductBarcode.not()
        button_add_to_calendar.isVisible = barcode.schema == BarcodeSchema.VEVENT
        button_add_to_contacts.isVisible = barcode.schema == BarcodeSchema.VCARD || barcode.schema == BarcodeSchema.MECARD
        button_call_phone_1.isVisible = barcode.phone.isNullOrEmpty().not()
        button_call_phone_2.isVisible = barcode.secondaryPhone.isNullOrEmpty().not()
        button_call_phone_3.isVisible = barcode.tertiaryPhone.isNullOrEmpty().not()
        button_send_sms_or_mms_1.isVisible = barcode.phone.isNullOrEmpty().not() || barcode.smsBody.isNullOrEmpty().not()
        button_send_sms_or_mms_2.isVisible = barcode.secondaryPhone.isNullOrEmpty().not()
        button_send_sms_or_mms_3.isVisible = barcode.tertiaryPhone.isNullOrEmpty().not()
        button_send_email_1.isVisible = barcode.email.isNullOrEmpty().not() || barcode.emailSubject.isNullOrEmpty().not() || barcode.emailBody.isNullOrEmpty().not()
        button_send_email_2.isVisible = barcode.secondaryEmail.isNullOrEmpty().not()
        button_send_email_3.isVisible = barcode.tertiaryEmail.isNullOrEmpty().not()
        button_show_location.isVisible = barcode.geoUri.isNullOrEmpty().not()
        button_connect_to_wifi.isVisible = barcode.schema == BarcodeSchema.WIFI
        button_open_wifi_settings.isVisible = barcode.schema == BarcodeSchema.WIFI
        button_copy_network_name.isVisible = barcode.networkName.isNullOrEmpty().not()
        button_copy_network_password.isVisible = barcode.networkPassword.isNullOrEmpty().not()
        button_open_app.isVisible = barcode.appPackage.isNullOrEmpty().not() && isAppInstalled(barcode.appPackage)
        button_open_in_app_market.isVisible = barcode.appMarketUrl.isNullOrEmpty().not()
        button_open_in_youtube.isVisible = barcode.youtubeUrl.isNullOrEmpty().not()
        button_show_otp.isVisible = barcode.otpUrl.isNullOrEmpty().not()
        button_open_otp.isVisible = barcode.otpUrl.isNullOrEmpty().not()
        button_open_bitcoin_uri.isVisible = barcode.bitcoinUri.isNullOrEmpty().not()
        button_open_link.isVisible = barcode.url.isNullOrEmpty().not()
        button_save_bookmark.isVisible = barcode.schema == BarcodeSchema.BOOKMARK
    }
    private fun showButtonText() {
        button_call_phone_1.text = getString(R.string.activity_barcode_call_phone, barcode.phone)
        button_call_phone_2.text = getString(R.string.activity_barcode_call_phone, barcode.secondaryPhone)
        button_call_phone_3.text = getString(R.string.activity_barcode_call_phone, barcode.tertiaryPhone)
        button_send_sms_or_mms_1.text = getString(R.string.activity_barcode_send_sms, barcode.phone)
        button_send_sms_or_mms_2.text = getString(R.string.activity_barcode_send_sms, barcode.secondaryPhone)
        button_send_sms_or_mms_3.text = getString(R.string.activity_barcode_send_sms, barcode.tertiaryPhone)
        button_send_email_1.text = getString(R.string.activity_barcode_send_email, barcode.email)
        button_send_email_2.text = getString(R.string.activity_barcode_send_email, barcode.secondaryEmail)
        button_send_email_3.text = getString(R.string.activity_barcode_send_email, barcode.tertiaryEmail)
    }
    private fun showConnectToWifiButtonEnabled(isEnabled: Boolean) {
        button_connect_to_wifi.isEnabled = isEnabled
    }
    private fun showDeleteBarcodeConfirmationDialog() {
        val dialog = DeleteConfirmationDialogFragment.newInstance(R.string.dialog_delete_barcode_message)
        dialog.show(supportFragmentManager, "")
    }
    private fun showEditBarcodeNameDialog() {
        val dialog = EditBarcodeNameDialogFragment.newInstance(barcode.name)
        dialog.show(supportFragmentManager, "")
    }
    private fun showSearchEnginesDialog() {
        val dialog = ChooseSearchEngineDialogFragment()
        dialog.show(supportFragmentManager, "")
    }
    private fun showLoading(isLoading: Boolean) {
        progress_bar_loading.isVisible = isLoading
        scroll_view.isVisible = isLoading.not()
    }
    private fun startActivityIfExists(action: String, uri: String) {
        val intent = Intent(action, Uri.parse(uri))
        startActivityIfExists(intent)
    }
    private fun startActivityIfExists(intent: Intent) {
        intent.apply {
            flags = flags or Intent.FLAG_ACTIVITY_NEW_TASK
        }
        if (intent.resolveActivity(packageManager) != null) {
            startActivity(intent)
        } else {
            showToast(R.string.activity_barcode_no_app)
        }
    }
    private fun isAppInstalled(appPackage: String?): Boolean {
        return packageManager?.getLaunchIntentForPackage(appPackage.orEmpty()) != null
    }
    private fun copyToClipboard(text: String) {
        val clipData = ClipData.newPlainText("", text)
        clipboardManager.setPrimaryClip(clipData)
    }
    private fun showToast(stringId: Int) {
        Toast.makeText(this, stringId, Toast.LENGTH_SHORT).show()
    }
    private fun increaseBrightnessToMax() {
        setBrightness(1.0f)
    }
    private fun restoreOriginalBrightness() {
        setBrightness(originalBrightness)
    }
    private fun setBrightness(brightness: Float) {
        window.attributes = window.attributes.apply {
            screenBrightness = brightness
        }
    }
}