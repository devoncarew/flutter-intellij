/*
 * Copyright 2016 The Chromium Authors. All rights reserved.
 * Use of this source code is governed by a BSD-style license that can be
 * found in the LICENSE file.
 */
package io.flutter.run;

import com.google.gson.JsonObject;
import com.intellij.execution.ExecutionResult;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.ui.ConsoleViewContentType;
import com.intellij.execution.ui.RunnerLayoutUi;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.Computable;
import com.intellij.ui.GuiUtils;
import com.intellij.ui.content.Content;
import com.intellij.xdebugger.XDebugSession;
import com.jetbrains.lang.dart.ide.runner.server.vmService.DartVmServiceDebugProcessZ;
import com.jetbrains.lang.dart.ide.runner.server.vmService.VmServiceConsumers;
import com.jetbrains.lang.dart.util.DartUrlResolver;
import io.flutter.actions.ReloadFlutterApp;
import io.flutter.actions.RestartFlutterApp;
import io.flutter.run.daemon.FlutterApp;
import io.flutter.run.daemon.RunMode;
import io.flutter.utils.VmServiceListenerAdapter;
import io.flutter.view.FlutterViewMessages;
import io.flutter.view.OpenFlutterViewAction;
import org.dartlang.vm.service.VmService;
import org.dartlang.vm.service.VmServiceListener;
import org.dartlang.vm.service.element.Event;
import org.dartlang.vm.service.element.InstanceRef;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * A debug process that handles hot reloads for Flutter.
 * <p>
 * It's used for both the 'Run' and 'Debug' modes. (We apparently need a debug process even
 * when not debugging in order to support hot reload.)
 */
public class FlutterDebugProcess extends DartVmServiceDebugProcessZ {
  private static final Logger LOG = Logger.getInstance(FlutterDebugProcess.class);

  private final @NotNull FlutterApp app;

  public FlutterDebugProcess(@NotNull FlutterApp app,
                             @NotNull ExecutionEnvironment executionEnvironment,
                             @NotNull XDebugSession session,
                             @NotNull ExecutionResult executionResult,
                             @NotNull DartUrlResolver dartUrlResolver,
                             @NotNull PositionMapper mapper) {
    super(executionEnvironment, session, executionResult, dartUrlResolver, app.getConnector(), mapper);
    this.app = app;
  }

  @Override
  protected void onVmConnected(@NotNull VmService vmService) {
    app.setFlutterDebugProcess(this);
    // TODO: We should add this listener earlier in the process, so that we don't loose messages.

    vmService.addVmServiceListener(new VmServiceListenerAdapter() {
      @Override
      public void received(String streamId, Event event) {
        // TODO: I would instead like to listen for "Logging"
        if ("_Logging".equals(streamId)) {
          final JsonObject logRecord = event.getJson().getAsJsonObject("logRecord");

          if (logRecord != null) {
            // TODO: be defensive here; these are all optional
            final int sequenceNumber = logRecord.getAsJsonPrimitive("sequenceNumber").getAsInt();
            //final long time = logRecord.getAsJsonPrimitive("time").getAsLong();
            final int level = logRecord.getAsJsonPrimitive("level").getAsInt();
            final InstanceRef loggerName = new InstanceRef(logRecord.getAsJsonObject("loggerName"));
            final InstanceRef message = new InstanceRef(logRecord.getAsJsonObject("message"));
            final boolean messageTruncated = message.getValueAsStringIsTruncated();

            // TODO: pipe to a logger manager; have that pipe to the debug console
            // SYSTEM_OUTPUT, USER_INPUT
            getSession().getConsoleView().print(
              "[" + loggerName.getValueAsString() + "] " + sequenceNumber + " ", ConsoleViewContentType.SYSTEM_OUTPUT);
            getSession().getConsoleView().print(
              message.getValueAsString() + (messageTruncated ? "..." : "") + "\n", ConsoleViewContentType.NORMAL_OUTPUT);

            // sequenceNumber, time, level, loggerName, message, error, stackTrace
            //System.out.println(logRecord);
            // nulls could be null, or could be
            // "{"type":"@Instance","class":{"type":"@Class","fixedId":true,"id":"classes/122","name":"Null"},
            //   "kind":"Null","fixedId":true,"id":"objects/null","valueAsString":"null"}"
          }
        }
      }
    });

    vmService.streamListen("_Logging", VmServiceConsumers.EMPTY_SUCCESS_CONSUMER);

    FlutterViewMessages.sendDebugActive(getSession().getProject(), app, vmService);
  }

  @Override
  public void registerAdditionalActions(@NotNull final DefaultActionGroup leftToolbar,
                                        @NotNull final DefaultActionGroup topToolbar,
                                        @NotNull final DefaultActionGroup settings) {

    if (app.getMode() != RunMode.DEBUG) {
      // Remove the debug-specific actions that aren't needed when we're not debugging.

      // Remove all but specified actions.
      final AnAction[] leftActions = leftToolbar.getChildActionsOrStubs();
      // Not all on the classpath so we resort to Strings.
      final List<String> actionClassNames = Arrays
        .asList("com.intellij.execution.actions.StopAction", "com.intellij.ui.content.tabs.PinToolwindowTabAction",
                "com.intellij.execution.ui.actions.CloseAction", "com.intellij.ide.actions.ContextHelpAction");
      for (AnAction a : leftActions) {
        if (!actionClassNames.contains(a.getClass().getName())) {
          leftToolbar.remove(a);
        }
      }

      // Remove all top actions.
      final AnAction[] topActions = topToolbar.getChildActionsOrStubs();
      for (AnAction action : topActions) {
        topToolbar.remove(action);
      }

      // Remove all settings actions.
      final AnAction[] settingsActions = settings.getChildActionsOrStubs();
      for (AnAction a : settingsActions) {
        settings.remove(a);
      }
    }

    // Add actions common to the run and debug windows.
    final Computable<Boolean> isSessionActive = () -> app.isStarted() && getVmConnected() && !getSession().isStopped();
    final Computable<Boolean> canReload = () -> app.getLaunchMode().supportsReload() && isSessionActive.compute() && !app.isReloading();
    final Computable<Boolean> observatoryAvailable = () -> isSessionActive.compute() && app.getConnector().getBrowserUrl() != null;

    if (app.getMode() == RunMode.DEBUG) {
      topToolbar.addSeparator();
      topToolbar.addAction(new FlutterPopFrameAction());
    }

    topToolbar.addSeparator();
    topToolbar.addAction(new ReloadFlutterApp(app, canReload));
    topToolbar.addAction(new RestartFlutterApp(app, canReload));
    topToolbar.addSeparator();
    topToolbar.addAction(new OpenObservatoryAction(app.getConnector(), observatoryAvailable));
    topToolbar.add(new OpenFlutterViewAction(isSessionActive));

    // Don't call super since we have our own observatory action.
  }

  @Override
  public void sessionInitialized() {
    if (app.getMode() != RunMode.DEBUG) {
      suppressDebugViews(getSession().getUI());
    }
  }

  /**
   * Turn off debug-only views (variables and frames).
   */
  private static void suppressDebugViews(@Nullable RunnerLayoutUi ui) {
    if (ui == null) {
      return;
    }

    for (Content c : ui.getContents()) {
      if (!Objects.equals(c.getTabName(), "Console")) {
        try {
          GuiUtils.runOrInvokeAndWait(() -> ui.removeContent(c, false /* dispose? */));
        }
        catch (InvocationTargetException | InterruptedException e) {
          LOG.warn(e);
        }
      }
    }
  }
}
