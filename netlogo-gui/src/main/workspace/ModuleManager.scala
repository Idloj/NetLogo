// (C) Uri Wilensky. https://github.com/NetLogo/NetLogo

package org.nlogo.workspace

import org.nlogo.core.{ ErrorSource, FrontEndProcedure, ModuleManager => CoreModuleManager }
import org.nlogo.nvm.CompilerResults

class ModuleManager(workspace: AbstractWorkspace) extends CoreModuleManager {
  private var modules = Map[String, CompilerResults]()

  override def reset(): Unit = {
    modules = Map[String, CompilerResults]()
  }

  override def importModule(name: String, errors: ErrorSource): Unit = {}

  override def getProcedure(name: String): Option[FrontEndProcedure] = None
}
