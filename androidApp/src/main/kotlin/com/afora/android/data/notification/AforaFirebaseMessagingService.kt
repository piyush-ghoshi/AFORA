package com.afora.android.data.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.afora.android.MainActivity
import com.afora.android.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class AforaFirebaseMessagingService : FirebaseMessagingService() {

    companion object {
        private const val TAG = "AforaFCM"
        const val CHANNEL_ATTENDANCE = "channel_attendance"
        const val CHANNEL_LEAVES = "channel_leaves"
        const val CHANNEL_GENERAL = "channel_general"

        /**
         * Upload device FCM token to Spring Boot backend.
         */
        fun syncTokenWithBackend(token: String) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val currentUser = FirebaseAuth.getInstance().currentUser ?: return@launch
                    val idTokenResult = currentUser.getIdToken(false).await()
                    val idToken = idTokenResult.token ?: return@launch

                    val url = URL("http://localhost:8080/api/notifications/fcm-token")
                    val conn = (url.openConnection() as HttpURLConnection).apply {
                        requestMethod = "POST"
                        setRequestProperty("Content-Type", "application/json")
                        setRequestProperty("Authorization", "Bearer $idToken")
                        doOutput = true
                        connectTimeout = 10000
                        readTimeout = 10000
                    }

                    val body = JSONObject().apply {
                        put("token", token)
                        put("deviceType", "ANDROID")
                    }.toString()

                    conn.outputStream.use { os ->
                        os.write(body.toByteArray(Charsets.UTF_8))
                    }

                    val responseCode = conn.responseCode
                    Log.i(TAG, "FCM token synced with backend: HTTP $responseCode")
                    conn.disconnect()
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to sync FCM token with backend", e)
                }
            }
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.i(TAG, "New FCM Token received: $token")
        // Cache token in SharedPreferences
        val prefs = getSharedPreferences("fcm_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString("latest_fcm_token", token).apply()
        // Sync to backend if user is already signed in
        syncTokenWithBackend(token)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.i(TAG, "FCM Message received from: ${remoteMessage.from}")

        val title = remoteMessage.notification?.title
            ?: remoteMessage.data["title"]
            ?: "AFORA Academic"

        val body = remoteMessage.notification?.body
            ?: remoteMessage.data["body"]
            ?: "New notification received."

        val channelId = remoteMessage.data["channelId"] ?: CHANNEL_GENERAL

        showNotification(title, body, channelId)
    }

    private fun showNotification(title: String, body: String, channelId: String) {
        createNotificationChannels()

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            System.currentTimeMillis().toInt(),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        try {
            NotificationManagerCompat.from(this).notify(
                System.currentTimeMillis().toInt(),
                notification
            )
        } catch (e: SecurityException) {
            Log.e(TAG, "Missing notification permission", e)
        }
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java) ?: return

            val attendanceChannel = NotificationChannel(
                CHANNEL_ATTENDANCE,
                "Attendance Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alerts when attendance is finalized or marked"
                enableVibration(true)
            }

            val leavesChannel = NotificationChannel(
                CHANNEL_LEAVES,
                "Leave Updates",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alerts on leave request approval or rejection"
                enableVibration(true)
            }

            val generalChannel = NotificationChannel(
                CHANNEL_GENERAL,
                "General Alerts",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "General university announcements and alerts"
            }

            manager.createNotificationChannel(attendanceChannel)
            manager.createNotificationChannel(leavesChannel)
            manager.createNotificationChannel(generalChannel)
        }
    }
}
