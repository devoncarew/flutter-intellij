/*
 * Copyright 2016 The Chromium Authors. All rights reserved.
 * Use of this source code is governed by a BSD-style license that can be
 * found in the LICENSE file.
 */
package io.flutter;

import com.intellij.ide.BrowserUtil;
import com.intellij.notification.*;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationInfo;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.startup.StartupActivity;
import io.flutter.analytics.AnalyticsSettings;
import io.flutter.analytics.ProjectAnalytics;
import io.flutter.android.IntelliJAndroidSdk;
import io.flutter.editor.FlutterSaveActionsManager;
import io.flutter.logging.FlutterConsoleLogManager;
import io.flutter.perf.FlutterWidgetPerfManager;
import io.flutter.performance.FlutterPerformanceViewFactory;
import io.flutter.pub.PubRoot;
import io.flutter.pub.PubRoots;
import io.flutter.run.FlutterReloadManager;
import io.flutter.run.FlutterRunNotifications;
import io.flutter.run.daemon.DeviceService;
import io.flutter.sdk.FlutterPluginsLibraryManager;
import io.flutter.settings.FlutterSettings;
import io.flutter.survey.FlutterSurveyNotifications;
import io.flutter.utils.FlutterModuleUtils;
import io.flutter.view.FlutterViewFactory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.event.HyperlinkEvent;

/**
 * Runs actions after the project has started up and the index is up to date.
 *
 * @see ProjectOpenActivity for actions that run earlier.
 * @see io.flutter.project.FlutterProjectOpenProcessor for additional actions that
 * may run when a project is being imported.
 */
public class FlutterInitializer implements StartupActivity {
  @Override
  public void runActivity(@NotNull Project project) {
    // Convert all modules of deprecated type FlutterModuleType.
    if (FlutterModuleUtils.convertFromDeprecatedModuleType(project)) {
      // If any modules were converted over, create a notification
      FlutterMessages.showInfo(
        FlutterBundle.message("flutter.initializer.module.converted.title"),
        "Converted from '" +
        FlutterModuleUtils.DEPRECATED_FLUTTER_MODULE_TYPE_ID +
        "' to '" +
        FlutterModuleUtils.getModuleTypeIDForFlutter() +
        "'.");
    }

    // Disable the 'Migrate Project to Gradle' notification.
    FlutterUtils.disableGradleProjectMigrationNotification(project);

    // Start watching for devices.
    DeviceService.getInstance(project);

    // Start watching for Flutter debug active events.
    FlutterViewFactory.init(project);

    FlutterPerformanceViewFactory.init(project);

    // If the project declares a Flutter dependency, do some extra initialization.
    boolean hasFlutterModule = false;

    for (Module module : ModuleManager.getInstance(project).getModules()) {
      final boolean declaresFlutter = FlutterModuleUtils.declaresFlutter(module);

      hasFlutterModule = hasFlutterModule || declaresFlutter;

      if (!declaresFlutter) {
        continue;
      }

      // Ensure SDKs are configured; needed for clean module import.
      FlutterModuleUtils.enableDartSDK(module);

      for (PubRoot root : PubRoots.forModule(module)) {
        // Set Android SDK.
        if (root.hasAndroidModule(project)) {
          ensureAndroidSdk(project);
        }

        // Setup a default run configuration for 'main.dart' (if it's not there already and the file exists).
        FlutterModuleUtils.autoCreateRunConfig(project, root);

        // If there are no open editors, show main.
        if (FileEditorManager.getInstance(project).getOpenFiles().length == 0) {
          FlutterModuleUtils.autoShowMain(project, root);
        }
      }
    }

    if (hasFlutterModule) {
      // Ensure a run config is selected and ready to go.
      FlutterModuleUtils.ensureRunConfigSelected(project);
    }

    if (hasFlutterModule) {
      // Check to see if we're on a supported version of Android Studio; warn otherwise.
      performAndroidStudioCanaryCheck();
    }

    FlutterRunNotifications.init(project);

    // Start watching for survey triggers.
    FlutterSurveyNotifications.init(project);

    // Start the widget perf manager.
    FlutterWidgetPerfManager.init(project);

    // Watch save actions for reload on save.
    FlutterReloadManager.init(project);

    // Watch save actions for format on save.
    FlutterSaveActionsManager.init(project);

    // Start watching for project structure and .packages file changes.
    final FlutterPluginsLibraryManager libraryManager = new FlutterPluginsLibraryManager(project);
    libraryManager.startWatching();

    // Initialize the analytics notification group.
    NotificationsConfiguration.getNotificationsConfiguration().register(
      ProjectAnalytics.GROUP_DISPLAY_ID,
      NotificationDisplayType.STICKY_BALLOON,
      false);

    // Set our preferred settings for the run console.
    FlutterConsoleLogManager.initConsolePreferences();

    // Initialize analytics.
    if (FlutterModuleUtils.declaresFlutter(project)) {
      if (!AnalyticsSettings.getAnalyticsDisclosureShown()) {
        showAnalyticsDisclosure(project);
      }

      final ProjectAnalytics analytics = ProjectAnalytics.getInstance(project);

      // Send initial loading hit.
      analytics.sendScreenView("main");

      // Listen for tool window events.
      analytics.updateProjectListeners();

      FlutterSettings.getInstance().sendSettingsToAnalytics(analytics);
    }
  }

  /**
   * Automatically set Android SDK based on ANDROID_HOME.
   */
  private void ensureAndroidSdk(@NotNull Project project) {
    if (ProjectRootManager.getInstance(project).getProjectSdk() != null) {
      return; // Don't override user's settings.
    }

    final IntelliJAndroidSdk wanted = IntelliJAndroidSdk.fromEnvironment();
    if (wanted == null) {
      return; // ANDROID_HOME not set or Android SDK not created in IDEA; not clear what to do.
    }

    ApplicationManager.getApplication().runWriteAction(() -> wanted.setCurrent(project));
  }

  @NotNull
  public static ProjectAnalytics getAnalytics(@NotNull Project project) {
    return ProjectAnalytics.getInstance(project);
  }

  // todo:
  public static void sendAnalyticsAction(@Nullable Project project, @NotNull AnAction action) {
    if (project != null) {
      ProjectAnalytics.getInstance(project).sendEvent("intellij", action.getClass().getSimpleName());
    }
  }

  // todo:
  public static void sendAnalyticsAction(@NotNull Project project, @NotNull String name) {
    ProjectAnalytics.getInstance(project).sendEvent("intellij", name);
  }

  private static void performAndroidStudioCanaryCheck() {
    if (!FlutterUtils.isAndroidStudio()) {
      return;
    }

    final ApplicationInfo info = ApplicationInfo.getInstance();
    if (info.getFullVersion().contains("Canary") && !info.getBuild().isSnapshot()) {
      FlutterMessages.showWarning(
        "Unsupported Android Studio version",
        "Canary versions of Android Studio are not supported by the Flutter plugin.");
    }
  }

  private void showAnalyticsDisclosure(@NotNull Project project) {
    final Notification notification = new Notification(
      ProjectAnalytics.GROUP_DISPLAY_ID,
      "Welcome to Flutter!",
      FlutterBundle.message("flutter.analytics.notification.content"),
      NotificationType.INFORMATION,
      (notification1, event) -> {
        if (event.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
          if ("url".equals(event.getDescription())) {
            BrowserUtil.browse("https://www.google.com/policies/privacy/");
          }
        }
      });
    //noinspection DialogTitleCapitalization
    notification.addAction(new AnAction("Sounds good!") {
      @Override
      public void actionPerformed(@NotNull AnActionEvent event) {
        notification.expire();

        AnalyticsSettings.getInstance().setCanReportAnalytics(true);
        ProjectAnalytics.getInstance(project).updateProjectListeners();
      }
    });
    //noinspection DialogTitleCapitalization
    notification.addAction(new AnAction("No thanks") {
      @Override
      public void actionPerformed(@NotNull AnActionEvent event) {
        notification.expire();

        AnalyticsSettings.getInstance().setCanReportAnalytics(false);
        ProjectAnalytics.getInstance(project).updateProjectListeners();
      }
    });

    AnalyticsSettings.setAnalyticsDisclosureShown(true);
    Notifications.Bus.notify(notification);
  }
}
