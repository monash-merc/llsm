package llsm

import cats.instances.EitherInstances
import cats.syntax.EitherSyntax
import org.scalatest._
import org.scalatest.prop.GeneratorDrivenPropertyChecks

class BaseSuite extends FlatSpec with GeneratorDrivenPropertyChecks with EitherInstances with EitherSyntax
