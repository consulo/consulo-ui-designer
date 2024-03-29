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

/*
 * Created by IntelliJ IDEA.
 * User: yole
 * Date: 05.10.2006
 * Time: 13:35:16
 */
package com.intellij.uiDesigner.impl.actions;

import consulo.fileEditor.FileEditorManager;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.language.editor.CommonDataKeys;
import consulo.fileEditor.FileEditor;
import consulo.project.Project;
import consulo.util.lang.ref.Ref;
import com.intellij.uiDesigner.impl.FormEditingUtil;
import com.intellij.uiDesigner.impl.LoaderFactory;
import com.intellij.uiDesigner.impl.designSurface.GuiEditor;
import com.intellij.uiDesigner.impl.editor.UIFormEditor;
import com.intellij.uiDesigner.lw.IComponent;
import com.intellij.uiDesigner.impl.radComponents.RadErrorComponent;

public class ReloadCustomComponentsAction extends AnAction {
  public void actionPerformed(AnActionEvent e) {
    Project project = e.getData(CommonDataKeys.PROJECT);
    if (project == null) return;
    LoaderFactory.getInstance(project).clearClassLoaderCache();
    final FileEditor[] fileEditors = FileEditorManager.getInstance(project).getAllEditors();
    for(FileEditor editor: fileEditors) {
      if (editor instanceof UIFormEditor) {
        ((UIFormEditor) editor).getEditor().readFromFile(true);
      }
    }
  }

  @Override
  public void update(AnActionEvent e) {
    final GuiEditor editor = FormEditingUtil.getActiveEditor(e.getDataContext());
    e.getPresentation().setVisible(editor != null && haveCustomComponents(editor));
  }

  private static boolean haveCustomComponents(final GuiEditor editor) {
    // quick & dirty check
    if (editor.isFormInvalid()) {
      return true;
    }
    final Ref<Boolean> result = new Ref<Boolean>();
    FormEditingUtil.iterate(editor.getRootContainer(), new FormEditingUtil.ComponentVisitor() {
      public boolean visit(final IComponent component) {
        if (component instanceof RadErrorComponent || !component.getComponentClassName().startsWith("javax.swing")) {
          result.set(Boolean.TRUE);
          return false;
        }
        return true;
      }
    });
    return !result.isNull();
  }
}