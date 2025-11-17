import 'package:flutter/services.dart';

class MicService {
  static const MethodChannel _channel = MethodChannel('com.example.micService');

  /// Запускає фоновий сервіс для запису, передаючи параметри якості та інтервал
  static Future<void> startMic({
    required String fileName,
    required int bitRate,
    required int autoSaveIntervalMinutes,
  }) async {
    try {
      await _channel.invokeMethod('startMic', {
        'fileName': fileName,
        'bitRate': bitRate,
        'autoSaveIntervalMinutes': autoSaveIntervalMinutes,
      });
    } on PlatformException catch (e) {
      print("Failed to start mic service: '${e.message}'.");
    }
  }

  /// Надсилає команду нативному коду для зупинки запису та перейменування
  /// поточного файлу на ім'я, що містить час закінчення.
  static Future<void> stopAndSaveFinalSegment({
    required String finalFileName,
  }) async {
    try {
      await _channel.invokeMethod('stopAndSaveFinalSegment', {
        'finalFileName': finalFileName,
      });
    } on PlatformException catch (e) {
      print("Failed to stop and save final segment: '${e.message}'.");
    }
  }

  /// Надсилає команду нативному коду, щоб зберегти поточний сегмент
  /// і одразу почати новий запис без зупинки таймера.
  static Future<void> saveSegmentAndContinue({
    required String newFileName,
    required int bitRate,
    required int autoSaveIntervalMinutes,
  }) async {
    try {
      await _channel.invokeMethod('saveSegmentAndContinue', {
        'fileName': newFileName,
        'bitRate': bitRate,
        'autoSaveIntervalMinutes': autoSaveIntervalMinutes,
      });
    } on PlatformException catch (e) {
      print("Failed to save segment and continue: '${e.message}'.");
    }
  }

  /// Скидає нативний таймер автозбереження
  static Future<void> resetTimer() async {
    try {
      await _channel.invokeMethod('resetTimer');
    } on PlatformException catch (e) {
      print("Failed to reset native timer: '${e.message}'.");
    }
  }
}
