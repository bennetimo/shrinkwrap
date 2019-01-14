package io.coderunner.shrinkwrap

import scala.collection.immutable.ListMap

object Preset {
  implicit val presetRead: scopt.Read[Preset] =
    scopt.Read.reads(apply _)

  def apply(value: String): Preset = value match {
    case "standard" => new StandardAudioVideo()
    case "gopro4"   => new GoProHero4()
    case "gopro5"   => new GoProHero5()
    case _          => new StandardAudioVideo()
  }
}

// A preset is essentially a Map of options to pass to ffmpeg, broken down into a base map, and
// specific settings for audio and video that can be togged on and off
sealed abstract class Preset() {
  def name: String
  def ffmpegOptionsBase: Map[String, String]
  def ffmpegOptionsVideo: Map[String, String]
  def ffmpegOptionsAudio: Map[String, String]

  def options(video: Boolean = true, audio: Boolean = true): Map[String, String] = {
    val combinedMap = ffmpegOptionsBase ++
      (if (video) ffmpegOptionsVideo else Map.empty[String, String]) ++
      (if (audio) ffmpegOptionsAudio else Map.empty[String, String])
    combinedMap
  }
}

// Standard (default) preset converting all input to h264 video and aac audio
class StandardAudioVideo extends Preset {

  override def name: String = "standard"

  def ffmpegOptionsBase = ListMap(
    "copy_unknown" -> "", //if there are streams ffmpeg doesn't know about, still copy them (e.g some GoPro data stuff)
    "map_metadata" -> "0", //copy over the global metadata from the first (only) input
    "map"          -> "0", //copy *all* streams found in the file, not just the best audio and video as is the default (e.g. including data)
    "codec"        -> "copy", //for all streams, default to just copying as it with no transcoding
    "preset"       -> "medium"
  )

  def ffmpegOptionsVideo = ListMap(
    "codec:v" -> "libx264", //specifically for the video stream, reencode using x264 and the specified pix_fmt and crf factor
    "pix_fmt" -> "yuv420p", //Default pix_fmt
    "crf"     -> "23" //Default constant rate factor for quality. 0-52 where 18 is near visually lossless
  )

  def ffmpegOptionsAudio = ListMap(
    "codec:a" -> "libfdk_aac",
    "vbr"     -> "4"
  )
}

// Same as standard, but uses the correct handler names for GoPro
class GoProHero4 extends StandardAudioVideo {

  override def name: String = "gopro4"

  override val ffmpegOptionsVideo = super.ffmpegOptionsVideo ++ ListMap(
    "metadata:s:v:" -> "handler='\tGoPro AVC'",
    "metadata:s:a:" -> "handler='\tGoPro AAC'"
  )

}

//  If we're converting video from a GoPro5, do extra to preserve the metadata (gps and sensor data) track
//    GoPro software (e.g. Quik) looks for the the go pro handler names, if not present it will not see the video as it
//    thinks it is from a non-gopro camera. By updating the handler names to what it is expecting, we can trick it into
//  recognising the videos to be GoPro content and editable in Quik. To enable the gauges (gps, sensor data) Quik is even
//    more picky, and must find the 'gpmd' data track inside the video, with a handler name starting with a tab!
//    For consistency, we map all the handler names with a tab as that seems to be what the GoPro itself spits out
//  -tag:d:2 'gpmd' => This is effectively a hack to force ffmpeg to copy the binary 'fdsc' stream, which is present in the
//  original go pro files. Not sure exactly what it is for, but would rather keep it just in case. Unfortunately ffmpeg does Not
//    understand this handler type, so spits out the message: '[mp4 @ 0x7fed5a80cc00] Unknown hldr_type for fdsc, writing dummy values'
//  This effectively just adds a useless dummy stream. Using this hack we instead get the original stream copied, by telling it the
//    the handler_type it should use is actually 'gpmd'. Since we're just copying it as is and not manipulating it this should be fine.
//  Then to be able to distinguish it from the *real* gpmd stream, we change the handler name in the metadata to be GoPro SOS (fdsc stream)
class GoProHero5() extends StandardAudioVideo {

  override def name: String = "gopro5"

  override def ffmpegOptionsBase = ListMap(
    "copy_unknown"                        -> "", //if there are streams ffmpeg doesn't know about, still copy them (e.g some GoPro data stuff)
    "map_metadata"                        -> "0", //copy over the global metadata from the first (only) input
    "codec"                               -> "copy", //for all streams, default to just copying as it with no transcoding
    "preset"                              -> "medium",
    "map 0:v"                             -> "",
    "map 0:a"                             -> "",
    "map 0:m:handler_name:'\tGoPro TCD'?" -> "",
    "map 0:m:handler_name:'\tGoPro MET'"  -> "",
    "map 0:m:handler_name:'\tGoPro SOS'"  -> "",
    "tag:d:1"                             -> "'gpmd'",
    "tag:d:2"                             -> "'gpmd'",
    "metadata:s:v:"                       -> "handler='\tGoPro AVC'",
    "metadata:s:a:"                       -> "handler='\tGoPro AAC'",
    "metadata:s:d:0"                      -> "handler='\tGoPro TCD'",
    "metadata:s:d:1"                      -> "handler='\tGoPro MET'",
    "metadata:s:d:2"                      -> "handler='\tGoPro SOS (original fdsc stream)'"
  )

  override val ffmpegOptionsVideo = super.ffmpegOptionsVideo ++ ListMap(
    "pix_fmt" -> "yuvj420p"
  )

  override val ffmpegOptionsAudio = ListMap()
}
