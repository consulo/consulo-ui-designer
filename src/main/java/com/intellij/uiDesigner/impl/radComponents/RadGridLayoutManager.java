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

import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.UIFormXmlConstants;
import com.intellij.uiDesigner.impl.XmlWriter;
import com.intellij.uiDesigner.impl.actions.DeleteAction;
import com.intellij.uiDesigner.impl.actions.InsertAfterAction;
import com.intellij.uiDesigner.impl.actions.InsertBeforeAction;
import com.intellij.uiDesigner.impl.actions.SplitAction;
import com.intellij.uiDesigner.impl.propertyInspector.Property;
import com.intellij.uiDesigner.impl.propertyInspector.properties.*;
import consulo.project.Project;
import consulo.ui.ex.action.ActionGroup;
import consulo.ui.ex.action.DefaultActionGroup;

import javax.swing.*;
import java.awt.*;
import java.util.List;

/**
 * @author yole
 */
public class RadGridLayoutManager extends RadAbstractGridLayoutManager
{
	private GridLayoutColumnProperties myPropertiesPanel;

	public String getName()
	{
		return UIFormXmlConstants.LAYOUT_INTELLIJ;
	}

	public LayoutManager createLayout()
	{
		return new GridLayoutManager(1, 1);
	}

	@Override
	protected void changeLayoutFromGrid(final RadContainer container, final List<RadComponent> contents, final List<Boolean> canRowsGrow,
										final List<Boolean> canColumnsGrow)
	{
		int rowCount = Math.max(1, canRowsGrow.size());
		int columnCount = Math.max(1, canColumnsGrow.size());
		container.setLayoutManager(this, new GridLayoutManager(rowCount, columnCount));
	}

	@Override
	protected void changeLayoutFromIndexed(final RadContainer container, final List<RadComponent> components)
	{
		container.setLayoutManager(this, new GridLayoutManager(1, Math.max(1, components.size())));
	}

	public void writeLayout(final XmlWriter writer, final RadContainer radContainer)
	{
		GridLayoutManager layout = (GridLayoutManager) radContainer.getLayout();

		writer.addAttribute("row-count", layout.getRowCount());
		writer.addAttribute("column-count", layout.getColumnCount());

		writer.addAttribute(UIFormXmlConstants.ATTRIBUTE_SAME_SIZE_HORIZONTALLY, layout.isSameSizeHorizontally());
		writer.addAttribute(UIFormXmlConstants.ATTRIBUTE_SAME_SIZE_VERTICALLY, layout.isSameSizeVertically());

		RadXYLayoutManager.INSTANCE.writeLayout(writer, radContainer);
	}

	public void addComponentToContainer(final RadContainer container, final RadComponent component, final int index)
	{
		super.addComponentToContainer(container, component, index);
		container.getDelegee().add(component.getDelegee(), component.getConstraints());
	}

	@Override
	protected void updateConstraints(RadComponent component)
	{
		GridLayoutManager layout = (GridLayoutManager) component.getParent().getLayout();
		final GridConstraints radConstraints = component.getConstraints();
		final GridConstraints delegeeConstraints = layout.getConstraintsForComponent(component.getDelegee());
		if(radConstraints != delegeeConstraints)
		{
			delegeeConstraints.restore(radConstraints);
		}
		super.updateConstraints(component);
	}

	public void writeChildConstraints(final XmlWriter writer, final RadComponent child)
	{
		writeGridConstraints(writer, child);
	}

	@Override
	public Property[] getContainerProperties(final Project project)
	{
		return new Property[]{
				MarginProperty.getInstance(project),
				HGapProperty.getInstance(project),
				VGapProperty.getInstance(project),
				SameSizeHorizontallyProperty.getInstance(project),
				SameSizeVerticallyProperty.getInstance(project)
		};
	}

	@Override
	public Property[] getComponentProperties(final Project project, final RadComponent component)
	{
		return new Property[]{
				HSizePolicyProperty.getInstance(project),
				VSizePolicyProperty.getInstance(project),
				HorzAlignProperty.getInstance(project),
				VertAlignProperty.getInstance(project),
				IndentProperty.getInstance(project),
				UseParentLayoutProperty.getInstance(project),
				MinimumSizeProperty.getInstance(project),
				PreferredSizeProperty.getInstance(project),
				MaximumSizeProperty.getInstance(project)
		};
	}

	@Override
	public int getGridRowCount(RadContainer container)
	{
		return ((GridLayoutManager) container.getLayout()).getRowCount();
	}

	@Override
	public int getGridColumnCount(RadContainer container)
	{
		return ((GridLayoutManager) container.getLayout()).getColumnCount();
	}

	@Override
	public int[] getHorizontalGridLines(RadContainer container)
	{
		GridLayoutManager grid = (GridLayoutManager) container.getLayout();
		return grid.getHorizontalGridLines();
	}

	@Override
	public int[] getVerticalGridLines(RadContainer container)
	{
		GridLayoutManager grid = (GridLayoutManager) container.getLayout();
		return grid.getVerticalGridLines();
	}

	public int[] getGridCellCoords(RadContainer container, boolean isRow)
	{
		GridLayoutManager grid = (GridLayoutManager) container.getLayout();
		return isRow ? grid.getYs() : grid.getXs();
	}

	public int[] getGridCellSizes(RadContainer container, boolean isRow)
	{
		GridLayoutManager grid = (GridLayoutManager) container.getLayout();
		return isRow ? grid.getHeights() : grid.getWidths();
	}

	@Override
	public CustomPropertiesPanel getRowColumnPropertiesPanel(RadContainer container, boolean isRow, int[] selectedIndices)
	{
		if(myPropertiesPanel == null)
		{
			myPropertiesPanel = new GridLayoutColumnProperties();
		}
		myPropertiesPanel.showProperties(container, isRow, selectedIndices);
		return myPropertiesPanel;
	}

	@Override
	public ActionGroup getCaptionActions()
	{
		DefaultActionGroup group = new DefaultActionGroup();
		group.add(new InsertBeforeAction());
		group.add(new InsertAfterAction());
		group.add(new SplitAction());
		group.add(new DeleteAction());
		return group;
	}

	@Override
	public boolean canCellGrow(RadContainer container, boolean isRow, int cellIndex)
	{
		final GridLayoutManager gridLayoutManager = ((GridLayoutManager) container.getLayout());
		int maxSizePolicy = 0;
		for(int i = 0; i < gridLayoutManager.getCellCount(isRow); i++)
		{
			maxSizePolicy = Math.max(maxSizePolicy, gridLayoutManager.getCellSizePolicy(isRow, i));
		}
		return gridLayoutManager.getCellSizePolicy(isRow, cellIndex) == maxSizePolicy;
	}

	public void processCellResized(RadContainer container, final boolean isRow, final int cell, final int newSize)
	{
		int cellCount = isRow ? container.getGridRowCount() : container.getGridColumnCount();
		if(container.getParent().isXY() && cell == cellCount - 1)
		{
			processRootContainerResize(container, isRow, newSize);
		}
		else
		{
			for(RadComponent component : container.getComponents())
			{
				GridConstraints c = component.getConstraints();
				if(c.getCell(isRow) == cell && c.getSpan(isRow) == 1)
				{
					Dimension preferredSize = new Dimension(c.myPreferredSize);
					if(isRow)
					{
						preferredSize.height = newSize;
						if(preferredSize.width == -1)
						{
							preferredSize.width = component.getDelegee().getPreferredSize().width;
						}
					}
					else
					{
						preferredSize.width = newSize;
						if(preferredSize.height == -1)
						{
							preferredSize.height = component.getDelegee().getPreferredSize().height;
						}
					}
					PreferredSizeProperty.getInstance(container.getProject()).setValueEx(component, preferredSize);
				}
			}
		}
	}

	private static void processRootContainerResize(final RadContainer container, final boolean isRow, final int newSize)
	{
		final JComponent parentDelegee = container.getDelegee();
		Dimension containerSize = parentDelegee.getSize();
		if(isRow)
		{
			containerSize.height = newSize + parentDelegee.getBounds().y;
		}
		else
		{
			containerSize.width = newSize + parentDelegee.getBounds().x;
		}
		parentDelegee.setSize(containerSize);
		parentDelegee.revalidate();
	}

	public void copyGridSection(final RadContainer source, final RadContainer destination, final Rectangle rc)
	{
		destination.setLayout(new GridLayoutManager(rc.height, rc.width));
	}

	@Override
	public LayoutManager copyLayout(LayoutManager layout, int rowDelta, int columnDelta)
	{
		GridLayoutManager oldLayout = (GridLayoutManager) layout;
		final GridLayoutManager newLayout = new GridLayoutManager(oldLayout.getRowCount() + rowDelta, oldLayout.getColumnCount() + columnDelta);
		newLayout.setMargin(oldLayout.getMargin());
		newLayout.setHGap(oldLayout.getHGap());
		newLayout.setVGap(oldLayout.getVGap());
		return newLayout;
	}
}
