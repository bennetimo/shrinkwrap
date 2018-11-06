package io.coderunner.shrinkwrap

case class Skip(reason: String, sf: ShrinkWrapFile)

// Set of checks to run on each file before considering it to be transcoded
object Checks {

  def checkIsTranscodedFile(sf: ShrinkWrapFile, config: Config): Either[Skip, ShrinkWrapFile] = {
    if (!sf.isOriginal) Left(Skip("already transcoded (is a transcoded file)", sf)) else Right(sf)
  }

  def checkHasTranscodeFile(sf: ShrinkWrapFile, config: Config): Either[Skip, ShrinkWrapFile] = {
    if (sf.hasBeenTranscoded && !config.overwriteExistingTranscodes)
      Left(Skip("already transcoded (detected transcoded version)", sf))
    else Right(sf)
  }

  def checkInputExtension(sf: ShrinkWrapFile, config: Config): Either[Skip, ShrinkWrapFile] = {
    if (sf.extension != config.inputExtension)
      Left(Skip(s"input extension does not match ${config.inputExtension}", sf))
    else Right(sf)
  }

}
