// (C) Uri Wilensky. https://github.com/NetLogo/NetLogo

package org.nlogo.headless

import java.nio.file.{ Files, Paths }

import org.nlogo.core.{ Femto, LiteralParser }
import org.nlogo.nvm.LabInterface.Settings
import org.nlogo.fileformat
import org.nlogo.xmllib.{ ScalaXmlElement, ScalaXmlElementFactory }

import org.scalatest.FunSuite

import scala.io.Source
import scala.xml.XML

class BehaviorSpaceCoordinatorTests extends FunSuite {
  private val literalParser =
    Femto.scalaSingleton[LiteralParser]("org.nlogo.parse.CompilerUtilities")

  private lazy val nlogoFormat =
    new fileformat.NLogoLabFormat(literalParser)

  private lazy val nlogoxFormat =
    new fileformat.NLogoXLabFormat(ScalaXmlElementFactory)

  test("opens XML setup file written in the old format") {
    pending
  }

  test("opens XML setup file written in the new format") {
    pending
  }

  test("suggests converting XML file written in the old format to use the new format") {
    pending
  }

  test("converts a file written in the old format to use the new format") {
    val tmpFile = Files.createTempFile("converted", "xml")
    val oldPath = Paths.get(TestBehaviorSpace.TestNLogoProtocolsFilePath)
    BehaviorSpaceCoordinator.convertToNewFormat(oldPath, tmpFile)
    val expectedContents = nlogoFormat.load(Source.fromFile(TestBehaviorSpace.TestNLogoProtocolsFilePath).getLines.toArray, None)
    val fileContents = nlogoxFormat.load(new ScalaXmlElement(XML.loadFile(tmpFile.toFile)), None)
    assertResult(expectedContents)(fileContents)
  }
}
