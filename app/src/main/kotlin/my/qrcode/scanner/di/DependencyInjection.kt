package my.qrcode.scanner.di
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import my.qrcode.scanner.usecase.BarcodeImageScanner
import my.qrcode.scanner.usecase.BarcodeSaver
import my.qrcode.scanner.usecase.BarcodeImageGenerator
import my.qrcode.scanner.usecase.BarcodeImageSaver
import my.qrcode.scanner.usecase.BarcodeParser
import my.qrcode.scanner.usecase.WifiConnector
import my.qrcode.scanner.usecase.OTPGenerator
import my.qrcode.scanner.usecase.Settings
import my.qrcode.scanner.usecase.BarcodeDatabase
import my.qrcode.scanner.usecase.ContactHelper
import my.qrcode.scanner.usecase.PermissionsHelper
import my.qrcode.scanner.usecase.RotationHelper
import my.qrcode.scanner.usecase.ScannerCameraHelper
import my.qrcode.scanner.App

val App.settings get() = my.qrcode.scanner.usecase.Settings.getInstance(applicationContext)
val AppCompatActivity.barcodeParser get() = BarcodeParser
val AppCompatActivity.barcodeImageScanner get() = BarcodeImageScanner
val AppCompatActivity.barcodeImageGenerator get() = BarcodeImageGenerator
val AppCompatActivity.barcodeSaver get() = BarcodeSaver
val AppCompatActivity.barcodeImageSaver get() = BarcodeImageSaver
val AppCompatActivity.wifiConnector get() = WifiConnector
val AppCompatActivity.otpGenerator get() = OTPGenerator
val AppCompatActivity.barcodeDatabase get() = BarcodeDatabase.getInstance(this)
val AppCompatActivity.settings get() = Settings.getInstance(this)
val AppCompatActivity.contactHelper get() = ContactHelper
val AppCompatActivity.permissionsHelper get() = PermissionsHelper
val AppCompatActivity.rotationHelper get() = RotationHelper
val Fragment.scannerCameraHelper get() = ScannerCameraHelper
val Fragment.barcodeParser get() = BarcodeParser
val Fragment.barcodeDatabase get() = BarcodeDatabase.getInstance(requireContext())
val Fragment.settings get() = Settings.getInstance(requireContext())
val Fragment.permissionsHelper get() = PermissionsHelper