/*
 * Copyright 2016 The Chromium Authors. All rights reserved.
 * Use of this source code is governed by a BSD-style license that can be
 * found in the LICENSE file.
 */
package io.flutter.analytics;

import io.flutter.testing.ProjectFixture;
import io.flutter.testing.Testing;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class ProjectAnalyticsTest {
  private ProjectAnalytics analytics;
  private MockAnalyticsSettings settings;
  private MockAnalyticsTransport transport;

  @Rule
  public final ProjectFixture fixture = Testing.makeEmptyModule();

  @Before
  public void setUp() {
    settings = new MockAnalyticsSettings(true);
    transport = new MockAnalyticsTransport();

    analytics = new ProjectAnalytics(fixture.getProject(), settings);
    analytics.setTransport(transport);
  }

  @Test
  public void testSendScreenView() {
    analytics.sendScreenView("testAnalyticsPage");
    assertEquals(1, transport.sentValues.size());
  }

  @Test
  public void testSendEvent() {
    analytics.sendEvent("flutter", "doctor");
    assertEquals(1, transport.sentValues.size());
  }

  @Test
  public void testSendTiming() {
    analytics.sendTiming("perf", "reloadTime", 100);
    assertEquals(1, transport.sentValues.size());
  }

  @Test
  public void testSendException() {
    final Throwable throwable = new UnsupportedOperationException("test operation");
    final StringWriter stringWriter = new StringWriter();
    final PrintWriter printWriter = new PrintWriter(stringWriter);
    throwable.printStackTrace(printWriter);

    analytics.sendException(stringWriter.toString().trim(), true);
    assertEquals(1, transport.sentValues.size());
  }

  @Test
  public void testOptOutDoesntSend() {
    settings = new MockAnalyticsSettings(false);
    transport = new MockAnalyticsTransport();

    analytics = new ProjectAnalytics(fixture.getProject(), settings);
    analytics.setTransport(transport);

    analytics.sendScreenView("testAnalyticsPage");
    assertEquals(0, transport.sentValues.size());
  }

  private static class MockAnalyticsTransport implements ProjectAnalytics.Transport {
    final public List<Map<String, String>> sentValues = new ArrayList<>();

    @Override
    public void send(String url, Map<String, String> values) {
      sentValues.add(values);
    }
  }
}

class MockAnalyticsSettings extends AnalyticsSettings {
  private final boolean canSend;

  MockAnalyticsSettings(boolean canSend) {
    this.canSend = canSend;
  }

  public boolean getCanReportAnalytics() {
    return canSend;
  }

  public void setCanReportAnalytics(boolean reportAnalytics) {
    // ignore
  }

  public static void setAnalyticsDisclosureShown(boolean value) {
    // ignore
  }
}
