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
import com.intellij.uiDesigner.core.UIFormXmlConstants;
import com.intellij.uiDesigner.impl.*;
import com.intellij.uiDesigner.impl.designSurface.*;
import com.intellij.uiDesigner.impl.palette.ComponentItem;
import com.intellij.uiDesigner.impl.palette.Palette;
import com.intellij.uiDesigner.impl.propertyInspector.*;
import com.intellij.uiDesigner.impl.propertyInspector.editors.IconEditor;
import com.intellij.uiDesigner.impl.propertyInspector.editors.string.StringEditor;
import com.intellij.uiDesigner.impl.propertyInspector.properties.AbstractBooleanProperty;
import com.intellij.uiDesigner.impl.propertyInspector.properties.IntroIconProperty;
import com.intellij.uiDesigner.impl.propertyInspector.renderers.IconRenderer;
import com.intellij.uiDesigner.impl.propertyInspector.renderers.LabelPropertyRenderer;
import com.intellij.uiDesigner.impl.propertyInspector.renderers.StringRenderer;
import com.intellij.uiDesigner.impl.snapShooter.SnapshotContext;
import com.intellij.uiDesigner.lw.*;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.util.lang.Comparing;
import consulo.util.lang.StringUtil;
import org.jetbrains.annotations.NonNls;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import javax.swing.*;
import javax.swing.plaf.TabbedPaneUI;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.HashMap;

/**
 * @author Anton Katilin
 * @author Vladimir Kondratyev
 * @author yole
 */
public final class RadTabbedPane extends RadContainer implements ITabbedPane
{

	public static class Factory extends RadComponentFactory
	{
		public RadComponent newInstance(ModuleProvider module, Class aClass, String id)
		{
			return new RadTabbedPane(module, aClass, id);
		}

		public RadComponent newInstance(final Class componentClass, final String id, final Palette palette)
		{
			return new RadTabbedPane(componentClass, id, palette);
		}
	}

	private static final Logger LOG = Logger.getInstance(RadTabbedPane.class);
	/**
	 * value: HashMap<String, LwTabbedPane.Constraints>
	 */
	@NonNls
	private static final String CLIENT_PROP_ID_2_CONSTRAINTS = "index2descriptor";

	private int mySelectedIndex = -1;
	private IntrospectedProperty mySelectedIndexProperty = null;

	public RadTabbedPane(final ModuleProvider module, Class componentClass, final String id)
	{
		super(module, componentClass, id);
	}

	public RadTabbedPane(Class componentClass, @Nonnull final String id, final Palette palette)
	{
		super(componentClass, id, palette);
	}

	@Override
	protected RadLayoutManager createInitialLayoutManager()
	{
		return new RadTabbedPaneLayoutManager();
	}

	@Override
	public RadComponent getComponentToDrag(final Point pnt)
	{
		final int i = getTabbedPane().getUI().tabForCoordinate(getTabbedPane(), pnt.x, pnt.y);
		if(i >= 0)
		{
			RadComponent c = getRadComponent(i);
			if(c != null)
			{
				return c;
			}
		}
		return this;
	}

	private RadComponent getRadComponent(final int i)
	{
		RadComponent c = null;
		final Component component = getTabbedPane().getComponentAt(i);
		if(component instanceof JComponent)
		{
			JComponent jc = (JComponent) component;
			c = (RadComponent) jc.getClientProperty(RadComponent.CLIENT_PROP_RAD_COMPONENT);
		}
		return c;
	}

	@Override
	public void init(final GuiEditor editor, @Nonnull final ComponentItem item)
	{
		super.init(editor, item);
		// add one tab by default
		addComponent(InsertComponentProcessor.createPanelComponent(editor));
	}

	@Nonnull
	private JTabbedPane getTabbedPane()
	{
		return (JTabbedPane) getDelegee();
	}

	private String calcTabName(final StringDescriptor titleDescriptor)
	{
		if(titleDescriptor == null)
		{
			return UIDesignerBundle.message("tab.untitled");
		}
		return getDescriptorText(titleDescriptor);
	}

	@Nullable
	private String getDescriptorText(@Nullable final StringDescriptor titleDescriptor)
	{
		if(titleDescriptor == null)
		{
			return null;
		}
		final String value = titleDescriptor.getValue();
		if(value == null)
		{ // from res bundle
			final String resolvedValue = StringDescriptorManager.getInstance(getModule()).resolve(this, titleDescriptor);
			titleDescriptor.setResolvedValue(resolvedValue);
			return resolvedValue;
		}
		return value;
	}

	/**
	 * @return inplace property for editing of the title of the clicked tab
	 */
	public Property getInplaceProperty(final int x, final int y)
	{
		final JTabbedPane tabbedPane = getTabbedPane();
		final TabbedPaneUI ui = tabbedPane.getUI();
		LOG.assertTrue(ui != null);
		final int index = ui.tabForCoordinate(tabbedPane, x, y);
		return index != -1 ? new MyTitleProperty(null, index) : null;
	}

	@Override
	@Nullable
	public Property getDefaultInplaceProperty()
	{
		final int index = getTabbedPane().getSelectedIndex();
		if(index >= 0)
		{
			return new MyTitleProperty(null, index);
		}
		return null;
	}

	public Rectangle getInplaceEditorBounds(final Property property, final int x, final int y)
	{
		final JTabbedPane tabbedPane = getTabbedPane();
		final TabbedPaneUI ui = tabbedPane.getUI();
		LOG.assertTrue(ui != null);
		final int index = ui.tabForCoordinate(tabbedPane, x, y);
		LOG.assertTrue(index != -1);
		return ui.getTabBounds(tabbedPane, index);
	}

	@Override
	@Nullable
	public Rectangle getDefaultInplaceEditorBounds()
	{
		final JTabbedPane tabbedPane = getTabbedPane();
		final int index = tabbedPane.getSelectedIndex();
		if(index >= 0)
		{
			return tabbedPane.getUI().getTabBounds(tabbedPane, index);
		}
		return null;
	}

	@Nullable
	public StringDescriptor getChildTitle(RadComponent component)
	{
		final HashMap<String, LwTabbedPane.Constraints> id2Constraints = getId2Constraints(this);
		final LwTabbedPane.Constraints constraints = id2Constraints.get(component.getId());
		return constraints == null ? null : constraints.myTitle;
	}

	public void setTabProperty(RadComponent component, final String propName, StringDescriptor title) throws Exception
	{
		final JComponent delegee = component.getDelegee();
		final JTabbedPane tabbedPane = getTabbedPane();
		int index = tabbedPane.indexOfComponent(delegee);
		if(index >= 0)
		{
			if(propName.equals(ITabbedPane.TAB_TITLE_PROPERTY))
			{
				new MyTitleProperty(null, index).setValue(component, title);
			}
			else if(propName.equals(ITabbedPane.TAB_TOOLTIP_PROPERTY))
			{
				new MyToolTipProperty(null, index).setValue(component, title);
			}
			else
			{
				throw new IllegalArgumentException("Invalid property name " + propName);
			}
		}
	}

	/**
	 * This allows user to select and scroll tabs via the mouse
	 */
	public void processMouseEvent(final MouseEvent event)
	{
		event.getComponent().dispatchEvent(event);
	}

	public void write(final XmlWriter writer)
	{
		writer.startElement(UIFormXmlConstants.ELEMENT_TABBEDPANE);
		try
		{
			writeId(writer);
			writeClassIfDifferent(writer, JTabbedPane.class.getName());
			writeBinding(writer);

			// Constraints and properties
			writeConstraints(writer);
			writeProperties(writer);

			writeBorder(writer);
			writeChildren(writer);
		}
		finally
		{
			writer.endElement();
		}
	}

	@Nonnull
	private static HashMap<String, LwTabbedPane.Constraints> getId2Constraints(final RadComponent component)
	{
		//noinspection unchecked
		HashMap<String, LwTabbedPane.Constraints> id2Constraints = (HashMap<String, LwTabbedPane.Constraints>) component.getClientProperty(CLIENT_PROP_ID_2_CONSTRAINTS);
		if(id2Constraints == null)
		{
			id2Constraints = new HashMap<String, LwTabbedPane.Constraints>();
			component.putClientProperty(CLIENT_PROP_ID_2_CONSTRAINTS, id2Constraints);
		}
		return id2Constraints;
	}

	@Nullable
	public RadComponent getSelectedTab()
	{
		int index = getTabbedPane().getSelectedIndex();
		return index < 0 ? null : getComponent(index);
	}

	public void selectTab(final RadComponent component)
	{
		final JTabbedPane tabbedPane = getTabbedPane();
		int index = tabbedPane.indexOfComponent(component.getDelegee());
		if(index >= 0)
		{
			tabbedPane.setSelectedIndex(index);
		}
	}

	public StringDescriptor getTabProperty(IComponent component, final String propName)
	{
		final HashMap<String, LwTabbedPane.Constraints> id2Constraints = getId2Constraints(this);
		final LwTabbedPane.Constraints constraints = id2Constraints.get(component.getId());
		return constraints == null ? null : constraints.getProperty(propName);
	}

	public boolean refreshChildTitle(final RadComponent radComponent)
	{
		StringDescriptor childTitle = getChildTitle(radComponent);
		if(childTitle == null)
		{
			return false;
		}
		String oldTitle = childTitle.getResolvedValue();
		childTitle.setResolvedValue(null);
		try
		{
			setTabProperty(radComponent, ITabbedPane.TAB_TITLE_PROPERTY, childTitle);
		}
		catch(Exception e)
		{
			LOG.error(e);
		}
		return !Comparing.equal(oldTitle, childTitle.getResolvedValue());
	}

	@Override
	public void loadLwProperty(final LwComponent lwComponent, final LwIntrospectedProperty lwProperty, final IntrospectedProperty property)
	{
		if(lwProperty.getName().equals(SwingProperties.SELECTED_INDEX))
		{
			mySelectedIndexProperty = property;
			mySelectedIndex = ((Integer) lwProperty.getPropertyValue(lwComponent)).intValue();
		}
		else
		{
			super.loadLwProperty(lwComponent, lwProperty, property);
		}
	}

	@Override
	public void doneLoadingFromLw()
	{
		if(mySelectedIndex >= 0)
		{
			getTabbedPane().setSelectedIndex(mySelectedIndex);
			markPropertyAsModified(mySelectedIndexProperty);
		}
	}

	@Override
	protected void importSnapshotComponent(final SnapshotContext context, final JComponent component)
	{
		JTabbedPane tabbedPane = (JTabbedPane) component;
		for(int i = 0; i < tabbedPane.getTabCount(); i++)
		{
			String title = tabbedPane.getTitleAt(i);
			Component child = tabbedPane.getComponentAt(i);
			if(child instanceof JComponent)
			{
				RadComponent childComponent = createSnapshotComponent(context, (JComponent) child);
				if(childComponent != null)
				{
					childComponent.setCustomLayoutConstraints(new LwTabbedPane.Constraints(StringDescriptor.create(title)));
					addComponent(childComponent);
				}
			}
		}
	}

	@Nonnull
	private LwTabbedPane.Constraints getConstraintsForComponent(final RadComponent tabComponent)
	{
		final HashMap<String, LwTabbedPane.Constraints> id2Constraints = getId2Constraints(this);
		LwTabbedPane.Constraints constraints = id2Constraints.get(tabComponent.getId());
		if(constraints == null)
		{
			int index = indexOfComponent(tabComponent);
			constraints = new LwTabbedPane.Constraints(StringDescriptor.create(getTabbedPane().getTitleAt(index)));
			id2Constraints.put(tabComponent.getId(), constraints);
		}
		return constraints;
	}

	private final class MyTabGroupProperty extends ReadOnlyProperty
	{
		private final int myIndex;
		private final LabelPropertyRenderer myRenderer = new LabelPropertyRenderer("");

		public MyTabGroupProperty(final int index)
		{
			super(null, "Tab");
			myIndex = index;
		}

		@Nonnull
		public PropertyRenderer getRenderer()
		{
			return myRenderer;
		}


		@Nonnull
		@Override
		public Property[] getChildren(final RadComponent component)
		{
			return new Property[]{
					new MyTitleProperty(this, myIndex),
					new MyToolTipProperty(this, myIndex),
					new MyIconProperty(this, myIndex, false),
					new MyIconProperty(this, myIndex, true),
					new MyEnabledProperty(this, myIndex)
			};
		}
	}

	private class MyTitleProperty extends Property<RadComponent, StringDescriptor>
	{
		protected final int myIndex;
		private final StringEditor myEditor = new StringEditor(getProject());
		private final StringRenderer myRenderer = new StringRenderer();

		public MyTitleProperty(final Property parent, final int index)
		{
			super(parent, TAB_TITLE_PROPERTY);
			myIndex = index;
		}

		protected MyTitleProperty(final Property parent, @NonNls final String name, final int index)
		{
			super(parent, name);
			myIndex = index;
		}

		public StringDescriptor getValue(final RadComponent component)
		{
			final RadComponent tabComponent = getRadComponent(myIndex);
			// 1. resource bundle
			final LwTabbedPane.Constraints constraints = getId2Constraints(RadTabbedPane.this).get(tabComponent.getId());
			final StringDescriptor descriptor = constraints == null ? null : getValueFromConstraints(constraints);
			if(descriptor != null)
			{
				return descriptor;
			}

			// 2. plain value
			return StringDescriptor.create(getValueFromTabbedPane());
		}

		protected void setValueImpl(final RadComponent component, StringDescriptor value) throws Exception
		{
			final RadComponent tabComponent = getRadComponent(myIndex);
			// 1. Put value into map
			if(value == null)
			{
				value = StringDescriptor.create("");
			}
			LwTabbedPane.Constraints constraints = getConstraintsForComponent(tabComponent);
			putValueToConstraints(value, constraints);

			// 2. Apply real string value to JComponent peer
			final String text = StringDescriptorManager.getInstance(getModule()).resolve(RadTabbedPane.this, value);
			if(value.getValue() == null)
			{
				value.setResolvedValue(text);
			}
			putValueToTabbedPane(text);
		}

		protected String getValueFromTabbedPane()
		{
			return getTabbedPane().getTitleAt(myIndex);
		}

		protected StringDescriptor getValueFromConstraints(final LwTabbedPane.Constraints constraints)
		{
			return constraints.myTitle;
		}

		protected void putValueToTabbedPane(final String text)
		{
			getTabbedPane().setTitleAt(myIndex, text);
		}

		protected void putValueToConstraints(final StringDescriptor value, final LwTabbedPane.Constraints constraints)
		{
			constraints.myTitle = value;
		}

		@Nonnull
		public PropertyRenderer<StringDescriptor> getRenderer()
		{
			return myRenderer;
		}

		public PropertyEditor<StringDescriptor> getEditor()
		{
			return myEditor;
		}

		@Override
		public boolean isModified(final RadComponent component)
		{
			return !getTabbedPane().getTitleAt(myIndex).equals(UIDesignerBundle.message("tab.untitled"));
		}

		@Override
		public void resetValue(final RadComponent component) throws Exception
		{
			setValue(component, StringDescriptor.create(UIDesignerBundle.message("tab.untitled")));
		}
	}

	private class MyToolTipProperty extends MyTitleProperty
	{
		protected MyToolTipProperty(final Property parent, final int index)
		{
			super(parent, TAB_TOOLTIP_PROPERTY, index);
		}

		@Override
		protected String getValueFromTabbedPane()
		{
			return getTabbedPane().getToolTipTextAt(myIndex);
		}

		@Override
		protected StringDescriptor getValueFromConstraints(final LwTabbedPane.Constraints constraints)
		{
			return constraints.myToolTip;
		}

		@Override
		protected void putValueToTabbedPane(final String text)
		{
			getTabbedPane().setToolTipTextAt(myIndex, text);
		}

		@Override
		protected void putValueToConstraints(final StringDescriptor value, final LwTabbedPane.Constraints constraints)
		{
			constraints.myToolTip = value;
		}

		@Override
		public boolean isModified(final RadComponent component)
		{
			String toolTipText = getTabbedPane().getToolTipTextAt(myIndex);
			return !StringUtil.isEmpty(toolTipText);
		}

		@Override
		public void resetValue(final RadComponent component) throws Exception
		{
			setValue(component, StringDescriptor.create(""));
		}
	}

	private class MyIconProperty extends Property<RadComponent, IconDescriptor>
	{
		private final int myIndex;
		private final boolean myDisabledIcon;
		private final IconRenderer myRenderer = new IconRenderer();
		private final IconEditor myEditor = new IconEditor();

		public MyIconProperty(final Property parent, final int index, final boolean disabledIcon)
		{
			super(parent, disabledIcon ? "Tab Disabled Icon" : "Tab Icon");
			myIndex = index;
			myDisabledIcon = disabledIcon;
		}

		public IconDescriptor getValue(final RadComponent component)
		{
			LwTabbedPane.Constraints constraints = getConstraintsForComponent(component);
			return myDisabledIcon ? constraints.myDisabledIcon : constraints.myIcon;
		}

		protected void setValueImpl(final RadComponent component, final IconDescriptor value) throws Exception
		{
			Icon icon = (value != null) ? value.getIcon() : null;
			LwTabbedPane.Constraints constraints = getConstraintsForComponent(component);
			if(myDisabledIcon)
			{
				constraints.myDisabledIcon = value;
				getTabbedPane().setDisabledIconAt(myIndex, icon);
			}
			else
			{
				constraints.myIcon = value;
				getTabbedPane().setIconAt(myIndex, icon);
			}
		}

		@Nonnull
		public PropertyRenderer<IconDescriptor> getRenderer()
		{
			return myRenderer;
		}

		public PropertyEditor<IconDescriptor> getEditor()
		{
			return myEditor;
		}

		@Override
		public boolean isModified(final RadComponent radComponent)
		{
			return getValue(radComponent) != null;
		}

		@Override
		public void resetValue(final RadComponent radComponent) throws Exception
		{
			setValue(radComponent, null);
		}
	}

	private class MyEnabledProperty extends AbstractBooleanProperty<RadComponent>
	{
		private final int myIndex;

		public MyEnabledProperty(final Property parent, final int index)
		{
			super(parent, "Tab Enabled", true);
			myIndex = index;
		}

		public Boolean getValue(final RadComponent component)
		{
			LwTabbedPane.Constraints constraints = getConstraintsForComponent(component);
			return constraints.myEnabled;
		}

		protected void setValueImpl(final RadComponent component, final Boolean value) throws Exception
		{
			LwTabbedPane.Constraints constraints = getConstraintsForComponent(component);
			constraints.myEnabled = value.booleanValue();
			getTabbedPane().setEnabledAt(myIndex, value.booleanValue());
		}
	}

	private class RadTabbedPaneLayoutManager extends RadLayoutManager
	{

		@Nullable
		public String getName()
		{
			return null;
		}

		@Override
		@Nonnull
		public ComponentDropLocation getDropLocation(RadContainer container, @Nullable final Point location)
		{
			final JTabbedPane tabbedPane = getTabbedPane();
			final TabbedPaneUI ui = tabbedPane.getUI();
			if(location != null && tabbedPane.getTabCount() > 0)
			{
				for(int i = 0; i < tabbedPane.getTabCount(); i++)
				{
					Rectangle rc = ui.getTabBounds(tabbedPane, i);
					if(location.x < rc.getCenterX())
					{
						return new InsertTabDropLocation(i, new Rectangle(rc.x - 4, rc.y, 8, rc.height));
					}
				}
			}
			return new InsertTabDropLocation(tabbedPane.getTabCount(), null);
		}

		public void writeChildConstraints(final XmlWriter writer, final RadComponent child)
		{
			writer.startElement(UIFormXmlConstants.ELEMENT_TABBEDPANE);
			try
			{
				final JComponent delegee = child.getDelegee();
				final JTabbedPane tabbedPane = getTabbedPane();
				final int i = tabbedPane.indexOfComponent(delegee);
				if(i == -1)
				{
					throw new IllegalArgumentException("cannot find tab for " + child);
				}

				final HashMap<String, LwTabbedPane.Constraints> id2Constraints = getId2Constraints(RadTabbedPane.this);
				final LwTabbedPane.Constraints tabTitleConstraints = id2Constraints.get(child.getId());
				if(tabTitleConstraints != null)
				{
					writer.writeStringDescriptor(tabTitleConstraints.myTitle,
							UIFormXmlConstants.ATTRIBUTE_TITLE,
							UIFormXmlConstants.ATTRIBUTE_TITLE_RESOURCE_BUNDLE,
							UIFormXmlConstants.ATTRIBUTE_TITLE_KEY);

					if(tabTitleConstraints.myIcon != null)
					{
						writer.addAttribute(UIFormXmlConstants.ATTRIBUTE_ICON, tabTitleConstraints.myIcon.getIconPath());
					}
					if(tabTitleConstraints.myDisabledIcon != null)
					{
						writer.addAttribute(UIFormXmlConstants.ATTRIBUTE_DISABLED_ICON, tabTitleConstraints.myDisabledIcon.getIconPath());
					}
					if(!tabTitleConstraints.myEnabled)
					{
						writer.addAttribute(UIFormXmlConstants.ATTRIBUTE_ENABLED, false);
					}
					if(tabTitleConstraints.myToolTip != null)
					{
						writer.startElement(UIFormXmlConstants.ELEMENT_TOOLTIP);
						writer.writeStringDescriptor(tabTitleConstraints.myToolTip,
								UIFormXmlConstants.ATTRIBUTE_VALUE,
								UIFormXmlConstants.ATTRIBUTE_RESOURCE_BUNDLE,
								UIFormXmlConstants.ATTRIBUTE_KEY);
						writer.endElement();
					}
				}
				else
				{
					final String title = tabbedPane.getTitleAt(i);
					writer.addAttribute(UIFormXmlConstants.ATTRIBUTE_TITLE, title != null ? title : "");
				}
			}
			finally
			{
				writer.endElement();
			}
		}

		public void addComponentToContainer(final RadContainer container, final RadComponent component, final int index)
		{
			final JTabbedPane tabbedPane = getTabbedPane();
			LwTabbedPane.Constraints constraints = null;
			if(component.getCustomLayoutConstraints() instanceof LwTabbedPane.Constraints)
			{
				constraints = ((LwTabbedPane.Constraints) component.getCustomLayoutConstraints());
			}
			component.setCustomLayoutConstraints(null);
			final HashMap<String, LwTabbedPane.Constraints> id2Constraints = getId2Constraints(RadTabbedPane.this);
			id2Constraints.put(component.getId(), constraints);
			final String tabName = calcTabName(constraints == null ? null : constraints.myTitle);
			String toolTip = null;
			Icon icon = null;
			if(constraints != null)
			{
				toolTip = getDescriptorText(constraints.myToolTip);
				if(constraints.myIcon != null)
				{
					IntroIconProperty.ensureIconLoaded(getModule(), constraints.myIcon);
					icon = constraints.myIcon.getIcon();
				}
			}
			tabbedPane.insertTab(tabName, icon, component.getDelegee(), toolTip, index);
			if(constraints != null)
			{
				if(constraints.myDisabledIcon != null)
				{
					IntroIconProperty.ensureIconLoaded(getModule(), constraints.myDisabledIcon);
					tabbedPane.setDisabledIconAt(index, constraints.myDisabledIcon.getIcon());
				}
				tabbedPane.setEnabledAt(index, constraints.myEnabled);
			}
		}

		@Override
		public void removeComponentFromContainer(final RadContainer container, final RadComponent component)
		{
			LOG.debug("Removing component with ID " + component.getId());
			final JTabbedPane tabbedPane = getTabbedPane();

			final JComponent delegee = component.getDelegee();
			final int i = tabbedPane.indexOfComponent(delegee);
			if(i == -1)
			{
				throw new IllegalArgumentException("cannot find tab for " + component);
			}
			final HashMap<String, LwTabbedPane.Constraints> id2Constraints = getId2Constraints(RadTabbedPane.this);
			LwTabbedPane.Constraints constraints = id2Constraints.get(component.getId());
			if(constraints == null)
			{
				LOG.debug("title of removed component is null");
				constraints = new LwTabbedPane.Constraints(StringDescriptor.create(tabbedPane.getTitleAt(i)));
			}
			else
			{
				LOG.debug("title of removed component is " + constraints.myTitle.toString());
			}
			component.setCustomLayoutConstraints(constraints);
			id2Constraints.remove(component.getId());
			tabbedPane.removeTabAt(i);
		}

		@Override
		public Property[] getComponentProperties(final Project project, final RadComponent component)
		{
			final JComponent delegee = component.getDelegee();
			final JTabbedPane tabbedPane = getTabbedPane();
			int index = tabbedPane.indexOfComponent(delegee);
			if(index >= 0)
			{
				return new Property[]{new MyTabGroupProperty(index)};
			}
			return Property.EMPTY_ARRAY;
		}

		@Override
		public boolean isSwitchedToChild(RadContainer container, RadComponent child)
		{
			return child == getSelectedTab();
		}

		@Override
		public boolean switchContainerToChild(RadContainer container, RadComponent child)
		{
			RadTabbedPane.this.selectTab(child);
			return true;
		}

		@Override
		public boolean areChildrenExclusive()
		{
			return true;
		}
	}

	private final class InsertTabDropLocation implements ComponentDropLocation
	{
		private int myInsertIndex;
		private String myInsertBeforeId;
		private final Rectangle myFeedbackRect;

		public InsertTabDropLocation(final int insertIndex, final Rectangle feedbackRect)
		{
			myInsertIndex = insertIndex;
			if(myInsertIndex < getTabbedPane().getTabCount())
			{
				myInsertBeforeId = getRadComponent(myInsertIndex).getId();
			}
			myFeedbackRect = feedbackRect;
		}

		public RadContainer getContainer()
		{
			return RadTabbedPane.this;
		}

		public boolean canDrop(ComponentDragObject dragObject)
		{
			return dragObject.getComponentCount() == 1;
		}

		public void placeFeedback(FeedbackLayer feedbackLayer, ComponentDragObject dragObject)
		{
			final String tooltipText = UIDesignerBundle.message("insert.feedback.add.tab", getDisplayName(), myInsertIndex);
			if(myInsertIndex < getTabbedPane().getTabCount())
			{
				feedbackLayer.putFeedback(getDelegee(), myFeedbackRect, VertInsertFeedbackPainter.INSTANCE, tooltipText);
			}
			else
			{
				Rectangle rcFeedback;
				final JTabbedPane tabbedPane = getTabbedPane();
				final TabbedPaneUI ui = tabbedPane.getUI();
				if(tabbedPane.getTabCount() > 0)
				{
					Rectangle rc = ui.getTabBounds(tabbedPane, tabbedPane.getTabCount() - 1);
					rcFeedback = new Rectangle(rc.x + rc.width, rc.y, 50, rc.height);
				}
				else
				{
					// approximate
					rcFeedback = new Rectangle(0, 0, 50, tabbedPane.getFontMetrics(tabbedPane.getFont()).getHeight() + 8);
				}
				feedbackLayer.putFeedback(getDelegee(), rcFeedback, tooltipText);
			}
		}

		public void processDrop(GuiEditor editor,
								RadComponent[] components,
								GridConstraints[] constraintsToAdjust,
								ComponentDragObject dragObject)
		{
			if(myInsertBeforeId != null)
			{
				for(int i = 0; i < getTabbedPane().getTabCount(); i++)
				{
					if(getRadComponent(i).getId().equals(myInsertBeforeId))
					{
						myInsertIndex = i;
						break;
					}
				}
			}
			if(myInsertIndex > getTabbedPane().getTabCount())
			{
				myInsertIndex = getTabbedPane().getTabCount();
			}
			RadComponent componentToAdd = components[0];
			if(componentToAdd instanceof RadContainer)
			{
				addComponent(componentToAdd, myInsertIndex);
			}
			else
			{
				Palette palette = Palette.getInstance(editor.getProject());
				RadContainer panel = InsertComponentProcessor.createPanelComponent(editor);
				addComponent(panel);
				panel.getDropLocation(null).processDrop(editor, new RadComponent[]{componentToAdd}, null,
						new ComponentItemDragObject(palette.getPanelItem()));
			}
			getTabbedPane().setSelectedIndex(myInsertIndex);
		}

		@Nullable
		public ComponentDropLocation getAdjacentLocation(Direction direction)
		{
			return null;
		}
	}
}
