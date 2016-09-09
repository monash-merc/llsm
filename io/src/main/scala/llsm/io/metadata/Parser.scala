package llsm
package io.metadata

import cats.data.Xor
import scala.util.{Try, Success, Failure}

/**
 * Parser typeclass.
 */
trait Parser[A] {
  def apply(s: String): Parser.Result[A]
}

/**
 * Utilities and basic instances for [[Parser]] typeclass.
 *
 * @author Keith Schulze
 */
final object Parser extends ParserInstances {

  def apply[A](s: String)(implicit parser: Parser[A]): Result[A] = parser(s)

  // Parser.Result[A] type is simply a alias for Xor
  type Result[A] = Xor[ParsingFailure, A]

}

private[metadata] sealed abstract class ParserInstances {

  implicit val stringParser = new Parser[String] {
    def apply(s: String) = Xor.right(s)
  }

  implicit val intParser = new Parser[Int] {
    def apply(s: String) = Try(s.toInt) match {
      case Success(v) => Xor.right(v)
      case Failure(e) => Xor.left(ParsingFailure("Failed to parse Int", e))
    }
  }

  implicit val doubleParser = new Parser[Double] {
    def apply(s: String) = Try(s.toDouble) match {
      case Success(v) => Xor.right(v)
      case Failure(e) => Xor.left(ParsingFailure("Failed to parse Double", e))
    }
  }

  implicit val booleanParser = new Parser[Boolean] {
    def apply(s: String) = Try(s.toBoolean) match {
      case Success(v) => Xor.right(v)
      case Failure(e) => Xor.left(ParsingFailure("Failed to parse Boolean", e))
    }
  }
}

/**
 * Exception class to represent failures in parsing.
 */
final case class ParsingFailure(message: String, underlying: Throwable) extends Exception {
  final override def getMessage: String = message
}
