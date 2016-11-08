/*
 * Copyright 2016 The Chromium Authors. All rights reserved.
 * Use of this source code is governed by a BSD-style license that can be
 * found in the LICENSE file.
 */
package io.flutter.run;

import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.ConfigurationTypeBase;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.openapi.extensions.Extensions;
import com.intellij.openapi.project.Project;
import com.intellij.psi.search.FileTypeIndex;
import com.intellij.psi.search.GlobalSearchScope;
import com.jetbrains.lang.dart.DartFileType;
import icons.FlutterIcons;
import io.flutter.FlutterBundle;
import org.jetbrains.annotations.NotNull;

public class FlutterBazelConfigurationType extends ConfigurationTypeBase {
  public FlutterBazelConfigurationType() {
    super("FlutterBazelConfigurationType", FlutterBundle.message("flutter.bazel.configuration.name"),
          FlutterBundle.message("flutter.bazel.configuration.description"), FlutterIcons.Flutter);
    addFactory(new FlutterBazelConfigurationType.BazelConfigurationFactory(this));
  }

  public static FlutterBazelConfigurationType getInstance() {
    return Extensions.findExtension(CONFIGURATION_TYPE_EP, FlutterBazelConfigurationType.class);
  }

  public static class BazelConfigurationFactory extends ConfigurationFactory {
    public BazelConfigurationFactory(FlutterBazelConfigurationType type) {
      super(type);
    }

    @Override
    @NotNull
    public RunConfiguration createTemplateConfiguration(@NotNull Project project) {
      return new FlutterRunConfiguration(project, this, "Flutter Bazel Run Target");
    }

    @Override
    @NotNull
    public RunConfiguration createConfiguration(String name, RunConfiguration template) {
      return super.createConfiguration(template.getProject().getName(), template);
    }

    @Override
    public boolean isApplicable(@NotNull Project project) {
      return FileTypeIndex.containsFileOfType(DartFileType.INSTANCE, GlobalSearchScope.projectScope(project));
    }
  }
}
