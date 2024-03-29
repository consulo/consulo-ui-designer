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

package com.intellij.uiDesigner.impl.componentTree;

import com.intellij.uiDesigner.impl.UIDesignerBundle;
import com.intellij.uiDesigner.impl.radComponents.RadButtonGroup;
import consulo.ui.ex.tree.NodeDescriptor;

/**
 * @author yole
 */
public class ButtonGroupListDescriptor extends NodeDescriptor
{
	private final RadButtonGroup[] myButtonGroups;

	public ButtonGroupListDescriptor(final NodeDescriptor parentDescriptor, final RadButtonGroup[] buttonGroups)
	{
		super(parentDescriptor);
		myButtonGroups = buttonGroups;
	}

	public boolean update()
	{
		return false;
	}

	public Object getElement()
	{
		return myButtonGroups;
	}

	@Override
	public String toString()
	{
		return UIDesignerBundle.message("node.button.groups");
	}
}
