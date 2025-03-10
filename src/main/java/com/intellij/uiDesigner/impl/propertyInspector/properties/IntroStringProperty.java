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

import com.intellij.uiDesigner.core.SupportCode;
import com.intellij.uiDesigner.core.UIFormXmlConstants;
import com.intellij.uiDesigner.impl.FormEditingUtil;
import com.intellij.uiDesigner.impl.StringDescriptorManager;
import com.intellij.uiDesigner.impl.SwingProperties;
import com.intellij.uiDesigner.impl.XmlWriter;
import com.intellij.uiDesigner.impl.propertyInspector.IntrospectedProperty;
import com.intellij.uiDesigner.impl.propertyInspector.PropertyEditor;
import com.intellij.uiDesigner.impl.propertyInspector.PropertyRenderer;
import com.intellij.uiDesigner.impl.propertyInspector.editors.string.StringEditor;
import com.intellij.uiDesigner.impl.propertyInspector.renderers.StringRenderer;
import com.intellij.uiDesigner.impl.radComponents.RadComponent;
import com.intellij.uiDesigner.impl.radComponents.RadRootContainer;
import com.intellij.uiDesigner.impl.snapShooter.SnapshotContext;
import com.intellij.uiDesigner.lw.IProperty;
import com.intellij.uiDesigner.lw.StringDescriptor;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.util.lang.Comparing;
import org.jetbrains.annotations.NonNls;

import jakarta.annotation.Nonnull;
import javax.swing.*;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Locale;

/**
 * @author Anton Katilin
 * @author Vladimir Kondratyev
 */
public final class IntroStringProperty extends IntrospectedProperty<StringDescriptor> {
  private static final Logger LOG = Logger.getInstance("#com.intellij.uiDesigner.propertyInspector.IntroStringProperty");

  /**
   * value: HashMap<String, StringDescriptor>
   */
  @NonNls
  private static final String CLIENT_PROP_NAME_2_DESCRIPTOR = "name2descriptor";

  private final StringRenderer myRenderer;
  private StringEditor myEditor;
  private final Project myProject;

  public IntroStringProperty(final String name,
                             final Method readMethod,
                             final Method writeMethod,
                             final Project project,
                             final boolean storeAsClient) {
    super(name, readMethod, writeMethod, storeAsClient);
    myProject = project;
    myRenderer = new StringRenderer();
  }

  @Nonnull
  public PropertyRenderer<StringDescriptor> getRenderer() {
    return myRenderer;
  }

  public PropertyEditor<StringDescriptor> getEditor() {
    if (myEditor == null) {
      myEditor = new StringEditor(myProject, this);
    }
    return myEditor;
  }

  /**
   * @return per RadComponent map between string property name and its StringDescriptor value.
   */
  @Nonnull
  private static HashMap<String, StringDescriptor> getName2Descriptor(final RadComponent component){
    //noinspection unchecked
    HashMap<String, StringDescriptor> name2Descriptor = (HashMap<String, StringDescriptor>)component.getClientProperty(CLIENT_PROP_NAME_2_DESCRIPTOR);
    if(name2Descriptor == null){
      name2Descriptor = new HashMap<String,StringDescriptor>();
      component.putClientProperty(CLIENT_PROP_NAME_2_DESCRIPTOR, name2Descriptor);
    }
    return name2Descriptor;
  }

  /**
   * Utility method which merge together text and mnemonic at some position
   */
  private static String mergeTextAndMnemonic(String text, final int mnemonic, final int mnemonicIndex){
    if (text == null) {
      text = "";
    }
    final int index;
    if(
      mnemonicIndex >= 0 &&
      mnemonicIndex < text.length() &&
      Character.toUpperCase(text.charAt(mnemonicIndex)) == mnemonic
    ){
      // Index really corresponds to the mnemonic
      index = mnemonicIndex;
    }
    else{
      // Mnemonic exists but index is wrong
      index = -1;
    }

    final StringBuffer buffer = new StringBuffer(text);
    if(index != -1){
      buffer.insert(index, '&');
      // Quote all '&' except inserted one
      for(int i = buffer.length() - 1; i >= 0; i--){
        if(buffer.charAt(i) == '&' && i != index){
          buffer.insert(i, '&');
        }
      }
    }
    return buffer.toString();
  }

  /**
   * It's good that method is overriden here.
   *
   * @return instance of {@link StringDescriptor}
   */
  public StringDescriptor getValue(final RadComponent component) {
    // 1. resource bundle
    {
      final StringDescriptor descriptor = getName2Descriptor(component).get(getName());
      if(descriptor != null){
        return descriptor;
      }
    }

    // 2. plain value
    final JComponent delegee = component.getDelegee();
    return stringDescriptorFromValue(component, delegee);
  }

  private StringDescriptor stringDescriptorFromValue(final RadComponent component, final JComponent delegee) {
    final StringDescriptor result;
    if(SwingProperties.TEXT.equals(getName()) && (delegee instanceof JLabel)){
      final JLabel label = (JLabel)delegee;
      result = StringDescriptor.create(
        mergeTextAndMnemonic(label.getText(), label.getDisplayedMnemonic(), label.getDisplayedMnemonicIndex())
      );
    }
    else
    if(SwingProperties.TEXT.equals(getName()) && (delegee instanceof AbstractButton)){
      final AbstractButton button = (AbstractButton)delegee;
      result = StringDescriptor.create(
        mergeTextAndMnemonic(button.getText(), button.getMnemonic(), button.getDisplayedMnemonicIndex())
      );
    }
    else {
      if (component != null) {
        result = StringDescriptor.create((String) invokeGetter(component));
      }
      else {
        try {
          result = StringDescriptor.create((String) myReadMethod.invoke(delegee, EMPTY_OBJECT_ARRAY));
        }
        catch (Exception e) {
          throw new RuntimeException(e);
        }
      }
    }

    if (result != null) {
      // in this branch, the StringDescriptor is always a plain string, so resolve() is not necessary
      result.setResolvedValue(result.getValue());
    }
    return result;
  }

  protected void setValueImpl(final RadComponent component, final StringDescriptor value) throws Exception {
    // 1. Put value into map
    if(value == null || (value.getBundleName() == null && !value.isNoI18n())) {
      getName2Descriptor(component).remove(getName());
    }
    else{
      getName2Descriptor(component).put(getName(), value);
    }

    // 2. Apply real string value to JComponent peer
    final JComponent delegee = component.getDelegee();

    Locale locale = (Locale)component.getClientProperty(RadComponent.CLIENT_PROP_LOAD_TIME_LOCALE);
    if (locale == null) {
      RadRootContainer root = (RadRootContainer) FormEditingUtil.getRoot(component);
      if (root != null) {
        locale = root.getStringDescriptorLocale();
      }
    }
    final String resolvedValue = (value != null && value.getValue() != null)
                                 ? value.getValue()
                                 : StringDescriptorManager.getInstance(component.getModule()).resolve(value, locale);

    if (value != null) {
      value.setResolvedValue(resolvedValue);
    }

    if(SwingProperties.TEXT.equals(getName())) {
      final SupportCode.TextWithMnemonic textWithMnemonic = SupportCode.parseText(resolvedValue);
      if (delegee instanceof JLabel) {
        final JLabel label = (JLabel)delegee;
        label.setText(textWithMnemonic.myText);
        if(textWithMnemonic.myMnemonicIndex != -1){
          label.setDisplayedMnemonic(textWithMnemonic.getMnemonicChar());
          label.setDisplayedMnemonicIndex(textWithMnemonic.myMnemonicIndex);
        }
        else{
          label.setDisplayedMnemonic(0);
        }
      }
      else if (delegee instanceof AbstractButton) {
        final AbstractButton button = (AbstractButton)delegee;
        button.setText(textWithMnemonic.myText);
        if(textWithMnemonic.myMnemonicIndex != -1){
          button.setMnemonic(textWithMnemonic.getMnemonicChar());
          button.setDisplayedMnemonicIndex(textWithMnemonic.myMnemonicIndex);
        }
        else{
          button.setMnemonic(0);
        }
      }
      else {
        invokeSetter(component, resolvedValue);
      }
      checkUpdateBindingFromText(component, value, textWithMnemonic);
    }
    else{
      invokeSetter(component, resolvedValue);
    }
  }

  private static void checkUpdateBindingFromText(final RadComponent component, final StringDescriptor value, final SupportCode.TextWithMnemonic textWithMnemonic) {
    if (component.isLoadingProperties()) {
      return;
    }
    // only generate binding from text if default locale is active (IDEADEV-9427)
    if (value.getValue() == null) {
      RadRootContainer root = (RadRootContainer) FormEditingUtil.getRoot(component);
      Locale locale = root.getStringDescriptorLocale();
      if (locale != null && locale.getDisplayName().length() > 0) {
        return;
      }
    }

    BindingProperty.checkCreateBindingFromText(component, textWithMnemonic.myText);
    if (component.getDelegee() instanceof JLabel) {
      for(IProperty prop: component.getModifiedProperties()) {
        if (prop.getName().equals(SwingProperties.LABEL_FOR) && prop instanceof IntroComponentProperty) {
          ((IntroComponentProperty) prop).updateLabelForBinding(component);
        }
      }
    }
  }

  public boolean refreshValue(RadComponent component) {
    StringDescriptor descriptor = getValue(component);
    if (descriptor.getValue() != null) return false;
    String oldResolvedValue = descriptor.getResolvedValue();
    descriptor.setResolvedValue(null);
    try {
      setValueImpl(component, descriptor);
      return !Comparing.equal(oldResolvedValue, descriptor.getResolvedValue());
    }
    catch (Exception e) {
      LOG.error(e);
      return false;
    }
  }

  public void write(@Nonnull final StringDescriptor value, final XmlWriter writer) {
    writer.writeStringDescriptor(value,
                                 UIFormXmlConstants.ATTRIBUTE_VALUE,
                                 UIFormXmlConstants.ATTRIBUTE_RESOURCE_BUNDLE,
                                 UIFormXmlConstants.ATTRIBUTE_KEY);
  }

  @Override public void importSnapshotValue(final SnapshotContext context, final JComponent component, final RadComponent radComponent) {
    try {
      Object value = myReadMethod.invoke(component, EMPTY_OBJECT_ARRAY);
      if (value != null) {
        setValue(radComponent, stringDescriptorFromValue(null, component));
      }
    }
    catch (Exception e) {
      // ignore
    }
  }
}
