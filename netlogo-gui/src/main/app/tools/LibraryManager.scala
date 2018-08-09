// (C) Uri Wilensky. https://github.com/NetLogo/NetLogo

package org.nlogo.app.tools

import java.io.File
import java.net.URL
import java.nio.file.{ Files, Paths }
import javax.swing.{ DefaultListModel, ListModel }

import com.typesafe.config.{ Config, ConfigException, ConfigFactory, ConfigRenderOptions, ConfigValueFactory }

import org.nlogo.api.{ APIVersion, FileIO }
import org.nlogo.swing.ProgressListener

object LibraryManager {
  private val LibrariesConfBasename = "libraries.conf"
  private val MetadataURL = new URL(s"https://raw.githubusercontent.com/NetLogo/NetLogo-Libraries/${APIVersion.version}/$LibrariesConfBasename")
  private val BundledLibrariesConfig = ConfigFactory.parseResources("system/bundled-libraries.conf")
  val LibrariesConf = FileIO.perUserFile(LibrariesConfBasename)
  val InstalledLibrariesConf = FileIO.perUserFile("installed-libraries.conf")

  def updateInstalledVersion(category: String, lib: LibraryInfo, uninstall: Boolean = false) = synchronized {
    val config = ConfigFactory.parseFile(new File(InstalledLibrariesConf))
    val updatedConfig =
      if (uninstall)
        config.withoutPath(s"""$category."${lib.codeName}"""")
      else
        config.withValue(
          s"""$category."${lib.codeName}".installedVersion""", ConfigValueFactory.fromAnyRef(lib.version))
    val options = ConfigRenderOptions.defaults.setOriginComments(false)
    FileIO.writeFile(LibraryManager.InstalledLibrariesConf, updatedConfig.root.render(options), false)
  }
}

class LibraryManager(categories: Map[String, LibrariesCategoryInstaller], progressListener: ProgressListener) {
  import LibraryManager._

  private val categoryNames = categories.keys
  private val mutableListModels = categoryNames.map(c => c -> new DefaultListModel[LibraryInfo]).toMap
  val listModels: Map[String, ListModel[LibraryInfo]] = mutableListModels

  private val metadataFetcher = new SwingUpdater(MetadataURL, updateLists _, progressListener)
  private var initialLoading = true

  if (!Files.exists(Paths.get(InstalledLibrariesConf)))
    Files.createFile(Paths.get(InstalledLibrariesConf))

  reloadMetadata()
  initialLoading = false

  def installer(categoryName: String)   = categories(categoryName).install _
  def uninstaller(categoryName: String) = categories(categoryName).uninstall _

  def reloadMetadata() = updateLists(new File(LibrariesConf))
  def updateMetadataFromGithub() = metadataFetcher.reload()

  private def updateLists(configFile: File): Unit = {
    if (configFile.exists) {
      try {
        val config = ConfigFactory.parseFile(configFile)
        val installedLibsConf =
          ConfigFactory.parseFile(new File(InstalledLibrariesConf))
            .withFallback(BundledLibrariesConfig)
        categoryNames.foreach(c => updateList(config, installedLibsConf, c, mutableListModels(c)))
      } catch {
        case ex: ConfigException =>
          if (initialLoading)
            // In case only the local copy got messed up somehow -- EL 2018-06-02
            metadataFetcher.invalidateCache()
          else
            throw new MetadataLoadingException(ex)
      }
    } else {
      metadataFetcher.invalidateCache()
    }
  }

  private def updateList(config: Config, installedLibsConf: Config, category: String, listModel: DefaultListModel[LibraryInfo]) = {
    import scala.collection.JavaConverters._

    val configList = config.getConfigList(category).asScala
    listModel.clear()
    listModel.ensureCapacity(configList.length)
    configList foreach { c =>
      val name        = c.getString("name")
      val codeName    = if (c.hasPath("codeName")) c.getString("codeName") else name.toLowerCase
      val shortDesc   = c.getString("shortDescription")
      val longDesc    = c.getString("longDescription")
      val version     = c.getString("version")
      val homepage    = new URL(c.getString("homepage"))
      val downloadURL = new URL(c.getString("downloadURL"))

      val installedVersionPath = s"""$category."$codeName".installedVersion"""
      val bundled = BundledLibrariesConfig.hasPath(installedVersionPath)
      val status =
        if (!installedLibsConf.hasPath(installedVersionPath))
          LibraryStatus.CanInstall
        else if (installedLibsConf.getString(installedVersionPath) == version)
          LibraryStatus.UpToDate
        else
          LibraryStatus.CanUpdate

      listModel.addElement(
        LibraryInfo(name, codeName, shortDesc, longDesc, version, homepage, downloadURL, bundled, status))
    }
  }
}

class MetadataLoadingException(cause: Throwable = null) extends RuntimeException(cause)
