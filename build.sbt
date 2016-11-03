import org.typelevel.{Dependencies => typelevel}
import llsm.{Dependencies => llsm}

addCommandAlias("gitSnapshots", ";set version in ThisBuild := git.gitDescribedVersion.value.get + \"-SNAPSHOT\"")

scalaVersion in ThisBuild := "2.11.8"

disablePlugins(AssemblyPlugin)

//Project Settings
val gh = GitHubSettings(org = "keithschulze", proj = "llsm", publishOrg = "edu.monash", license = mit)
val devs = Seq(Dev("Keith Schulze", "keithschulze"))

val vers = typelevel.versions ++ llsm.versions
val libs = typelevel.libraries ++ llsm.libraries
val addins = typelevel.scalacPlugins ++ llsm.scalacPlugins
val vAll = Versions(vers, libs, addins)

lazy val rootSettings = buildSettings ++ commonSettings ++ publishSettings ++ scoverageSettings

lazy val root = project
  .disablePlugins(AssemblyPlugin)
  .configure(mkRootJvmConfig(gh.proj, rootSettings, commonJvmSettings))
  .aggregate(core, io, ij, tests, docs)
  .dependsOn(core, io, ij, tests % "compile;test-internal -> test")

lazy val core = project
  .disablePlugins(AssemblyPlugin)
  .settings(moduleName := "llsm-core")
  .settings(rootSettings:_*)
  .settings(commonJvmSettings:_*)
  .settings(
    libraryDependencies ++= Seq(
      "net.imglib2"   % "imglib2"                 % "3.2.0"           % "provided",
      "net.imglib2"   % "imglib2-realtransform"   % "2.0.0-beta-32"   % "provided"
    )
  )

lazy val io = project
  .disablePlugins(AssemblyPlugin)
  .dependsOn(core)
  .settings(moduleName := "llsm-io")
  .settings(rootSettings:_*)
  .settings(commonJvmSettings:_*)
  .settings(addLibs(vAll, "cats-core"):_*)
  .settings(
    libraryDependencies ++= Seq(
      "io.scif"   % "scifio"              % "0.29.0"  % "provided",
      "io.scif"   % "scifio-bf-compat"    % "2.0.0"   % "provided"
    )
  )

lazy val streaming = project
  .disablePlugins(AssemblyPlugin)
  .dependsOn(core, io)
  .settings(moduleName := "llsm-streaming")
  .settings(rootSettings:_*)
  .settings(commonJvmSettings:_*)
  .settings(addLibs(vAll, "shapeless", "iteratee-files"):_*)

lazy val ij = project
  .dependsOn(core, io)
  .settings(moduleName := "llsm-ij")
  .settings(rootSettings:_*)
  .settings(commonJvmSettings:_*)
  .settings(
    libraryDependencies ++= Seq(
      "net.imagej"    % "imagej"          % "2.0.0-rc-55" % "provided",
      "net.imagej"    % "imagej-legacy"   % "0.23.2"      % "provided",
      "org.scijava"   % "scripting-scala" % "0.1.0"       % "provided"
    ),
    mainClass in (Compile, run) := Some("net.imagej.Main"),
    fork in run := true,
    javaOptions += "-Xmx4G",
    test in assembly := {},
    assemblyJarName in assembly := "llsm-ij.jar",
    assemblyOption in assembly := (assemblyOption in assembly).value.copy(includeScala = false),
    run in Compile <<= Defaults.runTask(fullClasspath in Compile, mainClass in (Compile, run), runner in (Compile, run))
  )

lazy val tests = project
  .disablePlugins(AssemblyPlugin)
  .dependsOn(core, io, ij)
  .settings(moduleName := "llsm-tests")
  .settings(rootSettings:_*)
  .settings(commonJvmSettings:_*)
  .settings(noPublishSettings:_*)
  .settings(addLibs(vAll, "scalatest", "scalacheck"):_*)
  .settings(
    libraryDependencies ++= Seq(
      "net.imglib2"   % "imglib2"                 % "3.2.0"           % "test",
      "net.imglib2"   % "imglib2-realtransform"   % "2.0.0-beta-32"   % "test",
      "io.scif"       % "scifio"                  % "0.29.0"          % "test",
      "io.scif"       % "scifio-bf-compat"        % "2.0.0"           % "test"
    )
  )

lazy val benchmark = project
  .disablePlugins(AssemblyPlugin)
  .dependsOn(core, io, ij)
  .enablePlugins(JmhPlugin)
  .settings(rootSettings:_*)
  .settings(commonJvmSettings:_*)
  .settings(noPublishSettings:_*)

lazy val docs = project
  .disablePlugins(AssemblyPlugin)
  .configure(mkDocConfig(gh, rootSettings, commonJvmSettings, core, io))


/**
 * Settings
 */
lazy val buildSettings = localSharedBuildSettings(vAll)

lazy val commonSettings = sharedCommonSettings ++
  addTestLibs(vAll) ++
  addCompilerPlugins(vAll, "kind-projector", "paradise") ++ Seq(
  scalacOptions ++= scalacAllOptions,
  scalacOptions += "-Ypartial-unification",
  scalacOptions += "-Yinline-warnings",
  parallelExecution in Test := false,
  resolvers ++= Seq(
    "imagej.public" at "http://maven.imagej.net/content/groups/public"
  )
  // libraryDependencies += "com.lihaoyi" % "ammonite" % "0.7.8" % "test" cross CrossVersion.full,
  // initialCommands in (Test, console) := """ammonite.Main().run()"""
) ++ warnUnusedImport ++ unidocCommonSettings ++ update2_12// spurious warnings from macro annotations expected

lazy val commonJvmSettings = sharedJvmSettings

lazy val disciplineDependencies = Seq(addLibs(vAll, "discipline", "scalacheck" ):_*)

lazy val publishSettings = sharedPublishSettings(gh, devs) ++ credentialSettings ++ sharedReleaseProcess

lazy val scoverageSettings = sharedScoverageSettings(60)

// sharedScoverageSettings from sbt-catalyst not compatible with recent
// sbt-coverage build
def sharedScoverageSettings(min: Int = 80) = Seq(
  coverageMinimum := min,
  coverageFailOnMinimum := false
  //   ,coverageHighlighting := scalaBinaryVersion.value != "2.10"
  )

def localSharedBuildSettings(v: Versions) = Seq(
  scalaOrganization := "org.typelevel",
  scalaVersion := v.vers("scalac"),
  crossScalaVersions := Seq("2.12.0-RC2", scalaVersion.value)
  )

val cmdlineProfile = sys.props.getOrElse("sbt.profile", default = "")

def profile: Project ⇒ Project = p => cmdlineProfile match {
  case "2.12.x" => p.disablePlugins(scoverage.ScoverageSbtPlugin)
  case _ => p
}

lazy val update2_12 = Seq(
  scalacOptions -= {
    CrossVersion.partialVersion(scalaVersion.value) match {
      case Some((2, 12)) =>
        "-Yinline-warnings"
      case _ =>
        ""
    }
  }
  )

