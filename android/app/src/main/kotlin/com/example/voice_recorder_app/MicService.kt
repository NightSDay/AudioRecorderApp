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

// Константа, що визначає ключі, які використовуються в Intent
private const val EXTRA_FILE_NAME = "fileName"
private const val EXTRA_BIT_RATE = "bitRate" 

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

        val fileName = intent?.getStringExtra(EXTRA_FILE_NAME) ?: "background_recording_fallback.m4a"
        val bitRate = intent?.getIntExtra(EXTRA_BIT_RATE, 128000) ?: 128000 
        
        val file = File(externalCacheDir, fileName)
        outputFile = file.absolutePath
        
        Log.d("MicService", "Saving recording to: $outputFile. Requested BitRate: $bitRate bps.")

        // ✅ НОВА ЛОГІКА: Динамічна зміна частоти дискретизації
        // Якщо обрано низький бітрейт (64000), ми повинні використовувати низьку частоту (8000 Гц), 
        // щоб кодер AAC прийняв цей бітрейт.
        val samplingRate = if (bitRate <= 64000) {
            8000 // Низька частота для сумісності з низьким бітрейтом (AMR/телефонна якість)
        } else {
            16000 // Стандартна частота для голосового запису (хороша якість)
        }
        
        Log.d("MicService", "Selected Sampling Rate: $samplingRate Hz.")


        startForeground(1, buildNotification())

        try {
            recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) { 
                MediaRecorder(this)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                // Використовуємо MPEG_4 / AAC для універсальності та кращої якості
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4) 
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                
                // ✅ ЗАСТОСУВАННЯ ДИНАМІЧНОЇ ЧАСТОТИ ДИСКРЕТИЗАЦІЇ
                setAudioSamplingRate(samplingRate) 
                
                // Встановлення бітрейту
                setAudioEncodingBitRate(bitRate) 
                
                setOutputFile(outputFile)
                prepare()
                start()
            }
            Log.d("MicService", "Recording started successfully.")
        } catch (e: IOException) {
            Log.e("MicService", "MediaRecorder preparation failed: ${e.message}")
            stopSelf()
        } catch (e: IllegalStateException) {
            Log.e("MicService", "MediaRecorder failed to start: ${e.message}")
            stopSelf()
        }

        return START_STICKY
    }

    override fun onDestroy() {
        Log.d("MicService", "Stopping recording")
        recorder?.apply {
            try {
                stop() 
            } catch (e: RuntimeException) {
                Log.e("MicService", "Stop failed: MediaRecorder might not have been started or might have already stopped.")
            }
            release()
        }
        recorder = null
        super.onDestroy()
        stopForeground(true)
    }

    override fun onBind(intent: Intent?): IBinder? = null

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
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }
}