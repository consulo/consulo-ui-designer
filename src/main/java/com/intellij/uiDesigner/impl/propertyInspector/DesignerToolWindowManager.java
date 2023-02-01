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
package com.intellij.uiDesigner.impl.propertyInspector;

import com.intellij.uiDesigner.impl.AbstractToolWindowManager;
import com.intellij.uiDesigner.impl.UIDesignerBundle;
import com.intellij.uiDesigner.impl.UIDesignerIcons;
import com.intellij.uiDesigner.impl.designSurface.GuiEditor;
import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import consulo.fileEditor.FileEditorManager;
import consulo.ide.impl.idea.designer.DesignerEditorPanelFacade;
import consulo.ide.impl.idea.designer.LightToolWindow;
import consulo.project.Project;
import consulo.project.ui.wm.ToolWindowManager;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.ex.content.Content;
import consulo.ui.ex.content.ContentManager;
import consulo.ui.ex.toolWindow.ToolWindow;
import consulo.ui.ex.toolWindow.ToolWindowAnchor;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author Alexander Lobas
 */
@Singleton
@ServiceAPI(ComponentScope.PROJECT)
@ServiceImpl
public class DesignerToolWindowManager extends AbstractToolWindowManager
{
	private final DesignerToolWindow myToolWindowPanel;

	@Inject
	public DesignerToolWindowManager(Project project, FileEditorManager fileEditorManager)
	{
		super(project, fileEditorManager);
		myToolWindowPanel = new DesignerToolWindow(project);
	}

	public static DesignerToolWindow getInstance(GuiEditor designer)
	{
		DesignerToolWindowManager manager = getInstance(designer.getProject());
		if(manager.isEditorMode())
		{
			return (DesignerToolWindow) manager.getContent(designer);
		}
		return manager.myToolWindowPanel;
	}

	public static DesignerToolWindowManager getInstance(Project project)
	{
		return project.getComponent(DesignerToolWindowManager.class);
	}

	@Nullable
	public GuiEditor getActiveFormEditor()
	{
		return (GuiEditor) getActiveDesigner();
	}

	public void initToolWindow(ToolWindow window)
	{
		myToolWindow = window;

		initGearActions();

		ContentManager contentManager = window.getContentManager();
		Content content = contentManager.getFactory().createContent(myToolWindowPanel.getToolWindowPanel(), UIDesignerBundle.message("toolwindow.ui.designer.title"), false);
		content.setCloseable(false);
		content.setPreferredFocusableComponent(myToolWindowPanel.getComponentTree());
		contentManager.addContent(content);
		contentManager.setSelectedContent(content, true);
	}

	@Override
	protected void updateToolWindow(@Nullable DesignerEditorPanelFacade designer)
	{
		if(isEditorMode())
		{
			return;
		}

		if(designer == null)
		{
			if(myToolWindow != null)
			{
				myToolWindow.setAvailable(false, null);
			}
		}
		else
		{
			if(myToolWindow != null)
			{
				myToolWindow.setAvailable(true, null);
				myToolWindow.show(null);
			}
			else
			{
				myToolWindow = ToolWindowManager.getInstance(myProject).getToolWindow("UI Designer");
				myToolWindow.activate(() ->
				{
					myToolWindow.setAvailable(true, null);
					myToolWindow.show(null);
				});
			}
		}
	}

	@Override
	protected ToolWindowAnchor getAnchor()
	{
		return ToolWindowAnchor.LEFT;
	}

	@Override
	protected LightToolWindow createContent(@Nonnull DesignerEditorPanelFacade designer)
	{
		DesignerToolWindow toolWindowContent = new DesignerToolWindow(myProject);
		toolWindowContent.update((GuiEditor) designer);

		return createContent(designer, toolWindowContent, UIDesignerBundle.message("toolwindow.ui.designer.title"), TargetAWT.to(UIDesignerIcons.ToolWindowUIDesigner), toolWindowContent
						.getToolWindowPanel(),
				toolWindowContent.getComponentTree(), 320, null);
	}

	@Override
	public void disposeComponent()
	{
		if(myToolWindowPanel != null)
		{
			myToolWindowPanel.dispose();
		}
	}
}