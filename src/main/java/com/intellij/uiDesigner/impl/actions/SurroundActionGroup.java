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

package com.intellij.uiDesigner.impl.actions;

import consulo.ui.ex.action.ActionGroup;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;

/**
 * @author yole
 */
public class SurroundActionGroup extends ActionGroup {
  private final AnAction[] myChildren;

  public SurroundActionGroup() {
    myChildren = new AnAction[4];
    myChildren [0] = new SurroundAction(JPanel.class.getName());
    myChildren [1] = new SurroundAction(JScrollPane.class.getName());
    myChildren [2] = new SurroundAction(JSplitPane.class.getName());
    myChildren [3] = new SurroundAction(JTabbedPane.class.getName());
  }

  @Nonnull
  public AnAction[] getChildren(@Nullable AnActionEvent e) {
    return myChildren;
  }
}
