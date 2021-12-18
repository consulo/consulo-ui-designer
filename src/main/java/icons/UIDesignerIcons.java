package icons;

import consulo.annotation.DeprecationInfo;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.image.Image;
import consulo.uiDesigner.icon.UIDesignerIconGroup;

@Deprecated
@DeprecationInfo("Use UIDesignerIconGroup or PlatformIconGroup")
public class UIDesignerIcons {
  public static final Image ButtonGroup = PlatformIconGroup.uiDesignerButtonGroup();
  public static final Image CollapseNode = PlatformIconGroup.uiDesignerCollapseNode();
  public static final Image DeleteCell = PlatformIconGroup.generalRemove();
  public static final Image Drag = PlatformIconGroup.uiDesignerDrag();

  public static final Image Empty = Image.empty(Image.DEFAULT_ICON_SIZE, Image.DEFAULT_ICON_SIZE);
  public static final Image ExpandNode = PlatformIconGroup.uiDesignerExpandNode();
  
  public static final Image InsertColumnLeft = PlatformIconGroup.uiDesignerInsertColumnLeft();
  public static final Image InsertColumnRight = PlatformIconGroup.uiDesignerInsertColumnRight();
  public static final Image InsertRowAbove = PlatformIconGroup.uiDesignerInsertRowAbove();
  public static final Image InsertRowBelow = PlatformIconGroup.uiDesignerInsertRowBelow();
  public static final Image InspectionSuppression = UIDesignerIconGroup.inspectionSuppression();
  public static final Image Label = PlatformIconGroup.uiDesignerLabel();
  
  public static final Image List = PlatformIconGroup.uiDesignerList();
  public static final Image Listener = PlatformIconGroup.uiDesignerListener();

  public static final Image SplitColumn = PlatformIconGroup.uiDesignerSplitColumn();

  public static final Image SplitRow = PlatformIconGroup.uiDesignerSplitRow();

  public static final Image ToolWindowUIDesigner = UIDesignerIconGroup.toolWindowUIDesigner();
  
  public static final Image Unknown = PlatformIconGroup.uiDesignerUnknown();
}
