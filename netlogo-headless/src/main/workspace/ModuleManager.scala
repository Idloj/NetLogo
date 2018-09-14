// (C) Uri Wilensky. https://github.com/NetLogo/NetLogo

package org.nlogo.workspace

import org.nlogo.api.SourceOwner
import org.nlogo.core.{ AgentKind, ErrorSource, FrontEndProcedure, I18N,
  ModuleManager => CoreModuleManager, Program }
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
          val compilerResults =
            workspace.compiler.compileProgram("",
              Seq(new Module(path, source)),
              Program.empty,
              workspace.getExtensionManager,
              new ModuleManager(workspace),
              workspace.getCompilationEnvironment)
          compilerResults.procedures.foreach { proc =>
            proc.owner = new ExternalFileInterface(path)
            proc.init(workspace)
          }
          modules = modules.updated(name.toUpperCase, compilerResults)
        case None =>
          errors.signalError(I18N.errors.getN("org.nlogo.workspace.ModuleManager.moduleNotFound", name))
      }
    }
  }

  override def getProcedure(name: String): Option[FrontEndProcedure] = None

  class Module(path: String, var innerSource: String) extends SourceOwner {
    override def classDisplayName: String = path
    override def headerSource: String = ""
    override def source: String = headerSource + innerSource
    override def kind: AgentKind = AgentKind.Observer
  }
}
