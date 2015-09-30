// (C) Uri Wilensky. https://github.com/NetLogo/NetLogo

package org.nlogo.awt

class ValueImageFilter(value: Double)
extends java.awt.image.RGBImageFilter {
  require(value >= 0)
  private final val OpaqueAlpha = 0xff000000
  canFilterIndexColorModel = true
  override def filterRGB(x: Int, y: Int, rgb: Int): Int = {
    var red = (rgb >> 16) & 0xff
    var green = (rgb >> 8) & 0xff
    var blue = (rgb) & 0xff
    red = (value * red).toInt
    green = (value * green).toInt
    blue = (value * blue).toInt
    val difference = Math.max(red, Math.max(green, blue)) - 255
    if (difference > 0) {
      red -= difference
      green -= difference
      blue -= difference
    }
    (rgb & OpaqueAlpha) | (red << 16) | (green << 8) | blue
  }
}
