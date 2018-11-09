package io.coderunner.shrinkwrap

import java.io.File

import org.mockito.MockitoSugar

class ProcessorSpec extends UnitTest("ProcessorSpec") with MockitoSugar {

  it should "process all files recusirvely list all files" in {
    val p = new Processor(Config(), Nil)

    val dir  = new File("target/test-dir"); dir.mkdir()
    val dir2 = new File("target/test-dir/test-dir2"); dir2.mkdir()
    val dir3 = new File("target/test-dir/test-dir2/test-dir3"); dir3.mkdir()

    val f1 = new File("target/test-dir/testfile1"); f1.createNewFile()
    val f2 = new File("target/test-dir/test-dir2/testfile2"); f2.createNewFile()
    val f3 = new File("target/test-dir/test-dir2/test-dir3/testfile3"); f3.createNewFile()

    p.findAllFilesInDir(dir) should be(List(f1, f2, f3))
  }

}
