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
import consulo.annotation.component.ExtensionImpl;
import consulo.language.editor.refactoring.localize.RefactoringLocalize;
import consulo.language.editor.refactoring.rename.AutomaticRenamer;
import consulo.language.editor.refactoring.rename.AutomaticRenamerFactory;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.localize.LocalizeValue;
import consulo.usage.UsageInfo;

import jakarta.annotation.Nonnull;
import java.util.Collection;
import java.util.List;

@ExtensionImpl
public class FormsRenamerFactory implements AutomaticRenamerFactory
{
	public boolean isApplicable(final PsiElement element)
	{
		if(!(element instanceof PsiClass))
		{
			return false;
		}
		List<PsiFile> forms = FormClassIndex.findFormsBoundToClass((PsiClass) element);
		return forms.size() > 0;
	}

	@Nonnull
	public LocalizeValue getOptionName()
	{
		return RefactoringLocalize.renameBoundForms();
	}

	public boolean isEnabled()
	{
		return true;
	}

	public void setEnabled(final boolean enabled)
	{
	}

	public AutomaticRenamer createRenamer(final PsiElement element, final String newName, final Collection<UsageInfo> usages)
	{
		return new FormsRenamer((PsiClass) element, newName);
	}
}
