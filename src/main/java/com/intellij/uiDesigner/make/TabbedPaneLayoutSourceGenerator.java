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
package com.intellij.uiDesigner.make;

import com.intellij.java.language.psi.PsiKeyword;
import com.intellij.uiDesigner.lw.LwComponent;
import com.intellij.uiDesigner.lw.LwTabbedPane;

/**
 * @author yole
 */
public class TabbedPaneLayoutSourceGenerator extends LayoutSourceGenerator {
  public void generateComponentLayout(final LwComponent component,
                                      final FormSourceCodeGenerator generator,
                                      final String variable,
                                      final String parentVariable) {
    final LwTabbedPane.Constraints tabConstraints = (LwTabbedPane.Constraints)component.getCustomLayoutConstraints();
    if (tabConstraints == null){
      throw new IllegalArgumentException("tab constraints cannot be null: " + component.getId());
    }

    generator.startMethodCall(parentVariable, "addTab");
    generator.push(tabConstraints.myTitle);
    if (tabConstraints.myIcon != null || tabConstraints.myToolTip != null) {
      if (tabConstraints.myIcon == null) {
        generator.pushVar(PsiKeyword.NULL);
      }
      else {
        generator.pushIcon(tabConstraints.myIcon);
      }
    }
    generator.pushVar(variable);
    if (tabConstraints.myToolTip != null) {
      generator.push(tabConstraints.myToolTip);
    }
    generator.endMethod();

    int index = component.getParent().indexOfComponent(component);
    if (tabConstraints.myDisabledIcon != null) {
      generator.startMethodCall(parentVariable, "setDisabledIconAt");
      generator.push(index);
      generator.pushIcon(tabConstraints.myDisabledIcon);
      generator.endMethod();
    }
    if (!tabConstraints.myEnabled) {
      generator.startMethodCall(parentVariable, "setEnabledAt");
      generator.push(index);
      generator.push(tabConstraints.myEnabled);
      generator.endMethod();
    }
  }
}
