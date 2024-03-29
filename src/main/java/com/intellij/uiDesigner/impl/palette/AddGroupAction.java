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

package com.intellij.uiDesigner.impl.palette;

import com.intellij.uiDesigner.impl.UIDesignerBundle;
import consulo.application.CommonBundle;
import consulo.language.editor.CommonDataKeys;
import consulo.project.Project;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.awt.Messages;

import java.util.ArrayList;

/**
 * @author yole
 */
public class AddGroupAction extends AnAction
{
  public void actionPerformed(AnActionEvent e) {
    Project project = e.getData(CommonDataKeys.PROJECT);
    if (project == null) return;
    // Ask group name
    final String groupName = Messages.showInputDialog(
      project,
      UIDesignerBundle.message("message.enter.group.name"),
      UIDesignerBundle.message("title.add.group"),
      Messages.getQuestionIcon()
    );
    if(groupName == null){
      return;
    }

    Palette palette = Palette.getInstance(project);
    // Check that name of the group is unique
    final ArrayList<GroupItem> groups = palette.getGroups();
    for(int i = groups.size() - 1; i >= 0; i--){
      if(groupName.equals(groups.get(i).getName())){
        Messages.showErrorDialog(project,
                                 UIDesignerBundle.message("error.group.name.unique"),
                                 CommonBundle.getErrorTitle());
        return;
      }
    }

    final GroupItem groupToBeAdded = new GroupItem(groupName);
    ArrayList<GroupItem> newGroups = new ArrayList<GroupItem>(groups);
    newGroups.add(groupToBeAdded);
    palette.setGroups(newGroups);
  }
}
