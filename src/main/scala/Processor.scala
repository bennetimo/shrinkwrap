import java.io.File

import com.typesafe.scalalogging.Logger
import sys.process._
import scala.language.postfixOps

case class ShrinkWrapFile(file: File) {
  val path = file.getAbsolutePath
  def transcoded(transcodeSuffix: String, outputExtension: String): Boolean = {
    //File is transcoded if it has the suffix in it's name (it is the transcode), or
    //if the transcoded version of it exists as a file in the same directory.
    file.getName.contains(transcodeSuffix) ||
    new File(transcodePath(transcodeSuffix, outputExtension)).exists()
  }
  def ext: String = fileExtension(file.getAbsolutePath)
  def transcodePath(transcodeSuffix: String, outputExtension: String): String =
    s"${path.take(path.lastIndexOf("."))}${transcodeSuffix}.${outputExtension}"
  private def fileExtension(filePath: String) =
    filePath.substring(filePath.lastIndexOf(".") + 1)
}

class Processor(config: Config) {

  val logger = Logger("shrinkwrap")
  logger.debug(s"Config is: $config")

  private var totalFiles = 0
  private var filesProcessed = 0
  private var filesSkipped = 0
  private var bytesProcessed = 0l
  private var bytesSaved = 0l

  def processFile(file: File): Unit = {
    ShrinkWrapFile(file) match {
      case sf
          if sf.transcoded(config.transcodeSuffix, config.outputExtension) => {
        logger.debug(
          s"Skipping file: ${file.getAbsolutePath} (already transcoded)")
        filesSkipped += 1
      }
      case sf if sf.ext != config.inputExtension => {
        logger.debug(
          s"Skipping file: ${file.getAbsolutePath} (ignored input extension)")
        filesSkipped += 1
      }
      case sf => {
        logger.debug(s"Shrinkwrapping file: ${file.getAbsolutePath}")

        bytesProcessed += file.length()

        val opts = (new StandardAudioVideo()
          .options(config.transcodeVideo, config.transcodeAudio) ++ config.ffmpegOpts)
          .map { case (k, v) => s"-$k $v" }
          .mkString(" ")

        val cmd = s"ffmpeg -noautorotate -i ${file.getAbsolutePath} ${opts} ${sf
          .transcodePath(config.transcodeSuffix, config.outputExtension)}"
        logger.debug(s"Executing cmd: $cmd")
        cmd !
      }
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

}
