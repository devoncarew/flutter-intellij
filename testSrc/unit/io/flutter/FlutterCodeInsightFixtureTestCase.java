/*
 * Copyright 2016 The Chromium Authors. All rights reserved.
 * Use of this source code is governed by a BSD-style license that can be
 * found in the LICENSE file.
 */
package io.flutter;

import com.intellij.ide.startup.StartupManagerEx;
import com.intellij.ide.startup.impl.StartupManagerImpl;
import com.intellij.openapi.application.ex.ApplicationManagerEx;
import com.intellij.testFramework.fixtures.LightPlatformCodeInsightFixtureTestCase;
import com.jetbrains.lang.dart.util.DartTestUtils;
import io.flutter.sdk.FlutterSdk;
import io.flutter.testing.FlutterTestUtils;

abstract public class FlutterCodeInsightFixtureTestCase extends LightPlatformCodeInsightFixtureTestCase {
  protected void setUp() throws Exception {
    boolean unitTestMode = ApplicationManagerEx.getApplication().isUnitTestMode();

    // checkCleared
    //StartupManagerEx.getInstanceEx(getProject()).checkCleared();
    //((StartupManagerImpl)StartupManagerEx.getInstanceEx(getProject())).checkCleared();

    super.setUp();

    FlutterTestUtils.configureFlutterSdk(getModule(), getTestRootDisposable(), true);

    final FlutterSdk sdk = FlutterSdk.getFlutterSdk(getProject());
    assert (sdk != null);

    final String path = sdk.getHomePath();
    final String dartSdkPath = path + "/bin/cache/dart-sdk";

    System.setProperty("dart.sdk", dartSdkPath);
    DartTestUtils.configureDartSdk(getModule(), getTestRootDisposable(), true);
  }

  @Override
  protected String getTestDataPath() {
    return FlutterTestUtils.BASE_TEST_DATA_PATH + "/" + getBasePath();
  }
}
