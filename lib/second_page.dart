import 'package:flutter/material.dart';

class SecondPage extends StatefulWidget {
  const SecondPage({Key? key}) : super(key: key);

  @override
  State<SecondPage> createState() => _SecondPageState();
}

class _SecondPageState extends State<SecondPage> {
  final TextEditingController _controller = TextEditingController();

  void navigateBack() => Navigator.pop(context, [_controller.text]);

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: Center(
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.center,
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            const Text("SecondPage!"),
            TextField(
              controller: _controller,
              key: const ValueKey('login.textField'),
            ),
            ElevatedButton.icon(
                key: const ValueKey('login.backButton'), onPressed: navigateBack, icon: const Icon(Icons.arrow_back), label: const Text("Navigate back"))
          ],
        ),
      ),
    );
  }
}
