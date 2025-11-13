import 'package:flutter/services.dart';

class MicService {
  // Назва каналу має збігатися з назвою в MainActivity.kt
  static const MethodChannel _channel = MethodChannel('com.example.micService');

  /// Запускає фоновий сервіс для запису, передаючи ім'я файлу.
  static Future<void> startMic({required String fileName}) async {
    try {
      // Викликаємо нативний метод 'startMic' і передаємо Map з аргументами
      await _channel.invokeMethod('startMic', {'fileName': fileName});
    } on PlatformException catch (e) {
      // Обробка помилок, якщо MethodChannel не може викликати нативний метод
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
