/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package com.intellij.uiDesigner.impl;

import com.intellij.ide.palette.impl.PaletteToolWindowManager;
import com.intellij.uiDesigner.impl.editor.UIFormEditor;
import com.intellij.uiDesigner.impl.propertyInspector.DesignerToolWindowManager;
import consulo.disposer.Disposable;
import consulo.fileEditor.FileEditor;
import consulo.fileEditor.FileEditorManager;
import consulo.ide.impl.idea.designer.DesignerEditorPanelFacade;
import consulo.ide.impl.idea.designer.LightToolWindowManager;
import consulo.ide.impl.idea.designer.ToggleEditorModeAction;
import consulo.project.Project;
import consulo.ui.ex.toolWindow.ToolWindowAnchor;

import jakarta.annotation.Nullable;

/**
 * @author Alexander Lobas
 */
public abstract class AbstractToolWindowManager extends LightToolWindowManager implements Disposable
{
	public AbstractToolWindowManager(Project project, FileEditorManager fileEditorManager)
	{
		super(project, fileEditorManager);
	}

	@Nullable
	@Override
	protected DesignerEditorPanelFacade getDesigner(FileEditor editor)
	{
		if(editor instanceof UIFormEditor)
		{
			UIFormEditor formEditor = (UIFormEditor) editor;
			return formEditor.getEditor();
		}
		return null;
	}

	@Override
	protected ToggleEditorModeAction createToggleAction(ToolWindowAnchor anchor)
	{
		return new ToggleEditorModeAction(this, myProject, anchor)
		{
			@Override
			protected LightToolWindowManager getOppositeManager()
			{
				AbstractToolWindowManager designerManager = DesignerToolWindowManager.getInstance(myProject);
				AbstractToolWindowManager paletteManager = PaletteToolWindowManager.getInstance(myProject);
				return myManager == designerManager ? paletteManager : designerManager;
			}
		};
	}
}