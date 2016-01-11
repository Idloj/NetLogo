// (C) Uri Wilensky. https://github.com/NetLogo/NetLogo

package org.nlogo.app

import org.nlogo.api.{ I18N, ModelSection }
import org.nlogo.swing.ToolBar
import org.nlogo.workspace.AbstractWorkspace

// This is THE Code tab.  Certain settings and things that are only accessible here.
// Other Code tabs come and go.

class MainCodeTab(workspace: AbstractWorkspace)
extends CodeTab(workspace)
with org.nlogo.window.Events.LoadSectionEvent.Handler
{
  override def getToolBar =
    new ToolBar {
      override def addControls() {
        add(new javax.swing.JButton(
          org.nlogo.app.FindDialog.FIND_ACTION))
        add(new javax.swing.JButton(compileAction))
        add(new org.nlogo.swing.ToolBar.Separator)
        add(new ProceduresMenu(MainCodeTab.this))
        // we add this here, however, unless there are includes it will not be displayed, as it sets
        // it's preferred size to 0x0 -- CLB
        add(new IncludesMenu(MainCodeTab.this))
      }
    }

  def handle(e: org.nlogo.window.Events.LoadSectionEvent) {
    if(e.section == ModelSection.Code) {
      innerSource(workspace.autoConvert(e.text, false, false, e.version))
      recompile()
    }
  }
}
