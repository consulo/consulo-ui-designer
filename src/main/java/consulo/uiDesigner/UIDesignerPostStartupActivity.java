package consulo.uiDesigner;

import com.intellij.ide.palette.impl.PaletteToolWindowManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupActivity;
import com.intellij.uiDesigner.propertyInspector.DesignerToolWindowManager;
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
