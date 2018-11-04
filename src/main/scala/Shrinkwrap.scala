import java.io.File

import build.BuildInfo

import scala.sys.process._
import scala.language.postfixOps

object Shrinkwrap extends App {

  val parser = new scopt.OptionParser[Config]("shrinkwrap") {
    head(BuildInfo.name, BuildInfo.version)

    opt[String]('i', "input-extension").action((x, c) =>
      c.copy(inputExtension = x)).text("input file format to process (e.g. wmv, mpeg2, mp4")

    opt[String]('o', "output-extension").action((x, c) =>
      c.copy(outputExtension = x)).text("output file format (e.g. mp4, aac)")

    opt[Boolean]('a', "audio").action((x, c) =>
      c.copy(transcodeAudio = x)).text("whether to transcode audio or copy unchanged")

    opt[Boolean]('v', "video").action((x, c) =>
      c.copy(transcodeVideo = x)).text("whether to transcode video or copy unchanged")

    opt[Int]('c', "crf").action((x, c) =>
      c.copy(crf = x)).text("ffmpeg crf quality value for transcoding video")

    opt[String]('f', "pixfmt").action((x, c) =>
      c.copy(pixFmt = x)).text("ffmpeg pixfmt for transcoding video")

    opt[String]('p', "preset").action((x, c) =>
      c.copy(ffmpegPreset = x)).text("ffmpeg preset for transcoding video")

    arg[File]("<file>...").unbounded().minOccurs(1).action( (x, c) =>
      c.copy(files = c.files :+ x) ).text("files or directories to shrinkwrap recursively")

    help("help").text("prints this usage text")
  }

  // parser.parse returns Option[C]
  parser.parse(args, Config()) match {
    case Some(config) =>
      println("config is : " + config)
      new Processor(config).processFiles
    case None =>  // arguments are bad, error message will have been displayed

  }
}