package edu.uw.ischool.jrahman.awty

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import android.util.Log


const val ALARM_ACTION = "ACTION_SEND_MESSAGE_ALARM"

class MainActivity : AppCompatActivity() {
    lateinit var messageEditText: EditText
    lateinit var phoneNumberEditText: EditText
    lateinit var intervalEditText: EditText
    lateinit var startStopButton: Button
    private var alarmManager: AlarmManager? = null
    private var pendingIntent: PendingIntent? = null
    private var receiver: BroadcastReceiver? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        messageEditText = findViewById(R.id.messageEditText)
        phoneNumberEditText = findViewById(R.id.phoneNumberEditText)
        intervalEditText = findViewById(R.id.intervalEditText)
        startStopButton = findViewById(R.id.startStopButton)
        setupReceiver()

        startStopButton.setOnClickListener {
            val (isValid, validationMessage) = validateInputs()
            if (isValid) {
                startAlarm()
            } else {
                Toast.makeText(this, validationMessage, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun validateInputs(): Pair<Boolean, String> {
        val message = messageEditText.text.toString()
        val phoneNumber = phoneNumberEditText.text.toString()
        val interval = intervalEditText.text.toString().toIntOrNull()

        return when {
            message.isEmpty() -> Pair(false, "Message cannot be empty.")
            phoneNumber.isEmpty() -> Pair(false, "Phone number cannot be empty.")
            interval == null -> Pair(false, "Interval must be a number.")
            interval <= 0 -> Pair(false, "Interval must be greater than 0.")
            else -> Pair(true, "")
        }
    }

    private fun setupReceiver() {
        if (receiver == null) {
            receiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent?) {
                    val message = intent?.getStringExtra("message") ?: "No message"
                    val phoneNumber = intent?.getStringExtra("phone_number") ?: "No phone number"
                    Toast.makeText(context, "$phoneNumber: $message", Toast.LENGTH_LONG).show()
                    Log.d("setupReceiver", "Toast displayed")
                }
            }
        }
        val filter = IntentFilter(ALARM_ACTION)
        registerReceiver(receiver, filter)
    }

    private fun startAlarm() {
        val message = messageEditText.text.toString()
        val phoneNumber = phoneNumberEditText.text.toString()
        val intervalMillis = intervalEditText.text.toString().toInt() * 60 * 1000

        val intent = Intent(ALARM_ACTION).apply {
            putExtra("message", message)
            putExtra("phone_number", phoneNumber)
        }
        pendingIntent = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

        alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        pendingIntent?.let {
            alarmManager?.setInexactRepeating(
                AlarmManager.RTC_WAKEUP,
                System.currentTimeMillis() + intervalMillis,
                intervalMillis.toLong(),
                it
            )
        }

        startStopButton.text = "Stop"
    }

    private fun stopAlarm() {
        pendingIntent?.let {
            alarmManager?.cancel(it)
            it.cancel()
        }
        pendingIntent = null


        receiver?.let {
            unregisterReceiver(it)
            receiver = null
        }
        startStopButton.text = "Start"
    }

    override fun onDestroy() {
        super.onDestroy()
        receiver?.let { alarmReceiver ->
            unregisterReceiver(alarmReceiver)
        }
    }
}

