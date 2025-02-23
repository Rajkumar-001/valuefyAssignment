package com.example.assigmentvaluefy

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.CalendarContract
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
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
    private lateinit var btnCreateEvent: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var speechRecognizer: SpeechRecognizer
    private var isRecording = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize UI elements
        tvTranscription = findViewById(R.id.tvTranscription)
        etManualInput = findViewById(R.id.etManualInput)
        btnRecord = findViewById(R.id.btnRecord)
        btnCreateEvent = findViewById(R.id.btnCreateEvent)
        progressBar = findViewById(R.id.progressBar)
        progressBar.visibility = View.GONE // Initially hide progress bar

        // Initialize Speech Recognizer
        initializeSpeechRecognizer()

        // Set onClickListener for Record Button
        btnRecord.setOnClickListener {
            if (isRecording) {
                stopRecording()
            } else {
                startRecording()
            }
        }

        // Set onClickListener for Create Event Button
        btnCreateEvent.setOnClickListener {
            val text = etManualInput.text.toString()
            if (text.isNotEmpty()) {
                createCalendarEvent(text)
            } else {
                showToast("Enter event details first")
            }
        }
    }

    private fun initializeSpeechRecognizer() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            showToast("Speech Recognition not available on this device")
            return
        }
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this).apply {
            setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    progressBar.visibility = View.VISIBLE
                    tvTranscription.text = "Listening..."
                }

                override fun onBeginningOfSpeech() {
                    // User has started to speak
                }

                override fun onRmsChanged(rmsdB: Float) {
                    // 音量变化，可以用于动画效果
                }

                override fun onBufferReceived(buffer: ByteArray?) {
                    // Audio buffer received
                }

                override fun onEndOfSpeech() {
                    progressBar.visibility = View.INVISIBLE
                    tvTranscription.text = "Processing..."
                    isRecording = false // Recording stopped by system or user
                    btnRecord.text = "Start Recording" // Update button text
                }

                override fun onError(error: Int) {
                    progressBar.visibility = View.INVISIBLE
                    isRecording = false // Recording stopped due to error
                    btnRecord.text = "Start Recording" // Update button text
                    val errorMessage = getErrorMessage(error)
                    showToast("Speech Recognition Error: $errorMessage")
                    tvTranscription.text = "Error: $errorMessage" // Display error on transcription view
                    Log.e("SpeechRecognizer", "ERROR: $errorMessage") // Log error for debugging
                }

                override fun onResults(results: Bundle?) {
                    progressBar.visibility = View.GONE
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    if (!matches.isNullOrEmpty()) {
                        val text = matches[0]
                        tvTranscription.text = "Transcription:\n$text" // Display transcription with label
                        etManualInput.setText(text) // Fill EditText for editing/event creation
                        // Basic Action Extraction (Prototype Level) - can be enhanced later
                        extractActionsAndDetails(text)
                    } else {
                        tvTranscription.text = "No speech detected. Please try again."
                        showToast("No speech detected.")
                    }
                }

                override fun onPartialResults(partialResults: Bundle?) {
                    //实时识别的中间结果，可以用于实时显示，但这里我们主要用最终结果
                    val partialText = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.get(0) ?: ""
                    tvTranscription.text = "Listening...\n$partialText" // Real-time update during speech
                }

                override fun onEvent(eventType: Int, params: Bundle?) {
                    // 监听其他事件
                }
            })
        }
    }

    private fun startRecording() {
        if (checkAudioPermission()) {
            startVoiceRecognition()
        } else {
            requestAudioPermission()
        }
    }

    private fun stopRecording() {
        speechRecognizer.stopListening()
        isRecording = false
        btnRecord.text = "Start Recording" // Update button text
        progressBar.visibility = View.GONE // Hide progress bar when manually stopped
        tvTranscription.text = "Stopped. Processing..." // Indicate processing after stop
    }


    private fun checkAudioPermission(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestAudioPermission() {
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), AUDIO_PERMISSION_REQUEST_CODE)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == AUDIO_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                showToast("Audio permission granted. Tap record button again.")
            } else {
                showToast("Audio permission denied. Please allow in settings.")
            }
        }
    }


    private fun startVoiceRecognition() {
        isRecording = true
        btnRecord.text = "Stop Recording" // Change button text to indicate recording state

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak now...")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true) // Enable partial results for real-time transcription
        }

        try {
            speechRecognizer.startListening(intent)
        } catch (e: Exception) {
            showToast("Error starting speech recognition: ${e.message}")
            tvTranscription.text = "Error starting recognition: ${e.message}"
            isRecording = false
            btnRecord.text = "Start Recording"
            progressBar.visibility = View.GONE
        }
    }


    private fun createCalendarEvent(eventDetails: String) {
        val intent = Intent(Intent.ACTION_INSERT).apply {
            data = CalendarContract.Events.CONTENT_URI
            putExtra(CalendarContract.Events.TITLE, "New Event from App") // More descriptive title
            putExtra(CalendarContract.Events.DESCRIPTION, eventDetails)
            putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, System.currentTimeMillis() + 3600000) // 1 hour from now
            putExtra(CalendarContract.EXTRA_EVENT_END_TIME, System.currentTimeMillis() + 7200000)   // 2 hours from now
        }
        try {
            startActivity(intent)
            showToast("Event created in Calendar!")
        } catch (e: Exception) {
            showToast("Failed to create event: ${e.message}")
        }
    }


    private fun extractActionsAndDetails(text: String) {
        // Basic keyword-based action item extraction (very rudimentary for prototype)
        val actionKeywords = listOf("task", "action item", "todo", "remember to", "need to", "let's", "we should")
        val isActionItem = actionKeywords.any { text.lowercase(Locale.getDefault()).contains(it) }

        if (isActionItem) {
            showToast("Action item detected!")
            // You can further process 'text' to extract more details if needed.
            // For now, just showing a toast and logging.
            Log.d("ActionExtraction", "Potential action item: $text")
            // In a more advanced version, you would parse the sentence to find the action itself and details.
        } else {
            Log.d("ActionExtraction", "No action item keywords detected.")
        }

        // Basic date/time extraction (very rudimentary - can be greatly improved with NLP)
        // For prototype, we are skipping date/time extraction due to complexity without NLP.
        // In a real application, use NLP libraries for robust date/time extraction.

        // Basic key discussion points - For now, we can consider the whole transcription as notes.
        // In a real app, you'd use summarization techniques to extract key points.
    }


    private fun getErrorMessage(error: Int): String {
        return when (error) {
            SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
            SpeechRecognizer.ERROR_CLIENT -> "Client-side error"
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Insufficient permissions"
            SpeechRecognizer.ERROR_NETWORK -> "Network error"
            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
            SpeechRecognizer.ERROR_NO_MATCH -> "No speech match found"
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognizer busy"
            SpeechRecognizer.ERROR_SERVER -> "Server error"
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Speech timeout"
            else -> "Unknown speech error code: $error"
        }
    }


    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroy() {
        super.onDestroy()
        speechRecognizer.destroy()
    }

    companion object {
        private const val AUDIO_PERMISSION_REQUEST_CODE = 101
    }
}