// (C) Uri Wilensky. https://github.com/NetLogo/NetLogo

package org.nlogo.headless

import
  java.nio.file.{ Files, Path, Paths }

import
  javax.xml.transform.{ OutputKeys, TransformerFactory, stream },
    stream.{ StreamResult, StreamSource }

import
  org.nlogo.{ api, core, fileformat, nvm, workspace },
    core.{ Femto, LiteralParser, Model },
    api.{ FileIO, LabProtocol, Version, Workspace },
    nvm.LabInterface.Settings,
    workspace.OpenModelFromURI

import
  scala.util.{ Failure, Success }

import
  scala.io.Source

object BehaviorSpaceCoordinator {
  val ConversionStylesheetResource = "/system/behaviorspace-to-nlogox.xslt"

  private val literalParser =
    Femto.scalaSingleton[LiteralParser]("org.nlogo.parse.CompilerUtilities")

  private lazy val labFormat: fileformat.NLogoLabFormat =
    new fileformat.NLogoLabFormat(literalParser)

  private def bsSection = labFormat.componentName

  private def modelProtocols(m: Model): Option[Seq[LabProtocol]] =
    m.optionalSectionValue[Seq[LabProtocol]](bsSection)

  def selectProtocol(settings: Settings, workspace: Workspace): Option[(LabProtocol, Model)] = {
    val model = modelAtPath(settings.modelPath, settings.version, workspace)

    val modelWithExtraProtocols =
      settings.externalXMLFile.map { file =>
        val loadableXML = Source.fromFile(file).mkString
        val additionalProtos = labFormat.load(loadableXML.lines.toArray, None)
        model.withOptionalSection(bsSection, additionalProtos, Seq[LabProtocol]())
      }.getOrElse(model)

    val namedProtocol: Option[LabProtocol] =
      for {
        name   <- settings.protocolName
        protos <- modelProtocols(modelWithExtraProtocols)
        proto  <- protos.find(_.name == name)
      } yield proto

    lazy val firstSetupFileProtocol: Option[LabProtocol] =
      for {
        hasExternalFile <- settings.externalXMLFile
        protos          <- modelProtocols(modelWithExtraProtocols)
      } yield protos.head

    (namedProtocol orElse firstSetupFileProtocol).map(p => (p, model))
  }

  private def modelAtPath(path: String, version: Version, workspace: Workspace): Model = {
    val allAutoConvertables = fileformat.defaultAutoConvertables :+
      Femto.scalaSingleton[org.nlogo.api.AutoConvertable]("org.nlogo.sdm.SDMAutoConvertable")
    val converter =
      fileformat.converter(workspace.getExtensionManager, workspace.getCompilationEnvironment, literalParser, allAutoConvertables) _
    val loader = fileformat.standardLoader(literalParser)
    val modelConverter = converter(workspace.world.program.dialect)

    OpenModelFromURI(Paths.get(path).toUri, HeadlessFileController, loader, modelConverter, version)
    loader.readModel(Paths.get(path).toUri) match {
      case Success(m) => m
      case Failure(e) => throw new Exception("Unable to open model at: " + path + ". " + e.getMessage)
    }
  }

  def protocolsFromModel(modelPath: String, version: Version, workspace: Workspace): Seq[LabProtocol] = {
    modelProtocols(modelAtPath(modelPath, version, workspace)).getOrElse(Seq[LabProtocol]())
  }

  def externalProtocols(path: String): Option[Seq[LabProtocol]] = {
    val fileSource = Source.fromFile(path).mkString
    labFormat.load(fileSource.lines.toArray, None)
  }

  def convertToNewFormat(oldPath: Path, newPath: Path): Unit = {
    Files.write(newPath, Files.readAllBytes(oldPath))
    val stylesource = new StreamSource(FileIO.getResourceAsString(ConversionStylesheetResource))
    stylesource.setSystemId(getClass.getResource(ConversionStylesheetResource).toString)
    val inputSource = new StreamSource(oldPath.toFile)
    val outputResult = new StreamResult(newPath.toFile)
    val transformer = TransformerFactory.newInstance.newTransformer(stylesource)
    transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes")
    transformer.transform(inputSource, outputResult)
  }
}
