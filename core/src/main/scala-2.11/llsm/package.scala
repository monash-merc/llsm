import cats.instances.{EitherInstances, ListInstances}
import cats.syntax.{EitherSyntax, ListSyntax}

package object llsm {
  object either extends EitherInstances with EitherSyntax
  object list extends ListInstances with ListSyntax {
    def sequenceListEither[A, E](l: List[Either[E, A]]): Either[E, List[A]] =
      l.foldRight(Right(Nil): Either[E, List[A]]) {
        (el, acc) => for { xs <- acc.right; x <- el.right } yield x :: xs
      }
  }
}
