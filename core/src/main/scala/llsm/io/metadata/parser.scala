package llsm.io.metadata

import scala.util.{Left, Failure, Right, Success, Try}

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

  // Parser.Result[A] type is simply a alias for Either
  type Result[A] = Either[ParsingFailure, A]

}

private[metadata] abstract class ParserInstances {

  def createParser[A](func: String => A): Parser[A] =
    new Parser[A] {
      def apply(s: String): Parser.Result[A] =
        Try(func(s)) match {
          case Success(r) => Right(r)
          case Failure(e) =>
            Left(ParsingFailure(s"Failed to parse:\n${e.getMessage}", e))
        }
    }

  final implicit val stringParser: Parser[String] =
    createParser(str => str)

  final implicit val booleanParser: Parser[Boolean] =
    createParser(str => str.toBoolean)

  final implicit val intParser: Parser[Int] =
    createParser(str => str.toInt)

  final implicit val longParser: Parser[Long] =
    createParser(str => str.toLong)

  final implicit val floatParser: Parser[Float] =
    createParser(str => str.toFloat)

  final implicit val doubleParser: Parser[Double] =
    createParser(str => str.toDouble)
}

/**
  * Exception class to represent failures in parsing.
  */
final case class ParsingFailure(message: String, underlying: Throwable)
    extends Exception {
  final override def getMessage: String = message
}
