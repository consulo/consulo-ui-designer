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

import com.intellij.uiDesigner.lw.*;
import com.intellij.uiDesigner.impl.propertyInspector.properties.BorderProperty;
import consulo.application.util.NotNullLazyValue;
import consulo.module.Module;
import org.jetbrains.annotations.NonNls;

import jakarta.annotation.Nonnull;

/**
 * @author yole
 */
public abstract class StringDescriptorInspection extends BaseFormInspection {
  private static final NotNullLazyValue<BorderProperty> myBorderProperty = new NotNullLazyValue<BorderProperty>() {
    @Nonnull
    @Override
    protected BorderProperty compute() {
      return new BorderProperty(null);
    }
  };

  public StringDescriptorInspection(@NonNls String inspectionKey) {
    super(inspectionKey);
  }

  protected void checkComponentProperties(Module module, IComponent component, FormErrorCollector collector) {
    for(IProperty prop: component.getModifiedProperties()) {
      Object propValue = prop.getPropertyValue(component);
      if (propValue instanceof StringDescriptor) {
        StringDescriptor descriptor = (StringDescriptor) propValue;
        checkStringDescriptor(module, component, prop, descriptor, collector);
      }
    }

    if (component instanceof IContainer) {
      IContainer container = (IContainer) component;
      StringDescriptor descriptor = container.getBorderTitle();
      if (descriptor != null) {
        checkStringDescriptor(module, component, myBorderProperty.getValue(), descriptor, collector);
      }
    }

    if (component.getParentContainer() instanceof ITabbedPane) {
      ITabbedPane parentTabbedPane = (ITabbedPane) component.getParentContainer();
      StringDescriptor descriptor = parentTabbedPane.getTabProperty(component, ITabbedPane.TAB_TITLE_PROPERTY);
      if (descriptor != null) {
        checkStringDescriptor(module, component, MockTabTitleProperty.INSTANCE, descriptor, collector);
      }
      descriptor = parentTabbedPane.getTabProperty(component, ITabbedPane.TAB_TOOLTIP_PROPERTY);
      if (descriptor != null) {
        checkStringDescriptor(module, component, MockTabToolTipProperty.INSTANCE, descriptor, collector);
      }
    }
  }

  protected abstract void checkStringDescriptor(final consulo.module.Module module,
                                                final IComponent component,
                                                final IProperty prop,
                                                final StringDescriptor descriptor,
                                                final FormErrorCollector collector);

  private static class MockTabTitleProperty implements IProperty {
    public static MockTabTitleProperty INSTANCE = new MockTabTitleProperty();

    public String getName() {
      return ITabbedPane.TAB_TITLE_PROPERTY;
    }

    public Object getPropertyValue(final IComponent component) {
      return null;
    }
  }

  private static class MockTabToolTipProperty implements IProperty {
    public static MockTabToolTipProperty INSTANCE = new MockTabToolTipProperty();

    public String getName() {
      return ITabbedPane.TAB_TOOLTIP_PROPERTY;
    }

    public Object getPropertyValue(final IComponent component) {
      return null;
    }
  }
}
