/*
 * Copyright 2000-2012 JetBrains s.r.o.
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

import com.intellij.java.language.psi.JavaPsiFacade;
import com.intellij.java.language.psi.PsiClass;
import com.intellij.uiDesigner.impl.GuiFormFileType;
import com.intellij.uiDesigner.compiler.Utils;
import consulo.annotation.component.ExtensionImpl;
import consulo.language.navigation.GotoRelatedItem;
import consulo.language.navigation.GotoRelatedProvider;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.scope.GlobalSearchScope;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.project.Project;

import jakarta.annotation.Nonnull;
import java.util.Collections;
import java.util.List;

/**
 * @author Dmitry Avdeev
 */
@ExtensionImpl
public class FormRelatedFilesProvider extends GotoRelatedProvider
{
	@Nonnull
	@Override
	public List<? extends GotoRelatedItem> getItems(@Nonnull PsiElement context)
	{
		PsiClass psiClass = PsiTreeUtil.getParentOfType(context, PsiClass.class, false);
		if(psiClass != null)
		{
			while(psiClass != null)
			{
				List<PsiFile> forms = FormClassIndex.findFormsBoundToClass(psiClass);
				if(!forms.isEmpty())
				{
					return GotoRelatedItem.createItems(forms, "UI Forms");
				}
				psiClass = PsiTreeUtil.getParentOfType(psiClass, PsiClass.class);
			}
		}
		else
		{
			PsiFile file = context.getContainingFile();
			if(file.getFileType() == GuiFormFileType.INSTANCE)
			{
				try
				{
					String className = Utils.getBoundClassName(file.getText());
					if(className != null)
					{
						Project project = file.getProject();
						PsiClass aClass = JavaPsiFacade.getInstance(project).findClass(className, GlobalSearchScope.allScope(project));
						if(aClass != null)
						{
							return Collections.singletonList(new GotoRelatedItem(aClass, "Java"));
						}
					}
				}
				catch(Exception ignore)
				{

				}
			}
		}
		return Collections.emptyList();
	}
}
