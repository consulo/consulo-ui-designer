/*
 * Copyright 2000-2011 JetBrains s.r.o.
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

import consulo.virtualFileSystem.VirtualFile;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiField;
import consulo.language.psi.PsiFile;
import com.intellij.psi.search.ProjectScope;
import com.intellij.refactoring.MultiFileTestCase;
import consulo.language.editor.refactoring.rename.RenameProcessor;
import consulo.psi.PsiPackage;
import junit.framework.Assert;

/**
 * @author ven
 */
public abstract class RenameUIRelatedTest extends MultiFileTestCase {
  @Override
  protected String getTestRoot() {
    return "/renameUIRelated/";
  }

  @Override
  protected String getTestDataPath() {
    return "/testData";
  }

  @Override
  protected void prepareProject(VirtualFile rootDir) {
    //LanguageLevelProjectExtension.getInstance(myJavaFacade.getProject()).setLanguageLevel(LanguageLevel.JDK_1_5);
    super.prepareProject(rootDir);
  }

  public void testRenameClass() throws Exception {
    doTest(new PerformAction() {
      @Override
      public void performAction(VirtualFile rootDir, VirtualFile rootAfter) throws Exception {
        PsiClass aClass = myJavaFacade.findClass("UIClass", ProjectScope.getAllScope(myProject));
        Assert.assertNotNull(aClass);

        new RenameProcessor(myProject, aClass, "NewClass", true, true).run();
      }
    });
  }

  public void testRenameBoundField() throws Exception {
    doTest(new PerformAction() {
      @Override
      public void performAction(VirtualFile rootDir, VirtualFile rootAfter) throws Exception {
        PsiClass aClass = myJavaFacade.findClass("UIClass", ProjectScope.getAllScope(myProject));
        Assert.assertNotNull(aClass);
        final PsiField field = aClass.findFieldByName("UIField", false);
        Assert.assertNotNull(field);

        new RenameProcessor(myProject, field, "OtherName", true, true).run();
      }
    });
  }

  public void testRenamePackage() throws Exception {
    doTest(new PerformAction() {
      @Override
      public void performAction(VirtualFile rootDir, VirtualFile rootAfter) throws Exception {
        PsiPackage aPackage = JavaPsiFacade.getInstance(myPsiManager.getProject()).findPackage("gov");
        Assert.assertNotNull(aPackage);


        new RenameProcessor(myProject, aPackage, "org", true, true).run();
      }
    });
  }

  public void testRenamePackageNested() throws Exception {                     // IDEADEV-28864
    doTest(new PerformAction() {
      @Override
      public void performAction(VirtualFile rootDir, VirtualFile rootAfter) throws Exception {
        PsiPackage aPackage = JavaPsiFacade.getInstance(myPsiManager.getProject()).findPackage("org.withoutForms");
        Assert.assertNotNull(aPackage);

        new RenameProcessor(myProject, aPackage, "withForms", true, true).run();
      }
    });
  }

  public void testRenamePackageWithComponentClass() throws Exception {         // IDEADEV-5615
    doTest(new PerformAction() {
      @Override
      public void performAction(VirtualFile rootDir, VirtualFile rootAfter) throws Exception {
        PsiPackage aPackage = JavaPsiFacade.getInstance(myPsiManager.getProject()).findPackage("org.withoutForms");
        Assert.assertNotNull(aPackage);

        new RenameProcessor(myProject, aPackage, "withForms", true, true).run();
      }
    });
  }

  public void testRenameEnumConstant() throws Exception {
    doTest(new PerformAction() {
      @Override
      public void performAction(VirtualFile rootDir, VirtualFile rootAfter) throws Exception {
        PsiClass aClass = myJavaFacade.findClass("PropEnum", ProjectScope.getAllScope(myProject));
        Assert.assertNotNull(aClass);
        PsiField enumConstant = aClass.findFieldByName("valueB", false);
        Assert.assertNotNull(enumConstant);

        new RenameProcessor(myProject, enumConstant, "newValueB", true, true).run();
      }
    });
  }

  public void testRenameResourceBundle() throws Exception {
    doTest(new PerformAction() {
      @Override
      public void performAction(VirtualFile rootDir, VirtualFile rootAfter) throws Exception {
        PsiFile file = myPsiManager.findFile(rootDir.findChild("F1.properties"));
        Assert.assertNotNull(file);


        new RenameProcessor(myProject, file, "F2.properties", true, true).run();
      }
    });
  }

  public void testRenameNestedForm() throws Exception {
    doTest(new PerformAction() {
      @Override
      public void performAction(VirtualFile rootDir, VirtualFile rootAfter) throws Exception {
        PsiFile file = myPsiManager.findFile(rootDir.findChild("p1").findChild("Form1.form"));
        Assert.assertNotNull(file);


        new RenameProcessor(myProject, file, "Form2.form", true, true).run();
      }
    });
  }

  public void testRenameImage() throws Exception {
    doTest(new PerformAction() {
      @Override
      public void performAction(VirtualFile rootDir, VirtualFile rootAfter) throws Exception {
        PsiFile file = myPsiManager.findFile(rootDir.findFileByRelativePath("org/withoutForms/child/abstractClass.png"));
        Assert.assertNotNull(file);

        new RenameProcessor(myProject, file, "specificClass.png", true, true).run();
      }
    });
  }
}
