package llsm

import sbt._
import sbtcatalysts.CatalystsPlugin.autoImport._

object Dependencies {

  // Versions for libraries and packages
  // Package -> version
  val versions = Map[String, String](
    "scalac"          ->  "2.11.8",
    "cats"            ->  "0.8.1",
    "kind-projector"  ->  "0.9.3",
    "iteratee"        ->  "0.7.1",
    "scalacheck"      ->  "1.13.4",
    "scalatest"       ->  "3.0.1"
  )

  // library definitions and links to their versions
  // Note that one version may apply to more than one library.
  // Library name -> version key, org, library
  val libraries = Map[String, (String, String, String)](
    "iteratee-core"   -> ("iteratee", "io.iteratee", "iteratee-core"),
    "iteratee-files"  -> ("iteratee", "io.iteratee", "iteratee-files")
  )

  // compiler plugins definitions and links to their versions
  // Note that one version may apply to more than one plugin.
  // Library name -> version key, org, librar, crossVersion
  val scalacPlugins = Map[String, (String, String, String, CrossVersion)](
  )

  // Some helper methods to combine libraries
}
