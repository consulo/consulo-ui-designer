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

package com.intellij.uiDesigner.impl.propertyInspector.properties;

import jakarta.annotation.Nonnull;

import com.intellij.uiDesigner.impl.radComponents.RadComponent;
import com.intellij.uiDesigner.impl.propertyInspector.Property;
import com.intellij.uiDesigner.impl.propertyInspector.PropertyRenderer;
import com.intellij.uiDesigner.impl.propertyInspector.PropertyEditor;
import com.intellij.uiDesigner.impl.propertyInspector.editors.BooleanEditor;
import com.intellij.uiDesigner.impl.propertyInspector.renderers.BooleanRenderer;
import org.jetbrains.annotations.NonNls;

/**
 * @author yole
 */
public abstract class AbstractBooleanProperty<T extends RadComponent> extends Property<T, Boolean> {
  private BooleanRenderer myRenderer;
  private BooleanEditor myEditor;
  private final boolean myDefaultValue;

  protected AbstractBooleanProperty(final Property parent, @NonNls final String name, final boolean defaultValue) {
    super(parent, name);
    myDefaultValue = defaultValue;
  }

  @Nonnull
  public PropertyRenderer<Boolean> getRenderer() {
    if (myRenderer == null) {
      myRenderer = new BooleanRenderer();
    }
    return myRenderer;
  }

  public PropertyEditor<Boolean> getEditor() {
    if (myEditor == null) {
      myEditor = new BooleanEditor();
    }
    return myEditor;
  }

  @Override public boolean isModified(final T component) {
    Boolean intValue = getValue(component);
    return intValue != null && intValue.booleanValue() != getDefaultValue(component);
  }

  @Override public void resetValue(T component) throws Exception {
    setValue(component, getDefaultValue(component));
  }

  protected boolean getDefaultValue(final T component) {
    return myDefaultValue;
  }
}
