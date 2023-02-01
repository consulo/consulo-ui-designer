package consulo.uiDesigner.toolWindow;

import com.intellij.ide.palette.impl.PaletteToolWindowManager;
import consulo.annotation.component.ExtensionImpl;
import consulo.application.AllIcons;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.project.ui.wm.ToolWindowFactory;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.toolWindow.ToolWindow;
import consulo.ui.ex.toolWindow.ToolWindowAnchor;
import consulo.ui.image.Image;

import javax.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 31/01/2023
 */
@ExtensionImpl
public class PaletteToolWindowFactory implements ToolWindowFactory
{
	@Nonnull
	@Override
	public String getId()
	{
		return "Palette";
	}

	@RequiredUIAccess
	@Override
	public void createToolWindowContent(@Nonnull Project project, @Nonnull ToolWindow toolWindow)
	{
		PaletteToolWindowManager manager = PaletteToolWindowManager.getInstance(project);

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
		return ToolWindowAnchor.RIGHT;
	}

	@Nonnull
	@Override
	public Image getIcon()
	{
		return AllIcons.Toolwindows.ToolWindowPalette;
	}

	@Nonnull
	@Override
	public LocalizeValue getDisplayName()
	{
		return LocalizeValue.localizeTODO("Palette");
	}
}
