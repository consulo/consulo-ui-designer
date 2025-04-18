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

import com.intellij.uiDesigner.impl.radComponents.RadComponent;
import com.intellij.uiDesigner.lw.IProperty;
import com.intellij.uiDesigner.lw.IComponent;
import consulo.logging.Logger;
import org.jetbrains.annotations.NonNls;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.List;

/**
 * @author Anton Katilin
 * @author Vladimir Kondratyev
 */
public abstract class Property<T extends RadComponent, V> implements IProperty {
  public static final Property[] EMPTY_ARRAY=new Property[]{};

  private static final Logger LOG = Logger.getInstance(Property.class);

  /**
   * Parent property
   */
  private final Property myParent;
  /**
   * Propertie's name
   */
  private final String myName;

  public Property(final Property parent, @Nonnull @NonNls final String name) {
    myParent = parent;
    myName = name;
  }

  /**
   * @return property's name.
   */
  @Nonnull
  public final String getName() {
    return myName;
  }

  public Object getPropertyValue(final IComponent component) {
    //noinspection unchecked
    return getValue((T) component);
  }

  /**
   * This method can extract value of the property from the
   * instance of the RadComponent. This value is passed to the
   * PropertyRenderer and PropertyEditor.
   */
  public abstract V getValue(T component);

  /**
   * Do not invoke this method outside Property class, bacuse
   * <code>setValue(Component,Object)</code> does some additional work.
   * This method exists only for convenience.
   *
   * @see #setValue(RadComponent,Object)
   */
  protected abstract void setValueImpl(T component, V value) throws Exception;


  /**
   * Sets the <code>value</code> of the property. This method is invoked
   * after editing is complete.
   *
   * @param component component which property should be set
   * @param value new propertie's value
   *
   * @exception Exception if passed <code>value</code> cannot
   * be applied to the <code>component</code>. Note, the exception's
   * message will be shown to the user.
   */
  public final void setValue(final T component, final V value) throws Exception{
    setValueImpl(component, value);
    markTopmostModified(component, true);
    component.getDelegee().invalidate();
  }

  public final void setValueEx(T component, V value) {
    try {
      setValue(component, value);
    }
    catch(Exception ex) {
      LOG.error(ex);
    }
  }

  protected void markTopmostModified(final T component, final boolean modified) {
    Property topmostParent = this;
    while (topmostParent.getParent() != null) {
      topmostParent = topmostParent.getParent();
    }
    if (modified) {
      component.markPropertyAsModified(topmostParent);
    }
    else {
      component.removeModifiedProperty(topmostParent);
    }
  }

  /**
   * @return property which is the parent for this property.
   * The method can return <code>null</code> if the property
   * doesn't have parent.
   */
  @Nullable
  public final Property getParent() {
    return myParent;
  }

  /**
   * @return child properties.
   * @param component
   */
  @Nonnull
  public Property[] getChildren(final RadComponent component) {
    return EMPTY_ARRAY;
  }

  /**
   * @return property's renderer.
   */
  @Nonnull
  public abstract PropertyRenderer<V> getRenderer();

  /**
   * @return property's editor. The method allows to return <code>null</code>.
   * In this case property is not editable.
   */
  @Nullable
  public abstract PropertyEditor<V> getEditor();

  public boolean appliesTo(T component) {
    return true;
  }

  public boolean isModified(final T component) {
    return false;
  }

  public void resetValue(T component) throws Exception {
  }

  public boolean appliesToSelection(List<RadComponent> selection) {
    return true;
  }

  public boolean needRefreshPropertyList() {
    return false;
  }
}
