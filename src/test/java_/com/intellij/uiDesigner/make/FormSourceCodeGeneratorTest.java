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

package com.intellij.uiDesigner.make;

import com.intellij.uiDesigner.impl.make.FormSourceCodeGenerator;
import consulo.util.lang.StringUtil;
import consulo.ide.impl.idea.openapi.vfs.VfsUtil;
import consulo.virtualFileSystem.VirtualFile;
import com.intellij.psi.PsiClass;
import consulo.language.psi.PsiFile;
import com.intellij.psi.search.ProjectScope;
import com.intellij.testFramework.IdeaTestUtil;
import com.intellij.testFramework.PsiTestCase;
import com.intellij.testFramework.PsiTestUtil;
import consulo.undoRedo.CommandProcessor;

import java.io.IOException;

/**
 * @author yole
 */
public abstract class FormSourceCodeGeneratorTest extends PsiTestCase {
  private VirtualFile myTestProjectRoot;
  private FormSourceCodeGenerator myGenerator;

  @Override
  protected void setUp() throws Exception {
    super.setUp();

    String root = "/testData/sourceCodeGenerator/" + getTestName(true);
    PsiTestUtil.removeAllRoots(myModule, IdeaTestUtil.getMockJdk17());
    myTestProjectRoot = PsiTestUtil.createTestProjectStructure(myProject, myModule, root, myFilesToDelete);
    myGenerator = new FormSourceCodeGenerator(getProject());
  }

  @Override protected void tearDown() throws Exception {
    myGenerator = null;
    myTestProjectRoot = null;
    super.tearDown();
  }

  public void testSimple() throws IOException {
    doTest();
  }

  public void testCustomCreateComponent() throws IOException {
    doTest();
  }

  public void testCustomComponentReferencedInConstructor() throws IOException {
    doTest();
  }

  public void testMethodCallInConstructor() throws IOException {
    doTest();
  }

  public void testMultipleConstructors() throws IOException {
    doTest();
  }

  public void testConstructorsCallingThis() throws IOException {
    doTest();
  }

  public void testSuperCall() throws IOException {
    doTest();
  }

  public void testDuplicateSetupCall() throws IOException {
    doTest();
  }

  private void doTest() throws IOException {
    final VirtualFile form = myTestProjectRoot.findChild("Test.form");
    assertNotNull(form);
    CommandProcessor.getInstance().executeCommand(myProject, new Runnable() {
      @Override
      public void run() {
        try {
          myGenerator.generate(form);
        }
        catch (Exception e) {
          fail(e.getMessage());
        }
      }
    }, "", null);

    final PsiClass bindingTestClass = myJavaFacade.findClass("BindingTest", ProjectScope.getAllScope(myProject));
    assertNotNull(bindingTestClass);
    final VirtualFile testAfter = myTestProjectRoot.findChild("BindingTest.java.after");
    assertNotNull(testAfter);
    String expectedText = StringUtil.convertLineSeparators(VfsUtil.loadText(testAfter));
    final PsiFile psiFile = bindingTestClass.getContainingFile();
    assertNotNull(psiFile);
    final String text = StringUtil.convertLineSeparators(psiFile.getText());
    assertEquals(expectedText, text);
  }
}
