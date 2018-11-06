package io.coderunner.shrinkwrap

import java.io.File

case class Config(inputExtension: String = "",
                  outputExtension: String = "mp4",
                  transcodeAudio: Boolean = true,
                  transcodeVideo: Boolean = true,
                  backupMetadata: Boolean = false,
                  metadataSuffix: String = "-metadata",
                  overwriteExistingTranscodes: Boolean = false,
                  transcodeSuffix: String = "-tc",
                  preset: Preset = new StandardAudioVideo(),
                  ffmpegOpts: Map[String, String] = Map.empty,
                  files: Seq[File] = Nil) {

  override def toString: String = {
    val sb: StringBuilder = new StringBuilder()
    sb.append(s"Input Extension: $inputExtension\n")
    sb.append(s"Output Extension: $outputExtension\n")
    sb.append(s"Transcode Audio: $transcodeAudio\n")
    sb.append(s"Transcode Video: $transcodeVideo\n")
    sb.append(s"Backup Metadata: $backupMetadata\n")
    sb.append(s"Metadata Suffix: $metadataSuffix\n")
    sb.append(s"Overwrite Existing Transcodes: $overwriteExistingTranscodes\n")
    sb.append(s"Transcoded Files Suffix: $transcodeSuffix\n")
    sb.append(s"Using Shrinkwrap Preset: ${preset.name}\n")
    sb.append(s"FFMpeg Additional Options: ${ffmpegOpts.map { case (k, v) => s"-$k $v" }.mkString(" ")}\n")
    sb.append(s"Input: ${files.mkString(",")}\n")
    sb.mkString
  }

}
