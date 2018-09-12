// (C) Uri Wilensky. https://github.com/NetLogo/NetLogo

package org.nlogo.workspace

import org.nlogo.core.{ ErrorSource, FrontEndProcedure, I18N, ModuleManager => CoreModuleManager, Program }
import org.nlogo.nvm.CompilerResults
import org.nlogo.parse.IncludeFile

class ModuleManager(workspace: AbstractWorkspace) extends CoreModuleManager {
  private var modules = Map[String, CompilerResults]()

  override def reset(): Unit = {
    modules = Map[String, CompilerResults]()
  }

  override def importModule(name: String, errors: ErrorSource): Unit = {
    if (!modules.contains(name.toUpperCase)) {
      IncludeFile(workspace.getCompilationEnvironment, name + ".nls") match {
        case Some((path, source)) =>
          modules = modules.updated(name.toUpperCase,
            workspace.compiler.compileProgram(
              source,
              Program.fromDialect(workspace.dialect),
              workspace.getExtensionManager,
              new ModuleManager(workspace),
              workspace.getCompilationEnvironment))
        case None =>
          errors.signalError(I18N.errors.getN("org.nlogo.workspace.ModuleManager.moduleNotFound", name))
      }
    }
  }

  override def getProcedure(name: String): Option[FrontEndProcedure] = None
}
