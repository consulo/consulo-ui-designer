/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package com.intellij.uiDesigner.impl.actions;

import java.util.List;

import consulo.ui.ex.action.AnActionEvent;
import com.intellij.uiDesigner.impl.designSurface.GuiEditor;
import com.intellij.uiDesigner.impl.radComponents.RadComponent;

/**
 * @author Alexander Lobas
 */
public class ShowFormSourceAction extends AbstractGuiEditorAction
{
	@Override
	protected void actionPerformed(GuiEditor editor, List<RadComponent> selection, AnActionEvent e)
	{
		editor.showFormSource();
	}
}