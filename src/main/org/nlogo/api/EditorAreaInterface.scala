// (C) Uri Wilensky. https://github.com/NetLogo/NetLogo

package org.nlogo.api

trait EditorAreaInterface {
  def getSelectionStart: Int
  def getSelectionEnd: Int
  def setSelectionStart(pos: Int): Unit
  def setSelectionEnd(pos: Int): Unit
  def offsetToLine(pos: Int): Int
  def lineToStartOffset(pos: Int): Int
  def lineToEndOffset(pos: Int): Int
  def getText(start: Int, len: Int): String
  def getLineOfText(lineNum: Int): String
  def insertString(pos: Int, spaces: String): Unit
  def replaceSelection(text: String): Unit
  def remove(start: Int, len: Int): Unit
}
