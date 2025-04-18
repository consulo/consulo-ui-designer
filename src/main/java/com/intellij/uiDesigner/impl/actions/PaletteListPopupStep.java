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

package com.intellij.uiDesigner.impl.actions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import jakarta.annotation.Nonnull;

import consulo.application.util.function.Processor;
import consulo.ui.ex.popup.*;
import consulo.ui.ex.popup.PopupStep;
import consulo.ui.ex.popup.SpeedSearchFilter;
import com.intellij.uiDesigner.impl.UIDesignerBundle;
import com.intellij.uiDesigner.impl.designSurface.GuiEditor;
import com.intellij.uiDesigner.impl.designSurface.InsertComponentProcessor;
import com.intellij.uiDesigner.impl.palette.ComponentItem;
import com.intellij.uiDesigner.impl.palette.GroupItem;
import com.intellij.uiDesigner.impl.palette.Palette;
import consulo.project.Project;
import consulo.ui.ex.popup.ListSeparator;
import consulo.ui.ex.popup.MnemonicNavigationFilter;
import consulo.ui.image.Image;

/**
 * @author yole
 */
class PaletteListPopupStep implements ListPopupStep<ComponentItem>, SpeedSearchFilter<ComponentItem> {
  private final ArrayList<ComponentItem> myItems = new ArrayList<ComponentItem>();
  private final ComponentItem myInitialSelection;
  private final Processor<ComponentItem> myRunnable;
  private final String myTitle;
  private final Project myProject;

  PaletteListPopupStep(GuiEditor editor, ComponentItem initialSelection, final Processor<ComponentItem> runnable, final String title) {
    myInitialSelection = initialSelection;
    myRunnable = runnable;
    myProject = editor.getProject();
    Palette palette = Palette.getInstance(editor.getProject());
    for(GroupItem group: palette.getToolWindowGroups()) {
      Collections.addAll(myItems, group.getItems());
    }
    myTitle = title;
  }

  @Nonnull
  public List<ComponentItem> getValues() {
    return myItems;
  }

  public boolean isSelectable(final ComponentItem value) {
    return true;
  }

  public Image getIconFor(final ComponentItem aValue) {
    return aValue.getSmallIcon();
  }

  @Nonnull
  public String getTextFor(final ComponentItem value) {
    if (value.isAnyComponent()) {
      return UIDesignerBundle.message("palette.non.palette.component");
    }
    return value.getClassShortName();
  }

  public ListSeparator getSeparatorAbove(final ComponentItem value) {
    return null;
  }

  public int getDefaultOptionIndex() {
    if (myInitialSelection != null) {
      int index = myItems.indexOf(myInitialSelection);
      if (index >= 0) {
        return index;
      }
    }
    return 0;
  }

  public String getTitle() {
    return myTitle;
  }

  public PopupStep onChosen(final ComponentItem selectedValue, final boolean finalChoice) {
    myRunnable.process(selectedValue);
    return PopupStep.FINAL_CHOICE;
  }

  public Runnable getFinalRunnable() {
    return null;
  }

  public boolean hasSubstep(final ComponentItem selectedValue) {
    return false;
  }

  public void canceled() {
  }

  public boolean isMnemonicsNavigationEnabled() {
    return false;
  }

  public MnemonicNavigationFilter<ComponentItem> getMnemonicNavigationFilter() {
    return null;
  }

  public boolean isSpeedSearchEnabled() {
    return true;
  }

  public boolean isAutoSelectionEnabled() {
    return false;
  }

  public SpeedSearchFilter<ComponentItem> getSpeedSearchFilter() {
    return this;
  }

  public boolean canBeHidden(final ComponentItem value) {
    return true;
  }

  public String getIndexedString(final ComponentItem value) {
    if (value.isAnyComponent()) {
      return "";
    }
    return value.getClassShortName();
  }

  public void hideComponentClass(final String componentClassName) {
    for(ComponentItem item: myItems) {
      if (item.getClassName().equals(componentClassName)) {
        myItems.remove(item);
        break;
      }
    }
  }

  public void hideNonAtomic() {
    for(int i=myItems.size()-1; i >= 0; i--) {
      ComponentItem item = myItems.get(i);
      if (InsertComponentProcessor.getRadComponentFactory(myProject, item.getClassName()) != null || item.getBoundForm() != null) {
        myItems.remove(i);
      }
    }
  }
}
