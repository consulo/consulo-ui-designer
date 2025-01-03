/*
 * Copyright 2000-2009 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.intellij.uiDesigner.impl.designSurface;

import com.intellij.uiDesigner.impl.HSpacer;
import com.intellij.uiDesigner.impl.VSpacer;
import com.intellij.uiDesigner.impl.UIDesignerBundle;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.impl.palette.ComponentItem;
import com.intellij.uiDesigner.impl.palette.Palette;
import com.intellij.uiDesigner.impl.radComponents.RadComponent;
import com.intellij.uiDesigner.impl.radComponents.RadContainer;
import com.intellij.uiDesigner.impl.radComponents.RadAbstractGridLayoutManager;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Dimension;

/**
 * @author yole
 */
public class FirstComponentInsertLocation extends GridDropLocation {
  protected final Rectangle myCellRect;
  protected final int myXPart;
  protected final int myYPart;

  public FirstComponentInsertLocation(@Nonnull final RadContainer container,
                                      final Point targetPoint,
                                      @Nonnull final Rectangle cellRect) {
    super(container, 0, 0);
    myCellRect = cellRect;
    int midX1 = myCellRect.x + myCellRect.width / 3;
    int midX2 = myCellRect.x + (myCellRect.width*2) / 3;
    int midY1 = myCellRect.y + myCellRect.height / 3;
    int midY2 = myCellRect.y + (myCellRect.height*2) / 3;
    if (targetPoint.x < midX1) {
      myXPart = 0;
    }
    else if (targetPoint.x < midX2) {
      myXPart = 1;
    }
    else {
      myXPart = 2;
    }
    if (targetPoint.y < midY1) {
      myYPart = 0;
    }
    else if (targetPoint.y < midY2) {
      myYPart = 1;
    }
    else {
      myYPart = 2;
    }
  }

  public FirstComponentInsertLocation(@Nonnull final RadContainer container,
                                      final Rectangle cellRect,
                                      final int xPart,
                                      final int yPart) {
    super(container, 0, 0);
    myCellRect = cellRect;
    myXPart = xPart;
    myYPart = yPart;
  }

  @Override public void placeFeedback(FeedbackLayer feedbackLayer, ComponentDragObject dragObject) {
    int midX1 = myCellRect.x + myCellRect.width / 3;
    int midX2 = myCellRect.x + (myCellRect.width*2) / 3;
    int midY1 = myCellRect.y + myCellRect.height / 3;
    int midY2 = myCellRect.y + (myCellRect.height*2) / 3;

    Rectangle rc = new Rectangle();
    if (myXPart == 0) {
      rc.x = myCellRect.x;
      rc.width = initialWidth(dragObject, midX1 - myCellRect.x);
    }
    else if (myXPart == 1) {
      rc.x = myCellRect.x;
      rc.width = myCellRect.width;
    }
    else {
      rc.width = initialWidth(dragObject, myCellRect.width - (midX2 - myCellRect.x));
      rc.x = myCellRect.width - rc.width;
    }

    if (myYPart == 0) {
      rc.y = myCellRect.y;
      rc.height = initialHeight(dragObject, midY1 - myCellRect.y);
    }
    else if (myYPart == 1) {
      rc.y = myCellRect.y;
      rc.height = myCellRect.height;
    }
    else {
      rc.height = initialHeight(dragObject, myCellRect.height - (midY2 - myCellRect.y));
      rc.y = myCellRect.height - rc.height;
    }

    feedbackLayer.putFeedback(myContainer.getDelegee(), rc, getInsertFeedbackTooltip());
  }

  private String getInsertFeedbackTooltip() {
    StringBuilder result = new StringBuilder(myContainer.getDisplayName());
    result.append(" (");
    if (myXPart == 1 && myYPart == 1) {
      result.append(UIDesignerBundle.message("insert.feedback.fill"));
    }
    else {
      if (myYPart == 0) {
        result.append(UIDesignerBundle.message("insert.feedback.top"));
      }
      else if (myYPart == 2) {
        result.append(UIDesignerBundle.message("insert.feedback.bottom"));
      }
      if (myYPart != 1 && myXPart != 1) {
        result.append(" ");
      }
      if (myXPart == 0) {
        result.append(UIDesignerBundle.message("insert.feedback.left"));
      }
      else if (myXPart == 2) {
        result.append(UIDesignerBundle.message("insert.feedback.right"));
      }
    }
    result.append(")");
    return result.toString();
  }

  private int initialWidth(ComponentDragObject dragObject, int defaultSize) {
    Dimension initialSize = dragObject.getInitialSize(getContainer());
    if (initialSize.width > 0 && initialSize.width < defaultSize) {
      return initialSize.width;
    }
    return defaultSize;
  }

  private int initialHeight(ComponentDragObject dragObject, int defaultSize) {
    Dimension initialSize = dragObject.getInitialSize(getContainer());
    if (initialSize.height > 0 && initialSize.height < defaultSize) {
      return initialSize.height;
    }
    return defaultSize;
  }

  @Override
  public boolean canDrop(final ComponentDragObject dragObject) {
    if (dragObject.getComponentCount() == 1 && (myContainer.getGridRowCount() == 0 || myContainer.getGridColumnCount() == 0)) {
      return true;
    }
    return super.canDrop(dragObject);
  }

  @Override public void processDrop(final GuiEditor editor,
                                    final RadComponent[] components,
                                    final GridConstraints[] constraintsToAdjust,
                                    final ComponentDragObject dragObject) {
    RadAbstractGridLayoutManager gridLayout = myContainer.getGridLayoutManager();
    if (myContainer.getGridRowCount() == 0 && myContainer.getGridColumnCount() == 0) {
      gridLayout.insertGridCells(myContainer, 0, false, true, true);
      gridLayout.insertGridCells(myContainer, 0, true, true, true);
    }

    super.processDrop(editor, components, constraintsToAdjust, dragObject);

    Palette palette = Palette.getInstance(editor.getProject());
    ComponentItem hSpacerItem = palette.getItem(HSpacer.class.getName());
    ComponentItem vSpacerItem = palette.getItem(VSpacer.class.getName());

    InsertComponentProcessor icp = new InsertComponentProcessor(editor);

    if (myXPart == 0) {
      insertSpacer(icp, hSpacerItem, GridInsertMode.ColumnAfter);
    }
    if (myXPart == 2) {
      insertSpacer(icp, hSpacerItem, GridInsertMode.ColumnBefore);
    }

    if (myYPart == 0) {
      insertSpacer(icp, vSpacerItem, GridInsertMode.RowAfter);
    }
    if (myYPart == 2) {
      insertSpacer(icp, vSpacerItem, GridInsertMode.RowBefore);
    }
  }

  @Nullable
  public ComponentDropLocation getAdjacentLocation(Direction direction) {
    if (direction == Direction.DOWN && myYPart < 2) {
      return createAdjacentLocation(myXPart, myYPart+1);
    }
    if (direction == Direction.UP && myYPart > 0) {
      return createAdjacentLocation(myXPart, myYPart-1);
    }
    if (direction == Direction.RIGHT && myXPart < 2) {
      return createAdjacentLocation(myXPart+1, myYPart);
    }
    if (direction == Direction.LEFT && myXPart > 0) {
      return createAdjacentLocation(myXPart-1, myYPart);
    }
    return null;
  }

  protected FirstComponentInsertLocation createAdjacentLocation(final int xPart, final int yPart) {
    return new FirstComponentInsertLocation(myContainer, myCellRect, xPart, yPart);
  }

  private void insertSpacer(InsertComponentProcessor icp, ComponentItem spacerItem, GridInsertMode mode) {
    GridInsertLocation location = new GridInsertLocation(myContainer, 0, 0, mode);
    icp.processComponentInsert(spacerItem, location);
  }
}
