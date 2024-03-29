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
package com.intellij.uiDesigner.impl.propertyInspector;

import java.util.EventListener;

/**
 * @author Anton Katilin
 * @author Vladimir Kondratyev
 */
public interface PropertyEditorListener extends EventListener {
  /**
   * This method is invoked when user finished editing.
   * For example, user pressed "Enter" in text field or selected
   * somthing from combo box. This doesn't mean that editing
   * is cancelled. PropertyInspector, for example, applies
   * new value and continue editing.
   */
  void valueCommitted(PropertyEditor source, final boolean continueEditing, final boolean closeEditorOnError);

  /**
   * This method is invoked when user cancelled editing.
   * Foe example, user pressed "Esc" in the text field.
   */
  void editingCanceled(PropertyEditor source);

  /**
   * Editor can notify listeners that its preferred size changed.
   * In some cases (for example, during inplace editing) it's possible
   * to adjust size of the editor component.
   */
  void preferredSizeChanged(PropertyEditor source);
}
