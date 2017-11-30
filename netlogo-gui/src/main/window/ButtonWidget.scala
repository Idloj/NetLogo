// (C) Uri Wilensky. https://github.com/NetLogo/NetLogo

package org.nlogo.window

import java.awt.{ Color, Cursor, Dimension, Font, Graphics, Graphics2D, RenderingHints }
import java.awt.Toolkit.getDefaultToolkit
import java.awt.event.{ MouseEvent, MouseListener }
import java.awt.image.FilteredImageSource
import javax.swing.ImageIcon

import org.nlogo.api.{ Editable, MersenneTwisterFast, Options, Version }
import org.nlogo.awt.{ DarkenImageFilter, Fonts, Mouse }, Mouse.hasButton1
import org.nlogo.core.{ AgentKind, I18N, Button => CoreButton }
import org.nlogo.swing.Utils.icon

object ButtonWidget {

  private val FOREVER_GRAPHIC: ImageIcon = icon("/images/forever.gif")
  private val FOREVER_GRAPHIC_PRESSED: ImageIcon = icon("/images/forever-pressed.gif")

  private object ButtonType {

    private def darkenedImage(image: ImageIcon) = new ImageIcon(getDefaultToolkit.createImage(
      new FilteredImageSource(image.getImage.getSource, new DarkenImageFilter(0.5))))

    private def apply(headerCode: String, agentKind: AgentKind, forever: Boolean, imagePath: String): ButtonType = {
      val img = icon(imagePath)
      ButtonType(headerCode, agentKind, forever, Some(img), Some(darkenedImage(img)))
    }

    // If creating a ButtonKind every time turns out as wasteful, we can store
    // all the 8 possible combinations in advance -- Idloj 11/27/2017
    def apply(kind: AgentKind, forever: Boolean): ButtonType = kind match {
      case AgentKind.Observer  => ButtonType("observer", AgentKind.Observer, forever, img = None, pressedImg = None)
      case AgentKind.Turtle    => ButtonType("turtle", AgentKind.Turtle,     forever, "/images/turtle.gif")
      case AgentKind.Link      => ButtonType("link", AgentKind.Link,         forever, "/images/link.gif")
      case AgentKind.Patch     => ButtonType("patch", AgentKind.Patch,       forever, "/images/patch.gif")
    }
  }

  private case class ButtonType(name: String, agentKind: AgentKind, forever: Boolean,
                        img: Option[ImageIcon], pressedImg: Option[ImageIcon]) {
    def img(pressed: Boolean): Option[ImageIcon] = if (pressed) pressedImg else img
    val header = s"to __button [] __${name}code " + (if (forever) " loop [ " else "")
    val footer = "\n" + // protect against comments
      (if (forever) "__foreverbuttonend ] " else "__done ") + "end"
  }
}

class ButtonWidget(random: MersenneTwisterFast) extends JobWidget(random)
  with Editable with MouseListener
  with Events.JobRemovedEvent.Handler with Events.TickStateChangeEvent.Handler {

  import ButtonWidget._

  type WidgetModel = CoreButton

  locally {
    addMouseListener(this)
    setBackground(InterfaceColors.BUTTON_BACKGROUND)
    Fonts.adjustDefaultFont(this)
  }

  private var _buttonType: ButtonType = ButtonType(AgentKind.Observer, false)
  private def buttonType = _buttonType
  private def buttonType_=(bt: ButtonType) = {
    _buttonType = bt
    recompile()
    repaint()
  }

  // buttonType controls the agentKind. no one should ever be setting
  // agentKind from outside of this class anyway.
  // the ui edits work through agent options, which just set the button type
  override def kind = buttonType.agentKind
  override def agentKind(c: AgentKind) { /* ignoring, no one should call this. */ }

  private var _name = ""
  def name = _name
  def name_=(n: String): Unit = {
    _name = n
    chooseDisplayName()
  }

  val agentOptions = new Options[AgentKind] {
    implicit val i18nPrefix = I18N.Prefix("common")
    addOption(I18N.gui("observer"), AgentKind.Observer)
    addOption(I18N.gui("turtles"), AgentKind.Turtle)
    addOption(I18N.gui("patches"), AgentKind.Patch)
    addOption(I18N.gui("links"), AgentKind.Link)
    selectValue(AgentKind.Observer)

    override def selectByName(s: String) = if (chosenName != s) {
      super.selectByName(s)
      buttonType = ButtonType(chosenValue, forever)
    }
  }
  def agentOptions_=(ao: Options[AgentKind]) = { /* we update the button agent from selectByName, which is called from OptionsEditor */ }

  var needsTicks = false
  private var ticksInitialized = false

  private var _buttonUp= true
  private def buttonUp = _buttonUp
  private def buttonUp_=(u: Boolean): Unit = {
    _buttonUp = u
    if (u) {
      setBorder(widgetBorder)
    } else {
      setBorder(widgetPressedBorder)
      // this is an attempt to get the button to invert for at least
      // a fraction of a second when a keyboard shortcut is used on
      // a once button - ST 8/6/04
      paintImmediately(0, 0, getWidth, getHeight)
    }
  }

  def forever = buttonType.forever
  def forever_=(f: Boolean) =
    if (forever != f)
      buttonType = buttonType.copy(forever = f)

  /// keyboard stuff

  private var _actionKey: Option[Char] = None
  def actionKey = _actionKey.getOrElse(0.toChar)
  def actionKey_=(ak: Char) = if (actionKey != ak) {
    _actionKey = ak match {
      case 0 => None
      case _ => Some(ak)
    }
    repaint()
  }

  private var _keyEnabled = false
  def keyEnabled = _keyEnabled
  def keyEnabled_=(ke: Boolean): Unit = if(keyEnabled != ke) {
    _keyEnabled = ke
    repaint()
  }

  def keyTriggered(): Unit = respondToClick()

  // This is used in mouseReleased to know if we're just bringing the popup menu up,
  // and therefore it shouldn't be considered a button click -- Idloj 12/01/2017
  private var lastMousePressedWasPopupTrigger = false

  /// mouse handlers

  private def disabledWaitingForSetup = needsTicks && !ticksInitialized

  override def mousePressed(e: MouseEvent) = {
    new Events.InputBoxLoseFocusEvent().raise(this)
    lastMousePressedWasPopupTrigger = e.isPopupTrigger
    if (!anyErrors && !e.isPopupTrigger && hasButton1(e) && !disabledWaitingForSetup)
      buttonUp = false
  }
  override def mouseReleased(e: MouseEvent) =
    if (!lastMousePressedWasPopupTrigger && getBounds().contains(e.getPoint))
      respondToClick()
  override def mouseClicked(e: MouseEvent) = {}
  override def mouseEntered(e: MouseEvent) = {}
  override def mouseExited(e: MouseEvent)  = if (!buttonUp && !running) buttonUp = true

  private def respondToClick(): Unit = {
    if (anyErrors)
      new Events.EditWidgetEvent(this).raise(this)
    else if (!disabledWaitingForSetup)
      action()
  }

  /// editability
  override def classDisplayName = I18N.gui.get("tabs.run.widgets.button")
  def propertySet = Properties.button

  /// compilation & jobs
  
  private var _running = false
  def running = _running
  private def running_=(r: Boolean) = {
    _running = r
    buttonUp = !r
  }

  private var stoppedByUser = false

  override def isButton = true
  override def isTurtleForeverButton = buttonType.agentKind == AgentKind.Turtle && forever
  override def isLinkForeverButton = buttonType.agentKind == AgentKind.Link && forever

  private def action(): Unit = {
    assert(!anyErrors)

    if (running) {
      setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR))
      if (forever) {
        stoppedByUser = true
        new Events.JobStoppingEvent(this).raise(this)
      }
    } else {
      new Events.AddJobEvent(this, agents, procedure).raise(this)
      if (Version.isLoggingEnabled)
        org.nlogo.log.Logger.logButtonPressed(displayName)
      running = true
    }
  }

  def handle(e: Events.JobRemovedEvent) = if (e.owner == this) {
    if (Version.isLoggingEnabled)
      org.nlogo.log.Logger.logButtonStopped(displayName, !forever, stoppedByUser)
    popUpStoppingButton()
  }

  def handle(e: Events.TickStateChangeEvent) = {
    ticksInitialized = e.tickCounterInitialized
    repaint()
  }

  def popUpStoppingButton() = {
    running = false
    stoppedByUser = false
    setCursor(null)
  }

  private def chooseDisplayName() = displayName(if (name != "") name else getSourceAsName)

  // behold the mighty regular expression
  private def getSourceAsName: String = innerSource.trim.replaceAll("\\s+", " ")

  override def innerSource_=(src: String) = if (src != "" && src != innerSource) {
    super.innerSource_=(src)
    chooseDisplayName()
    recompile()
  }

  private def recompile(): Unit = {
    new Events.RemoveJobEvent(this).raise(this)
    source(buttonType.header, innerSource, buttonType.footer)
  }

  /// sizing

  override def getMinimumSize = new Dimension(55, 33)
  override def getPreferredSize(font: Font) = {
    val fm = getFontMetrics(font)
    val size = getMinimumSize
    size.width  = size.width  max (fm.stringWidth(displayName) + 28)
    size.height = size.height max (fm.getMaxDescent + fm.getMaxAscent + 12)
    size
  }

  /// painting

  override def paintComponent(g: Graphics) {
    g.asInstanceOf[Graphics2D].setRenderingHint(
      RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
    val getPaintColor = if (buttonUp) getBackground else getForeground
    def paintButtonRectangle(g: Graphics) = {
      g.setColor(getPaintColor)
      g.fillRect(0, 0, getWidth, getHeight)
      setBorder(if (buttonUp) widgetBorder else widgetPressedBorder)
      def renderImages(g: Graphics) {
        def maybePaintForeverImage() {
          if (forever) {
            val image = if (buttonUp) FOREVER_GRAPHIC else FOREVER_GRAPHIC_PRESSED
            image.paintIcon(this, g, getWidth - image.getIconWidth - 4, getHeight - image.getIconHeight - 4)
          }
        }
        def maybePaintAgentImage() {
          buttonType.img(!buttonUp).map(_.paintIcon(this, g, 3, 3))
        }
        maybePaintForeverImage()
        maybePaintAgentImage()
      }
      renderImages(g)
    }
    def paintKeyboardShortcut(g: Graphics) = {
      val actionKeyString = _actionKey.map(_.toString).getOrElse("")
      if (actionKeyString != "") {
        val ax = getSize().width - 4 - g.getFontMetrics.stringWidth(actionKeyString)
        val ay = g.getFontMetrics.getMaxAscent + 2
        if (buttonUp)
          g.setColor(if (keyEnabled) Color.BLACK else Color.GRAY)
        else
          g.setColor(if (keyEnabled && forever) getBackground else Color.BLACK)
        g.drawString(actionKeyString, ax - 1, ay)
      }
    }
    def paintButtonText(g: Graphics) = {
      val stringWidth = g.getFontMetrics.stringWidth(displayName)
      val color = {
        val c = if (buttonUp) getForeground else getBackground
        if (disabledWaitingForSetup && !anyErrors) Color.GRAY else c
      }
      g.setColor(color)
      val availableWidth = getSize().width - 8
      val shortString = org.nlogo.awt.Fonts.shortenStringToFit(displayName, availableWidth, g.getFontMetrics)
      val nx = if (stringWidth > availableWidth) 4 else (getSize().width / 2) - (stringWidth / 2)
      val labelHeight = g.getFontMetrics.getMaxDescent + g.getFontMetrics.getMaxAscent
      val ny = (getSize().height / 2) + (labelHeight / 2)
      g.drawString(shortString, nx, ny)
      setToolTipText(if (displayName != shortString) displayName else null)
    }
    paintButtonRectangle(g)
    paintButtonText(g)
    paintKeyboardShortcut(g)
  }

  // saving and loading
  override def model: WidgetModel = {
    val b              = getBoundsTuple
    val savedActionKey = if (actionKey == 0 || actionKey == ' ') None else Some(actionKey)
    CoreButton(
      display = name.potentiallyEmptyStringToOption,
      left = b._1, top = b._2, right = b._3, bottom = b._4,
      source    = innerSource.potentiallyEmptyStringToOption,
      forever   = buttonType.forever,        buttonKind             = buttonType.agentKind,
      actionKey = savedActionKey, disableUntilTicksStart = needsTicks)
  }

  override def load(button: WidgetModel): AnyRef = {
    name = button.display.optionToPotentiallyEmptyString
    setSize(button.right - button.left, button.bottom - button.top)
    button.source.foreach(innerSource_=)
    buttonType = ButtonType(button.buttonKind, button.forever)
    button.actionKey.foreach(actionKey_=)
    needsTicks = button.disableUntilTicksStart
    this
  }
}
