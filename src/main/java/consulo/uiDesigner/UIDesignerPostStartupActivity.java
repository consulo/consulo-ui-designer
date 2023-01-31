package consulo.uiDesigner;

import com.intellij.ide.palette.impl.PaletteToolWindowManager;
import com.intellij.uiDesigner.propertyInspector.DesignerToolWindowManager;
import consulo.project.Project;
import consulo.project.startup.StartupActivity;
import consulo.ui.UIAccess;

import javax.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 2020-11-12
 */
public class UIDesignerPostStartupActivity implements StartupActivity.DumbAware
{
	@Override
	public void runActivity(@Nonnull UIAccess uiAccess, @Nonnull Project project)
	{
		PaletteToolWindowManager.getInstance(project).projectOpened();

		DesignerToolWindowManager.getInstance(project).projectOpened();
	}
}
