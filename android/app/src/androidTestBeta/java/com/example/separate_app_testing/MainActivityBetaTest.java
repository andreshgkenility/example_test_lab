// Change this line to YOUR package name:
package com.example.separate_app_testing;

import androidx.test.rule.ActivityTestRule;
import dev.flutter.plugins.integration_test.FlutterTestRunner;
import org.junit.Rule;
import org.junit.runner.RunWith;
// Import MainActivity from YOUR package
import com.example.separate_app_testing.MainActivity;

@RunWith(FlutterTestRunner.class)
public class MainActivityBetaTest {
  @Rule
  public ActivityTestRule<MainActivity> rule = new ActivityTestRule<>(MainActivity.class, true, false);
}