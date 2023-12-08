package com.example.calenderapp

import android.app.Activity
import android.app.Dialog
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.TimePicker
import android.widget.Toast
import com.example.calenderapp.auth.LoginActivity
import com.example.calenderapp.databinding.ActivityMainBinding
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException
import com.google.api.client.util.DateTime
import com.google.api.client.util.ExponentialBackOff
import com.google.api.services.calendar.CalendarScopes
import com.google.api.services.calendar.model.Event
import com.google.api.services.calendar.model.EventDateTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Collections.singletonList
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var gso: GoogleSignInOptions
    private lateinit var gsc: GoogleSignInClient
    private lateinit var credential: GoogleAccountCredential
    companion object{
        private val REQUEST_USER_CONSENT = 1001
    }
    private var startTimeMillis: Long = 0
    private var endTimeMillis: Long = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding= ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.web_client_id))
            .requestEmail()
            .build()
        val account: GoogleSignInAccount? = GoogleSignIn.getLastSignedInAccount(this)

        gsc = GoogleSignIn.getClient(this, gso)

        credential = GoogleAccountCredential.usingOAuth2(
            applicationContext, singletonList(CalendarScopes.CALENDAR)
        )
        credential.backOff = ExponentialBackOff()
        credential.selectedAccount = account?.account


        if (account != null){
            binding.tvWelcomeMessage.text = "Welcome ${account.displayName}"
        }
        else{
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        binding.btnListEvent.setOnClickListener {
            startActivity(Intent(this, ListEventActivity::class.java))
        }
        binding.btnSignOut.setOnClickListener {
            gsc.signOut().addOnSuccessListener {
                val intent = Intent(this, LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finishAffinity()
            }
        }

        binding.calendarView.setOnDateChangeListener { view, year, month, dayOfMonth ->
            val selectedDate = Calendar.getInstance()
            selectedDate.set(year, month, dayOfMonth)
            showEventDialog(selectedDate.timeInMillis)
        }
    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_USER_CONSENT) {
            if (resultCode == Activity.RESULT_OK) {
                // User granted consent, attempt to add the event again
                addEventToGoogleCalendar("Title", "Description", startTimeMillis, endTimeMillis)
            } else {
                // User denied consent
                Toast.makeText(this, "User denied consent", Toast.LENGTH_SHORT).show()
            }
        }
    }


    private fun showEventDialog(selectedDateMillis: Long) {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.dialog_event)
        dialog.setCancelable(true)

        val window = dialog.window
        val layoutParams = window?.attributes
        layoutParams?.width = (resources.displayMetrics.widthPixels * 0.8).toInt()
        window?.attributes = layoutParams

        val startTimePicker = dialog.findViewById<TimePicker>(R.id.startTimePicker)
        val endTimePicker = dialog.findViewById<TimePicker>(R.id.endTimePicker)

        val selectedDate = Calendar.getInstance()
        selectedDate.timeInMillis = selectedDateMillis
        val dayOfWeek =
            selectedDate.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.LONG, Locale.getDefault())
        val formattedDate =
            SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(selectedDate.time)

        val tvDay = dialog.findViewById<TextView>(R.id.tvDay)
        val tvDate = dialog.findViewById<TextView>(R.id.tvDate)
        val btnAddEvent = dialog.findViewById<Button>(R.id.btnSaveEvent)

        tvDay.text = dayOfWeek
        tvDate.text = formattedDate
        dialog.show()

        btnAddEvent.setOnClickListener {
            val title = dialog.findViewById<EditText>(R.id.etEventTitle).text.toString()
            val description = dialog.findViewById<EditText>(R.id.etEventDescription).text.toString()

            Log.d("CalendarApp", "Title: $title")
            Log.d("CalendarApp", "Description: $description")

            val startHour = startTimePicker.currentHour
            val startMinute = startTimePicker.currentMinute

            val endHour = endTimePicker.currentHour
            val endMinute = endTimePicker.currentMinute



            // Pastikan judul event tidak kosong
            if (title.isNotEmpty()) {
                val startTimeMillis = selectedDateMillis + startHour * 60 * 60 * 1000 + startMinute * 60 * 1000
                val endTimeMillis = selectedDateMillis + endHour * 60 * 60 * 1000 + endMinute * 60 * 1000




                    addEventToGoogleCalendar(title, description, startTimeMillis, endTimeMillis)


                dialog.dismiss()
            } else {
                Toast.makeText(this, "Event title cannot be empty", Toast.LENGTH_SHORT).show()
            }
        }

    }

    private fun addEventToGoogleCalendar(
        title: String,
        description: String,
        startTimeMillis: Long,
        endTimeMillis: Long
    ) {
        GlobalScope.launch(Dispatchers.Main) {
            try {
                val service = withContext(Dispatchers.IO) {
                    getCalendarService(credential)
                }

                val event = Event()
                    .setSummary(title)
                    .setDescription(description)
                    .setEventType("default")

                val startDateTime = EventDateTime().setDateTime(DateTime(startTimeMillis))
                val endDateTime = EventDateTime().setDateTime(DateTime(endTimeMillis))
                event.start = startDateTime
                event.end = endDateTime

                try {
                    val calendarId = "primary"
                    withContext(Dispatchers.IO) {
                        service.events().insert(calendarId, event).execute()
                    }

                    // Event added successfully
                    Toast.makeText(
                        this@MainActivity,
                        "Event added to Google Calendar",
                        Toast.LENGTH_SHORT
                    ).show()
                } catch (e: UserRecoverableAuthIOException) {
                    startActivityForResult(e.intent, REQUEST_USER_CONSENT)
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(
                        this@MainActivity,
                        "Failed to add event: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(
                    this@MainActivity,
                    "Failed to add event: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }


    private fun getCalendarService(credential: GoogleAccountCredential): com.google.api.services.calendar.Calendar {
        return com.google.api.services.calendar.Calendar.Builder(
            com.google.api.client.googleapis.javanet.GoogleNetHttpTransport.newTrustedTransport(),
            com.google.api.client.json.gson.GsonFactory.getDefaultInstance(),
            credential
        )
            .setApplicationName("YourAppName")
            .build()
    }
}