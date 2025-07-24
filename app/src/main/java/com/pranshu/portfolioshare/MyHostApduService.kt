
package com.pranshu.portfolioshare

import android.content.Context
import android.content.SharedPreferences
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.cardemulation.HostApduService
import android.os.Bundle
import android.util.Log
import java.util.Arrays
import kotlin.OptIn // Import OptIn

@OptIn(kotlin.ExperimentalStdlibApi::class) // Required for hex conversion extensions
class MyHostApduService : HostApduService() {

    companion object {
        private const val TAG = "MyHostApduService"

        // C-APDU for application selection (NDEF Application AID)
        private val SELECT_APP_AID = byteArrayOf(
            0x00, 0xA4.toByte(), 0x04, 0x00,
            0x07, 0xD2.toByte(), 0x76, 0x00, 0x00, 0x85.toByte(), 0x01, 0x01,
            0x00 // Le for response, typically 00 for SELECT
        )

        // C-APDU for CC file selection
        private val SELECT_CC_FILE = byteArrayOf(
            0x00, 0xA4.toByte(), 0x00, 0x0C,
            0x02, 0xE1.toByte(), 0x03
        )

        // C-APDU for NDEF record file selection
        private val SELECT_NDEF_FILE = byteArrayOf(
            0x00, 0xA4.toByte(), 0x00, 0x0C,
            0x02, 0xE1.toByte(), 0x04
        )

        // Success Status Word (used in response)
        private val SUCCESS_SW = byteArrayOf(
            0x90.toByte(), 0x00
        )

        // Failure Status Word (used in response)
        private val FAILURE_SW = byteArrayOf(
            0x6A.toByte(), 0x82.toByte()
        )

        // CC file data
        // CCLEN (2 bytes) | Mapping Version (1 byte) | MLe (2 bytes) | MLc (2 bytes) |
        // TLV for NDEF file: 0x04 (tag) | Length (2 bytes) | NDEF_FILE_ID (2 bytes) | Max NDEF size (2 bytes) | Read Access (1 byte) | Write Access (1 byte)
        private val CC_FILE = byteArrayOf(
            0x00, 0x0F, // CCLEN (15 bytes)
            0x20, // Mapping Version 2.0
            0x00, 0x3B, // Maximum R-APDU data size (59 bytes)
            0x00, 0x34, // Maximum C-APDU data size (52 bytes)
            0x04, 0x06, // TLV Tag & Length for NDEF File Control
            0xE1.toByte(), 0x04, // NDEF File Identifier (E104)
            0x00, 0xFF.toByte(), // Maximum NDEF size (255 bytes) - Adjusted for larger URLs
            0x00, // NDEF file read access granted
            0xFF.toByte() // NDEF File write access denied
        )
    }

    private lateinit var prefs: SharedPreferences
    private var mNdefRecordFile: ByteArray = byteArrayOf()

    // Flags indicating current selection state
    private var mAppSelected: Boolean = false
    private var mCcSelected: Boolean = false
    private var mNdefSelected: Boolean = false

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "MyHostApduService created.")
        prefs = getSharedPreferences(MainActivity.PREFS_NAME, Context.MODE_PRIVATE)
        // Clear state
        resetState()
        // Generate NDEF record file from SharedPreferences
        updateNdefMessageFromPrefs()
    }

    override fun onStartCommand(intent: android.content.Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "MyHostApduService onStartCommand.")
        // Re-read URL from preferences in case it changed while service was running
        updateNdefMessageFromPrefs()
        return super.onStartCommand(intent, flags, startId)
    }

    /**
     * Updates the NDEF message from SharedPreferences.
     */
    private fun updateNdefMessageFromPrefs() {
        val url = prefs.getString(MainActivity.KEY_URL, "[https://www.example.com/default-link](https://www.example.com/default-link)")
        Log.d(TAG, "Service: Retrieved URL from SharedPreferences: $url")

        // Create an NDEF URI record.
        val record = NdefRecord.createUri(url)
        val ndefMessage = NdefMessage(record)

        // Get the byte representation of the NDEF message.
        // Add 2 bytes for the NDEF message length as per Type 4 Tag spec
        val nlen = ndefMessage.toByteArray().size
        mNdefRecordFile = ByteArray(nlen + 2)
        mNdefRecordFile[0] = ((nlen shr 8) and 0xFF).toByte() // MSB of length
        mNdefRecordFile[1] = (nlen and 0xFF).toByte()       // LSB of length
        System.arraycopy(ndefMessage.toByteArray(), 0, mNdefRecordFile, 2, nlen)
        Log.d(TAG, "Service: NDEF message updated. Total size: ${mNdefRecordFile.size} bytes (payload + 2-byte length prefix).")
    }

    /**
     * Performs processing to behave as NFC Forum Tag Type 4.
     * Receives C-APDU and returns corresponding R-APDU.
     */
    override fun processCommandApdu(commandApdu: ByteArray?, extras: Bundle?): ByteArray {
        commandApdu ?: return FAILURE_SW // Handle null command

        val commandHex = commandApdu.toHex()
        Log.d(TAG, "Received APDU: $commandHex")

        if (Arrays.equals(SELECT_APP_AID, commandApdu)) {
            // App selection (NDEF Application AID)
            Log.d(TAG, "NDEF Application AID selected.")
            mAppSelected = true
            mCcSelected = false
            mNdefSelected = false
            return SUCCESS_SW // Success
        } else if (mAppSelected && Arrays.equals(SELECT_CC_FILE, commandApdu)) {
            // CC file selection
            Log.d(TAG, "CC File selected.")
            mCcSelected = true
            mNdefSelected = false
            return SUCCESS_SW // Success
        } else if (mAppSelected && Arrays.equals(SELECT_NDEF_FILE, commandApdu)) {
            // NDEF file selection
            Log.d(TAG, "NDEF File selected.")
            mCcSelected = false
            mNdefSelected = true
            return SUCCESS_SW // Success
        } else if (commandApdu[0] == 0x00.toByte() && commandApdu[1] == 0xB0.toByte()) {
            // READ_BINARY (file read)

            // Extract offset and length
            val offset = ((commandApdu[2].toInt() and 0xFF) shl 8) or (commandApdu[3].toInt() and 0xFF)
            val le = (commandApdu[4].toInt() and 0xFF) // Expected length of response data

            Log.d(TAG, "READ BINARY command: offset=$offset, length=$le")

            // Generate buffer for R-APDU
            val responseApdu = ByteArray(le + SUCCESS_SW.size)

            if (mCcSelected) {
                // When CC is selected, offset must be 0, and length must match the requested length
                if (offset == 0 && le <= CC_FILE.size) {
                    System.arraycopy(CC_FILE, offset, responseApdu, 0, le)
                    System.arraycopy(SUCCESS_SW, 0, responseApdu, le, SUCCESS_SW.size)
                    Log.d(TAG, "Serving CC File data. Response len: ${responseApdu.size}")
                    return responseApdu
                } else {
                    Log.e(TAG, "Invalid READ BINARY for CC File: offset=$offset, length=$le")
                    return FAILURE_SW // Return error
                }
            } else if (mNdefSelected) {
                if (offset + le <= mNdefRecordFile.size) {
                    System.arraycopy(mNdefRecordFile, offset, responseApdu, 0, le)
                    System.arraycopy(SUCCESS_SW, 0, responseApdu, le, SUCCESS_SW.size)
                    Log.d(TAG, "Serving NDEF File data. Response len: ${responseApdu.size}")
                    return responseApdu
                } else {
                    Log.e(TAG, "Read beyond NDEF File bounds: offset=$offset, length=$le, NDEF file size=${mNdefRecordFile.size}")
                    return FAILURE_SW // Return error
                }
            }
        }

        Log.w(TAG, "Unrecognized APDU command: $commandHex")
        // Return error
        // Originally, in IC card applications, the error value should be changed
        // according to the error type, but here it is omitted and only one type is returned.
        return FAILURE_SW
    }

    /**
     * Called when the card application is deselected.
     * In this application, the state is reset to the initial state.
     */
    override fun onDeactivated(reason: Int) {
        Log.i(TAG, "NFC communication deactivated. Reason code: $reason")
        resetState()
    }

    private fun resetState() {
        mAppSelected = false
        mCcSelected = false
        mNdefSelected = false
        Log.d(TAG, "Service state reset.")
    }

    /**
     * Helper method to convert a byte array to a hexadecimal string.
     * @param-bytes The byte array.
     * @return The hexadecimal string.
     */
    private fun ByteArray.toHex(): String {
        return joinToString("") { "%02X".format(it) }
    }

    /**
     * Helper method to convert a hexadecimal string to a byte array.
     * @param-s The hexadecimal string.
     * @return The byte array.
     */
    private fun String.hexToByteArray(): ByteArray {
        val result = ByteArray(length / 2)
        for (i in indices step 2) {
            val byte = substring(i, i + 2).toInt(16)
            result[i / 2] = byte.toByte()
        }
        return result
    }
}