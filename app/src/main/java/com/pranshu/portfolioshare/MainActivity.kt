package com.pranshu.portfolioshare

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast

import com.pranshu.portfolioshare.databinding.ActivityMainBinding
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    // Companion object to hold constants that can be accessed from other classes (like our HceService)
    companion object {
        const val PREFS_NAME = "sharedPrefs"
        const val PREF_KEY_URL = "STRING_KEY"
        const val TAG = "MainActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        loadData()

        binding.saveButton.setOnClickListener {
            saveData()
        }
    }

    private fun saveData() {
        val insertedText = binding.urlInput.text.toString().trim()

        // Basic validation to ensure it's a plausible URL
        if (insertedText.isNotEmpty() && android.util.Patterns.WEB_URL.matcher(insertedText).matches()) {
            val sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val editor = sharedPreferences.edit()
            editor.putString(PREF_KEY_URL, insertedText)
            editor.apply() // apply() saves the data in the background

            Toast.makeText(this, "URL Saved!", Toast.LENGTH_SHORT).show()
            Log.d(TAG, "Saved URL: $insertedText")
        } else {
            Toast.makeText(this, "Please enter a valid URL.", Toast.LENGTH_LONG).show()
            Log.w(TAG, "Invalid URL entered: $insertedText")
        }
    }

    private fun loadData() {
        val sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        // Load the saved URL, providing a default value if nothing is saved yet
        val savedString = sharedPreferences.getString(PREF_KEY_URL, "https://github.com/namdpran8")
        binding.urlInput.setText(savedString)
        Log.d(TAG, "Loaded URL: $savedString")
    }
}
