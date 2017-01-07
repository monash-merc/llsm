import ReleaseTransformations._
import com.typesafe.sbt.SbtGhPages.GhPagesKeys._
import sbtunidoc.Plugin.UnidocKeys._
import com.typesafe.tools.mima.plugin.MimaPlugin.mimaDefaultSettings

addCommandAlias("gitSnapshots", ";set version in ThisBuild := git.gitDescribedVersion.value.get + \"-SNAPSHOT\"")

/**
 * Settings
 */

val disabledReplOptions = Set("-Ywarn-unused-import")

lazy val buildSettings = Seq(
  organization := "edu.monash",
  licenses ++= Seq(
    ("MIT", url("http://opensource.org/licenses/MIT")),
    ("BSD New", url("http://opensource.org/licenses/BSD-3-Clause"))
	),
  scalaVersion := "2.12.1",
  crossScalaVersions := Seq(scalaVersion.value, "2.11.8"),
  addCompilerPlugin("org.spire-math" % "kind-projector" % "0.9.3" cross CrossVersion.binary)
  )

lazy val commonSettings = List(
  scalacOptions ++= List(
    "-deprecation",
    "-encoding", "UTF-8",
    "-feature",
    "-language:existentials",
    "-language:higherKinds",
    "-language:implicitConversions",
    "-unchecked",
    "-Xfatal-warnings",
    "-Xlint",
    "-Yno-adapted-args",
    "-Ywarn-dead-code",
    "-Ywarn-numeric-widen",
    "-Ywarn-value-discard"
  ) ++ scalaVersionFlags(scalaVersion.value),
  resolvers ++= Seq(
    "imagej.public" at "http://maven.imagej.net/content/groups/public"
  ),
  scalacOptions in (Compile, console) ~= { _.filterNot(disabledReplOptions.contains(_)) },
  scalacOptions in (Test, console) := (scalacOptions in (Compile, console)).value,
  libraryDependencies ++= scalaVersionDeps(scalaVersion.value),
  libraryDependencies ++= Seq("com.lihaoyi" % "ammonite" % "0.8.1" % "test" cross CrossVersion.full),
  initialCommands in (Test, console) := """ammonite.Main().run()"""
)

lazy val publishSettings = List(
  publishMavenStyle := true,
  publishTo := {
    val nexus = "https://oss.sonatype.org/"
    if (isSnapshot.value)
      Some("snapshots" at nexus + "content/repositories/snapshots")
    else
      Some("releases"  at nexus + "service/local/staging/deploy/maven2")
  },
  publishArtifact in Test := false,
  homepage := Some(url("https://github.com/keithschulze/llsm")),
  pomIncludeRepository := Function.const(false),
  pomExtra := (
    <scm>
      <url>git@github.com:keithschulze/llsm.git</url>
      <connection>scm:git:git@github.com:keithschulze/llsm.git</connection>
    </scm>
    <developers>
      <developer>
        <id>keithschulze</id>
        <name>Keith Schulze</name>
        <url>http://keithschulze.com</url>
      </developer>
    </developers>
  ),
  releaseCrossBuild := true,
  releasePublishArtifactsAction := PgpKeys.publishSigned.value,
  releaseProcess := List[ReleaseStep](
    checkSnapshotDependencies,
    inquireVersions,
    runClean,
    ReleaseStep(action = Command.process("package", _)),
    setReleaseVersion,
    commitReleaseVersion,
    tagRelease,
    ReleaseStep(action = Command.process("publishSigned", _)),
    setNextVersion,
    commitNextVersion,
    ReleaseStep(action = Command.process("sonatypeReleaseAll", _)),
    pushChanges)
)

lazy val noPublishSettings = List(
  publish := (),
  publishLocal := (),
  publishArtifact := false
)

lazy val scoverageSettings = Seq(
  coverageMinimum := 60,
  coverageFailOnMinimum := false
  )

lazy val llsmSettings = buildSettings ++ commonSettings ++ publishSettings ++ scoverageSettings

lazy val docMappingsApiDir = settingKey[String]("Subdirectory in site target directory for API docs")

lazy val docSettings = Seq(
  micrositeName := "llsm",
  micrositeDescription := "Lattice LightSheet Microscopy image processing library",
  micrositeAuthor := "Keith Schulze",
  micrositeHighlightTheme := "atom-one-light",
  micrositeBaseUrl := "llsm",
  micrositeDocumentationUrl := "/llsm/docs/",
  micrositeGithubOwner := "keithschulze",
  micrositeGithubRepo := "llsm",
  autoAPIMappings := true,
  unidocProjectFilter in (ScalaUnidoc, unidoc) := inProjects(core, io),
  docMappingsApiDir := "api",
  addMappingsToSiteDir(mappings in (ScalaUnidoc, packageDoc), docMappingsApiDir),
  git.remoteRepo := "git@github.com:keithschulze/llsm.git",
  ghpagesNoJekyll := false,
  fork in tut := true,
  fork in (ScalaUnidoc, unidoc) := true,
  scalacOptions in (ScalaUnidoc, unidoc) ++= Seq(
      "-doc-source-url", "https://github.com/keithschulze/llsm/tree/master€{FILE_PATH}.scala",
      "-sourcepath", baseDirectory.in(LocalRootProject).value.getAbsolutePath,
      "-diagrams"
    ),
  includeFilter in makeSite := "*.html" | "*.css" | "*.png" | "*.jpg" | "*.gif" | "*.js" | "*.swf" | "*.yml" | "*.md"
)

disablePlugins(AssemblyPlugin)

//Projects

lazy val docs = project.in(file("docs"))
  .disablePlugins(AssemblyPlugin)
  .enablePlugins(MicrositesPlugin)
  .settings(moduleName := "llsm-docs")
  .settings(llsmSettings)
  .settings(noPublishSettings)
  .settings(unidocSettings)
  .settings(ghpages.settings)
  .settings(docSettings)
  .settings(tutScalacOptions ~= (_.filterNot(Set("-Ywarn-unused-import", "-Ywarn-dead-code"))))
  .dependsOn(core, io)

lazy val llsm = project.in(file("."))
  .disablePlugins(AssemblyPlugin)
  .settings(llsmSettings)
  .settings(noPublishSettings)
  .aggregate(core, io, ij, tests, docs)
  .dependsOn(core, io, ij, tests % "compile;test-internal -> test", benchmark % "compile-internal;test-internal -> test")

lazy val core = project.in(file("core"))
  .disablePlugins(AssemblyPlugin)
  .settings(moduleName := "llsm-core")
  .settings(llsmSettings)
  .settings(
    libraryDependencies ++= Seq(
      "net.imglib2"   % "imglib2"                 % "3.2.0"           % "provided",
      "net.imglib2"   % "imglib2-realtransform"   % "2.0.0-beta-32"   % "provided"
    )
  )

lazy val io = project.in(file("io"))
  .disablePlugins(AssemblyPlugin)
  .dependsOn(core)
  .settings(moduleName := "llsm-io")
  .settings(llsmSettings)
  .settings(
    libraryDependencies ++= Seq(
      "org.typelevel"   %% "cats-core"            % "0.8.1",
      "com.chuusai"     %% "shapeless"            % "2.3.2",
      "io.scif"         %  "scifio"               % "0.29.0"  % "provided",
      "io.scif"         %  "scifio-bf-compat"     % "2.0.0"   % "provided"
    )
  )

lazy val streaming = project.in(file("streaming"))
  .disablePlugins(AssemblyPlugin)
  .dependsOn(core, io)
  .settings(moduleName := "llsm-streaming")
  .settings(llsmSettings)
  .settings(
    libraryDependencies ++= Seq(
      "io.iteratee"   %% "iteratee-files"            % "0.8.0"
    )
  )

lazy val ij = project.in(file("ij"))
  .dependsOn(core, io)
  .settings(moduleName := "llsm-ij")
  .settings(llsmSettings)
  .settings(
    scalaVersion := "2.12.0",
    crossScalaVersions := Seq(scalaVersion.value),
    libraryDependencies ++= Seq(
      "net.imagej"    % "imagej"          % "2.0.0-rc-55" % "provided",
      "net.imagej"    % "imagej-legacy"   % "0.23.2"      % "provided",
      "org.scijava"   % "scripting-scala" % "0.1.0"       % "provided"
    ),
    mainClass in (Compile, run) := Some("net.imagej.Main"),
    fork in run := true,
    javaOptions += "-Xmx8G",
    test in assembly := {},
    assemblyJarName in assembly := "llsm-ij.jar",
    assemblyOption in assembly := (assemblyOption in assembly).value.copy(includeScala = false),
    run in Compile <<= Defaults.runTask(fullClasspath in Compile, mainClass in (Compile, run), runner in (Compile, run))
  )

lazy val tests = project.in(file("tests"))
  .disablePlugins(AssemblyPlugin)
  .dependsOn(core, io)
  .settings(moduleName := "llsm-tests")
  .settings(llsmSettings:_*)
  .settings(noPublishSettings:_*)
  .settings(
    libraryDependencies ++= Seq(
      "org.scalacheck"  %%  "scalacheck"              % "1.13.4",
      "org.scalatest"   %%  "scalatest"               % "3.0.1",
      "net.imglib2"     %   "imglib2"                 % "3.2.0"           % "test",
      "net.imglib2"     %   "imglib2-realtransform"   % "2.0.0-beta-32"   % "test",
      "io.scif"         %   "scifio"                  % "0.29.0"          % "test",
      "io.scif"         %   "scifio-bf-compat"        % "2.0.0"           % "test"
    )
  )

lazy val benchmark = project.in(file("benchmark"))
  .disablePlugins(AssemblyPlugin)
  .enablePlugins(JmhPlugin)
  .settings(llsmSettings:_*)
  .settings(noPublishSettings:_*)
  .dependsOn(core, io, ij)




def scalaVersionFlags(version: String): List[String] = CrossVersion.partialVersion(version) match {
  case Some((2, 11)) => List("-Ywarn-unused-import")
  case Some((2, 12)) => List("-Ypartial-unification", "-Ywarn-unused-import")
  case _             => List.empty
}

def scalaVersionDeps(version: String): List[ModuleID] = CrossVersion.partialVersion(version) match {
  case Some((2, 11)) => List(compilerPlugin("com.milessabin" % "si2712fix-plugin_2.11.8" % "1.2.0"))
  case Some((2, 12)) => List.empty
  case _             => List.empty
}
