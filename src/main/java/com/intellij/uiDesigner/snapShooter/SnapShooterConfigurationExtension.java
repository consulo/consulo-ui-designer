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

import java.io.IOException;
import java.util.Set;
import java.util.TreeSet;

import javax.annotation.Nonnull;

import org.jdom.Element;
import com.intellij.execution.Location;
import com.intellij.execution.RunConfigurationExtension;
import com.intellij.execution.application.ApplicationConfiguration;
import com.intellij.execution.configurations.RunConfigurationBase;
import com.intellij.execution.configurations.RunnerSettings;
import com.intellij.execution.configurations.RuntimeConfigurationException;
import com.intellij.execution.process.ProcessAdapter;
import com.intellij.execution.process.ProcessEvent;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.ide.ui.LafManagerListener;
import com.intellij.openapi.actionSystem.DataProvider;
import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.components.BaseComponent;
import com.intellij.openapi.components.ProjectComponent;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.util.InvalidDataException;
import com.intellij.openapi.util.WriteExternalException;
import com.intellij.pom.Navigatable;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.lw.LwComponent;
import com.intellij.util.PathUtil;
import com.intellij.util.net.NetUtils;
import com.intellij.xml.util.XmlStringUtil;
import com.jgoodies.forms.layout.FormLayout;
import consulo.java.execution.configurations.OwnJavaParameters;

/**
 * @author yole
 */
public class SnapShooterConfigurationExtension extends RunConfigurationExtension {
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
      paths.add(PathUtil.getJarPathForClass(BaseComponent.class));       // appcore-api
      paths.add(PathUtil.getJarPathForClass(ProjectComponent.class));    // openapi
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
    throws RuntimeConfigurationException {

  }
}
