import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:integration_test/integration_test.dart';

import 'package:separate_app_testing/main_beta.dart' as app;

void main() {
  group('end-to-end test', () {
    final binding = IntegrationTestWidgetsFlutterBinding.ensureInitialized() as IntegrationTestWidgetsFlutterBinding;

    binding.framePolicy = LiveTestWidgetsFlutterBindingFramePolicy.fullyLive;
    binding.testTextInput.register();
    testWidgets('tap on the floating action button, verify counter', (WidgetTester tester) async {
      app.main(); // beta flavor
      await tester.pumpAndSettle(const Duration(seconds: 1));
      // Verify the counter starts at 0.
      expect(find.text('0'), findsOneWidget);

      final Finder enterButton = find.byKey(const ValueKey('login.enterButton'));
      // await tester.tap(enterButton);

      await Future.delayed(const Duration(seconds: 2));
      await tester.pumpAndSettle();
      await Future.delayed(const Duration(seconds: 1));

      final Finder textField = find.byKey(const ValueKey('login.textField'));

      await tester.pump(const Duration(milliseconds: 400));
      await tester.enterText(textField, "Text 1234");
      print("Should type!");
      await tester.pumpAndSettle(const Duration(seconds: 1));
      final Finder backButton = find.byKey(const ValueKey('login.backButton'));
      await Future.delayed(const Duration(seconds: 2));
      await tester.tap(backButton);
      await tester.pumpAndSettle();
      await Future.delayed(const Duration(seconds: 5));
      expect(find.text('Info is: \nText 1234'), findsOneWidget);
    });
  });
}
