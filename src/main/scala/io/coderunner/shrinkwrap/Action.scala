package io.coderunner.shrinkwrap

import scala.language.postfixOps
import scala.sys.process._

sealed trait Action extends Logging {

  def run(sf: ShrinkWrapFile, config: Config): Unit = {
    val cmd = Seq("/bin/sh", "-c", action(sf, config))
    logger.debug(s"Executing cmd: $cmd")

    val exitCode = cmd !

    if (exitCode != 0) {
      logger.error("Encountered a problem. Exiting shrinkwrap for safety")
      System.exit(1)
    }
  }

  def action(sf: ShrinkWrapFile, config: Config): String

}

case class ShrinkAction() extends Action {
  override def action(sf: ShrinkWrapFile, config: Config): String = {
    val ffmpegArgs = (config.preset.options(config.transcodeVideo, config.transcodeAudio)
      ++ config.ffmpegOpts)
      .map { case (k, v) => s"-$k $v" }
      .mkString(" ")
    s"""ffmpeg -y -noautorotate -i "${sf.file.getAbsolutePath}" ${ffmpegArgs} "${sf.transcodedFile.getAbsolutePath}""""
  }
}

case class RecoverFileMetadata() extends Action {
  override def action(sf: ShrinkWrapFile, config: Config): String = {
    raw"""exiftool -tagsfromfile "${sf.file.getAbsolutePath}" -extractEmbedded -all:all \
       -"*gps*" -time:all --FileAccessDate --FileInodeChangeDate -FileModifyDate \
       -ext ${config.outputExtension} -overwrite_original "${sf.transcodedFile.getAbsolutePath}""""
  }
}

case class BackupMetadataAction() extends Action {
  override def action(sf: ShrinkWrapFile, config: Config): String = {
    s"""exiftool -s -ExtractEmbedded ${sf.file.getAbsolutePath} > ${sf.metdataFile.getAbsolutePath}"""
  }
}
