// (C) Uri Wilensky. https://github.com/NetLogo/NetLogo

package org.nlogo.core

class DummyModuleManager extends ModuleManager {
  override def reset() = {}
  override def importModule(name: String, errors: ErrorSource): Unit = throw new UnsupportedOperationException
  override def getProcedure(name: String): Option[FrontEndProcedure] = None
}
