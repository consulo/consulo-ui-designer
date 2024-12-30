package com.intellij.uiDesigner.impl.binding;

import com.intellij.java.language.JavaLanguage;
import consulo.annotation.component.ExtensionImpl;
import consulo.language.Language;
import consulo.language.editor.annotation.Annotator;
import consulo.language.editor.annotation.AnnotatorFactory;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author VISTALL
 * @since 31/01/2023
 */
@ExtensionImpl
public class FormClassAnnotatorFactory implements AnnotatorFactory
{
	@Nullable
	@Override
	public Annotator createAnnotator()
	{
		return new FormClassAnnotator();
	}

	@Nonnull
	@Override
	public Language getLanguage()
	{
		return JavaLanguage.INSTANCE;
	}
}
