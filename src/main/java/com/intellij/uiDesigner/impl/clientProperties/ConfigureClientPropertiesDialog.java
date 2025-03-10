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

package com.intellij.uiDesigner.impl.clientProperties;

import com.intellij.uiDesigner.impl.UIDesignerBundle;
import consulo.application.util.SystemInfo;
import consulo.ide.impl.idea.ide.util.PropertiesComponent;
import consulo.project.Project;
import consulo.ui.ex.SimpleTextAttributes;
import consulo.ui.ex.action.ActionToolbarPosition;
import consulo.ui.ex.awt.*;
import consulo.ui.ex.awt.table.JBTable;
import consulo.ui.ex.awt.tree.ColoredTreeCellRenderer;
import consulo.ui.ex.awt.tree.Tree;
import org.jetbrains.annotations.NonNls;

import jakarta.annotation.Nullable;
import javax.swing.*;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import java.util.*;

/**
 * @author yole
 */
public class ConfigureClientPropertiesDialog extends DialogWrapper {
  private JTree myClassTree;
  private JTable myPropertiesTable;
  private Class mySelectedClass;
  private List<ClientPropertiesManager.ClientProperty> mySelectedProperties = Collections.emptyList();
  private final MyTableModel myTableModel = new MyTableModel();
  private final Project myProject;
  private final ClientPropertiesManager myManager;
  private Splitter mySplitter;
  final private PropertiesComponent myPropertiesComponent = PropertiesComponent.getInstance();
  final private static String SPLITTER_PROPORTION_PROPERTY = "ConfigureClientPropertiesDialog.splitterProportion";

  public ConfigureClientPropertiesDialog(final Project project) {
    super(project, true);
    myProject = project;
    setTitle(UIDesignerBundle.message("client.properties.title"));
    myManager = ClientPropertiesManager.getInstance(project).clone();
    init();
  }

  public void save() {
    ClientPropertiesManager.getInstance(myProject).saveFrom(myManager);
  }

  @Override
  public void dispose() {
    myPropertiesComponent.setValue(SPLITTER_PROPORTION_PROPERTY, String.valueOf(mySplitter.getProportion()));
    super.dispose();
  }

  private void updateSelectedProperties() {
    mySelectedProperties = myManager.getConfiguredProperties(mySelectedClass);
    myTableModel.fireTableDataChanged();
  }

  @Nullable
  protected JComponent createCenterPanel() {
    myClassTree = new Tree();
    myClassTree.setRootVisible(false);
    myClassTree.getSelectionModel().addTreeSelectionListener(new TreeSelectionListener() {
      public void valueChanged(TreeSelectionEvent e) {
        final TreePath leadSelectionPath = e.getNewLeadSelectionPath();
        if (leadSelectionPath == null) return;
        final DefaultMutableTreeNode node = (DefaultMutableTreeNode)leadSelectionPath.getLastPathComponent();
        mySelectedClass = (Class)node.getUserObject();
        updateSelectedProperties();
      }
    });

    myClassTree.setCellRenderer(new ColoredTreeCellRenderer() {
      public void customizeCellRenderer(JTree tree,
                                        Object value,
                                        boolean selected,
                                        boolean expanded,
                                        boolean leaf,
                                        int row,
                                        boolean hasFocus) {
        DefaultMutableTreeNode node = (DefaultMutableTreeNode)value;
        if (node.getUserObject() instanceof Class) {
          Class cls = (Class)node.getUserObject();
          if (cls != null) {
            append(cls.getName(), SimpleTextAttributes.REGULAR_ATTRIBUTES);
          }
        }
      }
    });
    fillClassTree();

    myPropertiesTable = new JBTable();
    myPropertiesTable.setModel(myTableModel);


    mySplitter = new Splitter(false, Float.valueOf(myPropertiesComponent.getValue(SPLITTER_PROPORTION_PROPERTY, "0.5f")));
    mySplitter.setFirstComponent(
      ToolbarDecorator.createDecorator(myClassTree)
        .setAddAction(new AnActionButtonRunnable() {
          @Override
          public void run(AnActionButton button) {
            ClassNameInputDialog dlg = new ClassNameInputDialog(myProject, mySplitter);
            dlg.show();
            if (dlg.getExitCode() == OK_EXIT_CODE) {
              String className = dlg.getClassName();
              if (className.length() == 0) return;
              final Class aClass;
              try {
                aClass = Class.forName(className);
              }
              catch (ClassNotFoundException ex) {
                Messages.showErrorDialog(mySplitter,
                                         UIDesignerBundle.message("client.properties.class.not.found", className),
                                         UIDesignerBundle.message("client.properties.title"));
                return;
              }
              if (!JComponent.class.isAssignableFrom(aClass)) {
                Messages.showErrorDialog(mySplitter,
                                         UIDesignerBundle
                                           .message("client.properties.class.not.component", className),
                                         UIDesignerBundle.message("client.properties.title"));
                return;
              }
              myManager.addClientPropertyClass(className);
              fillClassTree();
            }
          }
        }).setRemoveAction(new AnActionButtonRunnable() {
        @Override
        public void run(AnActionButton button) {
          if (mySelectedClass != null) {
            myManager.removeClientPropertyClass(mySelectedClass);
            fillClassTree();
          }
        }
      }).setToolbarPosition(SystemInfo.isMac ? ActionToolbarPosition.BOTTOM : ActionToolbarPosition.RIGHT).createPanel());

    mySplitter.setSecondComponent(
      ToolbarDecorator.createDecorator(myPropertiesTable).disableUpDownActions()
        .setAddAction(new AnActionButtonRunnable() {
          @Override
          public void run(AnActionButton button) {
            AddClientPropertyDialog dlg = new AddClientPropertyDialog(myProject);
            dlg.show();
            if (dlg.getExitCode() == OK_EXIT_CODE) {
              List<ClientPropertiesManager.ClientProperty> props = myManager.getClientProperties(mySelectedClass);
              for (ClientPropertiesManager.ClientProperty prop : props) {
                if (prop.getName().equalsIgnoreCase(dlg.getEnteredProperty().getName())) {
                  Messages.showErrorDialog(mySplitter,
                                           UIDesignerBundle.message("client.properties.already.defined", prop.getName()),
                                           UIDesignerBundle.message("client.properties.title"));
                  return;
                }
              }
              myManager.addConfiguredProperty(mySelectedClass, dlg.getEnteredProperty());
              updateSelectedProperties();
            }
          }
        }).setRemoveAction(new AnActionButtonRunnable() {
        @Override
        public void run(AnActionButton button) {
          int row = myPropertiesTable.getSelectedRow();
          if (row >= 0 && row < mySelectedProperties.size()) {
            myManager.removeConfiguredProperty(mySelectedClass, mySelectedProperties.get(row).getName());
            updateSelectedProperties();
            if (mySelectedProperties.size() > 0) {
              if (row >= mySelectedProperties.size()) row--;
              myPropertiesTable.getSelectionModel().setSelectionInterval(row, row);
            }
          }
        }
      }).createPanel());

    return mySplitter;
  }

  private void fillClassTree() {
    List<Class> configuredClasses = myManager.getConfiguredClasses(myProject);
    Collections.sort(configuredClasses, new Comparator<Class>() {
      public int compare(final Class o1, final Class o2) {
        return getInheritanceLevel(o1) - getInheritanceLevel(o2);
      }

      private int getInheritanceLevel(Class aClass) {
        int level = 0;
        while (aClass.getSuperclass() != null) {
          level++;
          aClass = aClass.getSuperclass();
        }
        return level;
      }
    });

    DefaultMutableTreeNode root = new DefaultMutableTreeNode();
    DefaultTreeModel treeModel = new DefaultTreeModel(root);
    Map<Class, DefaultMutableTreeNode> classToNodeMap = new HashMap<Class, DefaultMutableTreeNode>();
    for (Class cls : configuredClasses) {
      DefaultMutableTreeNode parentNode = root;
      Class superClass = cls.getSuperclass();
      while (superClass != null) {
        if (classToNodeMap.containsKey(superClass)) {
          parentNode = classToNodeMap.get(superClass);
          break;
        }
        superClass = superClass.getSuperclass();
      }
      DefaultMutableTreeNode childNode = new DefaultMutableTreeNode(cls);
      classToNodeMap.put(cls, childNode);
      parentNode.add(childNode);
    }
    myClassTree.setModel(treeModel);
    myClassTree.expandRow(0);
    myClassTree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
    myClassTree.getSelectionModel().setSelectionPath(new TreePath(new Object[]{root, root.getFirstChild()}));
  }

  @Override
  @NonNls
  protected String getDimensionServiceKey() {
    return "ConfigureClientPropertiesDialog";
  }

  private class MyTableModel extends AbstractTableModel {
    public int getRowCount() {
      return mySelectedProperties.size();
    }

    public int getColumnCount() {
      return 2;
    }

    public Object getValueAt(int rowIndex, int columnIndex) {
      switch (columnIndex) {
        case 0:
          return mySelectedProperties.get(rowIndex).getName();
        default:
          return mySelectedProperties.get(rowIndex).getValueClass();
      }
    }

    @Override
    public String getColumnName(int column) {
      switch (column) {
        case 0:
          return UIDesignerBundle.message("client.properties.name");
        default:
          return UIDesignerBundle.message("client.properties.class");
      }
    }
  }
}
