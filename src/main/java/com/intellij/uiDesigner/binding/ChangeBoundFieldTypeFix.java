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
package com.intellij.uiDesigner.binding;

import javax.annotation.Nonnull;
import com.intellij.CommonBundle;
import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiType;
import com.intellij.util.IncorrectOperationException;
import consulo.java.JavaQuickFixBundle;

/**
 * @author Eugene Zhuravlev
 *         Date: Jun 15, 2005
 */
public class ChangeBoundFieldTypeFix implements IntentionAction
{
	private final PsiField myField;
	private final PsiType myTypeToSet;

	public ChangeBoundFieldTypeFix(PsiField field, PsiType typeToSet)
	{
		myField = field;
		myTypeToSet = typeToSet;
	}

	@Override
	@Nonnull
	public String getText()
	{
		return JavaQuickFixBundle.message("uidesigner.change.bound.field.type");
	}

	@Override
	@Nonnull
	public String getFamilyName()
	{
		return getText();
	}

	@Override
	public boolean isAvailable(@Nonnull Project project, Editor editor, PsiFile file)
	{
		return true;
	}

	@Override
	public void invoke(@Nonnull Project project, Editor editor, PsiFile file) throws IncorrectOperationException
	{
		CommandProcessor.getInstance().executeCommand(myField.getProject(), new Runnable()
		{
			@Override
			public void run()
			{
				try
				{
					final PsiManager manager = myField.getManager();
					myField.getTypeElement().replace(JavaPsiFacade.getInstance(manager.getProject()).getElementFactory().createTypeElement(myTypeToSet));
				}
				catch(final IncorrectOperationException e)
				{
					ApplicationManager.getApplication().invokeLater(new Runnable()
					{
						@Override
						public void run()
						{
							Messages.showErrorDialog(myField.getProject(), JavaQuickFixBundle.message("cannot.change.field.exception", myField.getName(), e.getLocalizedMessage()),
									CommonBundle.getErrorTitle());
						}
					});
				}
			}
		}, getText(), null);
	}

	@Override
	public boolean startInWriteAction()
	{
		return true;
	}
}
