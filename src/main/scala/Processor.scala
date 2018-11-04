import java.io.File

class Processor(config: Config) {

  val stats = new Stats()

  def processFile(file: File): Unit = {
    println(s"Shrinkwrapping file: ${file.getAbsolutePath}")
    stats.filesProcessed += 1
  }

  def processDir(dir: File): Unit = {
    println(s"Shrinkwrapping directory: ${dir.getAbsolutePath}")
    stats.dirsProcessed += 1

    val files = for {
      f <- dir.listFiles
      ext = fileExtension(f.getAbsolutePath)
      //transcoded = f.getName.matches(config.transcodeSuffix)
      if ext == config.inputExtension
    } yield f

    println("Processing: " + files.mkString("\n"))

  }

  def processFiles(): Unit = {
    val (d, f) = config.files.filter(_.exists()).partition(_.isDirectory)

    f.foreach(processFile)
    d.foreach(processDir)
  }

  def fileExtension(filePath: String) = filePath.substring(filePath.lastIndexOf(".") + 1)

}

case class Stats(var filesProcessed: Int = 0, filesSkipped: Int = 0,
                 var dirsProcessed: Int = 0, dirsSkipped: Int = 0,
                 bytesSaved: Int = 0)
