package com.pranshu.portfolioshare

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    // Declare UI elements as lateinit to initialize them later in onCreate
    private lateinit var urlInput: EditText
    private lateinit var saveButton: Button
    private lateinit var statusTextView: TextView
    private lateinit var prefs: SharedPreferences // SharedPreferences for persistent storage

    companion object {
        private const val TAG = "MainActivity" // Tag for logging purposes
        const val PREFS_NAME = "NFC_PREFS" // Name for the SharedPreferences file
        const val KEY_URL = "url" // Key for storing/retrieving the URL in SharedPreferences
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main) // Set the layout for this activity

        // Log that the app has started and UI setup is beginning
        Log.d(TAG, "App started - setting up UI elements")

        // Initialize UI elements by finding them by their IDs in the layout
        urlInput = findViewById(R.id.urlInput)
        saveButton = findViewById(R.id.saveButton)
        statusTextView = findViewById(R.id.statusTextView)

        // Initialize SharedPreferences to store and retrieve the URL
        prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        // Load the previously saved URL from SharedPreferences
        // If no URL is found, use a default example link
        val savedUrl = prefs.getString(KEY_URL, "https://www.example.com/default-link")
        // Log the loaded URL for debugging
        Log.d(TAG, "Loaded saved URL: $savedUrl")
        // Set the loaded URL to the EditText field
        urlInput.setText(savedUrl)

        // Update status text
        statusTextView.text = "Current HCE Link: $savedUrl\n\n" +
                "To share, bring another NFC-enabled device close to this phone.\n" +
                "Ensure NFC is enabled and screen is unlocked."

        saveButton.setOnClickListener {
            val url = urlInput.text.toString().trim()
            if (url.isNotEmpty()) {
                prefs.edit().putString(KEY_URL, url).apply()
                Log.d(TAG, "Saved new URL to SharedPreferences: $url")

                // Update status text with new URL
                statusTextView.text = "Current HCE Link: $url\n\n" +
                        "To share, bring another NFC-enabled device close to this phone.\n" +
                        "Ensure NFC is enabled and screen is unlocked."

                Toast.makeText(this, "Link saved successfully! HCE service will use this.", Toast.LENGTH_LONG).show()
            } else {
                Log.w(TAG, "Empty URL entered - not saving")
                Toast.makeText(this, "Please enter a valid URL", Toast.LENGTH_SHORT).show()
            }
        }
    }
}