/*
 * Copyright 2018 The Chromium Authors. All rights reserved.
 * Use of this source code is governed by a BSD-style license that can be
 * found in the LICENSE file.
 */
package io.flutter.preview;

import com.intellij.openapi.util.text.StringUtil;
import org.dartlang.analysis.server.protocol.Element;
import org.dartlang.analysis.server.protocol.FlutterOutline;

import java.util.List;

public class ModelUtils {
  private ModelUtils() {
  }

  public static boolean isBuildMethod(Element element) {
    return StringUtil.equals("build", element.getName()) &&
           StringUtil.startsWith(element.getParameters(), "(BuildContext ");
  }

  public static boolean containsBuildMethod(FlutterOutline outline) {
    final Element element = outline.getDartElement();
    if (element == null) {
      return false;
    }

    if (isBuildMethod(element)) {
      return true;
    }

    final List<FlutterOutline> children = outline.getChildren();
    if (children == null) {
      return false;
    }

    for (FlutterOutline child : children) {
      if (containsBuildMethod(child)) {
        return true;
      }
    }

    return false;
  }
}
