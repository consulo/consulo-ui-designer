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

import consulo.ui.ex.action.ToggleAction;
import consulo.ui.ex.action.AnActionEvent;
import com.intellij.uiDesigner.impl.designSurface.GuiEditor;
import com.intellij.uiDesigner.impl.FormEditingUtil;

/**
 * @author yole
 */
public class ShowGridAction extends ToggleAction {
  public void update(final AnActionEvent e) {
    super.update(e);
    GuiEditor editor = FormEditingUtil.getEditorFromContext(e.getDataContext());
    e.getPresentation().setEnabled(editor != null);
  }

  public boolean isSelected(AnActionEvent e) {
    GuiEditor editor = FormEditingUtil.getEditorFromContext(e.getDataContext());
    return editor != null && editor.isShowGrid();
  }

  public void setSelected(AnActionEvent e, boolean state) {
    GuiEditor editor = FormEditingUtil.getEditorFromContext(e.getDataContext());
    if (editor != null) {
      editor.setShowGrid(state);
    }
  }
}
