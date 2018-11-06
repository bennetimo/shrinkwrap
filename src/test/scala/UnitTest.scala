package io.coderunner.shrinkwrap

import org.scalatest.{AppendedClues, FlatSpec, Matchers}
import org.scalatest.prop.GeneratorDrivenPropertyChecks

abstract class UnitTest(component: String)
    extends FlatSpec
    with Matchers
    with GeneratorDrivenPropertyChecks
    with AppendedClues {
  behavior of component
}
