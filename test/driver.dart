import 'dart:io';

import 'package:integration_test/integration_test_driver.dart';

Future<void> main() async {
  final Map<String, String>? envVars = Platform.environment;

  String adbPath;
  String flutterAndroidSDK = const String.fromEnvironment("ANDROID_SDK_ROOT");
  print("ANDROID_SDK_ROOT ===> $flutterAndroidSDK");
  print("ENV Var ===> $envVars");

  await integrationDriver();
}
