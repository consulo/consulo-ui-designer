package com.intellij.uiDesigner.impl;

import consulo.annotation.DeprecationInfo;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.image.Image;
import consulo.uiDesigner.impl.icon.UIDesignerIconGroup;

@Deprecated
@DeprecationInfo("Use UIDesignerIconGroup or PlatformIconGroup")
public class UIDesignerIcons
{
	public static final Image ButtonGroup = PlatformIconGroup.uidesignerButtongroup();
	public static final Image CollapseNode = PlatformIconGroup.uidesignerCollapsenode();
	public static final Image DeleteCell = PlatformIconGroup.generalRemove();
	public static final Image Drag = PlatformIconGroup.uidesignerDrag();

	public static final Image Empty = Image.empty(Image.DEFAULT_ICON_SIZE, Image.DEFAULT_ICON_SIZE);
	public static final Image ExpandNode = PlatformIconGroup.uidesignerExpandnode();

	public static final Image InsertColumnLeft = PlatformIconGroup.uidesignerInsertcolumnleft();
	public static final Image InsertColumnRight = PlatformIconGroup.uidesignerInsertcolumnright();
	public static final Image InsertRowAbove = PlatformIconGroup.uidesignerInsertrowabove();
	public static final Image InsertRowBelow = PlatformIconGroup.uidesignerInsertrowabove();
	public static final Image InspectionSuppression = UIDesignerIconGroup.inspectionsuppression();
	public static final Image Label = PlatformIconGroup.uidesignerLabel();

	public static final Image List = PlatformIconGroup.uidesignerList();
	public static final Image Listener = PlatformIconGroup.uidesignerListener();

	public static final Image SplitColumn = PlatformIconGroup.uidesignerSplitcolumn();

	public static final Image SplitRow = PlatformIconGroup.uidesignerSplitrow();

	public static final Image ToolWindowUIDesigner = UIDesignerIconGroup.toolwindowuidesigner();

	public static final Image Unknown = PlatformIconGroup.uidesignerUnknown();
}
