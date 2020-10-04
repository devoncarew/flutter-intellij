/*
 * Copyright 2020 The Chromium Authors. All rights reserved.
 * Use of this source code is governed by a BSD-style license that can be
 * found in the LICENSE file.
 */
package io.flutter.analytics;

import com.google.common.annotations.VisibleForTesting;
import com.intellij.ide.util.PropertiesComponent;

/**
 * todo:
 */
public class AnalyticsSettings {
  private static final String analyticsOptOutKey = "io.flutter.analytics.optOut";
  private static final String analyticsDisclosureShown = "io.flutter.analytics.toastShown";

  private static AnalyticsSettings instance = new AnalyticsSettings();

  public static AnalyticsSettings getInstance() {
    return instance;
  }

  private boolean canSend;

  @VisibleForTesting
  public AnalyticsSettings() {
    final boolean hasDisclosed = getAnalyticsDisclosureShown();
    final boolean hasOptedOut = getProperties().getBoolean(analyticsOptOutKey, false);

    //noinspection SimplifiableConditionalExpression
    canSend = hasOptedOut ? false : hasDisclosed;
  }

  public boolean getCanReportAnalytics() {
    return canSend;
  }

  public boolean getHasNotOptedOut() {
    return !getProperties().getBoolean(analyticsOptOutKey, false);
  }

  public void setCanReportAnalytics(boolean reportAnalytics) {
    getProperties().setValue(analyticsOptOutKey, !reportAnalytics);

    canSend = reportAnalytics;
  }

  public static boolean getAnalyticsDisclosureShown() {
    return getProperties().getBoolean(analyticsDisclosureShown, false);
  }

  public static void setAnalyticsDisclosureShown(boolean value) {
    getProperties().setValue(analyticsDisclosureShown, value);
  }

  private static PropertiesComponent getProperties() {
    return PropertiesComponent.getInstance();
  }
}
