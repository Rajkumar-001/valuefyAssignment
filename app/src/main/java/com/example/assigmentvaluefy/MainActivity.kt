package com.example.assigmentvaluefy

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.CalendarContract
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.util.*

class MainActivity : AppCompatActivity() {
    private lateinit var tvTranscription: TextView
    private lateinit var etManualInput: EditText
    private lateinit var btnRecord: Button
    private lateinit var speechRecognizer: SpeechRecognizer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvTranscription = findViewById(R.id.tvTranscription)
        etManualInput = findViewById(R.id.etManualInput)
        btnRecord = findViewById(R.id.btnRecord)

        // Initialize Speech Recognizer
        initializeSpeechRecognizer()

        btnRecord.setOnClickListener {
            if (checkAudioPermission()) {
                startVoiceRecognition()
            } else {
                showToast("Please grant microphone permission")
            }
        }
    }

    // Check and Request Permission for Microphone
    private fun checkAudioPermission(): Boolean {
        return if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), 1)
            false
        } else {
            true
        }
    }

    // Initialize Speech Recognizer
    private fun initializeSpeechRecognizer() {
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        speechRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    val text = matches[0]
                    tvTranscription.text = text
                    etManualInput.setText(text)
                    createCalendarEvent(text)
                }
                restartListening()
            }

            override fun onError(error: Int) {
                showToast("Speech recognition failed. Restarting...")
                restartListening() // Restart on failure
            }

            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
            override fun onPartialResults(partialResults: Bundle?) {}
        })
    }

    // Start Speech Recognition
    private fun startVoiceRecognition() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak now...")

        speechRecognizer.startListening(intent)
    }

    // Restart Speech Recognition for Continuous Listening
    private fun restartListening() {
        speechRecognizer.stopListening() // Stop if active
        speechRecognizer.cancel()
        startVoiceRecognition() // Restart immediately
    }

    // Create Calendar Event Automatically
    private fun createCalendarEvent(eventDetails: String) {
        val intent = Intent(Intent.ACTION_INSERT).apply {
            data = CalendarContract.Events.CONTENT_URI
            putExtra(CalendarContract.Events.TITLE, "New Event")
            putExtra(CalendarContract.Events.DESCRIPTION, eventDetails)
            putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, System.currentTimeMillis() + 3600000)
            putExtra(CalendarContract.EXTRA_EVENT_END_TIME, System.currentTimeMillis() + 7200000)
        }

        try {
            startActivity(intent)
            showToast("Event created in Calendar!")
        } catch (e: Exception) {
            showToast("Failed to create event.")
        }
    }

    // Show Toast Message
    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroy() {
        super.onDestroy()
        speechRecognizer.destroy()
    }
}
