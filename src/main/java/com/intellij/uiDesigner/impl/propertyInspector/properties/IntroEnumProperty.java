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

import com.intellij.uiDesigner.impl.propertyInspector.IntrospectedProperty;
import com.intellij.uiDesigner.impl.propertyInspector.PropertyEditor;
import com.intellij.uiDesigner.impl.propertyInspector.PropertyRenderer;
import com.intellij.uiDesigner.impl.propertyInspector.editors.EnumEditor;
import com.intellij.uiDesigner.impl.propertyInspector.renderers.LabelPropertyRenderer;
import jakarta.annotation.Nonnull;

import java.lang.reflect.Method;

/**
 * @author yole
 */
public class IntroEnumProperty extends IntrospectedProperty<Enum> {
  private final Class myEnumClass;
  private LabelPropertyRenderer<Enum> myRenderer;
  private EnumEditor myEditor;

  public IntroEnumProperty(final String name, final Method readMethod, final Method writeMethod, final boolean storeAsClient,
                           Class enumClass) {
    super(name, readMethod, writeMethod, storeAsClient);
    myEnumClass = enumClass;
  }

  @Nonnull
  public PropertyRenderer<Enum> getRenderer() {
    if (myRenderer == null) {
      myRenderer = new LabelPropertyRenderer<Enum>();
    }
    return myRenderer;
  }

  public PropertyEditor<Enum> getEditor() {
    if (myEditor == null) {
      myEditor = new EnumEditor(myEnumClass);
    }
    return myEditor;
  }
}
