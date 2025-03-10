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
package com.intellij.uiDesigner.impl.quickFixes;

import jakarta.annotation.Nonnull;

import com.intellij.uiDesigner.impl.designSurface.GuiEditor;
import com.intellij.uiDesigner.impl.radComponents.RadComponent;

import jakarta.annotation.Nullable;

/**
 * @author Anton Katilin
 * @author Vladimir Kondratyev
 */
public abstract class QuickFix {
  public static final QuickFix[] EMPTY_ARRAY = new QuickFix[]{};

  protected final GuiEditor myEditor;
  private final String myName;
  protected RadComponent myComponent;

  public QuickFix(@Nonnull final GuiEditor editor, @Nonnull final String name, @Nullable RadComponent component) {
    myEditor = editor;
    myName = name;
    myComponent = component;
  }

  /**
   * @return name of the quick fix.
   */
  @Nonnull
  public final String getName() {
    return myName;
  }

  public abstract void run();

  public RadComponent getComponent() {
    return myComponent;
  }
}
