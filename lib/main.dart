import 'dart:async';
import 'package:flutter/material.dart';
import 'package:permission_handler/permission_handler.dart';
import 'mic_service.dart';

void main() {
  runApp(const MyApp());
}

class MyApp extends StatelessWidget {
  const MyApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      debugShowCheckedModeBanner: false,
      title: 'Audio Recorder',
      theme: ThemeData(
        // Використовуємо Material 3 для сучасного вигляду
        primarySwatch: Colors.blue,
        useMaterial3: true,
      ),
      home: const AudioRecorderWidget(),
    );
  }
}

class AudioRecorderWidget extends StatefulWidget {
  const AudioRecorderWidget({super.key});

  @override
  State<AudioRecorderWidget> createState() => _AudioRecorderWidgetState();
}

class _AudioRecorderWidgetState extends State<AudioRecorderWidget> {
  bool isRecording = false;
  int secondsElapsed = 0;
  Timer? _timer;

  @override
  void initState() {
    super.initState();
    // Запит дозволів при старті віджета
    requestPermissions();
  }

  @override
  void dispose() {
    _timer?.cancel();
    super.dispose();
  }

  // Запит дозволів на мікрофон
  Future<void> requestPermissions() async {
    await Permission.microphone.request();
  }

  void _startTimer() {
    // Запуск таймера, що оновлює інтерфейс кожну секунду
    _timer = Timer.periodic(const Duration(seconds: 1), (timer) {
      setState(() => secondsElapsed++);
    });
  }

  void _stopTimer() {
    // Зупинка та скасування таймера
    _timer?.cancel();
    _timer = null;
    setState(() => secondsElapsed = 0);
  }

  // Генерація імені файлу з розширенням .m4a
  String _generateFileName() {
    final now = DateTime.now();
    return 'Rec_${now.year}${now.month.toString().padLeft(2, '0')}'
        '${now.day.toString().padLeft(2, '0')}_'
        '${now.hour.toString().padLeft(2, '0')}${now.minute.toString().padLeft(2, '0')}'
        '${now.second.toString().padLeft(2, '0')}.m4a'; // Використовуємо .m4a для аудіо
  }

  Future<void> startRecording() async {
    final fileName = _generateFileName();

    // Виклик нативного сервісу для початку запису
    await MicService.startMic(fileName: fileName);

    setState(() => isRecording = true);
    _startTimer();
  }

  Future<void> stopRecording() async {
    // Виклик нативного сервісу для зупинки запису
    await MicService.stopMic();
    setState(() => isRecording = false);
    _stopTimer();
    // Тут можна додати спливаюче повідомлення про успішне збереження
  }

  @override
  Widget build(BuildContext context) {
    // Форматування часу MM:SS
    String formattedTime =
        '${(secondsElapsed ~/ 3600)} : ${(secondsElapsed ~/ 60).toString().padLeft(2, '0')}:${(secondsElapsed % 60).toString().padLeft(2, '0')}';

    return Scaffold(
      appBar: AppBar(
        title: const Text('🎙️ Голосовий Запис'),
        backgroundColor: Theme.of(context).colorScheme.inversePrimary,
      ),
      body: Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Text(
              isRecording ? 'ЗАПИС...' : 'Готовий до запису',
              style: TextStyle(
                fontSize: 24,
                fontWeight: FontWeight.w600,
                color: isRecording ? Colors.redAccent : Colors.grey.shade700,
              ),
            ),
            const SizedBox(height: 16),
            // Індикатор часу
            if (isRecording)
              Text(
                formattedTime,
                style: const TextStyle(
                  fontSize: 48,
                  fontWeight: FontWeight.bold,
                  color: Colors.black87,
                ),
              ),
            const SizedBox(height: 40),
            // Кнопка Старт/Стоп
            ElevatedButton.icon(
              icon: Icon(isRecording ? Icons.stop : Icons.mic_none, size: 30),
              label: Padding(
                padding: const EdgeInsets.symmetric(
                  horizontal: 10,
                  vertical: 8,
                ),
                child: Text(
                  isRecording ? 'ЗУПИНИТИ' : 'ПОЧАТИ ЗАПИС',
                  style: const TextStyle(fontSize: 18),
                ),
              ),
              style: ElevatedButton.styleFrom(
                backgroundColor: isRecording
                    ? Colors.red.shade700
                    : Colors.blue.shade700,
                foregroundColor: Colors.white,
                shape: RoundedRectangleBorder(
                  borderRadius: BorderRadius.circular(10),
                ),
                padding: const EdgeInsets.all(20),
                elevation: 10,
              ),
              onPressed: isRecording ? stopRecording : startRecording,
            ),
          ],
        ),
      ),
    );
  }
}
