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
      theme: ThemeData(primarySwatch: Colors.blue, useMaterial3: true),
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

  // 1. Опції бітрейту: [Display Name, Value in bps]
  // 64000 bps = 64 kbps (низька якість, малий файл)
  // 128000 bps = 128 kbps (стандартна/хороша якість)
  // 192000 bps = 192 kbps (висока якість, студійний запис)
  final List<Map<String, int>> bitRateOptions = const [
    {'64 kbps (Low)': 64000},
    {'128 kbps (Good)': 128000},
    {'192 kbps (High)': 192000},
  ];

  // 2. Змінна стану для вибраного бітрейту (за замовчуванням 128 kbps)
  late int _selectedBitRate;

  @override
  void initState() {
    super.initState();
    // Ініціалізуємо вибраний бітрейт значенням за замовчуванням
    _selectedBitRate = bitRateOptions[1].values.first;
    requestPermissions();
  }

  @override
  void dispose() {
    _timer?.cancel();
    super.dispose();
  }

  Future<void> requestPermissions() async {
    await Permission.microphone.request();
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

  String _generateFileName() {
    final now = DateTime.now();
    return 'Rec_${now.year}${now.month.toString().padLeft(2, '0')}'
        '${now.day.toString().padLeft(2, '0')}_'
        '${now.hour.toString().padLeft(2, '0')}${now.minute.toString().padLeft(2, '0')}'
        '${now.second.toString().padLeft(2, '0')}.m4a';
  }

  Future<void> startRecording() async {
    final fileName = _generateFileName();

    // Передаємо вибраний бітрейт
    await MicService.startMic(fileName: fileName, bitRate: _selectedBitRate);

    setState(() => isRecording = true);
    _startTimer();
  }

  Future<void> stopRecording() async {
    await MicService.stopMic();
    setState(() => isRecording = false);
    _stopTimer();
  }

  // Функція для перетворення bps у рядок для відображення
  String _getBitRateDisplayName(int bitRate) {
    for (var option in bitRateOptions) {
      if (option.values.first == bitRate) {
        return option.keys.first;
      }
    }
    return 'Unknown';
  }

  @override
  Widget build(BuildContext context) {
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
            // 3. Меню вибору бітрейту
            if (!isRecording) ...[
              const Text(
                'Виберіть якість запису:',
                style: TextStyle(fontSize: 16),
              ),
              const SizedBox(height: 8),
              Container(
                padding: const EdgeInsets.symmetric(
                  horizontal: 12,
                  vertical: 4,
                ),
                decoration: BoxDecoration(
                  borderRadius: BorderRadius.circular(8),
                  border: Border.all(color: Colors.blue.shade700, width: 2),
                ),
                child: DropdownButtonHideUnderline(
                  child: DropdownButton<int>(
                    value: _selectedBitRate,
                    // Дозволяємо змінювати якість тільки коли запис не йде
                    onChanged: isRecording
                        ? null
                        : (int? newValue) {
                            if (newValue != null) {
                              setState(() {
                                _selectedBitRate = newValue;
                              });
                            }
                          },
                    items: bitRateOptions.map<DropdownMenuItem<int>>((option) {
                      return DropdownMenuItem<int>(
                        value: option.values.first,
                        child: Text(
                          option.keys.first,
                          style: const TextStyle(fontSize: 16),
                        ),
                      );
                    }).toList(),
                  ),
                ),
              ),
              const SizedBox(height: 20),
            ],

            Text(
              isRecording
                  ? 'ЗАПИС: ${_getBitRateDisplayName(_selectedBitRate)}'
                  : 'Готовий до запису',
              style: TextStyle(
                fontSize: 24,
                fontWeight: FontWeight.w600,
                color: isRecording ? Colors.redAccent : Colors.grey.shade700,
              ),
            ),
            const SizedBox(height: 16),

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
