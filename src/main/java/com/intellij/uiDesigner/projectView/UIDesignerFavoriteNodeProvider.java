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

package com.intellij.uiDesigner.projectView;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.jetbrains.annotations.NonNls;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import com.intellij.ide.favoritesTreeView.FavoriteNodeProvider;
import com.intellij.ide.projectView.ViewSettings;
import com.intellij.ide.util.treeView.AbstractTreeNode;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.uiDesigner.GuiFormFileType;
import com.intellij.uiDesigner.compiler.Utils;

/**
 * @author yole
 */
public class UIDesignerFavoriteNodeProvider extends FavoriteNodeProvider
{
	@Override
	@Nullable
	public Collection<AbstractTreeNode> getFavoriteNodes(DataContext context, final ViewSettings viewSettings)
	{
		Project project = context.getData(CommonDataKeys.PROJECT);
		if(project == null)
		{
			return null;
		}
		Form[] forms = context.getData(Form.DATA_KEY);
		if(forms != null)
		{
			Collection<AbstractTreeNode> result = new ArrayList<AbstractTreeNode>();
			Set<PsiClass> bindClasses = new HashSet<PsiClass>();
			for(Form form : forms)
			{
				final PsiClass classToBind = form.getClassToBind();
				if(classToBind != null)
				{
					if(bindClasses.contains(classToBind))
					{
						continue;
					}
					bindClasses.add(classToBind);
					result.add(FormNode.constructFormNode(classToBind, project, viewSettings));
				}
			}
			if(!result.isEmpty())
			{
				return result;
			}
		}

		VirtualFile vFile = context.getData(PlatformDataKeys.VIRTUAL_FILE);
		if(vFile != null)
		{
			final FileType fileType = vFile.getFileType();
			if(fileType.equals(GuiFormFileType.INSTANCE))
			{
				final PsiFile formFile = PsiManager.getInstance(project).findFile(vFile);
				if(formFile == null)
				{
					return null;
				}
				String text = formFile.getText();
				String className;
				try
				{
					className = Utils.getBoundClassName(text);
				}
				catch(Exception e)
				{
					return null;
				}
				if(className == null)
				{
					return null;
				}
				final PsiClass classToBind = JavaPsiFacade.getInstance(project).findClass(className, GlobalSearchScope.allScope(project));
				if(classToBind != null)
				{
					Form form = new Form(classToBind);
					final AbstractTreeNode node = new FormNode(project, form, viewSettings);
					return Collections.singletonList(node);
				}
			}
		}

		return null;
	}

	public boolean elementContainsFile(final Object element, final VirtualFile vFile)
	{
		if(element instanceof Form)
		{
			Form form = (Form) element;
			return form.containsFile(vFile);
		}
		return false;
	}

	public int getElementWeight(final Object element, final boolean isSortByType)
	{
		if(element instanceof Form)
		{
			return 9;
		}
		return -1;
	}

	@Nullable
	public String getElementLocation(final Object element)
	{
		if(element instanceof Form)
		{
			final PsiFile[] psiFiles = ((Form) element).getFormFiles();
			VirtualFile vFile = null;
			if(psiFiles.length > 0)
			{
				vFile = psiFiles[0].getVirtualFile();
			}
			if(vFile != null)
			{
				return vFile.getPresentableUrl();
			}
		}
		return null;
	}

	public boolean isInvalidElement(Object element)
	{
		if(element instanceof Form)
		{
			return !((Form) element).isValid();
		}
		return false;
	}

	@Nonnull
	@NonNls
	public String getFavoriteTypeId()
	{
		return "form";
	}

	@Nullable
	@NonNls
	public String getElementUrl(Object element)
	{
		if(element instanceof Form)
		{
			Form form = (Form) element;
			return form.getClassToBind().getQualifiedName();
		}
		return null;
	}

	public String getElementModuleName(final Object element)
	{
		if(element instanceof Form)
		{
			Form form = (Form) element;
			final Module module = ModuleUtil.findModuleForPsiElement(form.getClassToBind());
			return module != null ? module.getName() : null;
		}
		return null;
	}

	public Object[] createPathFromUrl(final Project project, final String url, final String moduleName)
	{
		final PsiManager psiManager = PsiManager.getInstance(project);
		final PsiClass classToBind = JavaPsiFacade.getInstance(psiManager.getProject()).findClass(url, GlobalSearchScope.allScope(project));
		if(classToBind == null)
		{
			return null;
		}
		return new Object[]{new Form(classToBind)};
	}
}
