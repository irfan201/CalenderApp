package com.example.calenderapp

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.calenderapp.adapter.EventAdapter
import com.example.calenderapp.auth.LoginActivity
import com.example.calenderapp.databinding.ActivityListEventBinding
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException
import com.google.api.client.util.DateTime
import com.google.api.client.util.ExponentialBackOff
import com.google.api.services.calendar.Calendar
import com.google.api.services.calendar.CalendarScopes
import com.google.api.services.calendar.model.Events
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Collections
import java.util.Locale

class ListEventActivity : AppCompatActivity() {
    private lateinit var binding: ActivityListEventBinding
    private lateinit var gso: GoogleSignInOptions
    private lateinit var gsc: GoogleSignInClient
    private lateinit var eventAdapter: EventAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityListEventBinding.inflate(layoutInflater)
        setContentView(binding.root)


        gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.web_client_id))
            .requestEmail()
            .build()

        val account: GoogleSignInAccount? = GoogleSignIn.getLastSignedInAccount(this)

        if (account != null) {

            eventAdapter = EventAdapter(emptyList())
            binding.rvEvent.adapter = eventAdapter
            binding.rvEvent.layoutManager = LinearLayoutManager(this)
            Log.d("ListEventActivity", "itemcount: ${eventAdapter.itemCount}")


            val service = setupGoogleCalendarService(account)


            fetchAndDisplayEvents(service)
        } else {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        gsc = GoogleSignIn.getClient(this, gso)
    }

    private fun setupGoogleCalendarService(account: GoogleSignInAccount): Calendar {
        val credential = GoogleAccountCredential.usingOAuth2(
            applicationContext, Collections.singletonList(CalendarScopes.CALENDAR_READONLY)
        )
        credential.backOff = ExponentialBackOff()
        credential.selectedAccount = account.account

        return Calendar.Builder(
            com.google.api.client.googleapis.javanet.GoogleNetHttpTransport.newTrustedTransport(),
            com.google.api.client.json.gson.GsonFactory.getDefaultInstance(),
            credential
        )
            .setApplicationName("YourAppName")
            .build()
    }

    private fun fetchAndDisplayEvents(service: Calendar) {
        binding.calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
            val selectedDate = java.util.Calendar.getInstance()
            selectedDate.set(year, month, dayOfMonth, 0, 0, 0)
            val startOfDay = selectedDate.timeInMillis
            val endOfDay = startOfDay + (24 * 60 * 60 * 1000) - 1
            val dayOfWeek =
                selectedDate.getDisplayName(java.util.Calendar.DAY_OF_WEEK, java.util.Calendar.LONG, Locale.getDefault())
            val formattedDate =
                SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(selectedDate.time)
            binding.tvDay.text = dayOfWeek
            binding.tvDate.text = formattedDate

            GlobalScope.launch(Dispatchers.Main) {
                try {
                    val events: Events = withContext(Dispatchers.IO) {
                        service.events().list("primary")
                            .setTimeMin(DateTime(startOfDay))
                            .setTimeMax(DateTime(endOfDay))
                            .setMaxResults(10)
                            .execute()
                    }

                    eventAdapter.updateData(events.items)

                    Log.d("ListEventActivity", "Events: ${events.items}")
                } catch (e: UserRecoverableAuthIOException) {
                    startActivityForResult(e.intent, REQUEST_AUTHORIZATION)
                } catch (e: IOException) {
                    Log.e("ListEventActivity", "Error fetching events", e)
                }
            }
        }
    }

    companion object {
        private const val REQUEST_AUTHORIZATION = 1001
    }
}

