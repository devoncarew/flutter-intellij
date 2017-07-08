/*
 * Copyright 2017 The Chromium Authors. All rights reserved.
 * Use of this source code is governed by a BSD-style license that can be
 * found in the LICENSE file.
 */

package io.flutter.view;

import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBPanel;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

// TODO: scrolling

// TODO: tooltips

// TODO: overlay with a pause button and an fps display
// JBLayeredPane

public class FrameChartComponent extends JBPanel {
  private static final float PIXELS_PER_MILLI = 1.5f;
  private static final float MILLIS_PER_FRAME = (1000.0f / 60.0f);
  private static final float MAX_MILLIS = MILLIS_PER_FRAME * 2.0f;

  public final FramesModel model;

  // TODO: use double buffering?
  public FrameChartComponent(FramesModel model) {
    this.model = model;

    setLayout(new BorderLayout());

    setPreferredSize(new Dimension(100, Math.round(PIXELS_PER_MILLI * MAX_MILLIS)));

    model.addChangeListener(this::doUpdate);

    doUpdate();
  }

  private void doUpdate() {
    // TODO: not faster than the vsync

    SwingUtilities.invokeLater(() -> {
      setPreferredSize(new Dimension(6 * model.getFrames().size(), Math.round((PIXELS_PER_MILLI * MAX_MILLIS))));
      repaint();
    });
  }

  final static float dashPattern[] = {3.0f, 3.0f};
  final static BasicStroke dashedStroke =
    new BasicStroke(1.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER,
                    10.0f, dashPattern, 0.0f);

  final static float dottedPattern[] = {1.0f, 1.0f};
  final static BasicStroke dottedStroke =
    new BasicStroke(0.7f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER,
                    10.0f, dottedPattern, 0.0f);

  final static BasicStroke solidStroke = new BasicStroke(0.7f);

  @Override
  protected void paintComponent(Graphics graphics) {
    super.paintComponent(graphics);

    final Graphics2D g = (Graphics2D)graphics.create();

    // TODO: use components instead of painting


    final int height = getHeight();

    // TODO: round top corners

    final Stroke savedStroke = g.getStroke();

    // draw horizontal tick lines
    final float tickHeight = MILLIS_PER_FRAME * PIXELS_PER_MILLI;
    float starty = height - tickHeight;
    int y = Math.round(starty);
    g.setColor(JBColor.GRAY);
    g.setStroke(dashedStroke);
    while (y > 0) {
      g.drawLine(0, y, getWidth(), y);
      starty -= tickHeight;
      y = Math.round(starty);
    }
    g.setStroke(savedStroke);

    // draw the vertical frame bars
    int x = 1;
    g.setColor(JBColor.BLUE);

    for (Frame frame : model.getFrames()) {
      // Or, change the background color for the frames in alternating stripes?
      if (frame.divider != null) {
        g.setColor(frame.divider.kind == FrameDivider.Kind.Reload ? JBColor.RED : JBColor.GRAY);
        g.setStroke(frame.divider.kind == FrameDivider.Kind.Reload ? solidStroke : dottedStroke);
        g.drawLine(x, 0, x, height);
        g.setColor(JBColor.BLUE);
        g.setStroke(savedStroke);
      }

      g.setColor(frame.durationMicros > (MILLIS_PER_FRAME * 1000) ? JBColor.red : JBColor.blue);
      int frameHeight = Math.round(frame.durationMicros * PIXELS_PER_MILLI / 1000);
      frameHeight = Math.min(frameHeight, height);
      g.fillRect(x, height - frameHeight, 4, frameHeight);
      x += 6;
    }

    g.dispose();
  }
}

class FramesModel {
  static final int MAX_FRAME_COUNT = 120;

  interface ChangeListener {
    void handleChanged();
  }

  private final LinkedList<Frame> frames = new LinkedList<>();
  private final List<ChangeListener> listeners = new ArrayList<>();
  private FrameDivider waitingReloadMarker;

  // TODO: synchronization

  public FramesModel() {

  }

  public void addChangeListener(ChangeListener listener) {
    listeners.add(listener);
  }

  void addFrame(Frame frame) {
    // Look for a reload marker; else see if we need to group frames.
    if (waitingReloadMarker != null) {
      frame.divider = waitingReloadMarker;
      waitingReloadMarker = null;
    }
    else if (!frames.isEmpty()) {
      final Frame last = frames.getFirst();
      if ((frame.startTimeMicros - last.getEndTime()) > 400 * 1000) {
        frame.divider = new FrameDivider(frame.startTimeMicros, FrameDivider.Kind.FrameGroup);
      }
    }

    frames.add(frame);
    if (frames.size() > MAX_FRAME_COUNT) {
      frames.removeFirst();
    }
    fireChanged();
  }

  public void markReload() {
    waitingReloadMarker = new FrameDivider(0, FrameDivider.Kind.Reload);
  }

  public void clear() {
    frames.clear();
    fireChanged();
  }

  public List<Frame> getFrames() {
    return frames;
  }

  private void fireChanged() {
    for (ChangeListener listener : listeners) {
      listener.handleChanged();
    }
  }
}

class Frame {
  final int number;
  final int startTimeMicros;
  final int durationMicros;
  FrameDivider divider;

  public Frame(int number, int startTimeMicros, int durationMicros) {
    this.number = number;
    this.startTimeMicros = startTimeMicros;
    this.durationMicros = durationMicros;
  }

  public int getEndTime() {
    return startTimeMicros + durationMicros;
  }
}

class FrameDivider {
  enum Kind {
    FrameGroup,
    Reload
  }

  final int startTimeMicros;
  final Kind kind;

  public FrameDivider(int startTimeMicros, Kind kind) {
    this.startTimeMicros = startTimeMicros;
    this.kind = kind;
  }
}
