/*
 * Copyright 2017 The Chromium Authors. All rights reserved.
 * Use of this source code is governed by a BSD-style license that can be
 * found in the LICENSE file.
 */
package io.flutter.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import io.flutter.FlutterInitializer;
import io.flutter.gallery.GalleryView;
import org.jetbrains.annotations.NotNull;

public class FlutterWidgetGallery extends DumbAwareAction {
  @Override
  public void actionPerformed(@NotNull final AnActionEvent e) {
    FlutterInitializer.sendAnalyticsAction(this);

    final Project project = e.getProject();
    if (project != null) {
      final ToolWindow toolWindow = ToolWindowManager.getInstance(project).getToolWindow(GalleryView.TOOL_WINDOW_ID);
      if (!toolWindow.isActive()) {
        toolWindow.activate(null);
      }
    }
  }
}
