import 'package:flutter/services.dart';

class MicService {
  static const MethodChannel _channel = MethodChannel('com.example.micService');

  /// Запускає фоновий сервіс для запису, передаючи ім'я файлу та бітрейт.
  static Future<void> startMic({
    required String fileName,
    // Цей параметр 'bitRate' є ключовим і повинен бути тут
    int bitRate = 128000,
  }) async {
    try {
      // Передаємо і fileName, і bitRate на нативну сторону
      await _channel.invokeMethod('startMic', {
        'fileName': fileName,
        'bitRate': bitRate,
      });
    } on PlatformException catch (e) {
      print("Failed to start mic service: '${e.message}'.");
    }
  }

  /// Зупиняє фоновий сервіс.
  static Future<void> stopMic() async {
    try {
      await _channel.invokeMethod('stopMic');
    } on PlatformException catch (e) {
      print("Failed to stop mic service: '${e.message}'.");
    }
  }
}
