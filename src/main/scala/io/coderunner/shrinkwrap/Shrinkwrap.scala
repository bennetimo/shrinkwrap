package io.coderunner.shrinkwrap

import java.io.File

import build.BuildInfo

object Shrinkwrap extends App {

  val parser = new scopt.OptionParser[Config]("shrinkwrap") {
    head(BuildInfo.name, BuildInfo.version)

    opt[String]('i', "input-extension")
      .action((x, c) => c.copy(inputExtension = x))
      .text("input file format to process (e.g. wmv, mpeg2, mp4")

    opt[String]('o', "output-extension")
      .action((x, c) => c.copy(outputExtension = x))
      .text("output file format (e.g. mp4, aac)")

    opt[Boolean]('a', "audio")
      .action((x, c) => c.copy(transcodeAudio = x))
      .text("whether to transcode audio or copy unchanged")

    opt[Boolean]('v', "video")
      .action((x, c) => c.copy(transcodeVideo = x))
      .text("whether to transcode video or copy unchanged")

    opt[Preset]('p', "preset")
      .action((x, c) => c.copy(preset = x))
      .text("preset to use (standard, gopro4, gopro5)")

    opt[Boolean]('f', "force-overwrite")
      .action((x, c) => c.copy(overwriteExistingTranscodes = x))
      .text("whether to overwrite existing transcodes")

    opt[Map[String, String]]("ffmpeg-opts")
      .valueName("k1=v1,k2=v2...")
      .action((x, c) => c.copy(ffmpegOpts = x))
      .text("arbitrary ffmpeg options")

    arg[File]("<file>...")
      .unbounded()
      .minOccurs(1)
      .action((x, c) => c.copy(files = c.files :+ x))
      .text("files or directories to shrinkwrap recursively")

    help("help").text("prints this usage text")
  }

  // parser.parse returns Option[C]
  parser.parse(args, Config()) match {
    case Some(config) => new Processor(config).processFiles()
    case None         => // arguments are bad, error message will have been displayed
  }
}
