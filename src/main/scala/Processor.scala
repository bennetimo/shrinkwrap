import java.io.File

import com.typesafe.scalalogging.Logger

class Processor(config: Config) {

  val logger = Logger("shrinkwrap")
  logger.debug(s"Config is: $config")

  private var totalFiles = 0
  private var filesProcessed = 0
  private var filesSkipped = 0
  private var bytesProcessed = 0l
  private var bytesSaved = 0l

  def processFile(file: File): Unit = {
    val ext = fileExtension(file.getAbsolutePath)
    val transcoded = file.getName.contains(config.transcodeSuffix)

    if (ext == config.inputExtension && !transcoded) {
      logger.debug(s"Shrinkwrapping file: ${file.getAbsolutePath}")

      bytesProcessed += file.length()
    } else {
      logger.debug(s"Skipping file: ${file.getAbsolutePath}")
      filesSkipped += 1
    }
  }

  def processFiles(): Unit = {
    val (dirs, files) = config.files.filter(_.exists()).partition(_.isDirectory)
    val allFiles = dirs.flatMap(_.listFiles) ++ files

    totalFiles = allFiles.length

    logger.debug(s"Directories scanned: ${dirs.length}")
    logger.debug(s"Total files to inspect: ${allFiles.length}")

    allFiles.foreach(processFile)

    logger.debug(s"Total bytes processed: $bytesProcessed bytes")
    logger.debug(s"Total mb processed: ${bytesProcessed >> 20}mb")
  }

  private def fileExtension(filePath: String) =
    filePath.substring(filePath.lastIndexOf(".") + 1)
}
