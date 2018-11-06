package io.coderunner.shrinkwrap

import java.io.File

import com.typesafe.scalalogging.Logger

import scala.language.postfixOps
import scala.sys.process._

class Processor(config: Config) {

  val ascii =
    """
      |  ___ _        _      _
      | / __| |_  _ _(_)_ _ | |____ __ ___ _ __ _ _ __
      | \__ \ ' \| '_| | ' \| / /\ V  V / '_/ _` | '_ \
      | |___/_||_|_| |_|_||_|_\_\ \_/\_/|_| \__,_| .__/
      |                                          |_|
    """.stripMargin

  val logger = Logger("shrinkwrap")
  logger.debug(ascii)
  logger.debug(s"\nUsing config: \n$config")

  private var totalDirs                     = 0
  private var totalFiles                    = 0
  private var filesProcessed                = 0
  private var filesSkippedAlreadyTranscoded = 0
  private var filesSkippedInputExtension    = 0
  private var bytesProcessed                = 0l
  private var bytesSaved                    = 0l

  def processFile(file: File): Unit = {
    ShrinkWrapFile(file, config) match {
      case sf if sf.hasBeenTranscoded && !config.overwriteExistingTranscodes => {
        logger.debug(s"Skipping file: ${file.getAbsolutePath} (already transcoded)")
        filesSkippedAlreadyTranscoded += 1
      }
      case sf if !sf.isOriginal => {
        logger.debug(s"Skipping file: ${file.getAbsolutePath} (is a transcoded file)")
        filesSkippedAlreadyTranscoded += 1
      }
      case sf if sf.extension != config.inputExtension => {
        logger.debug(s"Skipping file: ${file.getAbsolutePath} (ignored input extension)")
        filesSkippedInputExtension += 1
      }
      case sf => {
        filesProcessed += 1
        logger.debug(s"Shrinkwrapping file: ${file.getAbsolutePath} ($filesProcessed/$totalFiles)")

        bytesProcessed += file.length()
        runFFmpeg(sf)
        runExiftool(sf)
        bytesSaved += (file.length() - sf.transcodedFile.length())
      }
    }
  }

  def runFFmpeg(sf: ShrinkWrapFile): Unit = {
    val opts =
      (config.preset.options(config.transcodeVideo, config.transcodeAudio)
        ++ config.ffmpegOpts)
        .map { case (k, v) => s"-$k $v" }
        .mkString(" ")

    val cmdString =
      s"""ffmpeg -y -noautorotate -i "${sf.file.getAbsolutePath}" ${opts} "${sf.transcodedFile.getAbsolutePath}""""

    val cmd = Seq("/bin/sh", "-c", cmdString)
    logger.debug(s"Executing cmd: $cmdString")
    cmd !
  }

  def runExiftool(sf: ShrinkWrapFile): Unit = {
    logger.debug("Recovering file metadata using the original file")
    val cmdString =
      raw"""exiftool -tagsfromfile "${sf.file.getAbsolutePath}" -extractEmbedded -all:all \
       -"*gps*" -time:all --FileAccessDate --FileInodeChangeDate -FileModifyDate \
       -ext ${config.outputExtension} -overwrite_original "${sf.transcodedFile.getAbsolutePath}""""
    val cmd = Seq("/bin/sh", "-c", cmdString)
    logger.debug(s"Executing cmd: $cmdString")
    cmd !
  }

  def getStats(): String = {
    val sb = new StringBuilder
    sb.append("\n\n")
    sb.append("*" * 60 + "\n")
    sb.append(s"Directories scanned: ${totalDirs}\n")
    sb.append(s"Total files scanned: ${totalFiles}\n")
    sb.append(s"Total files processed: ${filesProcessed}\n")
    sb.append(s"Files skipped (already transcoded): ${filesSkippedAlreadyTranscoded}\n")
    sb.append(s"Files skipped (not matching input extension ${config.inputExtension}): ${filesSkippedInputExtension}\n")
    sb.append(s"Files skipped total ${filesSkippedAlreadyTranscoded + filesSkippedInputExtension}\n")
    sb.append(s"Total bytes processed: $bytesProcessed bytes\n")
    sb.append(s"Total bytes saved: $bytesSaved bytes\n")
    sb.append(s"Total mb processed: ${bytesProcessed >> 20}mb\n")
    sb.append(s"Total mb saved: ${bytesSaved >> 20}mb\n")
    sb.append("*" * 60)
    sb.mkString
  }

  def processFiles(): Unit = {
    val (dirs, files) = config.files.filter(_.exists()).partition(_.isDirectory)
    val allFiles      = dirs.flatMap(_.listFiles) ++ files

    totalFiles = allFiles.length
    totalDirs = dirs.length

    allFiles.foreach(processFile)
    logger.debug("Shrinkwrapping complete!")
    logger.debug(getStats())
  }

}
