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
      theme: ThemeData(primarySwatch: Colors.blue),
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

  // Параметри запису
  final List<Map<String, dynamic>> bitRateOptions = [
    {'label': '192 kbps (High)', 'value': 192000},
    {'label': '128 kbps (Medium)', 'value': 128000},
    {'label': '64 kbps (Low)', 'value': 64000},
  ];
  int _selectedBitRate = 128000;

  // Параметри автозбереження
  final List<int> autoSaveOptions = [1, 5, 10, 15, 20, 30, 45, 60]; // Хвилини
  int _selectedAutoSaveInterval = 10; // 10 хвилин за замовчуванням

  @override
  void initState() {
    super.initState();
    requestPermissions();
  }

  Future<void> requestPermissions() async {
    await Permission.microphone.request();
    // Дозвіл на сховище потрібен, але для сучасних Android
    // достатньо доступу до кешу, якщо не зберігаємо у Завантаженнях.
    // Залишаємо для сумісності:
    await Permission.storage.request();
  }

  void _startTimer() {
    _timer = Timer.periodic(const Duration(seconds: 1), (timer) {
      setState(() => secondsElapsed++);
    });
  }

  void _stopTimer() {
    _timer?.cancel();
    _timer = null;
    setState(() => secondsElapsed = 0);
  }

  // Генерація імені файлу на момент початку (для першого сегмента) або автозбереження
  String _generateSegmentFileName() {
    final now = DateTime.now();
    return 'Rec_'
        '${now.year}${now.month.toString().padLeft(2, '0')}'
        '${now.day.toString().padLeft(2, '0')}_'
        '${now.hour.toString().padLeft(2, '0')}'
        '${now.minute.toString().padLeft(2, '0')}'
        '${now.second.toString().padLeft(2, '0')}.m4a';
  }

  // Генерація імені файлу на момент натискання STOP (для фінального сегмента)
  String _generateFinalFileName() {
    final now = DateTime.now();
    return 'End_' // Додаємо префікс End_
        '${now.year}${now.month.toString().padLeft(2, '0')}'
        '${now.day.toString().padLeft(2, '0')}_'
        '${now.hour.toString().padLeft(2, '0')}'
        '${now.minute.toString().padLeft(2, '0')}'
        '${now.second.toString().padLeft(2, '0')}.m4a';
  }

  Future<void> startRecording() async {
    final fileName = _generateSegmentFileName();
    await MicService.startMic(
      fileName: fileName,
      bitRate: _selectedBitRate,
      autoSaveIntervalMinutes: _selectedAutoSaveInterval,
    );
    setState(() => isRecording = true);
    _startTimer();
  }

  Future<void> stopRecording() async {
    // 1. Зупиняємо таймер та скидаємо нативний таймер автозбереження
    _stopTimer();
    await MicService.resetTimer();

    // 2. Генеруємо фінальне ім'я файлу (час закінчення)
    final finalFileName = _generateFinalFileName();

    // 3. Надсилаємо команду сервісу для зупинки та перейменування
    await MicService.stopAndSaveFinalSegment(finalFileName: finalFileName);

    setState(() => isRecording = false);
  }

  @override
  void dispose() {
    _timer?.cancel();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    // --- FORMATING TIME TO HH:MM:SS ---
    String formattedTime = // <-- ЦЕЙ РЯДОК
        '${(secondsElapsed ~/ 3600).toString().padLeft(2, '0')}:'
        '${((secondsElapsed % 3600) ~/ 60).toString().padLeft(2, '0')}:'
        '${(secondsElapsed % 60).toString().padLeft(2, '0')}';

    return Scaffold(
      appBar: AppBar(title: const Text('🎙️ Voice Recorder')),
      body: Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            // --- Відображення Таймера ---
            if (isRecording) ...[
              Text(
                'Recording: $formattedTime',
                style: const TextStyle(
                  fontSize: 28,
                  fontWeight: FontWeight.bold,
                  color: Colors.redAccent,
                ),
              ),
              const SizedBox(height: 30),
            ],

            // --- Меню Вибору Бітрейту ---
            DropdownButton<int>(
              value: _selectedBitRate,
              items: bitRateOptions.map((option) {
                return DropdownMenuItem<int>(
                  value: option['value'],
                  child: Text('Quality: ${option['label']}'),
                );
              }).toList(),
              onChanged: isRecording
                  ? null
                  : (int? newValue) {
                      setState(() {
                        _selectedBitRate = newValue!;
                      });
                    },
            ),

            // --- Меню Автозбереження ---
            DropdownButton<int>(
              value: _selectedAutoSaveInterval,
              items: autoSaveOptions.map((minutes) {
                return DropdownMenuItem<int>(
                  value: minutes,
                  child: Text('AutoSave: $minutes minutes'),
                );
              }).toList(),
              onChanged: isRecording
                  ? null
                  : (int? newValue) {
                      setState(() {
                        _selectedAutoSaveInterval = newValue!;
                      });
                    },
            ),

            const SizedBox(height: 40),

            // --- Кнопка Start/Stop ---
            ElevatedButton.icon(
              icon: Icon(isRecording ? Icons.stop : Icons.mic, size: 32),
              label: Text(
                isRecording ? 'Stop Recording' : 'Start Recording',
                style: const TextStyle(fontSize: 18),
              ),
              style: ElevatedButton.styleFrom(
                backgroundColor: isRecording ? Colors.red : Colors.blue,
                padding: const EdgeInsets.symmetric(
                  horizontal: 30,
                  vertical: 15,
                ),
                shape: RoundedRectangleBorder(
                  borderRadius: BorderRadius.circular(10),
                ),
              ),
              onPressed: isRecording ? stopRecording : startRecording,
            ),
          ],
        ),
      ),
    );
  }
}
