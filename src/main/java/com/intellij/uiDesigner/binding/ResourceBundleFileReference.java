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

import com.intellij.lang.properties.PropertiesUtil;
import com.intellij.lang.properties.PropertiesFileType;
import com.intellij.lang.properties.psi.PropertiesFile;
import consulo.language.plain.psi.PsiPlainTextFile;
import consulo.logging.Logger;
import consulo.module.Module;
import consulo.project.Project;
import consulo.module.content.ProjectFileIndex;
import consulo.module.content.ProjectRootManager;
import consulo.document.util.TextRange;
import consulo.virtualFileSystem.VirtualFile;
import consulo.language.psi.PsiElement;
import consulo.language.util.IncorrectOperationException;

/**
 * @author Eugene Zhuravlev
 *         Date: Jul 5, 2005
 */
public final class ResourceBundleFileReference extends ReferenceInForm {
  private static final Logger LOG = Logger.getInstance("#com.intellij.uiDesigner.ResourceBundleFileReference");

  public ResourceBundleFileReference(final PsiPlainTextFile file, TextRange bundleNameRange) {
    super(file, bundleNameRange);
  }

  public PsiElement resolve() {
    final Project project = myFile.getProject();

    final ProjectFileIndex fileIndex = ProjectRootManager.getInstance(project).getFileIndex();
    final VirtualFile formVirtualFile = myFile.getVirtualFile();
    if (formVirtualFile == null) {
      return null;
    }
    final Module module = fileIndex.getModuleForFile(formVirtualFile);
    if (module == null) {
      return null;
    }
    PropertiesFile propertiesFile = PropertiesUtil.getPropertiesFile(getRangeText(), module, null);
    return propertiesFile == null ? null : propertiesFile.getContainingFile();
  }

  @Override
  public boolean isReferenceTo(final PsiElement element) {
    if (!(element instanceof PropertiesFile)) return false;
    String baseName = PropertiesUtil.getFullName((PropertiesFile) element);
    if (baseName == null) return false;
    baseName = baseName.replace('.', '/');
    final String rangeText = getRangeText();
    return rangeText.equals(baseName);
  }

  public PsiElement handleElementRename(final String newElementName) {
    return handleFileRename(newElementName, PropertiesFileType.DOT_DEFAULT_EXTENSION, false);
  }

  public PsiElement bindToElement(@Nonnull final PsiElement element) throws IncorrectOperationException {
    if (!(element instanceof PropertiesFile)) {
      throw new IncorrectOperationException();
    }

    final PropertiesFile propertyFile = (PropertiesFile)element;
    final String bundleName = FormReferenceProvider.getBundleName(propertyFile);
    LOG.assertTrue(bundleName != null);
    updateRangeText(bundleName);
    return myFile;
  }
}
