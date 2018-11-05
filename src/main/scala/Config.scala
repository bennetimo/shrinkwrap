import java.io.File

case class Config(inputExtension: String = "",
                  outputExtension: String = "mp4",
                  transcodeAudio: Boolean = true,
                  transcodeVideo: Boolean = true,
                  crf: Int = 23,
                  pixFmt: String = "yuv420p",
                  ffmpegPreset: String = "medium",
                  backupMetadata: Boolean = false,
                  transcodeSuffix: String = "-tc",
                  ffmpegOpts: Map[String, String] = Map.empty,
                  files: Seq[File] = Nil) {

  override def toString: String = {
    val sb: StringBuilder = new StringBuilder()
    sb.append(s"Input Extension: $inputExtension\n")
    sb.append(s"Output Extension: $outputExtension\n")
    sb.append(s"Transcode Audio: $transcodeAudio\n")
    sb.append(s"Transcode Video: $transcodeVideo\n")
    sb.append(s"CRF: $crf\n")
    sb.append(s"PixFmt: $pixFmt\n")
    sb.append(s"FFmpeg Preset: $ffmpegPreset\n")
    sb.append(s"Backup Metadata: $backupMetadata\n")
    sb.append(s"Transcoded Files Suffix: $transcodeSuffix\n")
    sb.append(s"Input: ${files.mkString(",")}\n")
    sb.mkString
  }

}
