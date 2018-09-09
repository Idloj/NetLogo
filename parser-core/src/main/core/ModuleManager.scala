package org.nlogo.core

trait ModuleManager {
  /**
    * During compilation, we reach the modules [ ... ] block.
    * When that happens, the compiler tells the ModuleManager that it needs to
    * forget what modules are in the modules [ ... ] block, by calling this method.
    *
    * The compiler will then call the importModule method for each module in the block.
    */
  def reset(): Unit

  def importModule(name: String, errors: ErrorSource): Unit

  /** Returns the identifier "name" by its imported implementation, if any, or null if not. */
  def getProcedure(name: String): Option[FrontEndProcedure]
}
