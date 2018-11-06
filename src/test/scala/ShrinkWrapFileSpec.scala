package io.coderunner.shrinkwrap

import java.io.File

import org.mockito.MockitoSugar

class ShrinkWrapFileSpec extends UnitTest("ShrinkWrapFile") with MockitoSugar {

  var sf: ShrinkWrapFile = _

  it should "return a correct extension" in {
    sf = ShrinkWrapFile(new File("test.txt"), Config())
    sf.extension should be("txt")

    sf = ShrinkWrapFile(new File("test.mpeg"), Config())
    sf.extension should be("mpeg")

    sf = ShrinkWrapFile(new File("test.tar.gz"), Config())
    sf.extension should be("gz")

    sf = ShrinkWrapFile(new File("test"), Config())
    sf.extension should be("")
  }

  it should "return its transcoded file" in {
    sf = ShrinkWrapFile(new File("test.mp4"), Config())
    sf.transcodedFile.getName should be("test-tc.mp4")
  }

  it should s"be an original file if it does not contain the transcode suffix" in {
    sf = ShrinkWrapFile(new File("test-tc.mp4"), Config())
    sf.isOriginal should be(false) withClue "file with suffix is transcoded"

    sf = ShrinkWrapFile(new File("test-t-c.mp4"), Config())
    sf.isOriginal should be(true) withClue "suffix must be intact"

    sf = ShrinkWrapFile(new File("test-tcsomething-original.mp4"), Config())
    sf.isOriginal should be(true) withClue "suffix must come at the end"

    sf = ShrinkWrapFile(new File("test"), Config())
    sf.isOriginal should be(true) withClue "file with no extension or suffix should be considered original"
  }

}
