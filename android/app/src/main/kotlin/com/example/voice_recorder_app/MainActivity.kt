package com.example.voice_recorder_app

import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel
import android.content.Intent
import android.os.Build

class MainActivity : FlutterActivity() {
    private val CHANNEL = "com.example.micService"

    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)
        
        MethodChannel(flutterEngine.dartExecutor.binaryMessenger, CHANNEL).setMethodCallHandler {
            call, result ->
            when (call.method) {
                "startMic" -> {
                    // 1. Отримуємо ім'я файлу, яке передав Flutter
                    val fileName = call.argument<String>("fileName")
                    
                    if (fileName == null) {
                        result.error("ARG_ERROR", "fileName argument is missing", null)
                        return@setMethodCallHandler
                    }
                    
                    // 2. Створюємо Intent для MicService
                    val serviceIntent = Intent(this, MicService::class.java).apply {
                        // 3. Додаємо ім'я файлу як Extra до Intent
                        putExtra("fileName", fileName)
                    }
                    
                    // Запуск фонового сервісу
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        startForegroundService(serviceIntent)
                    } else {
                        startService(serviceIntent)
                    }
                    result.success(null)
                }
                "stopMic" -> {
                    // Зупинка фонового сервісу
                    val serviceIntent = Intent(this, MicService::class.java)
                    stopService(serviceIntent)
                    result.success(null)
                }
                else -> {
                    result.notImplemented()
                }
            }
        }
    }
}