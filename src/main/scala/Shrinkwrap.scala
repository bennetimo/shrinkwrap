import scala.sys.process._
import scala.language.postfixOps

case class Config (inputExtension: String = "", outputExtension: String = "",
                   transcodeAudio: Boolean = true,
                   transcodeVideo: Boolean = true,
                   crf: Int = 23,
                   pixFmt: String = "yuv420p",
                   ffmpegPreset: String = "medium",
                   backupMetadata: Boolean = false,
                   transcodeSuffix: String = "-tc"
                  )

object Skrinkwrap extends App {

  val parser = new scopt.OptionParser[Config]("shrinkwrap") {
    head("shrinkwrap", "0.1")

    opt[String]('i', "input-extension").action((x, c) =>
      c.copy(inputExtension = x)).text("input file extension to process")

    opt[String]('o', "output-extension").action((x, c) =>
      c.copy(outputExtension = x)).text("output file extension to process")

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

  }
  // parser.parse returns Option[C]
  parser.parse(args, Config()) match {
    case Some(config) =>
      println("config is : " + config)

      "ffmpeg --help" !
    // do stuff

    case None =>
    // arguments are bad, error message will have been displayed

  }

}
