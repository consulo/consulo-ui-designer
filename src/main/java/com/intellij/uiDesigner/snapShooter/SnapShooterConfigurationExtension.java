/*
 * Copyright 2000-2012 JetBrains s.r.o.
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

package com.intellij.uiDesigner.snapShooter;

import com.intellij.java.execution.impl.RunConfigurationExtension;
import com.intellij.java.execution.impl.application.ApplicationConfiguration;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.lw.LwComponent;
import com.jgoodies.forms.layout.FormLayout;
import consulo.dataContext.DataProvider;
import consulo.execution.RuntimeConfigurationException;
import consulo.execution.action.Location;
import consulo.execution.configuration.RunConfigurationBase;
import consulo.execution.configuration.RunnerSettings;
import consulo.execution.configuration.ui.SettingsEditor;
import consulo.ide.impl.idea.ide.ui.LafManagerListener;
import consulo.ide.impl.idea.openapi.application.PathManager;
import consulo.ide.impl.idea.util.PathUtil;
import consulo.ide.impl.idea.util.net.NetUtils;
import consulo.java.execution.configurations.OwnJavaParameters;
import consulo.navigation.Navigatable;
import consulo.process.ProcessHandler;
import consulo.process.event.ProcessAdapter;
import consulo.process.event.ProcessEvent;
import consulo.util.lang.xml.XmlStringUtil;
import consulo.util.xml.serializer.InvalidDataException;
import consulo.util.xml.serializer.WriteExternalException;
import org.jdom.Element;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.Set;
import java.util.TreeSet;

/**
 * @author yole
 */
public class SnapShooterConfigurationExtension extends RunConfigurationExtension
{
  @Override
  public void updateJavaParameters(RunConfigurationBase configuration, OwnJavaParameters params, RunnerSettings runnerSettings) {
    if (!isApplicableFor(configuration)) {
      return;
    }
    ApplicationConfiguration appConfiguration = (ApplicationConfiguration) configuration;
    SnapShooterConfigurationSettings settings = appConfiguration.getUserData(SnapShooterConfigurationSettings.SNAP_SHOOTER_KEY);
    if (settings == null) {
      settings = new SnapShooterConfigurationSettings();
      appConfiguration.putUserData(SnapShooterConfigurationSettings.SNAP_SHOOTER_KEY, settings);
    }
    if (appConfiguration.ENABLE_SWING_INSPECTOR) {
      try {
        settings.setLastPort(NetUtils.findAvailableSocketPort());
      }
      catch(IOException ex) {
        settings.setLastPort(-1);
      }
    }

    if (appConfiguration.ENABLE_SWING_INSPECTOR && settings.getLastPort() != -1) {
      params.getProgramParametersList().prepend(appConfiguration.MAIN_CLASS_NAME);
      params.getProgramParametersList().prepend(Integer.toString(settings.getLastPort()));
      // add +1 because idea_rt.jar will be added as the last entry to the classpath
      params.getProgramParametersList().prepend(Integer.toString(params.getClassPath().getPathList().size() + 1));
      Set<String> paths = new TreeSet<String>();
      paths.add(PathUtil.getJarPathForClass(SnapShooter.class));         // ui-designer-impl
      paths.add(PathUtil.getJarPathForClass(LwComponent.class));         // UIDesignerCore
      paths.add(PathUtil.getJarPathForClass(GridConstraints.class));     // forms_rt
      paths.add(PathUtil.getJarPathForClass(LafManagerListener.class));  // ui-impl
      paths.add(PathUtil.getJarPathForClass(DataProvider.class));        // action-system-openapi
      paths.add(PathUtil.getJarPathForClass(XmlStringUtil.class));       // idea
      paths.add(PathUtil.getJarPathForClass(Navigatable.class));         // pom
      paths.add(PathUtil.getJarPathForClass(FormLayout.class));          // jgoodies
      paths.addAll(PathManager.getUtilClassPath());
      for(String path: paths) {
        params.getClassPath().addFirst(path);
      }
      params.setMainClass("com.intellij.uiDesigner.snapShooter.SnapShooter");
    }
  }

  protected boolean isApplicableFor(@Nonnull RunConfigurationBase configuration) {
    return configuration instanceof ApplicationConfiguration;
  }

  public void attachToProcess(@Nonnull final RunConfigurationBase configuration, @Nonnull final ProcessHandler handler, RunnerSettings runnerSettings) {
    SnapShooterConfigurationSettings settings = configuration.getUserData(SnapShooterConfigurationSettings.SNAP_SHOOTER_KEY);
    if (settings != null) {
      final Runnable runnable = settings.getNotifyRunnable();
      if (runnable != null) {
        settings.setNotifyRunnable(null);
        handler.addProcessListener(new ProcessAdapter() {
          public void startNotified(final ProcessEvent event) {
            runnable.run();
          }
        });
      }
    }
  }

  @Override
  public SettingsEditor createEditor(@Nonnull RunConfigurationBase configuration) {
    return null;
  }

  @Override
  public String getEditorTitle() {
    return null;
  }

  @Nonnull
  @Override
  public String getSerializationId() {
    return "snapshooter";
  }

  @Override
  public void readExternal(@Nonnull RunConfigurationBase runConfiguration, @Nonnull Element element) throws InvalidDataException {
  }

  @Override
  public void writeExternal(@Nonnull RunConfigurationBase runConfiguration, @Nonnull Element element) throws WriteExternalException {
    throw new WriteExternalException();
  }

  @Override
  public void extendCreatedConfiguration(@Nonnull RunConfigurationBase runJavaConfiguration, @Nonnull Location location) {
  }

  @Override
  public void validateConfiguration(@Nonnull RunConfigurationBase runJavaConfiguration, boolean isExecution)
    throws RuntimeConfigurationException
  {

  }
}
