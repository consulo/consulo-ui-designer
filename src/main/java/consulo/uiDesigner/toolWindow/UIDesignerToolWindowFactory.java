package consulo.uiDesigner.toolWindow;

import com.intellij.uiDesigner.impl.UIDesignerIcons;
import com.intellij.uiDesigner.impl.propertyInspector.DesignerToolWindowManager;
import consulo.annotation.component.ExtensionImpl;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.project.ui.wm.ToolWindowFactory;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.toolWindow.ToolWindow;
import consulo.ui.ex.toolWindow.ToolWindowAnchor;
import consulo.ui.image.Image;

import jakarta.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 31/01/2023
 */
@ExtensionImpl
public class UIDesignerToolWindowFactory implements ToolWindowFactory
{
	@Nonnull
	@Override
	public String getId()
	{
		return "UI Designer";
	}

	@RequiredUIAccess
	@Override
	public void createToolWindowContent(@Nonnull Project project, @Nonnull ToolWindow toolWindow)
	{
		DesignerToolWindowManager manager = DesignerToolWindowManager.getInstance(project);

		manager.initToolWindow(toolWindow);
	}

	@Override
	public boolean isDoNotActivateOnStart()
	{
		return true;
	}

	@Override
	public boolean shouldBeAvailable(@Nonnull Project project)
	{
		return false;
	}

	@Nonnull
	@Override
	public ToolWindowAnchor getAnchor()
	{
		return ToolWindowAnchor.LEFT;
	}

	@Nonnull
	@Override
	public Image getIcon()
	{
		return UIDesignerIcons.ToolWindowUIDesigner;
	}

	@Nonnull
	@Override
	public LocalizeValue getDisplayName()
	{
		return LocalizeValue.of("UI Designer");
	}
}
