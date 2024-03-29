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

/*
 * Created by IntelliJ IDEA.
 * User: yole
 * Date: 11.10.2006
 * Time: 16:03:23
 */
package com.intellij.uiDesigner.binding;

import consulo.language.psi.search.ReferencesSearch;
import consulo.virtualFileSystem.VirtualFile;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiEnumConstant;
import com.intellij.psi.PsiField;
import com.intellij.psi.search.ProjectScope;
import com.intellij.testFramework.IdeaTestUtil;
import com.intellij.testFramework.PsiTestCase;
import com.intellij.testFramework.PsiTestUtil;
import consulo.util.lang.IncorrectOperationException;
import consulo.undoRedo.CommandProcessor;

public abstract class FormEnumUsageTest extends PsiTestCase {
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

  public void testEnumUsage() throws IncorrectOperationException {
    //LanguageLevelProjectExtension.getInstance(myJavaFacade.getProject()).setLanguageLevel(LanguageLevel.JDK_1_5);
    CommandProcessor.getInstance().executeCommand(myProject, new Runnable() {
      @Override
      public void run() {
        try {
          createFile(myModule, myTestProjectRoot, "PropEnum.java", "public enum PropEnum { valueA, valueB }");
          createFile(myModule, myTestProjectRoot, "CustomComponent.java",
                     "public class CustomComponent extends JLabel { private PropEnum e; public PropEnum getE() { return e; } public void setE(E newE) { e = newE; } }");
        }
        catch (Exception e) {
          fail(e.getMessage());
        }
      }
    }, "", null);

    PsiClass enumClass = myJavaFacade.findClass("PropEnum", ProjectScope.getAllScope(myProject));
    PsiField valueBField = enumClass.findFieldByName("valueB", false);
    assertNotNull(valueBField);
    assertTrue(valueBField instanceof PsiEnumConstant);
    final PsiClass componentClass = myJavaFacade.findClass("CustomComponent", ProjectScope.getAllScope(myProject));
    assertNotNull(componentClass);

    assertEquals(1, ReferencesSearch.search(componentClass).findAll().size());

    assertEquals(1, ReferencesSearch.search(valueBField).findAll().size());
  }

}
