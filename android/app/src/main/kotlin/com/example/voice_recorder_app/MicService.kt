package com.example.voice_recorder_app

import android.app.*
import android.content.Intent
import android.media.MediaRecorder
import android.os.*
import android.util.Log
import androidx.core.app.NotificationCompat
import java.io.File
import java.text.SimpleDateFormat // Додано для генерації імені файлу
import java.util.Date           // Додано для генерації імені файлу
import java.util.Locale         // Додано для форматування дати
import com.example.voice_recorder_app.MainActivity.Companion.EXTRA_BIT_RATE
import com.example.voice_recorder_app.MainActivity.Companion.EXTRA_FILE_NAME
import com.example.voice_recorder_app.MainActivity.Companion.EXTRA_FINAL_FILE_NAME
import com.example.voice_recorder_app.MainActivity.Companion.EXTRA_AUTO_SAVE_INTERVAL_MINUTES
import android.content.Context

class MicService : Service() {

    private var recorder: MediaRecorder? = null
    private var outputFile: String? = null // Шлях до поточного файлу запису
    private val CHANNEL_ID = "MicServiceChannel"

    // --- Логіка Автозбереження та Керування ---
    // Handler відповідає за планування автозбереження у фоновому потоці
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var restartRunnable: Runnable
    private var currentBitRate: Int = 128000
    private var currentIntervalMs: Long = 0

    // Додаємо onBind тут, щоб задовольнити вимоги абстрактного класу Service
    override fun onBind(intent: Intent?): IBinder? = null
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()

        // Ініціалізація Runnable для автозбереження. 
        // Цей код спрацьовує, коли таймер закінчується, і ініціює збереження нового сегмента.
        restartRunnable = Runnable {
            Log.d("MicService", "AutoSave timer elapsed. Restarting recording segment.")
            
            // 1. Генеруємо нове ім'я файлу на момент спрацьовування таймера
            val newFileName = generateNewSegmentFileName()

            // 2. Створюємо Intent з дією збереження сегмента
            val intent = Intent(this, MicService::class.java).apply {
                action = MainActivity.ACTION_SAVE_SEGMENT
                // Передаємо нове ім'я файлу сервісу, щоб він міг почати запис у новий файл
                putExtra(EXTRA_FILE_NAME, newFileName)
            }
            
            // 3. Викликаємо сервіс, щоб він обробив цей Intent (виконав стоп/старт у новий файл)
            startForegroundService(intent)
        }
    }override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action

        when (action) {
            MainActivity.ACTION_START_MIC, MainActivity.ACTION_SAVE_SEGMENT -> {
                // START_MIC - початковий запуск. SAVE_SEGMENT - перезапуск за таймером.

                // Отримуємо параметри. Для SAVE_SEGMENT ім'я файлу приходить з Runnable.
                val receivedFileName = intent.getStringExtra(EXTRA_FILE_NAME)
                val receivedBitRate = intent.getIntExtra(EXTRA_BIT_RATE, 128000)
                val receivedIntervalMinutes = intent.getIntExtra(EXTRA_AUTO_SAVE_INTERVAL_MINUTES, 0)

                // Якщо це початковий запуск, оновлюємо глобальні параметри
                if (action == MainActivity.ACTION_START_MIC) {
                    currentBitRate = receivedBitRate
                    currentIntervalMs = receivedIntervalMinutes.toLong() * 60 * 1000 // Хвилини в мс
                    Log.d("MicService", "INITIAL START. Interval set to: ${currentIntervalMs / 60000} min.")
                }

                // Обов'язково видаляємо старий таймер перед запуском нового сегмента
                handler.removeCallbacks(restartRunnable)

                // 1. Зупиняємо попередній запис, якщо він існує
                recorder?.stop()
                recorder?.release()
                recorder = null

                // 2. Встановлюємо новий шлях до файлу
                if (receivedFileName != null) {
                    outputFile = File(externalCacheDir, receivedFileName).absolutePath
                } else if (outputFile == null) {
                    // Запобігання помилці
                    outputFile = File(externalCacheDir, generateNewSegmentFileName()).absolutePath
                }

                startForeground(1, buildNotification())

                // (Решта логіки запуску в Частині 4)
                // 3. Налаштування та запуск нового MediaRecorder
                setupAndStartRecorder(outputFile)

                // 4. Запуск таймера автозбереження, якщо інтервал встановлено
                if (currentIntervalMs > 0) {
                    handler.postDelayed(restartRunnable, currentIntervalMs)
                    Log.d("MicService", "AutoSave timer scheduled for ${currentIntervalMs / 60000} min.")
                }
            }
            
            MainActivity.ACTION_STOP_AND_SAVE_FINAL -> {
                // Фінальне збереження файлу (час закінчення)
                val finalFileName = intent.getStringExtra(EXTRA_FINAL_FILE_NAME)
                
                // 1. Зупиняємо запис
                recorder?.stop()
                recorder?.release()
                recorder = null

                // 2. Перейменовуємо файл на ім'я, що містить час закінчення
                if (outputFile != null && finalFileName != null) {
                    val currentFile = File(outputFile)
                    val finalFile = File(currentFile.parent, finalFileName)
                    if (currentFile.exists()) {
                        currentFile.renameTo(finalFile)
                        Log.d("MicService", "Final file saved and renamed to: ${finalFile.name}")
                    }
                }
                
                // 3. Зупиняємо сервіс та таймер
                handler.removeCallbacks(restartRunnable)
                stopSelf()
            }
            
            MainActivity.ACTION_RESET_TIMER -> {
                // Скидання таймера (викликається, коли Flutter хоче скинути таймер)
                handler.removeCallbacks(restartRunnable)
                Log.d("MicService", "AutoSave timer reset by Flutter command.")
            }
            
            else -> {} // Невідома дія або null
        }
        return START_STICKY
    }override fun onDestroy() {
        Log.d("MicService", "Service destroyed. Cleaning up recorder and handler.")
        handler.removeCallbacks(restartRunnable)
        recorder?.apply {
            try {
                // Викликаємо stop() перед release(), але обробляємо можливі винятки
                stop() 
            } catch (e: Exception) {
                Log.e("MicService", "Stop failed, possibly already stopped.")
            }
            release()
        }
        recorder = null
        super.onDestroy()
        stopForeground(true) // Очищаємо сповіщення
    }

    // Допоміжна функція для налаштування MediaRecorder
    private fun setupAndStartRecorder(fileName: String?) {
        if (fileName == null) {
            Log.e("MicService", "Output file name is null, cannot start recording.")
            return
        }

        try {
            // --- Встановлення параметрів якості ---
            val selectedSamplingRate = when {
                currentBitRate >= 192000 -> 16000 // Висока якість
                currentBitRate >= 128000 -> 11025 // Середня якість (уникаємо 192k bug)
                else -> 8000 // Низька якість
            }
            Log.d("MicService", "Selected Sampling Rate: $selectedSamplingRate Hz.")

            recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(this)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(currentBitRate)
                setAudioSamplingRate(selectedSamplingRate)
                setOutputFile(fileName)
                prepare()
                start()
            }
            Log.d("MicService", "Recording started successfully.")

        } catch (e: Exception) {
            Log.e("MicService", "Error during recording setup: ${e.message}")
        }
    }// --- Допоміжна функція для генерації імені файлу ---
    private fun generateNewSegmentFileName(): String {
        val formatter = SimpleDateFormat("'Rec'_yyyyMMdd_HHmmss'.m4a'", Locale.getDefault())
        return formatter.format(Date())
    }
    
    // --- Логіка Сповіщень ---

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Mic Background Service",
                NotificationManager.IMPORTANCE_LOW
            )
            // Виправлено: getSystemService вимагає Context (тобто this)
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
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
            .setContentText("Tap to return to the app")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent)
            .build()
    }
}