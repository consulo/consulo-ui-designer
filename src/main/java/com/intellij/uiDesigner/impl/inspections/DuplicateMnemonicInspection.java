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
package com.intellij.uiDesigner.impl.inspections;

import com.intellij.uiDesigner.impl.FormEditingUtil;
import com.intellij.uiDesigner.impl.StringDescriptorManager;
import com.intellij.uiDesigner.impl.SwingProperties;
import com.intellij.uiDesigner.impl.UIDesignerBundle;
import com.intellij.uiDesigner.core.SupportCode;
import com.intellij.uiDesigner.impl.designSurface.GuiEditor;
import com.intellij.uiDesigner.lw.*;
import com.intellij.uiDesigner.impl.quickFixes.QuickFix;
import com.intellij.uiDesigner.impl.radComponents.RadComponent;
import consulo.annotation.component.ExtensionImpl;
import consulo.module.Module;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * @author yole
 */
@ExtensionImpl
public class DuplicateMnemonicInspection extends BaseFormInspection {
  private static final ThreadLocal<HashMap<IRootContainer, MnemonicMap>> myContainerMnemonicMap = new ThreadLocal<HashMap<IRootContainer, MnemonicMap>>() {
    @Override
    protected HashMap<IRootContainer, MnemonicMap> initialValue() {
      return new HashMap<IRootContainer, MnemonicMap>();
    }
  };

  public DuplicateMnemonicInspection() {
    super("DuplicateMnemonic");
  }

  @Nonnull
  @Override
  public String getDisplayName() {
    return UIDesignerBundle.message("inspection.duplicate.mnemonics");
  }

  @Override public void startCheckForm(IRootContainer radRootContainer) {
    myContainerMnemonicMap.get().put(radRootContainer, new MnemonicMap());
  }

  @Override public void doneCheckForm(IRootContainer rootContainer) {
    myContainerMnemonicMap.get().remove(rootContainer);
  }

  protected void checkComponentProperties(consulo.module.Module module, IComponent component, FormErrorCollector collector) {
    SupportCode.TextWithMnemonic twm = getTextWithMnemonic(module, component);
    if (twm != null) {
      checkTextWithMnemonic(module, component, twm, collector);
    }
  }

  @Nullable
  public static SupportCode.TextWithMnemonic getTextWithMnemonic(final Module module, final IComponent component) {
    if (module.isDisposed()) return null;
    IProperty prop = FormInspectionUtil.findProperty(component, SwingProperties.TEXT);
    if (prop != null) {
      Object propValue = prop.getPropertyValue(component);
      if (propValue instanceof StringDescriptor) {
        StringDescriptor descriptor = (StringDescriptor)propValue;
        String value;
        if (component instanceof RadComponent) {
          value = StringDescriptorManager.getInstance(module).resolve((RadComponent) component, descriptor);
        }
        else {
          value = StringDescriptorManager.getInstance(module).resolve(descriptor, null);
        }
        SupportCode.TextWithMnemonic twm = SupportCode.parseText(value);
        if (twm.myMnemonicIndex >= 0 &&
            (FormInspectionUtil.isComponentClass(module, component, JLabel.class) || FormInspectionUtil.isComponentClass(module, component, AbstractButton.class))) {
          return twm;
        }
      }
    }
    return null;
  }

  private void checkTextWithMnemonic(final consulo.module.Module module,
                                     final IComponent component,
                                     final SupportCode.TextWithMnemonic twm,
                                     final FormErrorCollector collector) {
    IRootContainer root = FormEditingUtil.getRoot(component);
    MnemonicMap map = myContainerMnemonicMap.get().get(root);
    MnemonicKey key = buildMnemonicKey(twm, component);
    if (map.containsKey(key)) {
      IProperty prop = FormInspectionUtil.findProperty(component, SwingProperties.TEXT);
      IComponent oldComponent = map.get(key);
      collector.addError(getID(), component, prop,
                         UIDesignerBundle.message("inspection.duplicate.mnemonics.message",
                                                  FormInspectionUtil.getText(module, oldComponent),
                                                  FormInspectionUtil.getText(module, component)),
                         new EditorQuickFixProvider() {
                           public QuickFix createQuickFix(GuiEditor editor, RadComponent component) {
                             return new AssignMnemonicFix(editor, component,
                                                          UIDesignerBundle.message("inspection.duplicate.mnemonics.quickfix"));
                           }
                         });
    }
    else {
      map.put(key, component);
    }
  }

  private static MnemonicKey buildMnemonicKey(final SupportCode.TextWithMnemonic twm, final IComponent component) {
    List<Integer> exclusiveContainerStack = new ArrayList<Integer>();
    IContainer parent = component.getParentContainer();
    IComponent child = component;
    while(parent != null) {
      if (parent.areChildrenExclusive()) {
        exclusiveContainerStack.add(0, parent.indexOfComponent(child));
      }
      child = parent;
      parent = parent.getParentContainer();
    }
    return new MnemonicKey(twm.getMnemonicChar(), exclusiveContainerStack);
  }

  private static class MnemonicKey {
    private final char myMnemonicChar;
    private final List<Integer> myExclusiveContainerStack;

    public MnemonicKey(final char mnemonicChar, final List<Integer> exclusiveContainerStack) {
      myMnemonicChar = mnemonicChar;
      myExclusiveContainerStack = exclusiveContainerStack;
    }

    public boolean equals(final Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;

      final MnemonicKey that = (MnemonicKey)o;

      if (myMnemonicChar != that.myMnemonicChar) return false;
      if (!myExclusiveContainerStack.equals(that.myExclusiveContainerStack)) return false;

      return true;
    }

    public int hashCode() {
      int result;
      result = (int)myMnemonicChar;
      result = 31 * result + myExclusiveContainerStack.hashCode();
      return result;
    }
  }

  private static class MnemonicMap extends HashMap<MnemonicKey, IComponent> {
  }
}
