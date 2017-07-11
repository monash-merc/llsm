package llsm

import cats.instances.{EitherInstances, ListInstances}
import cats.syntax.{EitherSyntax, ListSyntax}

trait EitherImplicits extends EitherInstances with EitherSyntax
trait ListImplicits extends ListInstances with ListSyntax {
  def sequenceListEither[A, E](l: List[Either[E, A]]): Either[E, List[A]] =
    l.foldRight(Right(Nil): Either[E, List[A]]) {
      (el, acc) => for { xs <- acc.right; x <- el.right } yield x :: xs
    }
}
