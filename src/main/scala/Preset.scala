import scala.collection.immutable.ListMap

abstract class Preset {
  def ffmpegOptionsBase: Map[String, String]
  def ffmpegOptionsVideo: Map[String, String]
  def ffmpegOptionsAudio: Map[String, String]

  def options(video: Boolean = true,
              audio: Boolean = true): Map[String, String] = {
    val combinedMap = ffmpegOptionsBase ++
      (if (video) ffmpegOptionsVideo else Map.empty[String, String]) ++
      (if (audio) ffmpegOptionsAudio else Map.empty[String, String])
    combinedMap
  }
}

class StandardAudioVideo extends Preset {

  def ffmpegOptionsBase = ListMap(
    "copy_unknown" -> "", //if there are streams ffmpeg doesn't know about, still copy them (e.g some GoPro data stuff)
    "map_metadata" -> "0", //copy over the global metadata from the first (only) input
    "map" -> "0", //copy *all* streams found in the file, not just the best audio and video as is the default (e.g. including data)
    "codec" -> "copy", //for all streams, default to just copying as it with no transcoding
    "preset" -> "medium"
  )

  def ffmpegOptionsVideo = ListMap(
    "codec:v" -> "libx264", //specifically for the video stream, reencode using x264 and the specified pix_fmt and crf factor
    "pix_fmt" -> "yuv420p", //Default pix_fmt
    "crf" -> "23" //Default constant rate factor for quality. 0-52 where 18 is near visually lossless
  )

  def ffmpegOptionsAudio = ListMap(
    "codec:a" -> "libfdk_aac",
    "vbr" -> "4"
  )

}

class GoProHero4 extends StandardAudioVideo {

  override val ffmpegOptionsVideo = super.ffmpegOptionsBase ++ ListMap(
    "medatadata:s:v:" -> "handler=\"	GoPro AVC\"",
    "medatadata:s:a:" -> "handler=\"	GoPro AAC\""
  )

}

class GoProHero5 extends StandardAudioVideo {

  override val ffmpegOptionsVideo = super.ffmpegOptionsBase ++ ListMap(
    "pix_fmt" -> "yuvj420p",
    "map" -> "0:v",
    "map" -> "0:a",
    "map" -> "0:m:handler_name:\"	GoPro TCD\"",
    "map" -> "0:m:handler_name:\"	GoPro MET\"",
    "map" -> "0:m:handler_name:\"	GoPro SOS\"",
    "tag:d:1" -> "\"gpmd\"",
    "tag:d:2" -> "\"gpmd\"",
    "medatadata:s:v:" -> "handler=\"	GoPro AVC\"",
    "medatadata:s:a:" -> "handler=\"	GoPro AAC\""
  )
}
