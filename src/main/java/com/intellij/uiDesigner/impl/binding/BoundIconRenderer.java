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
package com.intellij.uiDesigner.impl.binding;

import com.intellij.java.language.psi.PsiClass;
import com.intellij.java.language.psi.PsiClassType;
import com.intellij.java.language.psi.PsiField;
import com.intellij.java.language.psi.PsiType;
import com.intellij.uiDesigner.impl.UIDesignerBundle;
import com.intellij.uiDesigner.impl.editor.UIFormEditor;
import com.intellij.uiDesigner.impl.palette.ComponentItem;
import com.intellij.uiDesigner.impl.palette.Palette;
import consulo.application.AllIcons;
import consulo.codeEditor.markup.GutterIconRenderer;
import consulo.fileEditor.FileEditor;
import consulo.fileEditor.FileEditorManager;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.project.Project;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.image.Image;
import consulo.virtualFileSystem.VirtualFile;
import org.jetbrains.annotations.NonNls;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.Collections;
import java.util.List;

/**
 * @author yole
 */
public class BoundIconRenderer extends GutterIconRenderer
{
	@Nonnull
	private final PsiElement myElement;
	private Image myIcon;
	private final String myQName;

	public BoundIconRenderer(@Nonnull final PsiElement element)
	{
		myElement = element;
		if(myElement instanceof PsiField)
		{
			final PsiField field = (PsiField) myElement;
			final PsiType type = field.getType();
			if(type instanceof PsiClassType)
			{
				PsiClass componentClass = ((PsiClassType) type).resolve();
				if(componentClass != null)
				{
					String qName = componentClass.getQualifiedName();
					if(qName != null)
					{
						final ComponentItem item = Palette.getInstance(myElement.getProject()).getItem(qName);
						if(item != null)
						{
							myIcon = item.getIcon();
						}
					}
				}
			}
			myQName = field.getContainingClass().getQualifiedName() + "#" + field.getName();
		}
		else
		{
			myQName = ((PsiClass) element).getQualifiedName();
		}
	}

	@Nonnull
	public Image getIcon()
	{
		if(myIcon != null)
		{
			return myIcon;
		}
		return AllIcons.FileTypes.UiForm;
	}

	public boolean isNavigateAction()
	{
		return true;
	}

	@Nullable
	public AnAction getClickAction()
	{
		return new AnAction()
		{
			public void actionPerformed(AnActionEvent e)
			{
				List<PsiFile> formFiles = getBoundFormFiles();
				if(formFiles.size() > 0)
				{
					final VirtualFile virtualFile = formFiles.get(0).getVirtualFile();
					if(virtualFile == null)
					{
						return;
					}
					Project project = myElement.getProject();
					FileEditor[] editors = FileEditorManager.getInstance(project).openFile(virtualFile, true);
					if(myElement instanceof PsiField)
					{
						for(FileEditor editor : editors)
						{
							if(editor instanceof UIFormEditor)
							{
								((UIFormEditor) editor).selectComponent(((PsiField) myElement).getName());
							}
						}
					}
				}
			}
		};
	}

	@Nullable
	public String getTooltipText()
	{
		List<PsiFile> formFiles = getBoundFormFiles();

		if(formFiles.size() > 0)
		{
			return composeText(formFiles);
		}
		return super.getTooltipText();
	}

	private List<PsiFile> getBoundFormFiles()
	{
		List<PsiFile> formFiles = Collections.emptyList();
		PsiClass aClass;
		if(myElement instanceof PsiField)
		{
			aClass = ((PsiField) myElement).getContainingClass();
		}
		else
		{
			aClass = (PsiClass) myElement;
		}
		if(aClass != null && aClass.getQualifiedName() != null)
		{
			formFiles = FormClassIndex.findFormsBoundToClass(aClass);
		}
		return formFiles;
	}

	private static String composeText(final List<PsiFile> formFiles)
	{
		@NonNls StringBuilder result = new StringBuilder("<html><body>");
		result.append(UIDesignerBundle.message("ui.is.bound.header"));
		@NonNls String sep = "";
		for(PsiFile file : formFiles)
		{
			result.append(sep);
			sep = "<br>";
			result.append("&nbsp;&nbsp;&nbsp;&nbsp;");
			result.append(file.getName());
		}
		result.append("</body></html>");
		return result.toString();
	}

	@Override
	public boolean equals(Object o)
	{
		if(this == o)
		{
			return true;
		}
		if(o == null || getClass() != o.getClass())
		{
			return false;
		}

		BoundIconRenderer that = (BoundIconRenderer) o;

		if(!myQName.equals(that.myQName))
		{
			return false;
		}
		if(myIcon != null ? !myIcon.equals(that.myIcon) : that.myIcon != null)
		{
			return false;
		}

		return true;
	}

	@Override
	public int hashCode()
	{
		int result = myElement.hashCode();
		result = 31 * result + (myIcon != null ? myIcon.hashCode() : 0);
		return result;
	}
}
