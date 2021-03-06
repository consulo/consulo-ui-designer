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
package com.intellij.uiDesigner.editor;

import javax.annotation.Nonnull;

import org.jdom.Element;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorPolicy;
import com.intellij.openapi.fileEditor.FileEditorProvider;
import com.intellij.openapi.fileEditor.FileEditorState;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import consulo.disposer.Disposer;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.LightVirtualFile;
import com.intellij.uiDesigner.GuiFormFileType;
import com.intellij.util.ArrayUtil;

public final class UIFormEditorProvider implements FileEditorProvider, DumbAware
{
	private static final Logger LOG = Logger.getInstance("#com.intellij.uiDesigner.editor.UIFormEditorProvider");

	@Override
	public boolean accept(@Nonnull final Project project, @Nonnull final VirtualFile file)
	{
		return file.getFileType() == GuiFormFileType.INSTANCE &&
				!GuiFormFileType.INSTANCE.isBinary() &&
				(ModuleUtil.findModuleForFile(file, project) != null || file instanceof LightVirtualFile);
	}

	@Override
	@Nonnull
	public FileEditor createEditor(@Nonnull final Project project, @Nonnull final VirtualFile file)
	{
		LOG.assertTrue(accept(project, file));
		return new UIFormEditor(project, file);
	}

	@Override
	public void disposeEditor(@Nonnull final FileEditor editor)
	{
		Disposer.dispose(editor);
	}

	@Override
	@Nonnull
	public FileEditorState readState(@Nonnull final Element element, @Nonnull final Project project, @Nonnull final VirtualFile file)
	{
		//TODO[anton,vova] implement
		return new MyEditorState(-1, ArrayUtil.EMPTY_STRING_ARRAY);
	}

	@Override
	public void writeState(@Nonnull final FileEditorState state, @Nonnull final Project project, @Nonnull final Element element)
	{
		//TODO[anton,vova] implement
	}

	@Override
	@Nonnull
	public String getEditorTypeId()
	{
		return "ui-designer";
	}

	@Override
	@Nonnull
	public FileEditorPolicy getPolicy()
	{
		return FileEditorPolicy.HIDE_DEFAULT_EDITOR;
	}
}