package com.example.voice_recorder_app

import android.app.*
import android.content.Intent
import android.media.MediaRecorder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import java.io.File
import java.io.IOException

// Константа, що визначає ключ, який використовується в Intent
private const val EXTRA_FILE_NAME = "fileName"

class MicService : Service() {

    private var recorder: MediaRecorder? = null
    private var outputFile: String? = null
    private val CHANNEL_ID = "MicServiceChannel"

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("MicService", "Received command to start recording")

        // 1. Отримуємо ім'я файлу з Intent, яке було передано з Flutter через MainActivity
        val fileName = intent?.getStringExtra(EXTRA_FILE_NAME) ?: "background_recording_fallback.mp4"
        
        // Визначаємо повний шлях до файлу (використовуємо кеш-директорію)
        val file = File(externalCacheDir, fileName)
        outputFile = file.absolutePath
        Log.d("MicService", "Saving recording to: $outputFile")

        // Запуск як Foreground Service
        startForeground(1, buildNotification())

        try {
            recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(this)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setOutputFile(outputFile)
                prepare()
                start()
            }
            Log.d("MicService", "Recording started successfully.")
        } catch (e: IOException) {
            Log.e("MicService", "MediaRecorder preparation failed: ${e.message}")
            stopSelf() // Зупиняємо сервіс, якщо не вдалося підготувати рекордер
        } catch (e: IllegalStateException) {
            Log.e("MicService", "MediaRecorder failed to start: ${e.message}")
            stopSelf()
        }

        return START_STICKY
    }

    override fun onDestroy() {
        Log.d("MicService", "Stopping recording")
        recorder?.apply {
            // Обов'язково викликати stop() перед release()
            try {
                stop() 
            } catch (e: RuntimeException) {
                Log.e("MicService", "Stop failed: MediaRecorder might not have been started or might have already stopped.")
            }
            release()
        }
        recorder = null
        super.onDestroy()
        stopForeground(true) // Очищаємо сповіщення
    }

    override fun onBind(intent: Intent?): IBinder? = null

    // Решта методів (createNotificationChannel, buildNotification) залишаються без змін
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Mic Background Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }

    private fun buildNotification(): Notification {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                PendingIntent.FLAG_IMMUTABLE
            else 0
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("🎙️ Recording in background")
            .setContentText("Saving to: ${outputFile?.substringAfterLast('/') ?: "..."}")
            .setSmallIcon(R.mipmap.ic_launcher) // Переконайтеся, що іконка існує
            .setContentIntent(pendingIntent)
            .setOngoing(true) // Вказує, що це активний фоновий сервіс
            .build()
    }
}