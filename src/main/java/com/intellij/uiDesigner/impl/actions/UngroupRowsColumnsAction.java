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

package com.intellij.uiDesigner.impl.actions;

import com.intellij.uiDesigner.impl.CaptionSelection;
import com.intellij.uiDesigner.impl.UIDesignerBundle;
import com.jgoodies.forms.layout.FormLayout;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;

import jakarta.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

/**
 * @author yole
 */
public class UngroupRowsColumnsAction extends RowColumnAction
{
	public UngroupRowsColumnsAction()
	{
		super(UIDesignerBundle.message("action.ungroup.columns"), null, UIDesignerBundle.message("action.ungroup.rows"), null);
	}

	@RequiredUIAccess
	@Override
	public void update(@Nonnull final AnActionEvent e)
	{
		super.update(e);
		CaptionSelection selection = e.getData(CaptionSelection.DATA_KEY);
		if(selection != null)
		{
			e.getPresentation().setEnabled(selection.getContainer() != null && selection.getContainer().getLayout() instanceof FormLayout && GroupRowsColumnsAction.isGrouped(selection));
		}
	}

	@Override
	protected void actionPerformed(CaptionSelection selection)
	{
		FormLayout layout = (FormLayout) selection.getContainer().getLayout();
		int[][] oldGroups = selection.isRow() ? layout.getRowGroups() : layout.getColumnGroups();
		List<int[]> newGroups = new ArrayList<int[]>();
		int[] selInts = selection.getSelection();
		for(int[] group : oldGroups)
		{
			if(!GroupRowsColumnsAction.intersect(group, selInts))
			{
				newGroups.add(group);
			}
		}
		int[][] newGroupArray = newGroups.toArray(new int[newGroups.size()][]);
		if(selection.isRow())
		{
			layout.setRowGroups(newGroupArray);
		}
		else
		{
			layout.setColumnGroups(newGroupArray);
		}
	}
}
