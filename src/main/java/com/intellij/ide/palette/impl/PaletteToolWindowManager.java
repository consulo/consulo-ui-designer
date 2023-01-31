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
package com.intellij.ide.palette.impl;

import consulo.application.AllIcons;
import consulo.ide.impl.idea.designer.DesignerEditorPanelFacade;
import consulo.ide.impl.idea.designer.LightToolWindow;
import consulo.ide.IdeBundle;
import consulo.fileEditor.FileEditorManager;
import consulo.project.ui.wm.ToolWindowManager;
import consulo.ui.ex.content.Content;
import consulo.ui.ex.toolWindow.ToolWindowAnchor;
import consulo.ui.ex.content.ContentManager;
import com.intellij.uiDesigner.AbstractToolWindowManager;
import com.intellij.uiDesigner.designSurface.GuiEditor;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.project.Project;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author Alexander Lobas
 */
@Singleton
public class PaletteToolWindowManager extends AbstractToolWindowManager
{
	private final PaletteWindow myToolWindowPanel;

	@Inject
	public PaletteToolWindowManager(Project project, FileEditorManager fileEditorManager)
	{
		super(project, fileEditorManager);
		myToolWindowPanel = new PaletteWindow(project);
	}

	public static PaletteWindow getInstance(GuiEditor designer)
	{
		PaletteToolWindowManager manager = getInstance(designer.getProject());
		if(manager.isEditorMode())
		{
			return (PaletteWindow) manager.getContent(designer);
		}
		return manager.myToolWindowPanel;
	}

	public static PaletteToolWindowManager getInstance(Project project)
	{
		return project.getComponent(PaletteToolWindowManager.class);
	}

	@Override
	protected void initToolWindow()
	{
		myToolWindow = ToolWindowManager.getInstance(myProject).registerToolWindow(IdeBundle.message("toolwindow.palette"), false, getAnchor(),
				myProject, true);
		myToolWindow.setIcon(AllIcons.Toolwindows.ToolWindowPalette);
		initGearActions();

		ContentManager contentManager = myToolWindow.getContentManager();
		Content content = contentManager.getFactory().createContent(myToolWindowPanel, null, false);
		content.setCloseable(false);
		content.setPreferredFocusableComponent(myToolWindowPanel);
		contentManager.addContent(content);
		contentManager.setSelectedContent(content, true);
		myToolWindow.setAvailable(false, null);
	}

	@Override
	protected void updateToolWindow(@Nullable DesignerEditorPanelFacade designer)
	{
		myToolWindowPanel.refreshPaletteIfChanged((GuiEditor) designer);

		if(designer == null)
		{
			myToolWindow.setAvailable(false, null);
		}
		else
		{
			myToolWindow.setAvailable(true, null);
			myToolWindow.show(null);
		}
	}

	@Override
	protected ToolWindowAnchor getAnchor()
	{
		return ToolWindowAnchor.RIGHT;
	}

	@Override
	protected LightToolWindow createContent(@Nonnull DesignerEditorPanelFacade designer)
	{
		PaletteWindow palettePanel = new PaletteWindow(myProject);
		palettePanel.refreshPaletteIfChanged((GuiEditor) designer);

		return createContent(designer, palettePanel, IdeBundle.message("toolwindow.palette"), TargetAWT.to(AllIcons.Toolwindows.ToolWindowPalette), palettePanel,
				palettePanel, 180, null);
	}

	@Override
	public void disposeComponent()
	{
		if(myToolWindowPanel != null)
		{
			myToolWindowPanel.dispose();
		}
	}

	@Nonnull
	@Override
	public String getComponentName()
	{
		return "PaletteManager";
	}
}