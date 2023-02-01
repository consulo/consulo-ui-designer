/*
 * Copyright 2000-2011 JetBrains s.r.o.
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

import com.intellij.spellchecker.SpellCheckerManager;
import com.intellij.spellchecker.inspections.PlainTextSplitter;
import com.intellij.uiDesigner.impl.designSurface.GuiEditor;
import com.intellij.uiDesigner.impl.propertyInspector.properties.IntroStringProperty;
import com.intellij.uiDesigner.impl.quickFixes.PopupQuickFix;
import com.intellij.uiDesigner.impl.quickFixes.QuickFix;
import com.intellij.uiDesigner.impl.radComponents.RadComponent;
import com.intellij.uiDesigner.lw.IComponent;
import com.intellij.uiDesigner.lw.IProperty;
import com.intellij.uiDesigner.lw.StringDescriptor;
import consulo.annotation.component.ExtensionImpl;
import consulo.document.util.TextRange;
import consulo.module.Module;
import consulo.ui.ex.popup.*;

import java.util.List;
import java.util.function.Consumer;

/**
 * @author yole
 */
@ExtensionImpl
public class FormSpellCheckingInspection extends StringDescriptorInspection
{
	public static final String SHORT_NAME = "SpellCheckingInspection";

	public FormSpellCheckingInspection()
	{
		super(SHORT_NAME);
	}

	@Override
	protected void checkStringDescriptor(Module module,
										 final IComponent component,
										 final IProperty prop,
										 final StringDescriptor descriptor,
										 final FormErrorCollector collector)
	{
		final String value = descriptor.getResolvedValue();
		if(value == null)
		{
			return;
		}
		final SpellCheckerManager manager = SpellCheckerManager.getInstance(module.getProject());
		PlainTextSplitter.getInstance().split(value, TextRange.allOf(value), new Consumer<TextRange>()
		{
			@Override
			public void accept(TextRange textRange)
			{
				final String word = textRange.substring(value);
				if(manager.hasProblem(word))
				{
					final List<String> suggestions = manager.getSuggestions(value);
					if(suggestions.size() > 0 && prop instanceof IntroStringProperty)
					{
						EditorQuickFixProvider changeToProvider = new EditorQuickFixProvider()
						{
							@Override
							public QuickFix createQuickFix(final GuiEditor editor, final RadComponent component)
							{
								return new PopupQuickFix<String>(editor, "Change to...", component)
								{
									@Override
									public void run()
									{
										ListPopup popup = JBPopupFactory.getInstance().createListPopup(getPopupStep());
										popup.showUnderneathOf(component.getDelegee());
									}

									@Override
									public ListPopupStep<String> getPopupStep()
									{
										return new BaseListPopupStep<String>("Select Replacement", suggestions)
										{
											@Override
											public PopupStep onChosen(String selectedValue, boolean finalChoice)
											{
												FormInspectionUtil.updateStringPropertyValue(editor, component, (IntroStringProperty) prop, descriptor, selectedValue);
												return FINAL_CHOICE;
											}
										};
									}
								};
							}
						};
						EditorQuickFixProvider acceptProvider = new EditorQuickFixProvider()
						{
							@Override
							public QuickFix createQuickFix(final GuiEditor editor, RadComponent component)
							{
								return new QuickFix(editor, "Save '" + word + "' to dictionary", component)
								{
									@Override
									public void run()
									{
										manager.acceptWordAsCorrect(word, editor.getProject());
									}
								};
							}
						};
						collector.addError(getID(), component, prop, "Typo in word '" + word + "'", changeToProvider, acceptProvider);
					}
					else
					{
						collector.addError(getID(), component, prop, "Typo in word '" + word + "'");
					}
				}
			}
		});
	}
}
