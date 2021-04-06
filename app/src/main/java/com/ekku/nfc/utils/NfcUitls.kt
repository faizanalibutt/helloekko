package com.ekku.nfc.utils

import android.content.Context
import android.content.Intent
import android.nfc.NfcAdapter
import android.provider.Settings

object NfcUitls {
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
}