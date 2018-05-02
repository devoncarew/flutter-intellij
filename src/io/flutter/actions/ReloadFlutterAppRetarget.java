/*
 * Copyright 2016 The Chromium Authors. All rights reserved.
 * Use of this source code is governed by a BSD-style license that can be
 * found in the LICENSE file.
 */
package io.flutter.actions;

import com.intellij.openapi.actionSystem.ActionPlaces;
import com.intellij.openapi.actionSystem.AnActionEvent;
import io.flutter.run.FlutterAppManager;
import io.flutter.run.FlutterReloadManager;
import io.flutter.settings.FlutterSettings;

/**
 * A keystroke or tool-bar invoked {@link ReloadFlutterApp} action.
 */
public class ReloadFlutterAppRetarget extends FlutterRetargetAppAction {
  public ReloadFlutterAppRetarget() {
    super(ReloadFlutterApp.ID,
          ReloadFlutterApp.TEXT,
          ReloadFlutterApp.DESCRIPTION,
          ActionPlaces.MAIN_TOOLBAR,
          ActionPlaces.NAVIGATION_BAR_TOOLBAR,
          ActionPlaces.MAIN_MENU);
  }

  public void actionPerformed(AnActionEvent e) {
    if (FlutterSettings.getInstance().isReloadAllDevices() &&
        e.getProject() != null &&
        FlutterAppManager.getInstance(e.getProject()).getApps().size() > 1) {
      FlutterReloadManager.getInstance(e.getProject()).reloadAllDevices();
    }
    else {
      super.actionPerformed(e);
    }
  }
}
