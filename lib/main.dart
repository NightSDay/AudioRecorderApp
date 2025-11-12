import 'dart:async';
import 'package:flutter/material.dart';
import 'package:permission_handler/permission_handler.dart';
import 'mic_service.dart'; // Підключаємо наш MethodChannel-сервіс

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

  @override
  void initState() {
    super.initState();
    requestPermissions();
  }

  Future<void> requestPermissions() async {
    await Permission.microphone.request();
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

  Future<void> startRecording() async {
    await MicService.startMic();
    setState(() => isRecording = true);
    _startTimer();
  }

  Future<void> stopRecording() async {
    await MicService.stopMic();
    setState(() => isRecording = false);
    _stopTimer();
  }

  @override
  Widget build(BuildContext context) {
    String formattedTime =
        '${(secondsElapsed ~/ 60).toString().padLeft(2, '0')}:${(secondsElapsed % 60).toString().padLeft(2, '0')}';

    return Scaffold(
      appBar: AppBar(title: const Text('🎙️ Voice Recorder')),
      body: Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            ElevatedButton.icon(
              icon: Icon(isRecording ? Icons.stop : Icons.mic),
              label: Text(isRecording ? 'Stop Recording' : 'Start Recording'),
              style: ElevatedButton.styleFrom(
                backgroundColor: isRecording ? Colors.red : Colors.blue,
              ),
              onPressed: isRecording ? stopRecording : startRecording,
            ),
            const SizedBox(height: 20),
            if (isRecording) ...[
              Text(
                'Recording: $formattedTime',
                style: const TextStyle(
                  fontSize: 20,
                  fontWeight: FontWeight.bold,
                ),
              ),
            ],
          ],
        ),
      ),
    );
  }
}
