package llsm

import org.scijava.Context

trait BenchmarkContext {
  val context: Context = new Context
}

