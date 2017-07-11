package llsm

trait EitherImplicits

trait ListImplicits {
  def sequenceListEither[A, E](l: List[Either[E, A]]): Either[E, List[A]] =
    l.foldRight(Right(Nil): Either[E, List[A]]) {
      (el, acc) => for { xs <- acc.right; x <- el.right } yield x :: xs
    }
}