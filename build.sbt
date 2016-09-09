import org.typelevel.{Dependencies => typelevel}
import llsm.{Dependencies => llsm}

addCommandAlias("gitSnapshots", ";set version in ThisBuild := git.gitDescribedVersion.value.get + \"-SNAPSHOT\"")

scalaVersion in ThisBuild := "2.11.8"

//Project Settings
val gh = GitHubSettings(org = "keithschulze", proj = "llsm", publishOrg = "edu.monash", license = mit)
val devs = Seq(Dev("Keith Schulze", "keithschulze"))

val vers = typelevel.versions ++ llsm.versions
val libs = typelevel.libraries ++ llsm.libraries
val addins = typelevel.scalacPlugins ++ llsm.scalacPlugins
val vAll = Versions(vers, libs, addins)

lazy val rootSettings = buildSettings ++ commonSettings ++ publishSettings ++ scoverageSettings

lazy val root = project
  .configure(mkRootJvmConfig(gh.proj, rootSettings, commonJvmSettings))
  .aggregate(core, io, ij, tests, docs)
  .dependsOn(core, io, ij, tests % "compile;test-internal -> test")

lazy val core = project
  .settings(moduleName := "llsm-core")
  .settings(rootSettings:_*)
  .settings(commonJvmSettings:_*)
  .settings(addLibs(vAll, "cats-core"):_*)
  .settings(
    libraryDependencies ++= Seq(
      "net.imglib2"   % "imglib2"                 % "3.2.0",
      "net.imglib2"   % "imglib2-realtransform"   % "2.0.0-beta-32"
    )
  )

lazy val io = project
  .dependsOn(core)
  .settings(moduleName := "llsm-io")
  .settings(rootSettings:_*)
  .settings(commonJvmSettings:_*)
  .settings(addLibs(vAll, "shapeless", "simulacrum", "iteratee-files"):_*)
  .settings(
    libraryDependencies ++= Seq(
      "io.scif"   % "scifio"              % "0.27.3",
      "io.scif"   % "scifio-bf-compat"    % "2.0.0"
    )
  )

lazy val ij = project
  .dependsOn(core)
  .settings(moduleName := "llsm-ij")
  .settings(rootSettings:_*)
  .settings(commonJvmSettings:_*)
  .settings(
    libraryDependencies ++= Seq(
      "net.imagej"    % "imagej"          % "2.0.0-rc-49",
      "net.imagej"    % "imagej-legacy"   % "0.20.1",
      "org.scijava"   % "scripting-scala" % "0.1.0"
    ),
    mainClass in (Compile, run) := Some("net.imagej.Main"),
    fork in run := true,
    javaOptions += "-Xmx4G"
  )

lazy val tests = project
  .dependsOn(core, io, ij)
  .settings(moduleName := "llsm-tests")
  .settings(rootSettings:_*)
  .settings(commonJvmSettings:_*)
  .settings(noPublishSettings:_*)
  .settings(addLibs(vAll, "scalatest", "scalacheck"):_*)

lazy val benchmark = project
  .dependsOn(core, io, ij)
  .enablePlugins(JmhPlugin)
  .settings(rootSettings:_*)
  .settings(commonJvmSettings:_*)
  .settings(noPublishSettings:_*)

lazy val docs = project.configure(mkDocConfig(gh, rootSettings, commonJvmSettings, core, io, ij))


/**
 * Settings
 */
lazy val buildSettings = sharedBuildSettings(gh, vAll) ++ Seq(
  scalaOrganization := "org.typelevel"
  )

lazy val commonSettings = sharedCommonSettings ++
  addTestLibs(vAll) ++
  addCompilerPlugins(vAll, "kind-projector", "paradise") ++ Seq(
  scalacOptions ++= scalacAllOptions,
  parallelExecution in Test := false,
  resolvers ++= Seq(
    "imagej.public" at "http://maven.imagej.net/content/groups/public"
  ),
  libraryDependencies += "com.lihaoyi" % "ammonite-repl" % "0.6.2" % "test" cross CrossVersion.full,
  initialCommands in (Test, console) := """ammonite.repl.Main().run()"""
) ++ warnUnusedImport ++ unidocCommonSettings // spurious warnings from macro annotations expected

lazy val commonJvmSettings = sharedJvmSettings

lazy val disciplineDependencies = Seq(addLibs(vAll, "discipline", "scalacheck" ):_*)

lazy val publishSettings = sharedPublishSettings(gh, devs) ++ credentialSettings ++ sharedReleaseProcess

lazy val scoverageSettings = sharedScoverageSettings(60)
