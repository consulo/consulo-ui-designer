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
package com.intellij.uiDesigner.impl.propertyInspector.editors.string;

import com.intellij.java.language.psi.PsiMethod;
import com.intellij.java.language.util.TreeClassChooserFactory;
import com.intellij.lang.properties.IProperty;
import com.intellij.lang.properties.PropertiesFileType;
import com.intellij.lang.properties.PropertiesReferenceManager;
import com.intellij.lang.properties.PropertiesUtil;
import com.intellij.lang.properties.psi.PropertiesFile;
import com.intellij.lang.properties.psi.Property;
import com.intellij.uiDesigner.impl.FormEditingUtil;
import com.intellij.uiDesigner.impl.StringDescriptorManager;
import com.intellij.uiDesigner.impl.UIDesignerBundle;
import com.intellij.uiDesigner.impl.binding.FormReferenceProvider;
import com.intellij.uiDesigner.compiler.AsmCodeGenerator;
import com.intellij.uiDesigner.impl.designSurface.GuiEditor;
import com.intellij.uiDesigner.lw.StringDescriptor;
import consulo.application.ApplicationManager;
import consulo.application.CommonBundle;
import consulo.application.progress.ProgressManager;
import consulo.application.util.function.Processor;
import consulo.language.editor.ui.TreeFileChooser;
import consulo.language.editor.util.LanguageUndoUtil;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiReference;
import consulo.language.psi.scope.GlobalSearchScope;
import consulo.language.psi.search.ReferencesSearch;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.language.util.IncorrectOperationException;
import consulo.logging.Logger;
import consulo.module.Module;
import consulo.project.Project;
import consulo.ui.ex.InputValidator;
import consulo.ui.ex.awt.DialogWrapper;
import consulo.ui.ex.awt.Messages;
import consulo.ui.ex.awt.TextFieldWithBrowseButton;
import consulo.undoRedo.CommandProcessor;
import consulo.util.collection.ArrayUtil;
import consulo.virtualFileSystem.ReadonlyStatusHandler;
import consulo.virtualFileSystem.VirtualFile;
import org.jetbrains.annotations.NonNls;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.*;

/**
 * @author Anton Katilin
 * @author Vladimir Kondratyev
 */
public final class StringEditorDialog extends DialogWrapper
{
	private static final Logger LOG = Logger.getInstance(StringEditorDialog.class);

	@NonNls
	private static final String CARD_STRING = "string";
	@NonNls
	private static final String CARD_BUNDLE = "bundle";

	private final GuiEditor myEditor;
	/**
	 * Descriptor to be edited
	 */
	private StringDescriptor myValue;
	private final MyForm myForm;
	private final Locale myLocale;
	private boolean myDefaultBundleInitialized = false;

	StringEditorDialog(final Component parent,
					   final StringDescriptor descriptor,
					   @Nullable Locale locale,
					   final GuiEditor editor)
	{
		super(parent, true);
		myLocale = locale;

		myEditor = editor;

		myForm = new MyForm();
		setTitle(UIDesignerBundle.message("title.edit.text"));
		setValue(descriptor);

		init(); /* run initialization proc */
	}

	protected String getDimensionServiceKey()
	{
		return getClass().getName();
	}

	public JComponent getPreferredFocusedComponent()
	{
		if(myForm.myRbString.isSelected())
		{
			return myForm.myTfValue;
		}
		else
		{
			return super.getPreferredFocusedComponent();
		}
	}

	@Override
	protected void doOKAction()
	{
		if(myForm.myRbResourceBundle.isSelected())
		{
			final StringDescriptor descriptor = getDescriptor();
			if(descriptor != null && descriptor.getKey().length() > 0)
			{
				final String value = myForm.myTfRbValue.getText();
				final PropertiesFile propFile = getPropertiesFile(descriptor);
				if(propFile != null && propFile.findPropertyByKey(descriptor.getKey()) == null)
				{
					saveCreatedProperty(propFile, descriptor.getKey(), value, myEditor.getPsiFile());
				}
				else
				{
					final String newKeyName = saveModifiedPropertyValue(myEditor.getModule(), descriptor, myLocale, value, myEditor.getPsiFile());
					if(newKeyName != null)
					{
						myForm.myTfKey.setText(newKeyName);
					}
				}
			}
		}
		super.doOKAction();
	}

	private PropertiesFile getPropertiesFile(final StringDescriptor descriptor)
	{
		final PropertiesReferenceManager manager = PropertiesReferenceManager.getInstance(myEditor.getProject());
		return manager.findPropertiesFile(myEditor.getModule(), descriptor.getDottedBundleName(), myLocale);
	}

	@Nullable
	public static String saveModifiedPropertyValue(final Module module, final StringDescriptor descriptor,
												   final Locale locale, final String editedValue, final PsiFile formFile)
	{
		final PropertiesReferenceManager manager = PropertiesReferenceManager.getInstance(module.getProject());
		final PropertiesFile propFile = manager.findPropertiesFile(module, descriptor.getDottedBundleName(), locale);
		if(propFile != null)
		{
			final IProperty propertyByKey = propFile.findPropertyByKey(descriptor.getKey());
			if(propertyByKey instanceof Property && !editedValue.equals(propertyByKey.getValue()))
			{
				final Collection<PsiReference> references = findPropertyReferences((Property) propertyByKey, module);

				String newKeyName = null;
				if(references.size() > 1)
				{
					final int rc = Messages.showYesNoCancelDialog(module.getProject(), UIDesignerBundle.message("edit.text.multiple.usages",
							propertyByKey.getUnescapedKey(), references.size()),
							UIDesignerBundle.message("edit.text.multiple.usages.title"),
							UIDesignerBundle.message("edit.text.change.all"),
							UIDesignerBundle.message("edit.text.make.unique"),
							CommonBundle.getCancelButtonText(),
							Messages.getWarningIcon());
					if(rc == 2)
					{
						return null;
					}
					if(rc == 1)
					{
						newKeyName = promptNewKeyName(module.getProject(), propFile, descriptor.getKey());
						if(newKeyName == null)
						{
							return null;
						}
					}
				}
				final ReadonlyStatusHandler.OperationStatus operationStatus =
						ReadonlyStatusHandler.getInstance(module.getProject()).ensureFilesWritable(propFile.getVirtualFile());
				if(operationStatus.hasReadonlyFiles())
				{
					return null;
				}
				final String newKeyName1 = newKeyName;
				CommandProcessor.getInstance().executeCommand(
						module.getProject(),
						new Runnable()
						{
							public void run()
							{
								LanguageUndoUtil.markPsiFileForUndo(formFile);
								ApplicationManager.getApplication().runWriteAction(new Runnable()
								{
									public void run()
									{
										PsiDocumentManager.getInstance(module.getProject()).commitAllDocuments();
										try
										{
											if(newKeyName1 != null)
											{
												propFile.addProperty(newKeyName1, editedValue);
											}
											else
											{
												final IProperty propertyByKey = propFile.findPropertyByKey(descriptor.getKey());
												if(propertyByKey != null)
												{
													propertyByKey.setValue(editedValue);
												}
											}
										}
										catch(consulo.language.util.IncorrectOperationException e)
										{
											LOG.error(e);
										}
									}
								});
							}
						}, UIDesignerBundle.message("command.update.property"), null);
				return newKeyName;
			}
		}
		return null;
	}

	private static Collection<PsiReference> findPropertyReferences(final Property pproperty, final consulo.module.Module module)
	{
		final Collection<PsiReference> references = Collections.synchronizedList(new ArrayList<PsiReference>());
		ProgressManager.getInstance().runProcessWithProgressSynchronously(
				new Runnable()
				{
					public void run()
					{
						ReferencesSearch.search(pproperty).forEach(new Processor<PsiReference>()
						{
							public boolean process(final PsiReference psiReference)
							{
								PsiMethod method = PsiTreeUtil.getParentOfType(psiReference.getElement(), PsiMethod.class);
								if(method == null || !AsmCodeGenerator.SETUP_METHOD_NAME.equals(method.getName()))
								{
									references.add(psiReference);
								}
								return true;
							}
						});
					}
				}, UIDesignerBundle.message("edit.text.searching.references"), false, module.getProject()
		);
		return references;
	}

	private static String promptNewKeyName(final Project project, final PropertiesFile propFile, final String key)
	{
		String newName;
		int index = 0;
		do
		{
			index++;
			newName = key + index;
		}
		while(propFile.findPropertyByKey(newName) != null);

		InputValidator validator = new InputValidator()
		{
			public boolean checkInput(String inputString)
			{
				return inputString.length() > 0 && propFile.findPropertyByKey(inputString) == null;
			}

			public boolean canClose(String inputString)
			{
				return checkInput(inputString);
			}
		};
		return Messages.showInputDialog(project, UIDesignerBundle.message("edit.text.unique.key.prompt"),
				UIDesignerBundle.message("edit.text.multiple.usages.title"),
				Messages.getQuestionIcon(), newName, validator);
	}

	public static boolean saveCreatedProperty(final PropertiesFile bundle, final String name, final String value,
											  final PsiFile formFile)
	{
		final ReadonlyStatusHandler.OperationStatus operationStatus =
				ReadonlyStatusHandler.getInstance(bundle.getProject()).ensureFilesWritable(bundle.getVirtualFile());
		if(operationStatus.hasReadonlyFiles())
		{
			return false;
		}
		CommandProcessor.getInstance().executeCommand(
				bundle.getProject(),
				new Runnable()
				{
					public void run()
					{
						LanguageUndoUtil.markPsiFileForUndo(formFile);
						ApplicationManager.getApplication().runWriteAction(new Runnable()
						{
							public void run()
							{
								try
								{
									bundle.addProperty(name, value);
								}
								catch(IncorrectOperationException e1)
								{
									LOG.error(e1);
								}
							}
						});
					}
				}, UIDesignerBundle.message("command.create.property"), null);
		return true;
	}

	/**
	 * @return edited descriptor. If initial descriptor was <code>null</code>
	 * and user didn't change anything then this method returns <code>null</code>.
	 */
	@Nullable
	StringDescriptor getDescriptor()
	{
		if(myForm.myRbString.isSelected())
		{ // plain value
			final String value = myForm.myTfValue.getText();
			if(myValue == null && value.length() == 0)
			{
				return null;
			}
			else
			{
				final StringDescriptor stringDescriptor = StringDescriptor.create(value);
				stringDescriptor.setNoI18n(myForm.myNoI18nCheckbox.isSelected());
				return stringDescriptor;
			}
		}
		else
		{ // bundled value
			final String bundleName = myForm.myTfBundleName.getText();
			final String key = myForm.myTfKey.getText();
			return new StringDescriptor(bundleName, key);
		}
	}

	/**
	 * Applies specified descriptor to the proper card
	 */
	private void setValue(final StringDescriptor descriptor)
	{
		myValue = descriptor;
		final CardLayout cardLayout = (CardLayout) myForm.myCardHolder.getLayout();
		if(descriptor == null || descriptor.getValue() != null)
		{ // trivial descriptor
			myForm.myRbString.setSelected(true);
			myForm.showStringDescriptor(descriptor);
			cardLayout.show(myForm.myCardHolder, CARD_STRING);
		}
		else
		{ // bundled property
			myForm.myRbResourceBundle.setSelected(true);
			myForm.showResourceBundleDescriptor(descriptor);
			cardLayout.show(myForm.myCardHolder, CARD_BUNDLE);
		}
	}

	protected JComponent createCenterPanel()
	{
		return myForm.myPanel;
	}

	private final class MyForm
	{
		private JRadioButton myRbString;
		private JRadioButton myRbResourceBundle;
		private JPanel myCardHolder;
		private JPanel myPanel;
		private JTextArea myTfValue;
		private JCheckBox myNoI18nCheckbox;
		private TextFieldWithBrowseButton myTfBundleName;
		private TextFieldWithBrowseButton myTfKey;
		private JTextField myTfRbValue;
		private JLabel myLblKey;
		private JLabel myLblBundleName;

		public MyForm()
		{
			myRbString.addActionListener(
					new ActionListener()
					{
						public void actionPerformed(final ActionEvent e)
						{
							CardLayout cardLayout = (CardLayout) myCardHolder.getLayout();
							cardLayout.show(myCardHolder, CARD_STRING);
						}
					}
			);

			myRbResourceBundle.addActionListener(
					new ActionListener()
					{
						public void actionPerformed(final ActionEvent e)
						{
							if(!myDefaultBundleInitialized)
							{
								myDefaultBundleInitialized = true;
								Set<String> bundleNames = FormEditingUtil.collectUsedBundleNames(myEditor.getRootContainer());
								if(bundleNames.size() > 0)
								{
									myTfBundleName.setText(ArrayUtil.toStringArray(bundleNames)[0]);
								}
							}
							CardLayout cardLayout = (CardLayout) myCardHolder.getLayout();
							cardLayout.show(myCardHolder, CARD_BUNDLE);
						}
					}
			);

			setupResourceBundleCard();
		}

		private void setupResourceBundleCard()
		{
			// Enable keyboard pressing
			myTfBundleName.registerKeyboardAction(
					new AbstractAction()
					{
						public void actionPerformed(final ActionEvent e)
						{
							myTfBundleName.getButton().doClick();
						}
					},
					KeyStroke.getKeyStroke(myLblBundleName.getDisplayedMnemonic(), KeyEvent.ALT_DOWN_MASK),
					JComponent.WHEN_IN_FOCUSED_WINDOW
			);

			myTfBundleName.addActionListener(
					new ActionListener()
					{
						public void actionPerformed(final ActionEvent e)
						{
							Project project = myEditor.getProject();
							final String bundleNameText = myTfBundleName.getText().replace('/', '.');
							PropertiesFile file = PropertiesUtil.getPropertiesFile(bundleNameText, myEditor.getModule(), myLocale);
							PsiFile initialPropertiesFile = file == null ? null : file.getContainingFile();
							final GlobalSearchScope moduleScope = GlobalSearchScope.moduleWithDependenciesScope(myEditor.getModule());
							TreeFileChooser fileChooser = TreeClassChooserFactory.getInstance(project).createFileChooser(UIDesignerBundle.message("title.choose.properties.file"),
									initialPropertiesFile, PropertiesFileType.INSTANCE,
									file1 -> {
										final VirtualFile virtualFile = file1.getVirtualFile();
										return virtualFile != null && moduleScope.contains(virtualFile);
									});
							fileChooser.showDialog();
							PropertiesFile propertiesFile = (PropertiesFile) fileChooser.getSelectedFile();
							if(propertiesFile == null)
							{
								return;
							}
							final String bundleName = FormReferenceProvider.getBundleName(propertiesFile);
							if(bundleName == null)
							{
								return;
							}
							myTfBundleName.setText(bundleName);
						}
					}
			);

			// Enable keyboard pressing
			myTfKey.registerKeyboardAction(
					new AbstractAction()
					{
						public void actionPerformed(final ActionEvent e)
						{
							myTfKey.getButton().doClick();
						}
					},
					KeyStroke.getKeyStroke(myLblKey.getDisplayedMnemonic(), KeyEvent.ALT_DOWN_MASK),
					JComponent.WHEN_IN_FOCUSED_WINDOW
			);

			myTfKey.addActionListener(
					new ActionListener()
					{
						public void actionPerformed(final ActionEvent e)
						{
							// 1. Check that bundle exist. Otherwise we cannot show key chooser
							final String bundleName = myTfBundleName.getText();
							if(bundleName.length() == 0)
							{
								Messages.showErrorDialog(
										UIDesignerBundle.message("error.specify.bundle.name"),
										CommonBundle.getErrorTitle()
								);
								return;
							}
							final PropertiesReferenceManager manager = PropertiesReferenceManager.getInstance(myEditor.getProject());
							final PropertiesFile bundle = manager.findPropertiesFile(myEditor.getModule(), bundleName.replace('/', '.'), myLocale);
							if(bundle == null)
							{
								Messages.showErrorDialog(
										UIDesignerBundle.message("error.bundle.does.not.exist", bundleName),
										CommonBundle.getErrorTitle()
								);
								return;
							}

							// 2. Show key chooser
							final KeyChooserDialog dialog = new KeyChooserDialog(
									myTfKey,
									bundle,
									bundleName,
									myTfKey.getText(), // key to preselect
									myEditor
							);
							dialog.show();
							if(!dialog.isOK())
							{
								return;
							}

							// 3. Apply new key/value
							final StringDescriptor descriptor = dialog.getDescriptor();
							if(descriptor == null)
							{
								return;
							}
							myTfKey.setText(descriptor.getKey());
							myTfRbValue.setText(descriptor.getResolvedValue());
						}
					}
			);
		}

		public void showStringDescriptor(@Nullable final StringDescriptor descriptor)
		{
			myTfValue.setText(StringDescriptorManager.getInstance(myEditor.getModule()).resolve(descriptor, myLocale));
			myNoI18nCheckbox.setSelected(descriptor != null && descriptor.isNoI18n());
		}

		public void showResourceBundleDescriptor(@Nonnull final StringDescriptor descriptor)
		{
			final String key = descriptor.getKey();
			LOG.assertTrue(key != null);
			myTfBundleName.setText(descriptor.getBundleName());
			myTfKey.setText(key);
			myTfRbValue.setText(StringDescriptorManager.getInstance(myEditor.getModule()).resolve(descriptor, myLocale));
		}
	}
}
