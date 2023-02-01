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
 * Date: 26.10.2006
 * Time: 16:44:00
 */
package com.intellij.uiDesigner.impl.projectView;

import com.intellij.java.language.psi.PsiClass;
import consulo.annotation.component.ExtensionImpl;
import consulo.codeEditor.Editor;
import consulo.dataContext.DataContext;
import consulo.language.editor.refactoring.action.RefactoringActionHandlerFactory;
import consulo.language.editor.refactoring.rename.RenameHandler;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.project.Project;

import javax.annotation.Nonnull;

@ExtensionImpl
public class FormRenameHandler implements RenameHandler
{
	@Override
	public boolean isAvailableOnDataContext(DataContext dataContext)
	{
		Form[] forms = dataContext.getData(Form.DATA_KEY);
		return forms != null && forms.length == 1;
	}

	@Override
	public boolean isRenaming(DataContext dataContext)
	{
		return isAvailableOnDataContext(dataContext);
	}

	@Override
	public void invoke(@Nonnull Project project, Editor editor, PsiFile file, DataContext dataContext)
	{
		Form[] forms = dataContext.getData(Form.DATA_KEY);
		if(forms == null || forms.length != 1)
		{
			return;
		}
		PsiClass boundClass = forms[0].getClassToBind();
		RefactoringActionHandlerFactory.getInstance().createRenameHandler().invoke(project, new PsiElement[]{boundClass}, dataContext);
	}

	@Override
	public void invoke(@Nonnull Project project, @Nonnull PsiElement[] elements, DataContext dataContext)
	{
		invoke(project, null, null, dataContext);
	}
}