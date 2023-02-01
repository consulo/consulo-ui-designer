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
package com.intellij.uiDesigner.impl.propertyInspector.properties;

import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.impl.radComponents.RadComponent;
import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import consulo.ide.ServiceManager;
import consulo.project.Project;
import jakarta.inject.Singleton;

/**
 * @author yole
 */
@Singleton
@ServiceAPI(ComponentScope.PROJECT)
@ServiceImpl
public class IndentProperty extends AbstractIntProperty<RadComponent>
{
	public static IndentProperty getInstance(Project project)
	{
		return ServiceManager.getService(project, IndentProperty.class);
	}

	public IndentProperty()
	{
		super(null, "Indent", 0);
	}

	public Integer getValue(RadComponent component)
	{
		return component.getConstraints().getIndent();
	}

	protected void setValueImpl(RadComponent component, Integer value) throws Exception
	{
		final int indent = value.intValue();

		final GridConstraints constraints = component.getConstraints();
		if(constraints.getIndent() != indent)
		{
			GridConstraints oldConstraints = (GridConstraints) constraints.clone();
			constraints.setIndent(indent);
			component.fireConstraintsChanged(oldConstraints);
		}
	}
}
