package io.coderunner.shrinkwrap

class Stats {

  var totalDirs      = 0
  var totalFiles     = 0
  var filesToProcess = 0
  var filesProcessed = 0
  var filesSkipped   = 0
  var bytesProcessed = 0l
  var bytesSaved     = 0l

  def getStats(): String = {
    val sb = new StringBuilder
    sb.append("\n\n")
    sb.append("*" * 60 + "\n")
    sb.append(s"Directories scanned: ${totalDirs}\n")
    sb.append(s"Files scanned: ${totalFiles}\n")
    sb.append(s"Files processed: ${filesProcessed}\n")
    sb.append(s"Files skipped: ${filesSkipped}\n")
    sb.append(s"Bytes processed: $bytesProcessed bytes\n")
    sb.append(s"Bytes saved: $bytesSaved bytes\n")
    sb.append(s"mb processed: ${bytesProcessed >> 20}mb\n")
    sb.append(s"mb saved: ${bytesSaved >> 20}mb\n")
    sb.append("*" * 60)
    sb.mkString
  }

}
