/*
 * Copyright 2017 The Chromium Authors. All rights reserved.
 * Use of this source code is governed by a BSD-style license that can be
 * found in the LICENSE file.
 */
package io.flutter.gallery;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.intellij.icons.AllIcons;
import com.intellij.ide.ui.UISettings;
import com.intellij.notification.impl.ui.NotificationsUtil;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.SimpleToolWindowPanel;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.ui.*;
import com.intellij.ui.components.JBList;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.intellij.ui.content.ContentManager;
import com.intellij.util.ui.HtmlPanel;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.event.ListDataListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@com.intellij.openapi.components.State(
  name = "GalleryView",
  storages = {@Storage("$WORKSPACE_FILE$")}
)
public class GalleryView implements PersistentStateComponent<GalleryView.State> {
  public static final String TOOL_WINDOW_ID = "Widget Gallery";

  @NotNull private GalleryView.State state = new GalleryView.State();
  @SuppressWarnings("FieldCanBeLocal") private final Project myProject;

  public GalleryView(@NotNull Project project) {
    myProject = project;
  }

  public void initToolWindow(ToolWindow toolWindow) {
    final ContentFactory contentFactory = ContentFactory.SERVICE.getInstance();

    final Content toolContent = contentFactory.createContent(null, null, false);
    final SimpleToolWindowPanel toolWindowPanel = new SimpleToolWindowPanel(true, true);
    toolContent.setComponent(toolWindowPanel);
    toolContent.setCloseable(false);

    final DefaultActionGroup toolbarGroup = new DefaultActionGroup();
    toolbarGroup.add(new AnAction("Back", "Back", AllIcons.Actions.Back) {
      @Override
      public void actionPerformed(AnActionEvent event) {
        // TODO:
      }
    });
    toolbarGroup.add(new AnAction("Forward", "Forward", AllIcons.Actions.Forward) {
      @Override
      public void actionPerformed(AnActionEvent event) {
        // TODO:
      }
    });

    // toolbar
    final JPanel toolbarPanel = new JPanel(new BorderLayout());
    toolWindowPanel.setToolbar(toolbarPanel);
    toolbarPanel.add(
      ActionManager.getInstance().createActionToolbar("GalleryViewToolbar", toolbarGroup, true).getComponent(),
      BorderLayout.WEST);
    final SearchTextFieldWithStoredHistory searchField = new SearchTextFieldWithStoredHistory("widgetGallerySearch");
    searchField.setPreferredSize(new Dimension(140, searchField.getPreferredSize().height));
    toolbarPanel.add(searchField, BorderLayout.EAST);

    // main panel
    final JPanel mainContent = new JPanel(new BorderLayout());
    toolWindowPanel.setContent(mainContent);
    // TODO: ListModel
    // TODO: use NameFilteringListModel?
    final JBList<Widget> list = new JBList<>(new WidgetListModel(parseWidgets()));
    list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    list.setCellRenderer(new ColoredListCellRenderer<Widget>() {
      @Override
      protected void customizeCellRenderer(@NotNull JList<? extends Widget> list,
                                           Widget widget,
                                           int index,
                                           boolean selected,
                                           boolean hasFocus) {
        //setIcon(FlutterIcons.Flutter);
        setIcon(new TextIcon(widget.getPackage().substring(0, 1).toUpperCase()));
        append(widget.getName());
        if (widget.getDocs() != null) {
          append(" " + widget.getDocsSummary(), SimpleTextAttributes.GRAY_ATTRIBUTES);
        }
      }
    });
    final JBScrollPane scrollPane1 = new JBScrollPane(list);
    scrollPane1.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

    final JBSplitter splitter = new JBSplitter(true, "galleyViewSplitter", 0.6f);
    mainContent.add(splitter, BorderLayout.CENTER);

    // TODO:
    HtmlPanel tempLabel = new HtmlPanel();
    final JBScrollPane scrollPane2 = new JBScrollPane(tempLabel);
    scrollPane2.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

    splitter.setFirstComponent(scrollPane1);
    splitter.setSecondComponent(scrollPane2);

    list.addListSelectionListener(e -> {
      final Widget selection = list.getSelectedValue();
      if (selection == null) {
        tempLabel.setText(null);
      } else {
        final StringBuilder builder = new StringBuilder();
        builder.append(selection.getName() + "<br>\n<br>\n");
        builder.append("package:flutter/" + selection.getPackage() + ".dart<br>\n<br>\n");
        builder.append("<pre>\n" + selection.getDocs() + "\n</pre><br>\n");
        tempLabel.setText(builder.toString());
      }
    });

    final ContentManager contentManager = toolWindow.getContentManager();
    contentManager.addContent(toolContent);
    contentManager.setSelectedContent(toolContent);
  }

  @NotNull
  @Override
  public GalleryView.State getState() {
    return this.state;
  }

  @Override
  public void loadState(GalleryView.State state) {
    this.state = state;
  }

  /**
   * State for the view.
   */
  class State {
  }

  private static List<Widget> parseWidgets() {
    final JsonParser jp = new JsonParser();
    final JsonElement elem = jp.parse(new InputStreamReader(GalleryView.class.getResourceAsStream("/flutter/widgets.json")));
    final JsonObject obj = elem.getAsJsonObject();

    final List<Widget> widgets = new ArrayList<>();

    for (Map.Entry<String, JsonElement> entry : obj.entrySet()) {
      final JsonObject jsonObj = entry.getValue().getAsJsonObject();
      final String dartPackage = jsonObj.getAsJsonPrimitive("package").getAsString();
      final String docs = jsonObj.has("docs") ? jsonObj.getAsJsonPrimitive("docs").getAsString() : null;
      widgets.add(new Widget(entry.getKey(), dartPackage, docs));
    }

    widgets.sort(Widget.comparator());

    return widgets;
  }
}

class WidgetListModel implements ListModel {
  private final List<Widget> myWidgets;

  WidgetListModel(List<Widget> widgets) {
    this.myWidgets = widgets;
  }

  @Override
  public int getSize() {
    return myWidgets.size();
  }

  @Override
  public Object getElementAt(int index) {
    return myWidgets.get(index);
  }

  @Override
  public void addListDataListener(ListDataListener l) {

  }

  @Override
  public void removeListDataListener(ListDataListener l) {

  }
}

class Widget {
  private final String name;
  private final String dartPackage;
  private final String docs;

  static Comparator<Widget> comparator() {
    return Comparator.comparing(Widget::getName);
  }

  private static final Pattern TYPE_REFERENCE_PATTERN = Pattern.compile("\\[(\\w+)]");

  public Widget(String name, String dartPackage, String docs) {
    this.name = name;
    this.dartPackage = dartPackage;
    this.docs = docs;
  }

  public String getName() {
    return name;
  }

  public String getPackage() {
    return dartPackage;
  }

  public String getDocs() {
    return docs;
  }

  public String getDocsSummary() {
    if (docs == null) {
      return null;
    }

    // return the first markdown section
    final int index = docs.indexOf("\n\n");
    String summary = index == -1 ? docs : docs.substring(0, index);

    // TODO: make references to other types plain text
    //TYPE_REFERENCE_PATTERN.matcher(summary).
    return summary;
  }
}

class TextIcon implements Icon {
  @SuppressWarnings("UseJBColor")
  private static final Color FLUTTER_COLOR = new Color(0x29, 0xb6, 0xf6);

  private final String myStr;
  private final Font myFont;

  public TextIcon(@NotNull String str) {
    this.myStr = str;
    // TODO: Use Roboto for the font?
    this.myFont = new Font(NotificationsUtil.getFontName(), Font.BOLD, JBUI.scale(12));
  }

  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    else if (!(o instanceof TextIcon)) {
      return false;
    }
    else {
      final TextIcon icon = (TextIcon)o;
      return this.myStr.equals(icon.myStr);
    }
  }

  public int hashCode() {
    return this.myStr.hashCode();
  }

  public void paintIcon(Component c, Graphics g, int x, int y) {
    UISettings.setupAntialiasing(g);
    final Font originalFont = g.getFont();
    final Color originalColor = g.getColor();

    g.setFont(this.myFont);
    final int textWidth = g.getFontMetrics().stringWidth(myStr);
    final int textHeight = SimpleColoredComponent.getTextBaseLine(g.getFontMetrics(), this.getIconHeight());

    // background
    g.setColor(JBColor.background().darker());
    g.fillRect(x, y, getIconWidth(), getIconHeight());

    // text
    g.setColor(FLUTTER_COLOR); // JBColor.foreground()
    g.drawString(this.myStr, x + (getIconWidth() - textWidth) / 2, y + textHeight);

    // border
    g.setColor(JBColor.border());
    g.drawRect(x, y, getIconWidth(), getIconHeight());

    g.setFont(originalFont);
    g.setColor(originalColor);
  }

  public int getIconWidth() {
    return AllIcons.General.Add.getIconWidth();
  }

  public int getIconHeight() {
    return AllIcons.General.Add.getIconWidth();
  }
}