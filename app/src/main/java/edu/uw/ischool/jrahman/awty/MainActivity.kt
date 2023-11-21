package edu.uw.ischool.jrahman.awty

import android.Manifest
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Bundle
import android.telephony.SmsManager
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

const val ALARM_ACTION = "ACTION_SEND_MESSAGE_ALARM"
const val SEND_SMS_PERMISSION_REQUEST_CODE = 1

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
            if (startStopButton.text.toString().equals("Start", ignoreCase = true)) {
                val (isValid, validationMessage) = validateInputs()
                if (isValid) {
                    startAlarm()
                    startStopButton.text = "Stop"
                } else {
                    Toast.makeText(this, validationMessage, Toast.LENGTH_SHORT).show()
                }
            } else {
                stopAlarm()
                startStopButton.text = "Start"
            }
        }

        if (!checkPermission(Manifest.permission.SEND_SMS)) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.SEND_SMS),
                SEND_SMS_PERMISSION_REQUEST_CODE)
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

                    if (checkPermission(Manifest.permission.SEND_SMS)) {
                        sendSMS(phoneNumber, message)
                    } else {
                        Toast.makeText(context, "SMS Permission Required", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
        val filter = IntentFilter(ALARM_ACTION)
        registerReceiver(receiver, filter)
    }

    private fun sendSMS(phoneNumber: String, message: String) {
        try {
            val smsManager: SmsManager = SmsManager.getDefault()
            smsManager.sendTextMessage(phoneNumber, null, message, null, null)
            Toast.makeText(this, "Message Sent to $phoneNumber", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Failed to send message", Toast.LENGTH_SHORT).show()
        }
    }

    private fun startAlarm() {
        val message = messageEditText.text.toString()
        val phoneNumber = phoneNumberEditText.text.toString()
        val intervalMillis = intervalEditText.text.toString().toInt() * 60 * 1000

        if (receiver == null) {
            setupReceiver()
        }

        val intent = Intent(ALARM_ACTION).apply {
            putExtra("message", message)
            putExtra("phone_number", phoneNumber)
        }
        val localPendingIntent = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        pendingIntent = localPendingIntent

        alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager?.setInexactRepeating(
            AlarmManager.RTC_WAKEUP,
            System.currentTimeMillis() + intervalMillis,
            intervalMillis.toLong(),
            localPendingIntent
        )

        startStopButton.text = "Stop"
    }

    private fun stopAlarm() {
        val localPendingIntent = pendingIntent
        if (localPendingIntent != null) {
            alarmManager?.cancel(localPendingIntent)
            localPendingIntent.cancel()
            pendingIntent = null
        }

        receiver?.let {
            unregisterReceiver(it)
            receiver = null
        }
        startStopButton.text = "Start"
    }

    private fun checkPermission(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onDestroy() {
        super.onDestroy()
        receiver?.let { alarmReceiver ->
            unregisterReceiver(alarmReceiver)
        }
    }
}


