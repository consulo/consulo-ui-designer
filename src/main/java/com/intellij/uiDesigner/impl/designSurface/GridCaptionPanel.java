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

package com.intellij.uiDesigner.impl.designSurface;

import com.intellij.uiDesigner.impl.CaptionSelection;
import com.intellij.uiDesigner.impl.FormEditingUtil;
import com.intellij.uiDesigner.impl.GridChangeUtil;
import com.intellij.uiDesigner.impl.GuiDesignerConfiguration;
import com.intellij.uiDesigner.impl.componentTree.ComponentSelectionListener;
import com.intellij.uiDesigner.impl.radComponents.RadAbstractGridLayoutManager;
import com.intellij.uiDesigner.impl.radComponents.RadComponent;
import com.intellij.uiDesigner.impl.radComponents.RadContainer;
import com.intellij.uiDesigner.impl.radComponents.RadRootContainer;
import consulo.codeEditor.EditorColors;
import consulo.colorScheme.EditorColorsManager;
import consulo.dataContext.DataContext;
import consulo.dataContext.DataProvider;
import consulo.language.editor.PlatformDataKeys;
import consulo.logging.Logger;
import consulo.ui.color.ColorValue;
import consulo.ui.ex.DeleteProvider;
import consulo.ui.ex.JBColor;
import consulo.ui.ex.action.ActionGroup;
import consulo.ui.ex.action.ActionManager;
import consulo.ui.ex.action.ActionPlaces;
import consulo.ui.ex.action.ActionPopupMenu;
import consulo.ui.ex.awt.LightColors;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.awt.dnd.*;
import consulo.ui.ex.awt.util.Alarm;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.util.collection.ArrayUtil;
import consulo.util.dataholder.Key;
import consulo.util.lang.Pair;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.function.Supplier;

/**
 * @author yole
 */
public class GridCaptionPanel extends JPanel implements ComponentSelectionListener, DataProvider
{
	private static final Logger LOG = Logger.getInstance(GridCaptionPanel.class);

	private final GuiEditor myEditor;
	private final boolean myIsRow;
	private RadContainer mySelectedContainer;
	private final DefaultListSelectionModel mySelectionModel = new DefaultListSelectionModel();
	private int myResizeLine = -1;
	private int myDropInsertLine = -1;
	private final LineFeedbackPainter myFeedbackPainter = new LineFeedbackPainter();
	private final DeleteProvider myDeleteProvider = new MyDeleteProvider();
	private final Alarm myAlarm = new Alarm();

	public GridCaptionPanel(final GuiEditor editor, final boolean isRow)
	{
		myEditor = editor;
		myIsRow = isRow;
		mySelectionModel.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		mySelectionModel.addListSelectionListener(new ListSelectionListener()
		{
			public void valueChanged(ListSelectionEvent e)
			{
				repaint();
				myEditor.fireSelectedComponentChanged();
			}
		});
		setBackground(getGutterColor());
		editor.addComponentSelectionListener(this);

		final MyMouseListener listener = new MyMouseListener();
		addMouseListener(listener);
		addMouseMotionListener(listener);
		addKeyListener(new MyKeyListener());
		setFocusable(true);

		DnDManager.getInstance().registerSource(new MyDnDSource(), this);
		DnDManager.getInstance().registerTarget(new MyDnDTarget(), this);

		addFocusListener(new FocusAdapter()
		{
			@Override
			public void focusGained(FocusEvent e)
			{
				repaint();
				// ensure we don't have two repaints of properties panel - one from focus gain and another from click
				myAlarm.addRequest(new Runnable()
				{
					public void run()
					{
						editor.fireSelectedComponentChanged();
					}
				}, 1000);
			}

			@Override
			public void focusLost(FocusEvent e)
			{
				repaint();
			}
		});
	}

	public static JBColor getGutterColor()
	{
		return new JBColor(new Supplier<Color>()
		{
			@Nonnull
			@Override
			public Color get()
			{
				ColorValue color = EditorColorsManager.getInstance().getGlobalScheme().getColor(EditorColors.GUTTER_BACKGROUND);
				return color == null ? UIUtil.getPanelBackground() : TargetAWT.to(color);
			}
		});
	}

	public RadContainer getSelectedContainer()
	{
		// when the selected component changes, and we have focus, PropertyInspector asks us about our container and selection.
		// PropertyInspector's selection changed listener can be called before our own listener, so we need to update ourselves
		// so that we don't return stale and invalid data.
		checkSelectionChanged();
		return mySelectedContainer;
	}

	public boolean isRow()
	{
		return myIsRow;
	}

	@Override
	public Dimension getPreferredSize()
	{
		return new Dimension(16, 16);
	}

	@Override
	public Dimension getMinimumSize()
	{
		return getPreferredSize();
	}

	@Override
	public void paintComponent(Graphics g)
	{
		super.paintComponent(g);
		Graphics2D g2d = (Graphics2D) g;

		final Rectangle bounds = getBounds();
		final int paintedSize = 8;
		final int paintOffset = 7;

		RadContainer container = getSelectedGridContainer();
		if(container == null)
		{
			return;
		}
		RadAbstractGridLayoutManager layout = container.getGridLayoutManager();
		int[] coords = layout.getGridCellCoords(container, myIsRow);
		int[] sizes = layout.getGridCellSizes(container, myIsRow);
		int count = myIsRow ? layout.getGridRowCount(container) : layout.getGridColumnCount(container);

		for(int i = 0; i < count; i++)
		{
			int x = myIsRow ? 0 : coords[i];
			int y = myIsRow ? coords[i] : 0;
			Point pnt = SwingUtilities.convertPoint(container.getDelegee(), x, y, this);

			Rectangle rc = myIsRow ? new Rectangle(bounds.x + paintOffset, pnt.y, paintedSize, sizes[i]) : new Rectangle(pnt.x,
					bounds.y + paintOffset, sizes[i], paintedSize);

			g.setColor(getCaptionColor(i));
			g.fillRect(rc.x, rc.y, rc.width, rc.height);

			Rectangle rcDecoration = myIsRow ? new Rectangle(bounds.x, pnt.y, bounds.width, sizes[i]) : new Rectangle(pnt.x, bounds.y, sizes[i],
					bounds.height);
			layout.paintCaptionDecoration(container, myIsRow, i, g2d, rcDecoration);

			Stroke oldStroke = g2d.getStroke();
			int deltaX = 0;
			int deltaY = 0;
			if(isFocusOwner() && i == mySelectionModel.getLeadSelectionIndex())
			{
				g.setColor(Color.BLACK);
				g2d.setStroke(new BasicStroke(2.0f));
				deltaX = myIsRow ? 1 : 0;
				deltaY = myIsRow ? 0 : 1;
			}
			else
			{
				g.setColor(Color.DARK_GRAY);
			}
			g.drawRect(rc.x + deltaX, rc.y + deltaY, rc.width - deltaX, rc.height - deltaY);
			g2d.setStroke(oldStroke);
		}

		g.setColor(Color.DARK_GRAY);
		if(myIsRow)
		{
			g.drawLine(paintOffset + paintedSize, 0, paintOffset + paintedSize, bounds.height);
		}
		else
		{
			g.drawLine(0, paintOffset + paintedSize, bounds.width, paintOffset + paintedSize);
		}

		if(myDropInsertLine >= 0)
		{
			int[] lines = myIsRow ? layout.getHorizontalGridLines(container) : layout.getVerticalGridLines(container);
			int coord = lines[myDropInsertLine];
			if(myIsRow)
			{
				coord = SwingUtilities.convertPoint(container.getDelegee(), 0, coord, this).y;
			}
			else
			{
				coord = SwingUtilities.convertPoint(container.getDelegee(), coord, 0, this).x;
			}
			Stroke oldStroke = g2d.getStroke();
			g2d.setStroke(new BasicStroke(2.0f));
			g.setColor(JBColor.BLUE);
			if(myIsRow)
			{
				g.drawLine(bounds.x + 1, coord, bounds.x + bounds.width - 1, coord);
			}
			else
			{
				g.drawLine(coord, bounds.y + 1, coord, bounds.y + bounds.height - 1);
			}

			g2d.setStroke(oldStroke);
		}

	}

	private Color getCaptionColor(final int i)
	{
		if(mySelectionModel.isSelectedIndex(i))
		{
			return LightColors.BLUE;
		}
		if(mySelectedContainer != null)
		{
			if(i >= 0 && i < mySelectedContainer.getGridCellCount(myIsRow))
			{
				final GridChangeUtil.CellStatus status = GridChangeUtil.canDeleteCell(mySelectedContainer, i, myIsRow);
				if(status == GridChangeUtil.CellStatus.Empty || status == GridChangeUtil.CellStatus.Redundant)
				{
					return Color.PINK;
				}
			}
		}
		return LightColors.GREEN;
	}

	@Nullable
	private RadContainer getSelectedGridContainer()
	{
		final ArrayList<RadComponent> selection = FormEditingUtil.getSelectedComponents(myEditor);
		if(selection.size() == 1 && selection.get(0) instanceof RadContainer)
		{
			RadContainer container = (RadContainer) selection.get(0);
			if(container.getLayoutManager().isGrid() && (container.getParent() instanceof RadRootContainer || container.getComponentCount() > 0))
			{
				return container;
			}
		}
		RadContainer container = FormEditingUtil.getSelectionParent(selection);
		if(container == null && myEditor.getRootContainer().getComponentCount() > 0)
		{
			final RadComponent topComponent = myEditor.getRootContainer().getComponent(0);
			if(topComponent instanceof RadContainer)
			{
				container = (RadContainer) topComponent;
			}
		}
		if(container != null && !container.getLayoutManager().isGrid())
		{
			return null;
		}
		return container;
	}

	public void selectedComponentChanged(GuiEditor source)
	{
		checkSelectionChanged();
		repaint();
	}

	private void checkSelectionChanged()
	{
		RadContainer container = getSelectedGridContainer();
		if(container != mySelectedContainer)
		{
			mySelectedContainer = container;
			mySelectionModel.clearSelection();
			repaint();
		}
	}

	@Override
	@Nullable
	public Object getData(@Nonnull Key dataId)
	{
		if(GuiEditor.DATA_KEY == dataId)
		{
			return myEditor;
		}
		if(CaptionSelection.DATA_KEY == dataId)
		{
			return new CaptionSelection(mySelectedContainer, myIsRow, getSelectedCells(null), mySelectionModel.getLeadSelectionIndex());
		}
		if(PlatformDataKeys.DELETE_ELEMENT_PROVIDER == dataId)
		{
			return myDeleteProvider;
		}
		return myEditor.getData(dataId);
	}

	public void attachToScrollPane(final JScrollPane scrollPane)
	{
		scrollPane.getViewport().addChangeListener(e -> repaint());
	}

	private boolean canResizeCells()
	{
		return GuiDesignerConfiguration.getInstance(myEditor.getProject()).RESIZE_HEADERS &&
				mySelectedContainer != null &&
				mySelectedContainer.getGridLayoutManager().canResizeCells();
	}

	private int getCellAt(Point pnt)
	{
		if(mySelectedContainer == null)
		{
			return -1;
		}
		pnt = SwingUtilities.convertPoint(this, pnt, mySelectedContainer.getDelegee());
		return myIsRow ? mySelectedContainer.getGridRowAt(pnt.y) : mySelectedContainer.getGridColumnAt(pnt.x);
	}

	public int[] getSelectedCells(@Nullable final Point dragOrigin)
	{
		ArrayList<Integer> selection = new ArrayList<Integer>();
		RadContainer container = getSelectedGridContainer();
		if(container == null)
		{
			return ArrayUtil.EMPTY_INT_ARRAY;
		}
		int size = getCellCount();
		for(int i = 0; i < size; i++)
		{
			if(mySelectionModel.isSelectedIndex(i))
			{
				selection.add(i);
			}
		}
		if(selection.size() == 0 && dragOrigin != null)
		{
			int cell = getCellAt(dragOrigin);
			if(cell >= 0)
			{
				return new int[]{cell};
			}
		}
		int[] result = new int[selection.size()];
		for(int i = 0; i < selection.size(); i++)
		{
			result[i] = selection.get(i).intValue();
		}
		return result;
	}

	private int getCellCount()
	{
		final RadContainer gridContainer = getSelectedGridContainer();
		assert gridContainer != null;
		return myIsRow ? gridContainer.getGridRowCount() : gridContainer.getGridColumnCount();
	}

	private class MyMouseListener extends MouseAdapter implements MouseMotionListener
	{
		private static final int MINIMUM_RESIZED_SIZE = 8;

		@Override
		public void mouseExited(MouseEvent e)
		{
			setCursor(Cursor.getDefaultCursor());
		}

		@Override
		public void mousePressed(MouseEvent e)
		{
			if(mySelectedContainer == null)
			{
				return;
			}
			requestFocus();
			Point pnt = SwingUtilities.convertPoint(GridCaptionPanel.this, e.getPoint(), mySelectedContainer.getDelegee());
			if(canResizeCells())
			{
				myResizeLine = mySelectedContainer.getGridLayoutManager().getGridLineNear(mySelectedContainer, myIsRow, pnt, 4);
			}
			if(!checkShowPopupMenu(e))
			{
				int cell = getCellAt(e.getPoint());
				if(cell == -1)
				{
					return;
				}
				if((e.getModifiers() & MouseEvent.CTRL_MASK) != 0)
				{
					mySelectionModel.addSelectionInterval(cell, cell);
				}
				else if((e.getModifiers() & MouseEvent.SHIFT_MASK) != 0)
				{
					mySelectionModel.addSelectionInterval(mySelectionModel.getAnchorSelectionIndex(), cell);
				}
			}
		}

		@Override
		public void mouseReleased(MouseEvent e)
		{
			setCursor(Cursor.getDefaultCursor());
			myEditor.getActiveDecorationLayer().removeFeedback();

			if(myResizeLine > 0)
			{
				Point pnt = SwingUtilities.convertPoint(GridCaptionPanel.this, e.getPoint(), mySelectedContainer.getDelegee());
				doResize(pnt);
				myResizeLine = -1;
			}

			if(!checkShowPopupMenu(e))
			{
				int cell = getCellAt(e.getPoint());
				if(cell == -1)
				{
					return;
				}
				if((e.getModifiers() & (MouseEvent.CTRL_MASK | MouseEvent.SHIFT_MASK)) == 0)
				{
					mySelectionModel.setSelectionInterval(cell, cell);
				}
			}
		}

		private boolean checkShowPopupMenu(final MouseEvent e)
		{
			int cell = getCellAt(e.getPoint());

			if(cell >= 0 && e.isPopupTrigger())
			{
				if(!mySelectionModel.isSelectedIndex(cell))
				{
					mySelectionModel.setSelectionInterval(cell, cell);
				}
				ActionGroup group = mySelectedContainer.getGridLayoutManager().getCaptionActions();
				if(group != null)
				{
					final ActionPopupMenu popupMenu = ActionManager.getInstance().createActionPopupMenu(ActionPlaces.UNKNOWN, group);
					popupMenu.getComponent().show(GridCaptionPanel.this, e.getX(), e.getY());
					return true;
				}
			}
			return false;
		}

		private void doResize(final Point pnt)
		{
			int[] coords = mySelectedContainer.getGridLayoutManager().getGridCellCoords(mySelectedContainer, myIsRow);
			int prevCoord = coords[myResizeLine - 1];
			int newCoord = myIsRow ? pnt.y : pnt.x;
			if(newCoord < prevCoord + MINIMUM_RESIZED_SIZE)
			{
				return;
			}
			int newSize = newCoord - prevCoord;

			if(!myEditor.ensureEditable())
			{
				return;
			}

			mySelectedContainer.getGridLayoutManager().processCellResized(mySelectedContainer, myIsRow, myResizeLine - 1, newSize);
			mySelectedContainer.revalidate();
			myEditor.refreshAndSave(true);
		}

		public void mouseMoved(MouseEvent e)
		{
			if(!canResizeCells())
			{
				return;
			}

			Point pnt = SwingUtilities.convertPoint(GridCaptionPanel.this, e.getPoint(), mySelectedContainer.getDelegee());
			int gridLine = mySelectedContainer.getGridLayoutManager().getGridLineNear(mySelectedContainer, myIsRow, pnt, 4);

			// first grid line may not be dragged
			if(gridLine <= 0)
			{
				setCursor(Cursor.getDefaultCursor());
			}
			else if(myIsRow)
			{
				setCursor(Cursor.getPredefinedCursor(Cursor.N_RESIZE_CURSOR));
			}
			else
			{
				setCursor(Cursor.getPredefinedCursor(Cursor.W_RESIZE_CURSOR));
			}
		}

		public void mouseDragged(MouseEvent e)
		{
			if(myResizeLine > 0)
			{
				Point pnt = SwingUtilities.convertPoint(GridCaptionPanel.this, e.getPoint(), mySelectedContainer.getDelegee());
				int[] coords = mySelectedContainer.getGridLayoutManager().getGridCellCoords(mySelectedContainer, myIsRow);
				int prevCoord = coords[myResizeLine - 1];
				int newCoord = myIsRow ? pnt.y : pnt.x;
				if(newCoord < prevCoord + MINIMUM_RESIZED_SIZE)
				{
					return;
				}
				int newSize = newCoord - prevCoord;

				String toolTip = mySelectedContainer.getGridLayoutManager().getCellResizeTooltip(mySelectedContainer, myIsRow, myResizeLine - 1,
						newSize);
				final ActiveDecorationLayer layer = myEditor.getActiveDecorationLayer();
				Rectangle rc;
				if(myIsRow)
				{
					rc = new Rectangle(0, e.getPoint().y, layer.getSize().width, 1);
				}
				else
				{
					rc = new Rectangle(e.getPoint().x, 0, 1, layer.getSize().height);
				}
				layer.putFeedback(GridCaptionPanel.this, rc, myFeedbackPainter, toolTip);
			}
		}
	}

	private static class LineFeedbackPainter implements FeedbackPainter
	{
		public void paintFeedback(Graphics2D g, Rectangle rc)
		{
			g.setColor(LightColors.YELLOW);
			if(rc.width == 1)
			{
				g.drawLine(rc.x, rc.y, rc.x, rc.y + rc.height);
			}
			else
			{
				g.drawLine(rc.x, rc.y, rc.x + rc.width, rc.y);
			}
		}
	}

	private class MyDeleteProvider implements DeleteProvider
	{
		public void deleteElement(@Nonnull DataContext dataContext)
		{
			int[] selection = getSelectedCells(null);
			if(selection.length > 0)
			{
				FormEditingUtil.deleteRowOrColumn(myEditor, mySelectedContainer, selection, myIsRow);
			}
		}

		public boolean canDeleteElement(@Nonnull DataContext dataContext)
		{
			if(mySelectedContainer == null || mySelectionModel.isSelectionEmpty())
			{
				return false;
			}
			int[] selection = getSelectedCells(null);
			return mySelectedContainer.getGridCellCount(myIsRow) - selection.length >= mySelectedContainer.getGridLayoutManager().getMinCellCount();
		}
	}

	private class MyDnDSource implements DnDSource
	{
		public boolean canStartDragging(DnDAction action, Point dragOrigin)
		{
			LOG.debug("canStartDragging(): dragOrigin=" + dragOrigin);
			if(myResizeLine != -1)
			{
				LOG.debug("canStartDragging(): have resize line");
				return false;
			}
			RadContainer container = getSelectedGridContainer();
			if(container != null && container.getGridLayoutManager().getGridLineNear(mySelectedContainer, myIsRow, dragOrigin, 4) != -1)
			{
				LOG.debug("canStartDragging(): have gridline near");
				return false;
			}
			int[] selectedCells = getSelectedCells(dragOrigin);
			for(int cell : selectedCells)
			{
				if(!canDragCell(cell))
				{
					LOG.debug("canStartDragging(): cannot drag cell");
					return false;
				}
			}
			LOG.debug("canStartDragging(): starting drag");
			return true;
		}

		private boolean canDragCell(final int cell)
		{
			if(mySelectedContainer == null)
			{
				return false;
			}
			for(RadComponent c : mySelectedContainer.getComponents())
			{
				if(c.getConstraints().contains(myIsRow, cell) && c.getConstraints().getSpan(myIsRow) > 1)
				{
					return false;
				}
			}
			return true;
		}

		public DnDDragStartBean startDragging(DnDAction action, Point dragOrigin)
		{
			return new DnDDragStartBean(new MyDragBean(myIsRow, getSelectedCells(dragOrigin)));
		}

		@Nullable
		public Pair<Image, Point> createDraggedImage(DnDAction action, Point dragOrigin)
		{
			return null;
		}

		public void dragDropEnd()
		{
		}

		public void dropActionChanged(final int gestureModifiers)
		{
		}
	}

	private class MyDnDTarget implements DnDTarget
	{
		public boolean update(DnDEvent aEvent)
		{
			aEvent.setDropPossible(false);
			if(mySelectedContainer == null)
			{
				return false;
			}
			if(!(aEvent.getAttachedObject() instanceof MyDragBean))
			{
				return false;
			}
			MyDragBean bean = (MyDragBean) aEvent.getAttachedObject();
			if(bean.isRow != myIsRow || bean.cells.length == 0)
			{
				return false;
			}
			int gridLine = getDropGridLine(aEvent);
			setDropInsertLine(gridLine);
			aEvent.setDropPossible(gridLine >= 0);
			if(gridLine >= 0)
			{
				FeedbackPainter painter = myIsRow ? HorzInsertFeedbackPainter.INSTANCE : VertInsertFeedbackPainter.INSTANCE;
				Rectangle rcFeedback = new Rectangle(mySelectedContainer.getDelegee().getSize());
				Rectangle cellRect = new Rectangle(gridLine, gridLine, 1, 1);
				rcFeedback = GridInsertLocation.getInsertFeedbackPosition(myIsRow ? GridInsertMode.RowBefore : GridInsertMode.ColumnBefore,
						mySelectedContainer, cellRect, rcFeedback);
				myEditor.getActiveDecorationLayer().putFeedback(mySelectedContainer.getDelegee(), rcFeedback, painter, null);
			}
			else
			{
				myEditor.getActiveDecorationLayer().removeFeedback();
			}
			return false;
		}

		private int getDropGridLine(final DnDEvent aEvent)
		{
			final Point point = aEvent.getPointOn(mySelectedContainer.getDelegee());
			return mySelectedContainer.getGridLayoutManager().getGridLineNear(mySelectedContainer, myIsRow, point, 20);
		}

		public void drop(DnDEvent aEvent)
		{
			if(!(aEvent.getAttachedObject() instanceof MyDragBean))
			{
				return;
			}
			MyDragBean dragBean = (MyDragBean) aEvent.getAttachedObject();
			int targetCell = getDropGridLine(aEvent);
			if(targetCell < 0)
			{
				return;
			}
			mySelectedContainer.getGridLayoutManager().processCellsMoved(mySelectedContainer, myIsRow, dragBean.cells, targetCell);
			mySelectionModel.clearSelection();
			mySelectedContainer.revalidate();
			myEditor.refreshAndSave(true);
			cleanUpOnLeave();
		}

		public void cleanUpOnLeave()
		{
			setDropInsertLine(-1);
			myEditor.getActiveDecorationLayer().removeFeedback();
		}

		public void updateDraggedImage(Image image, Point dropPoint, Point imageOffset)
		{
		}

		private void setDropInsertLine(final int i)
		{
			if(myDropInsertLine != i)
			{
				myDropInsertLine = i;
				repaint();
			}
		}
	}

	private static class MyDragBean
	{
		public boolean isRow;
		public int[] cells;

		public MyDragBean(final boolean row, final int[] cells)
		{
			isRow = row;
			this.cells = cells;
		}
	}

	private class MyKeyListener extends KeyAdapter
	{
		@Override
		public void keyPressed(KeyEvent e)
		{
			if(e.getKeyCode() == KeyEvent.VK_HOME)
			{
				mySelectionModel.setSelectionInterval(0, 0);
			}
			else if(e.getKeyCode() == KeyEvent.VK_END)
			{
				int cellCount = getCellCount();
				mySelectionModel.setSelectionInterval(cellCount - 1, cellCount - 1);
			}
			else if(e.getKeyCode() == (myIsRow ? KeyEvent.VK_UP : KeyEvent.VK_LEFT))
			{
				moveSelection(e, -1);
			}
			else if(e.getKeyCode() == (myIsRow ? KeyEvent.VK_DOWN : KeyEvent.VK_RIGHT))
			{
				moveSelection(e, 1);
			}
		}

		private void moveSelection(final KeyEvent e, final int delta)
		{
			int leadIndex = mySelectionModel.getLeadSelectionIndex();
			int newLeadIndex = leadIndex + delta;
			if(newLeadIndex >= 0 && newLeadIndex < getCellCount())
			{
				if((e.getModifiers() & KeyEvent.SHIFT_MASK) != 0)
				{
					mySelectionModel.setSelectionInterval(mySelectionModel.getAnchorSelectionIndex(), newLeadIndex);
				}
				else
				{
					mySelectionModel.setSelectionInterval(newLeadIndex, newLeadIndex);
				}
			}
		}
	}
}
