package com.example.voice_recorder_app

import android.content.Intent
import androidx.annotation.NonNull
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel

class MainActivity : FlutterActivity() {

    private val MICCHANNEL = "com.example.micService"

    override fun configureFlutterEngine(@NonNull flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)
        MethodChannel(flutterEngine.dartExecutor.binaryMessenger, MICCHANNEL).setMethodCallHandler { call, result ->
            when (call.method) {
                "startMic" -> {
                    val intent = Intent(this, MicService::class.java)
                    startService(intent)
                    result.success(null)
                }
                "stopMic" -> {
                    val intent = Intent(this, MicService::class.java)
                    stopService(intent)
                    result.success(null)
                }
                else -> result.notImplemented()
            }
        }
    }
}
