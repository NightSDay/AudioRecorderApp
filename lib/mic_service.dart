import 'package:flutter/services.dart';

class MicService {
  static const MethodChannel _channel = MethodChannel('com.example.micService');

  /// Запускає фоновий сервіс для запису
  static Future<void> startMic() async {
    try {
      await _channel.invokeMethod('startMic');
    } on PlatformException catch (e) {
      print("Failed to start mic service: '${e.message}'.");
    }
  }

  /// Зупиняє фоновий сервіс
  static Future<void> stopMic() async {
    try {
      await _channel.invokeMethod('stopMic');
    } on PlatformException catch (e) {
      print("Failed to stop mic service: '${e.message}'.");
    }
  }
}
