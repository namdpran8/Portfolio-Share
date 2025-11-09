package com.pranshu.portfolioshare

import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.cardemulation.HostApduService
import android.os.Bundle
import android.util.Log

class HceService : HostApduService() {

    companion object {
        private const val TAG = "HceService"

        // Status words for APDU responses, defined directly as byte arrays.
        private val STATUS_SUCCESS = byteArrayOf(0x90.toByte(), 0x00.toByte())
        private val STATUS_FAILED = byteArrayOf(0x6F.toByte(), 0x00.toByte())

        // Helper function to convert a ByteArray to a Hex String for logging
        private fun toHexString(bytes: ByteArray): String {
            return bytes.joinToString("") { "%02x".format(it) }
        }
    }

    override fun onDeactivated(reason: Int) {
        Log.d(TAG, "Deactivated: $reason")
    }

    override fun processCommandApdu(commandApdu: ByteArray?, extras: Bundle?): ByteArray {
        if (commandApdu == null) {
            return STATUS_FAILED
        }

        Log.d(TAG, "Received APDU: ${toHexString(commandApdu)}")

        // The first command from a reader should be a SELECT AID command.
        if (isSelectAidApdu(commandApdu)) {
            Log.i(TAG, "Application selected by reader.")
            // The reader selected our app. Inform it that we are ready.
            return STATUS_SUCCESS
        }

        // If it's not a SELECT AID, the reader is likely trying to read our data.
        // We will create and send our NDEF message.
        val ndefMessage = createNdefMessage()
        return if (ndefMessage != null) {
            Log.i(TAG, "Sending NDEF message")
            ndefMessage
        } else {
            Log.e(TAG, "Could not create NDEF message, sending failure status.")
            STATUS_FAILED
        }
    }

    private fun createNdefMessage(): ByteArray? {
        // Retrieve the saved URL from SharedPreferences using the keys from MainActivity
        val sharedPrefs = getSharedPreferences(MainActivity.PREFS_NAME, MODE_PRIVATE)
        val url = sharedPrefs.getString(MainActivity.PREF_KEY_URL, null)

        if (url == null) {
            Log.e(TAG, "URL not found in SharedPreferences.")
            return null
        }

        Log.d(TAG, "Creating NDEF message for URL: $url")

        // Create an NDEF record with the URI
        val uriRecord = NdefRecord.createUri(url)

        // Create an NDEF message containing the record.
        return NdefMessage(uriRecord).toByteArray()
    }

    private fun isSelectAidApdu(apdu: ByteArray): Boolean {
        // Check if the APDU is a SELECT AID command for our NDEF AID
        return apdu.size >= 2 && apdu[0] == 0x00.toByte() && apdu[1] == 0xA4.toByte()
    }
}
