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

import jakarta.annotation.Nonnull;

import consulo.document.Document;
import consulo.document.FileDocumentManager;
import consulo.document.util.TextRange;
import consulo.language.plain.psi.PsiPlainTextFile;
import consulo.language.psi.PsiElement;
import consulo.document.RangeMarker;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiReference;
import consulo.util.collection.ArrayUtil;
import org.jetbrains.annotations.NonNls;

/**
 * @author Eugene Zhuravlev
 *         Date: Jul 5, 2005
 */
public abstract class ReferenceInForm implements PsiReference
{
  protected final PsiPlainTextFile myFile;
  private final RangeMarker myRangeMarker;

  protected ReferenceInForm(final PsiPlainTextFile file, TextRange range) {
    myFile = file;
    final Document document = FileDocumentManager.getInstance().getDocument(myFile.getVirtualFile());
    myRangeMarker = document.createRangeMarker(range);
  }

  public PsiElement getElement() {
    return myFile;
  }

  public PsiElement handleElementRename(final String newElementName){
    return handleElementRenameBase(newElementName);
  }

  private PsiElement handleElementRenameBase(final String newElementName) {
    updateRangeText(newElementName);
    return myFile;
  }

  public TextRange getRangeInElement() {
    return TextRange.create(myRangeMarker);
  }

  @Nonnull
  public String getCanonicalText() {
    return getRangeText();
  }

  protected void updateRangeText(final String text) {
    final Document document = myRangeMarker.getDocument();
    document.replaceString(myRangeMarker.getStartOffset(), myRangeMarker.getEndOffset(), text);
    PsiDocumentManager.getInstance(myFile.getProject()).commitDocument(document);
  }

  public String getRangeText() {
    return myRangeMarker.getDocument().getCharsSequence().subSequence(myRangeMarker.getStartOffset(), myRangeMarker.getEndOffset()).toString();
  }

  public boolean isReferenceTo(final PsiElement element) {
    return resolve() == element;
  }

  @Nonnull
  public Object[] getVariants() {
    return ArrayUtil.EMPTY_OBJECT_ARRAY;
  }

  public boolean isSoft() {
    return true;
  }

  protected PsiElement handleFileRename(final String newElementName, @NonNls final String extension,
                                        final boolean includeExtensionInReference) {
    final String currentName = getRangeText();
    final String baseName = newElementName.endsWith(extension)?
                            newElementName.substring(0, newElementName.length() - extension.length()) :
                            newElementName;
    final int slashIndex = currentName.lastIndexOf('/');
    final String extensionInReference = includeExtensionInReference ? extension : "";
    if (slashIndex >= 0) {
      final String prefix = currentName.substring(0, slashIndex);
      return handleElementRenameBase(prefix + "/" + baseName + extensionInReference);
    }
    else {
      return handleElementRenameBase(baseName + extensionInReference);
    }
  }
}
