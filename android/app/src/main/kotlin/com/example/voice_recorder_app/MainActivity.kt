package com.example.voice_recorder_app

import android.content.Intent
import android.os.Bundle
import android.util.Log
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel

class MainActivity : FlutterActivity() {
    private val CHANNEL = "com.example.micService"
    
    // Дії та ключі для Intent (повинні збігатися з MicService.kt)
    companion object {
        const val ACTION_START_MIC = "START_MIC"
        const val ACTION_STOP_MIC = "STOP_MIC"
        const val ACTION_STOP_AND_SAVE_FINAL = "STOP_AND_SAVE_FINAL" // Нова дія для фінального збереження
        const val ACTION_SAVE_SEGMENT = "SAVE_SEGMENT"
        const val ACTION_RESET_TIMER = "RESET_TIMER"

        const val EXTRA_FILE_NAME = "fileName"
        const val EXTRA_FINAL_FILE_NAME = "finalFileName" // Новий ключ для імені фінального файлу
        const val EXTRA_BIT_RATE = "bitRate"
        const val EXTRA_AUTO_SAVE_INTERVAL_MINUTES = "autoSaveIntervalMinutes"
    }override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)
        MethodChannel(flutterEngine.dartExecutor.binaryMessenger, CHANNEL).setMethodCallHandler {
            call, result ->
            
            // Отримання аргументів
            val fileName = call.argument<String>(EXTRA_FILE_NAME)
            val finalFileName = call.argument<String>(EXTRA_FINAL_FILE_NAME)
            val bitRate = call.argument<Int>(EXTRA_BIT_RATE)
            val autoSaveIntervalMinutes = call.argument<Int>(EXTRA_AUTO_SAVE_INTERVAL_MINUTES)
            
            val serviceIntent = Intent(this, MicService::class.java)

            when (call.method) {
                "startMic" -> {
                    if (fileName != null && bitRate != null && autoSaveIntervalMinutes != null) {
                        serviceIntent.action = ACTION_START_MIC
                        serviceIntent.putExtra(EXTRA_FILE_NAME, fileName)
                        serviceIntent.putExtra(EXTRA_BIT_RATE, bitRate)
                        serviceIntent.putExtra(EXTRA_AUTO_SAVE_INTERVAL_MINUTES, autoSaveIntervalMinutes)
                        
                        startForegroundService(serviceIntent)
                        result.success(null)
                    } else {
                        result.error("INVALID_ARGS", "Missing required arguments for startMic", null)
                    }
                }"stopAndSaveFinalSegment" -> {
                    // Команда, що надсилається при натисканні "Stop Recording" (час закінчення)
                    if (finalFileName != null) {
                        serviceIntent.action = ACTION_STOP_AND_SAVE_FINAL
                        serviceIntent.putExtra(EXTRA_FINAL_FILE_NAME, finalFileName)
                        
                        startForegroundService(serviceIntent) // Надсилаємо команду сервісу
                        result.success(null)
                    } else {
                        result.error("INVALID_ARGS", "Missing finalFileName argument for stopAndSaveFinalSegment", null)
                    }
                }

                "saveSegmentAndContinue" -> {
                    // Ця команда не використовується у поточній логіці, оскільки 
                    // сегментація відбувається автоматично в MicService.kt за таймером.
                    // Але ми залишаємо її тут для повноти:
                    if (fileName != null && bitRate != null && autoSaveIntervalMinutes != null) {
                        serviceIntent.action = ACTION_SAVE_SEGMENT
                        serviceIntent.putExtra(EXTRA_FILE_NAME, fileName)
                        serviceIntent.putExtra(EXTRA_BIT_RATE, bitRate)
                        serviceIntent.putExtra(EXTRA_AUTO_SAVE_INTERVAL_MINUTES, autoSaveIntervalMinutes)
                        
                        startForegroundService(serviceIntent)
                        result.success(null)
                    } else {
                        result.error("INVALID_ARGS", "Missing required arguments for saveSegmentAndContinue", null)
                    }
                }
                "resetTimer" -> {
                    // Надсилаємо команду сервісу для видалення всіх очікуваних
                    // постів з handler.removeCallbacks(restartRunnable).
                    serviceIntent.action = ACTION_RESET_TIMER
                    
                    // Ми не зупиняємо сервіс, а лише надсилаємо команду!
                    startService(serviceIntent)
                    result.success(null)
                }
                
                else -> {
                    result.notImplemented()
                }
            }
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // onCreate зазвичай порожній для FlutterActivity, 
        // оскільки вся логіка налаштування каналу відбувається в configureFlutterEngine.
    }
}