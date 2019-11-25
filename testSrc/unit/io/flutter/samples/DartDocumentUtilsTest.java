/*
 * Copyright 2019 The Chromium Authors. All rights reserved.
 * Use of this source code is governed by a BSD-style license that can be
 * found in the LICENSE file.
 */
package io.flutter.samples;

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.impl.DocumentImpl;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiFile;
import com.jetbrains.lang.dart.psi.DartClass;
import io.flutter.FlutterCodeInsightFixtureTestCase;

import java.util.List;

import static org.junit.Assert.assertArrayEquals;

// TODO: use CodeInsightFixtureTestCase ?

public class DartDocumentUtilsTest extends FlutterCodeInsightFixtureTestCase {
  //@Rule public final ProjectFixture projectFixture = Testing.makeCodeInsightModule();

  //@Test
  public void testFindsDocComments() {
    final Document doc = new DocumentImpl(
      "/// {@tool dartpad ...}\n" +
      "class Foo {\n" +
      "}\n"
    );

    final Project project = getProject();

    // TODO: which project to use?
    final PsiFile psiFile = PsiDocumentManager.getInstance(project).getPsiFile(doc);
    assertNotNull(psiFile);

    final DartClass[] clazz = new DartClass[1];

    psiFile.acceptChildren(new PsiElementVisitor() {
      @Override
      public void visitElement(PsiElement element) {
        if (element instanceof DartClass) {
          clazz[0] = (DartClass)element;
        }
      }
    });

    assertNotNull(clazz[0]);

    final List<String> lines = DartDocumentUtils.getDartdocFor(doc, clazz[0]);
    assertArrayEquals(new String[]{"/// {@tool dartpad ...}"}, lines.toArray());
  }

  //@Test
  public void testFindsDocCommentsBlankLine() {
    // TODO:


  }

  //@Test
  public void testFindsDocCommentsAnnotation() {
    // TODO:

  }

  //@Test
  public void testFindsDocCommentsIndentation() {
    // TODO:

  }
}
