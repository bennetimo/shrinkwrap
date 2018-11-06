package io.coderunner.shrinkwrap

import Checks._

class Processor(config: Config, actions: Seq[Action]) extends Logging {
  val stats = new Stats()

  logger.debug(s"\nUsing config: \n$config")

  def shrinkFile(sf: ShrinkWrapFile): Unit = {
    stats.filesProcessed += 1
    logger.debug(s"Shrinkwrapping file: ${sf.file.getAbsolutePath} (${stats.filesToProcess}/${stats.totalFiles})")

    actions.foreach(_.run(sf, config))

    stats.bytesProcessed += sf.file.length()
    stats.bytesSaved += (sf.file.length() - sf.transcodedFile.length())
  }

  def analyzeFiles(): (Stats, List[Skip]) = {
    val (dirs, files) = config.files.filter(_.exists()).partition(_.isDirectory)
    // Add files found in all the directories with the ones explicitly listed on the command line
    val allFiles = (dirs.flatMap(_.listFiles) ++ files).map(ShrinkWrapFile(_, config))

    // Filter the files to determine for each whether to process or skip it
    val checkedFiles = allFiles.map(sf =>
      for {
        c1 <- checkInputExtension(sf, config)
        c2 <- checkIsTranscodedFile(c1, config)
        c3 <- checkHasTranscodeFile(c2, config)
      } yield c3)

    val (skips, toProcess) = checkedFiles.foldRight((List[Skip](), List[ShrinkWrapFile]())) {
      case (e, (ls, rs)) => e.fold(l => (l :: ls, rs), r => (ls, r :: rs))
    }

    stats.totalFiles = allFiles.length
    stats.totalDirs = dirs.length
    stats.filesSkipped = skips.length
    stats.filesToProcess = toProcess.length

    toProcess.foreach(shrinkFile)
    (stats, skips)
  }

}
