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
package com.intellij.uiDesigner.impl.quickFixes;

import com.intellij.uiDesigner.impl.ErrorInfo;
import com.intellij.uiDesigner.impl.UIDesignerBundle;
import com.intellij.uiDesigner.impl.designSurface.GuiEditor;
import com.intellij.uiDesigner.impl.radComponents.RadComponent;
import consulo.application.AllIcons;
import consulo.ide.impl.idea.ui.LightweightHintImpl;
import consulo.language.editor.DaemonCodeAnalyzer;
import consulo.logging.Logger;
import consulo.ui.ex.awt.IJSwingUtilities;
import consulo.ui.ex.awt.hint.HintHint;
import consulo.ui.ex.awt.hint.LightweightHint;
import consulo.ui.ex.awt.util.Alarm;
import consulo.ui.ex.popup.BaseListPopupStep;
import consulo.ui.ex.popup.JBPopupFactory;
import consulo.ui.ex.popup.ListPopup;
import consulo.ui.ex.popup.PopupStep;
import consulo.undoRedo.CommandProcessor;
import consulo.util.lang.Pair;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.util.ArrayList;

/**
 * @author Anton Katilin
 * @author Vladimir Kondratyev
 */
public abstract class QuickFixManager <T extends JComponent>{
  private static final Logger LOG = Logger.getInstance(QuickFixManager.class);

  private GuiEditor myEditor;
  /** Component on which hint will be shown */
  protected final T myComponent;
  /**
   * This alarm contains request for showing of hint
   */
  private final Alarm myAlarm;
  /**
   * This request updates visibility of the hint
   */
  private final MyShowHintRequest myShowHintRequest;
  /**
   * My currently visible hint. May be null if there is no visible hint
   */
  private LightweightHint myHint;
  private Rectangle myLastHintBounds;

  public QuickFixManager(@Nullable final GuiEditor editor, @Nonnull final T component, @Nonnull final JViewport viewPort) {
    myEditor = editor;
    myComponent = component;
    myAlarm = new Alarm();
    myShowHintRequest = new MyShowHintRequest(this);

    (new VisibilityWatcherImpl(this, component)).install(myComponent);
    myComponent.addFocusListener(new FocusListenerImpl(this));

    // Alt+Enter
    new ShowHintAction(this, component);

    viewPort.addChangeListener(new ChangeListener() {
      public void stateChanged(ChangeEvent e) {
        updateIntentionHintPosition(viewPort);
      }
    });
  }

  public final GuiEditor getEditor(){
    return myEditor;
  }

  public void setEditor(final GuiEditor editor) {
    myEditor = editor;
  }

  /**
   * @return error info for the current {@link #myComponent} state.
   */
  @Nonnull
  protected abstract ErrorInfo[] getErrorInfos();

  /**
   * @return rectangle (in {@link #myComponent} coordinates) that represents
   * area that contains errors. This methods is invoked only if {@link #getErrorInfos()}
   * returned non empty list of error infos. <code>null</code> means that
   * error bounds are not defined.
   */
  @Nullable
  protected abstract Rectangle getErrorBounds();

  public void refreshIntentionHint() {
    if(!myComponent.isShowing() || !IJSwingUtilities.hasFocus(myComponent)){
      hideIntentionHint();
      return;
    }
    if (myHint == null || !myHint.isVisible()) {
      updateIntentionHintVisibility();
    }
    else {
      final ErrorInfo[] errorInfos = getErrorInfos();
      final Rectangle bounds = getErrorBounds();
      if (!haveFixes(errorInfos) || bounds == null || !bounds.equals(myLastHintBounds)) {
        hideIntentionHint();
        updateIntentionHintVisibility();
      }
    }
  }

  /**
   * Adds in timer queue requst for updating visibility of the popup hint
   */
  public final void updateIntentionHintVisibility(){
    myAlarm.cancelAllRequests();
    myAlarm.addRequest(myShowHintRequest, 500);
  }

  /**
   * Shows intention hint (light bulb) if it's not visible yet.
   */
  final void showIntentionHint(){
    if(!myComponent.isShowing() || !IJSwingUtilities.hasFocus(myComponent)){
      hideIntentionHint();
      return;
    }

    // 1. Hide previous hint (if any)
    hideIntentionHint();

    // 2. Found error (if any)
    final ErrorInfo[] errorInfos = getErrorInfos();
    if(!haveFixes(errorInfos)) {
      hideIntentionHint();
      return;
    }

    // 3. Determine position where this hint should be shown
    final Rectangle bounds = getErrorBounds();
    if(bounds == null){
      return;
    }

    // 4. Show light bulb to fix this error
    final LightBulbComponentImpl lightBulbComponent = new LightBulbComponentImpl(this, AllIcons.Actions.IntentionBulb);
    myHint = new LightweightHintImpl(lightBulbComponent);
    myLastHintBounds = bounds;
    myHint.show(myComponent, bounds.x - AllIcons.Actions.IntentionBulb.getWidth() - 4, bounds.y, myComponent, new HintHint(myComponent, bounds.getLocation()));
  }

  private void updateIntentionHintPosition(final JViewport viewPort) {
    if (myHint != null && myHint.isVisible()) {
      Rectangle rc = getErrorBounds();
      if (rc != null) {
        myLastHintBounds = rc;
        Rectangle hintRect = new Rectangle(rc.x - AllIcons.Actions.IntentionBulb.getWidth() - 4, rc.y, AllIcons.Actions.IntentionBulb
                                                                                                             .getWidth() + 4, AllIcons.Actions.IntentionBulb
                                                                                                                                    .getHeight() + 4);
        LOG.debug("hintRect=" + hintRect);
        if (getHintClipRect(viewPort).contains(hintRect)) {
          myHint.pack();
        }
        else {
          myHint.hide();
        }
      }
    }
  }

  protected Rectangle getHintClipRect(final JViewport viewPort) {
    return viewPort.getViewRect();
  }

  private static boolean haveFixes(final ErrorInfo[] errorInfos) {
    boolean haveFixes = false;
    for(ErrorInfo errorInfo: errorInfos) {
      if (errorInfo.myFixes.length > 0 || errorInfo.getInspectionId() != null) {
        haveFixes = true;
        break;
      }
    }
    return haveFixes;
  }

  /**
   * Hides currently visible hint (light bulb) .If any.
   */
  public final void hideIntentionHint(){
    myAlarm.cancelAllRequests();
    if(myHint != null && myHint.isVisible()){
      myHint.hide();
      myComponent.paintImmediately(myComponent.getVisibleRect());
    }
  }

  final void showIntentionPopup(){
    LOG.debug("showIntentionPopup()");
    if(myHint == null || !myHint.isVisible()){
      return;
    }
    final ErrorInfo[] errorInfos = getErrorInfos();
    if(!haveFixes(errorInfos)){
      return;
    }

    final ArrayList<ErrorWithFix> fixList = new ArrayList<ErrorWithFix>();
    for(ErrorInfo errorInfo: errorInfos) {
      final QuickFix[] quickFixes = errorInfo.myFixes;
      if (quickFixes.length > 0) {
        for (QuickFix fix: quickFixes) {
          fixList.add(new ErrorWithFix(errorInfo, fix));
        }
      }
      else if (errorInfo.getInspectionId() != null) {
        buildSuppressFixes(errorInfo, fixList, true);
      }
    }

    final ListPopup popup = JBPopupFactory.getInstance().createListPopup(new QuickFixPopupStep(fixList, true));
    popup.showUnderneathOf(((LightweightHintImpl) myHint).getComponent());
  }

  private void buildSuppressFixes(final ErrorInfo errorInfo, final ArrayList<ErrorWithFix> suppressList, boolean named) {
    final String suppressName = named
                                ? UIDesignerBundle.message("action.suppress.named.for.component", errorInfo.myDescription)
                                : UIDesignerBundle.message("action.suppress.for.component");
    final String suppressAllName = named
                                ? UIDesignerBundle.message("action.suppress.named.for.all.components", errorInfo.myDescription)
                                : UIDesignerBundle.message("action.suppress.for.all.components");

    final SuppressFix suppressFix = new SuppressFix(myEditor, suppressName,
                                                    errorInfo.getInspectionId(), errorInfo.getComponent());
    final SuppressFix suppressAllFix = new SuppressFix(myEditor, suppressAllName,
                                                       errorInfo.getInspectionId(), null);
    suppressList.add(new ErrorWithFix(errorInfo, suppressFix));
    suppressList.add(new ErrorWithFix(errorInfo, suppressAllFix));
  }

  private static class ErrorWithFix extends Pair<ErrorInfo, QuickFix> {
    public ErrorWithFix(final ErrorInfo first, final QuickFix second) {
      super(first, second);
    }
  }

  private class QuickFixPopupStep extends BaseListPopupStep<ErrorWithFix>
  {
    private final boolean myShowSuppresses;

    public QuickFixPopupStep(final ArrayList<ErrorWithFix> fixList, boolean showSuppresses) {
      super(null, fixList);
      myShowSuppresses = showSuppresses;
    }

    @Nonnull
    public String getTextFor(final ErrorWithFix value) {
      return value.second.getName();
    }

    public PopupStep onChosen(final ErrorWithFix selectedValue, final boolean finalChoice) {
      if (selectedValue.second instanceof PopupQuickFix) {
        return ((PopupQuickFix) selectedValue.second).getPopupStep();
      }
      if (finalChoice || !myShowSuppresses) {
        return doFinalStep(new Runnable() {
          public void run() {
            CommandProcessor.getInstance().executeCommand(myEditor.getProject(), new Runnable() {
              public void run() {
                selectedValue.second.run();
              }
            }, selectedValue.second.getName(), null);
          }
        });
      }
      if (selectedValue.first.getInspectionId() != null && selectedValue.second.getComponent() != null &&
          !(selectedValue.second instanceof SuppressFix)) {
        ArrayList<ErrorWithFix> suppressList = new ArrayList<ErrorWithFix>();
        buildSuppressFixes(selectedValue.first, suppressList, false);
        return new QuickFixPopupStep(suppressList, false);
      }
      return FINAL_CHOICE;
    }

    public boolean hasSubstep(final ErrorWithFix selectedValue) {
      return (myShowSuppresses && selectedValue.first.getInspectionId() != null && selectedValue.second.getComponent() != null &&
        !(selectedValue.second instanceof SuppressFix)) || selectedValue.second instanceof PopupQuickFix;
    }

    @Override public boolean isAutoSelectionEnabled() {
      return false;
    }
  }

  private static class SuppressFix extends QuickFix {
    private final String myInspectionId;

    public SuppressFix(final GuiEditor editor, final String name, final String inspectionId, final RadComponent component) {
      super(editor, name, component);
      myInspectionId = inspectionId;
    }

    public void run() {
      if (!myEditor.ensureEditable()) return;
      myEditor.getRootContainer().suppressInspection(myInspectionId, myComponent);
      myEditor.refreshAndSave(true);
      DaemonCodeAnalyzer.getInstance(myEditor.getProject()).restart();
    }
  }

  private final class MyShowHintRequest implements Runnable{
    private final QuickFixManager myManager;

    public MyShowHintRequest(@Nonnull final QuickFixManager manager) {
      myManager = manager;
    }

    public void run() {
      myManager.showIntentionHint();
    }
  }
}
