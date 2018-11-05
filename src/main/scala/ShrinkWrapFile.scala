import java.io.File

case class ShrinkWrapFile(file: File, config: Config) {
  val path = file.getAbsolutePath
  val transcodeSuffix = config.transcodeSuffix
  val outputExtension = config.outputExtension

  def isTranscoded: Boolean = {
    //File is transcoded if it has the suffix in it's name (it is the transcode), or
    //if the transcoded version of it exists as a file in the same directory.
    file.getName.contains(config.transcodeSuffix) || transcodedFile.exists()
  }

  def ext: String = path.substring(path.lastIndexOf(".") + 1)

  def transcodedFile: File =
    new File(
      s"${path.take(path.lastIndexOf("."))}${transcodeSuffix}.${outputExtension}")

}
