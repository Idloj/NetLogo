// (C) Uri Wilensky. https://github.com/NetLogo/NetLogo

package org.nlogo.api

import java.io.{ Serializable => JSerializable }

trait HubNetInterface extends ViewInterface {
  /// getting messages
  @throws(classOf[LogoException])
  def messageWaiting: Boolean
  def numberOfMessagesWaiting: Int
  @throws(classOf[LogoException])
  def enterMessage: Boolean
  @throws(classOf[LogoException])
  def exitMessage: Boolean
  @throws(classOf[LogoException])
  def fetchMessage(): Unit
  @throws(classOf[LogoException])
  def getMessage: AnyRef
  @throws(classOf[LogoException])
  def getMessageSource: String
  @throws(classOf[LogoException])
  def getMessageTag: String

  /// sending messages

  /**
   * Send a message to all clients
   */
  @throws(classOf[LogoException])
  def broadcast(variableName: String, data: Any): Unit
  @throws(classOf[LogoException])
  def broadcast(data: Any): Unit
  def sendText(nodes: Seq[String], text: String): Unit
  def clearText(nodes: Seq[String]): Unit
  def broadcastClearText(): Unit
  def sendUserMessage(nodes: Seq[String], text: String): Unit

  @throws(classOf[LogoException])
  def broadcastUserMessage(text: String): Unit

  /**
   * Send a message to each node (client) in the list for the given tag
   */
  @throws(classOf[LogoException])
  def send(nodes: Seq[String], tag: String, message: JSerializable): Unit

  /**
   * Send message to a single client for the given tag
   */
  @throws(classOf[LogoException])
  def send(node: String, tag: String, message: JSerializable): Boolean

  /// connection management
  def disconnect(): Unit
  def reset(): Unit
  // list of all clients connected to the server (their names)
  def clients:Iterable[String]
  // wait until n clients are connected to hubnet.
  def waitForClients(numClientsToWaitFor:Int, timeoutMillis: Long): (Boolean, Int)
  // wait until n messages are queued up on the server.
  def waitForMessages(numMessagesToWaitFor:Int, timeoutMillis: Long): (Boolean, Int)
  def kick(clientName:String): Unit
  def kickAll(): Unit
  def setViewMirroring(on:Boolean): Unit
  def setPlotMirroring(on:Boolean): Unit

  /// clients
  @throws(classOf[LogoException])
  def setClientInterface(clientType: String, interfaceInfo:Iterable[AnyRef]): Unit
  def newClient(isRobo: Boolean, waitTime: Int): Unit
  def sendFromLocalClient(clientName:String, tag: String, content: AnyRef): Option[String]
  def isOverridable(agentType: Class[_ <: org.nlogo.api.Agent], varName: String): Boolean
  def sendOverrideList(client: String, agentType: Class[_ <: org.nlogo.api.Agent],
                       varName: String, overrides: Map[java.lang.Long, AnyRef]): Unit
  def clearOverride(client: String, agentType: Class[_ <: org.nlogo.api.Agent],
                    varName: String, overrides: Seq[java.lang.Long]): Unit
  def clearOverrideLists(client: String): Unit
  def sendAgentPerspective(client: String, perspective: Int, agentType: Class[_ <: org.nlogo.api.Agent],
                           id: Long, radius: Double, serverMode: Boolean): Unit
  /// view updates
  def incrementalUpdateFromEventThread(): Unit

  /// mirror drawing
  def sendLine(x0: Double, y0: Double, x1: Double, y1: Double,
               color: Any, size: Double, mode: String): Unit

  def sendStamp(agent: Agent, erase: Boolean): Unit
  def sendClear(): Unit

  /// control center
  def showControlCenter(): Unit

  /// network info
  def getOutQueueSize: Double
  def getInQueueSize: Int

  /// client editor
  def save(buf: StringBuilder): Unit
  def closeClientEditor(): Unit
  def openClientEditor(): Unit
  def clientEditor: AnyRef
  def load(lines: Array[String], version: String): Unit
  @throws(classOf[java.io.IOException])
  def importClientInterface(path: String, client: Boolean): Unit
  def setTitle(title: String, directory: String, modelType: ModelType): Unit
  def getInterfaceWidth: Int
  def getInterfaceHeight: Int

  /// narrowcast plotting
  def addNarrowcastPlot(plotName: String): Boolean
  def plot(clientId: String, y: Double): Unit
  def plot(clientId: String, x: Double, y: Double): Unit
  def clearPlot(clientId: String): Unit
  def plotPenDown(clientId: String, penDown: Boolean): Unit
  def setPlotPenMode(clientId: String, plotPenMode: Int): Unit
  def setHistogramNumBars(clientId: String, num: Int): Unit
  def setPlotPenInterval(clientId: String, interval: Double): Unit
}
