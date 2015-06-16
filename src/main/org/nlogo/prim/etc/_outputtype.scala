// (C) Uri Wilensky. https://github.com/NetLogo/NetLogo

package org.nlogo.prim.etc

import org.nlogo.api.Syntax
import org.nlogo.nvm.{ Command, Context, Workspace }

class _outputtype extends Command {
  override def syntax =
    Syntax.commandSyntax(Array(Syntax.WildcardType))
  override def perform(context: Context) = {
    workspace.outputObject(
      args(0).report(context), null, false, false,
      Workspace.OutputDestination.OUTPUT_AREA)
    context.ip = next
  }
}
