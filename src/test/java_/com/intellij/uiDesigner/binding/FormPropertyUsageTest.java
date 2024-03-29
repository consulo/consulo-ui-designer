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

package com.intellij.uiDesigner.binding;

import com.intellij.lang.properties.psi.PropertiesFile;
import com.intellij.lang.properties.psi.Property;
import com.intellij.psi.PsiClass;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiReference;
import consulo.language.psi.scope.GlobalSearchScope;
import consulo.language.psi.search.ReferencesSearch;
import com.intellij.testFramework.IdeaTestUtil;
import com.intellij.testFramework.PsiTestCase;
import com.intellij.testFramework.PsiTestUtil;
import consulo.application.util.query.Query;
import consulo.virtualFileSystem.VirtualFile;

import javax.swing.*;
import java.util.Collection;

/**
 * @author yole
 */
public abstract class FormPropertyUsageTest extends PsiTestCase {
  private VirtualFile myTestProjectRoot;

  @Override
  protected void setUp() throws Exception {
    super.setUp();

    String root = "/testData/binding/" + getTestName(true);
    PsiTestUtil.removeAllRoots(myModule, IdeaTestUtil.getMockJdk17());
    myTestProjectRoot = PsiTestUtil.createTestProjectStructure(myProject, myModule, root, myFilesToDelete);
  }

  @Override protected void tearDown() throws Exception {
    myTestProjectRoot = null;
    super.tearDown();
  }

  public void testClassUsage() {
    PsiClass psiClass = myJavaFacade.findClass(JButton.class.getName(), GlobalSearchScope.allScope(myProject));
    final Query<PsiReference> query = ReferencesSearch.search(psiClass);
    final Collection<PsiReference> result = query.findAll();
    assertEquals(1, result.size());
  }

  public void testFormPropertyUsage() {
    doPropertyUsageTest("test.properties");
  }

  public void testLocalizedPropertyUsage() {
    doPropertyUsageTest("test_ru.properties");
  }

  private void doPropertyUsageTest(final String propertyFileName) {
    PropertiesFile propFile = (PropertiesFile) myPsiManager.findFile(myTestProjectRoot.findChild(propertyFileName));
    assertNotNull(propFile);
    final Property prop = (Property)propFile.findPropertyByKey("key");
    assertNotNull(prop);
    final Query<PsiReference> query = ReferencesSearch.search(prop);
    final Collection<PsiReference> result = query.findAll();
    assertEquals(1, result.size());
    verifyReference(result, 0, "form.form", 960);
  }

  public void testPropertyFileUsage() {
    doPropertyFileUsageTest("test.properties");
  }

  public void testLocalizedPropertyFileUsage() {
     doPropertyFileUsageTest("test_ru.properties");
  }

  private void doPropertyFileUsageTest(final String fileName) {
    PropertiesFile propFile = (PropertiesFile) myPsiManager.findFile(myTestProjectRoot.findChild(fileName));
    assertNotNull(propFile);
    final Query<PsiReference> query = ReferencesSearch.search(propFile.getContainingFile());
    final Collection<PsiReference> result = query.findAll();
    assertEquals(1, result.size());
    verifyReference(result, 0, "form.form", 949);
  }

  private void verifyReference(final Collection<PsiReference> result, final int index, final String fileName, final int offset) {
    PsiReference ref = result.toArray(new PsiReference[result.size()]) [index];
    final PsiElement element = ref.getElement();
    assertEquals(fileName, element.getContainingFile().getName());
    int startOffset = element.getTextOffset() + ref.getRangeInElement().getStartOffset();
    assertEquals(offset, startOffset);
  }

}
