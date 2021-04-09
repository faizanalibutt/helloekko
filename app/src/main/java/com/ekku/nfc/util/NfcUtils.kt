package com.ekku.nfc.util

import android.content.Context
import android.content.Intent
import android.nfc.NfcAdapter
import android.provider.Settings
import androidx.appcompat.app.AppCompatActivity

object NfcUtils {
    /**
     * check if there is nfc adapter present in a device or not.
     * */
    fun Context.getNfcAdapter(): NfcAdapter? = NfcAdapter.getDefaultAdapter(this) ?: null

    /**
     * Enable NFC
     */
    fun Context.showNFCSettings() {
        val intent = Intent(Settings.ACTION_NFC_SETTINGS)
        startActivity(intent)
    }

    /**
     * check if it's enabled.
     */
    fun Context.isNFCOnline() = getNfcAdapter()?.isEnabled == true

    /**
     * callback to listen for NFC tags
     */
    fun Context.addNfcCallback(
        activity: AppCompatActivity, nfcReaderCallback: NfcAdapter.ReaderCallback
    ) = getNfcAdapter()?.enableReaderMode(
        activity, nfcReaderCallback,
        NfcAdapter.FLAG_READER_NFC_A or
                NfcAdapter.FLAG_READER_NFC_B or
                NfcAdapter.FLAG_READER_NFC_F or
                NfcAdapter.FLAG_READER_NFC_V or
                NfcAdapter.FLAG_READER_NFC_BARCODE or
                NfcAdapter.FLAG_READER_NO_PLATFORM_SOUNDS, null
    )

    /**
     * disable callback
     */
    fun Context.removeNfcCallback(activity: AppCompatActivity) =
        getNfcAdapter()?.disableReaderMode(activity)
}