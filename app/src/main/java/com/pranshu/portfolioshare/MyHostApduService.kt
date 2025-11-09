package com.pranshu.portfolioshare

import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.cardemulation.HostApduService
import android.os.Bundle
import android.util.Log

class HceService : HostApduService() {

    // Internal state
    private var ndefMessage: ByteArray = byteArrayOf()
    private var selectedFile: ByteArray = FILE_NONE // Keep track of which file is selected

    companion object {
        private const val TAG = "HceService"

        // --- Status Words ---
        private val STATUS_SUCCESS = byteArrayOf(0x90.toByte(), 0x00.toByte())
        private val STATUS_FAILED = byteArrayOf(0x6F.toByte(), 0x00.toByte())
        private val STATUS_FILE_NOT_FOUND = byteArrayOf(0x6A.toByte(), 0x82.toByte())
        private val STATUS_INVALID_INSTRUCTION = byteArrayOf(0x6D.toByte(), 0x00.toByte())

        // --- APDU Commands ---
        private const val INS_SELECT: Byte = 0xA4.toByte()
        private const val INS_READ_BINARY: Byte = 0xB0.toByte()

        // --- File IDs ---
        private val FILE_NONE = byteArrayOf()
        private val FILE_CC = byteArrayOf(0xE1.toByte(), 0x03.toByte())
        private val FILE_NDEF = byteArrayOf(0xE1.toByte(), 0x04.toByte())

        // --- AIDs ---
        private val NDEF_AID = byteArrayOf(0xD2.toByte(), 0x76, 0x00, 0x00, 0x85.toByte(), 0x01, 0x01)

        /**
         * This is a standard NDEF Capability Container (CC) file.
         * It tells the reader:
         * - CCLEN (15 bytes)
         * - Mapping Version 2.0
         * - Max read/command sizes
         * - That there is an NDEF file (ID 0xE104) with read access
         */
        private val CAPABILITY_CONTAINER = byteArrayOf(
            0x00, 0x0F, // CCLEN (15 bytes)
            0x20,       // Mapping Version 2.0
            0x00, 0x3B, // MLe (max read size)
            0x00, 0x34, // MLc (max command size)
            0x04,       // NDEF File Control TLV
            0x06,       // Length of NDEF File Control TLV (6 bytes)
            0xE1.toByte(), 0x04, // NDEF File ID (0xE104)
            0x00, 0x40, // Max NDEF file size (64 bytes, example)
            0x00,       // Read Access (always allowed)
            0x00        // Write Access (never allowed)
        )
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "HceService created.")
        updateNdefMessage()
    }

    override fun onDeactivated(reason: Int) {
        Log.d(TAG, "Deactivated: $reason")
        selectedFile = FILE_NONE // Reset state
    }

    /**
     * Updates the NDEF message from SharedPreferences.
     * The NDEF message is prefixed with a 2-byte length (NLEN).
     */
    private fun updateNdefMessage() {
        val sharedPrefs = getSharedPreferences(MainActivity.PREFS_NAME, MODE_PRIVATE)
        val url = sharedPrefs.getString(MainActivity.PREF_KEY_URL, "https://github.com/namdpran8")

        Log.d(TAG, "Creating NDEF message for URL: $url")
        val uriRecord = NdefRecord.createUri(url)
        val msg = NdefMessage(uriRecord)
        val msgBytes = msg.toByteArray()

        // We need to prefix the NDEF message with its 2-byte length (NLEN)
        val nlen = msgBytes.size
        if (nlen > 0xFFFF) { // NLEN is 2 bytes
            Log.e(TAG, "NDEF message too large!")
            ndefMessage = byteArrayOf(0x00, 0x00) // 0-length message
        } else {
            // NLEN is 2 bytes, big-endian
            val nlenBytes = byteArrayOf((nlen shr 8).toByte(), (nlen and 0xFF).toByte())
            ndefMessage = nlenBytes + msgBytes
        }
    }

    override fun processCommandApdu(commandApdu: ByteArray?, extras: Bundle?): ByteArray {
        if (commandApdu == null) {
            return STATUS_FAILED
        }

        val hexCommand = toHexString(commandApdu)
        Log.d(TAG, "Received APDU: $hexCommand")

        // Check for minimum APDU length
        if (commandApdu.size < 5) {
            return STATUS_FAILED
        }

        // Basic APDU parsing
        val cla = commandApdu[0]
        val ins = commandApdu[1]
        val p1 = commandApdu[2]
        val p2 = commandApdu[3]
        val lc = commandApdu[4].toInt() and 0xFF // Length of data
        val data = if (commandApdu.size > 5) commandApdu.sliceArray(5 until (5 + lc)) else byteArrayOf()
        // Le (expected length) is often the last byte if Lc is not present or 0
        val le = commandApdu.last().toInt() and 0xFF

        // Handle SELECT command (INS = 0xA4)
        if (cla == 0x00.toByte() && ins == INS_SELECT) {
            return processSelectCommand(p1, p2, data)
        }

        // Handle READ_BINARY command (INS = 0xB0)
        if (cla == 0x00.toByte() && ins == INS_READ_BINARY) {
            return processReadBinaryCommand(p1, p2, le)
        }

        Log.w(TAG, "Unknown or invalid instruction: $ins")
        return STATUS_INVALID_INSTRUCTION
    }

    /**
     * Handles all SELECT commands (by AID or by File ID)
     */
    private fun processSelectCommand(p1: Byte, p2: Byte, data: ByteArray): ByteArray {
        // P1=0x04 -> Select by AID
        if (p1 == 0x04.toByte()) {
            if (data.contentEquals(NDEF_AID)) {
                Log.i(TAG, "NDEF Application AID selected.")
                updateNdefMessage() // Refresh NDEF message in case URL changed
                selectedFile = FILE_NONE // Reset file selection
                return STATUS_SUCCESS
            }
        }
        // P1=0x00 -> Select by File ID
        else if (p1 == 0x00.toByte()) {
            if (data.contentEquals(FILE_CC)) {
                Log.i(TAG, "Capability Container (CC) file selected.")
                selectedFile = FILE_CC
                return STATUS_SUCCESS
            } else if (data.contentEquals(FILE_NDEF)) {
                Log.i(TAG, "NDEF file selected.")
                selectedFile = FILE_NDEF
                return STATUS_SUCCESS
            }
        }

        Log.w(TAG, "Unknown SELECT command (P1=${toHexString(byteArrayOf(p1))}).")
        return STATUS_FILE_NOT_FOUND
    }

    /**
     * Handles all READ_BINARY commands
     */
    private fun processReadBinaryCommand(p1: Byte, p2: Byte, le: Int): ByteArray {
        // Offset is a 2-byte value from P1 and P2
        val offset = ((p1.toInt() and 0xFF) shl 8) or (p2.toInt() and 0xFF)

        val fileToRead = when {
            selectedFile.contentEquals(FILE_CC) -> {
                Log.d(TAG, "Reading from CC file.")
                CAPABILITY_CONTAINER
            }
            selectedFile.contentEquals(FILE_NDEF) -> {
                Log.d(TAG, "Reading from NDEF file.")
                ndefMessage
            }
            else -> {
                Log.e(TAG, "No file selected, cannot read.")
                return STATUS_FILE_NOT_FOUND
            }
        }

        if (offset >= fileToRead.size) {
            Log.e(TAG, "Read offset ($offset) out of bounds for file size (${fileToRead.size}).")
            return STATUS_FAILED // Offset out of bounds
        }

        // How many bytes to read. 'le' is the max expected length.
        val bytesToRead = (le).coerceAtMost(fileToRead.size - offset)
        val response = fileToRead.sliceArray(offset until (offset + bytesToRead))

        Log.i(TAG, "Sending ${response.size} bytes from file.")
        // Return the data + STATUS_SUCCESS
        return response + STATUS_SUCCESS
    }

    // Helper function to convert a ByteArray to a Hex String for logging
    private fun toHexString(bytes: ByteArray): String {
        return bytes.joinToString("") { "%02x".format(it) }
    }
}