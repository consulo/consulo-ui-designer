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
package com.intellij.uiDesigner.impl.radComponents;

import jakarta.annotation.Nonnull;

import com.intellij.uiDesigner.impl.ModuleProvider;
import com.intellij.uiDesigner.impl.XmlWriter;
import com.intellij.uiDesigner.impl.palette.Palette;

/**
 * @author Anton Katilin
 * @author Vladimir Kondratyev
 */
public class RadAtomicComponent extends RadComponent {
  public RadAtomicComponent(final ModuleProvider module, final Class aClass, final String id){
    super(module, aClass, id);
  }

  public RadAtomicComponent(@Nonnull final Class aClass, @Nonnull final String id, final Palette palette) {
    super(null, aClass, id, palette);
  }

  public void write(final XmlWriter writer) {
    writer.startElement("component");
    try{
      writeId(writer);
      writeClass(writer);
      writeBinding(writer);
      writeConstraints(writer);
      writeProperties(writer);
    }finally{
      writer.endElement(); // component
    }
  }
}
