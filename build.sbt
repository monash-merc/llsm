import ReleaseTransformations._
import com.typesafe.sbt.SbtGhPages.GhPagesKeys._
import sbtunidoc.Plugin.UnidocKeys._
import com.typesafe.tools.mima.plugin.MimaPlugin.mimaDefaultSettings

scalaVersion in ThisBuild := "2.11.8"

addCommandAlias(
  "gitSnapshots",
  ";set version in ThisBuild := git.gitDescribedVersion.value.get + \"-SNAPSHOT\"")

/**
  * Settings
  */
val disabledReplOptions = Set("-Ywarn-unused-import")

lazy val buildSettings = Seq(
  scalaVersion := "2.11.8",
  crossScalaVersions := Seq("2.11.8", "2.12.1"),
  libraryDependencies ++= scalaVersionDeps(scalaVersion.value)
)

lazy val commonSettings = List(
  organization := "edu.monash",
  licenses ++= Seq(
    ("MIT", url("http://opensource.org/licenses/MIT")),
    ("BSD New", url("http://opensource.org/licenses/BSD-3-Clause"))
  ),
  scalacOptions ++= List(
    "-deprecation",
    "-encoding",
    "UTF-8",
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
  scalacOptions in (Compile, console) ~= {
    _.filterNot(disabledReplOptions.contains(_))
  },
  scalacOptions in (Test, console) := (scalacOptions in (Compile, console)).value,
  libraryDependencies ++= Seq(
    "com.lihaoyi" % "ammonite" % "0.8.1" % "test" cross CrossVersion.full),
  initialCommands in (Test, console) := """ammonite.Main().run()""",
  addCompilerPlugin(
    "org.spire-math" % "kind-projector" % "0.9.3" cross CrossVersion.binary)
)

lazy val publishSettings = List(
  publishMavenStyle := true,
  publishTo := {
    val nexus = "https://oss.sonatype.org/"
    if (isSnapshot.value)
      Some("snapshots" at nexus + "content/repositories/snapshots")
    else
      Some("releases" at nexus + "service/local/staging/deploy/maven2")
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
    pushChanges
  )
)

lazy val ijPublishSettings = List(
  releaseProcess := List[ReleaseStep](
    checkSnapshotDependencies,
    inquireVersions,
    runClean,
    ReleaseStep(action = Command.process("ij/assembly", _)),
    setReleaseVersion,
    commitReleaseVersion,
    tagRelease,
    setNextVersion,
    commitNextVersion,
    pushChanges
  )
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

lazy val llsmSettings = buildSettings ++ commonSettings ++ noPublishSettings ++ scoverageSettings

lazy val docMappingsApiDir =
  settingKey[String]("Subdirectory in site target directory for API docs")

lazy val docSettings = Seq(
  micrositeName := "llsm",
  micrositeDescription := "Lattice LightSheet Microscopy image processing library",
  micrositeAuthor := "Keith Schulze",
  micrositeHighlightTheme := "atom-one-light",
  micrositeBaseUrl := "llsm",
  micrositeDocumentationUrl := "/llsm/docs/",
  micrositeGithubOwner := "keithschulze",
  micrositeGithubRepo := "llsm",
  micrositePalette := Map(
    "brand-primary"   -> "#FFFFFF",
    "brand-secondary" -> "#006CAC",
    "brand-tertiary"  -> "#004E7C",
    "gray-dark"       -> "#242424",
    "gray"            -> "#575757",
    "gray-light"      -> "#E3E2E3",
    "gray-lighter"    -> "#F5F6F6",
    "white-color"     -> "#FFFFFF"
  ),
  autoAPIMappings := true,
  unidocProjectFilter in (ScalaUnidoc, unidoc) := inProjects(core, api),
  docMappingsApiDir := "api",
  addMappingsToSiteDir(mappings in (ScalaUnidoc, packageDoc),
                       docMappingsApiDir),
  git.remoteRepo := "git@github.com:keithschulze/llsm.git",
  ghpagesNoJekyll := false,
  fork in tut := true,
  fork in (ScalaUnidoc, unidoc) := true,
  scalacOptions in (ScalaUnidoc, unidoc) ++= Seq(
    "-doc-source-url",
    "https://github.com/keithschulze/llsm/tree/master€{FILE_PATH}.scala",
    "-sourcepath",
    baseDirectory.in(LocalRootProject).value.getAbsolutePath,
    "-diagrams"
  ),
  includeFilter in makeSite := "*.html" | "*.css" | "*.png" | "*.jpg" | "*.gif" | "*.js" | "*.swf" | "*.yml" | "*.md"
)

disablePlugins(AssemblyPlugin)

//Projects

lazy val docs = project
  .in(file("docs"))
  .disablePlugins(AssemblyPlugin)
  .enablePlugins(MicrositesPlugin)
  .settings(moduleName := "llsm-docs")
  .settings(llsmSettings)
  .settings(noPublishSettings)
  .settings(unidocSettings)
  .settings(ghpages.settings)
  .settings(docSettings)
  .settings(tutScalacOptions ~= (_.filterNot(
    Set("-Ywarn-unused-import", "-Ywarn-dead-code"))))
  .dependsOn(core, api)

lazy val llsm = project
  .in(file("."))
  .disablePlugins(AssemblyPlugin)
  .settings(llsmSettings)
  .settings(noPublishSettings)
  .aggregate(core, api, ij, tests, docs)
  .dependsOn(core,
             api,
             ij,
             tests     % "compile;test-internal -> test",
             benchmark % "compile-internal;test-internal -> test")

lazy val core = project
  .in(file("core"))
  .disablePlugins(AssemblyPlugin)
  .settings(moduleName := "llsm-core")
  .settings(llsmSettings)
  .settings(
    libraryDependencies ++= Seq(
      "net.imglib2" % "imglib2"               % "3.2.1"         % "provided",
      "net.imglib2" % "imglib2-algorithm"     % "0.6.2"         % "provided",
      "net.imglib2" % "imglib2-realtransform" % "2.0.0-beta-34" % "provided",
      "io.scif"     % "scifio"                % "0.31.1"        % "provided"
    ) ++ coreVersionDeps(scalaVersion.value)
  )

lazy val api = project
  .in(file("api"))
  .disablePlugins(AssemblyPlugin)
  .dependsOn(core)
  .settings(moduleName := "llsm-api")
  .settings(llsmSettings)
  .settings(
    libraryDependencies ++= Seq(
      "org.typelevel" %% "cats-free"            % "0.9.0",
      "net.imglib2"   % "imglib2"               % "3.2.1"         % "provided",
      "net.imglib2"   % "imglib2-realtransform" % "2.0.0-beta-34" % "provided",
      "io.scif"       % "scifio"                % "0.31.1"        % "provided",
      "io.scif"       % "scifio-ome-xml"        % "0.14.2"        % "provided",
      "sc.fiji"       % "bigdataviewer-core"    % "3.0.3"         % "provided"
    )
  )

lazy val ij = project
  .in(file("ij"))
  .dependsOn(core, api)
  .settings(moduleName := "llsm-ij")
  .settings(llsmSettings)
  .settings(ijPublishSettings)
  .settings(
    libraryDependencies ++= Seq(
      "io.scif"       % "scifio-ome-xml"        % "0.14.2"        % "provided",
      "net.imagej"    % "imagej"                % "2.0.0-rc-59"   % "provided",
      "net.imagej"    % "imagej-legacy"         % "0.23.5"        % "provided",
      "org.scijava"   % "scripting-scala"       % "0.2.0"         % "provided",
      "sc.fiji"       % "bigdataviewer-core"    % "3.0.3"         % "provided"
    ),
    mainClass in (Compile, run) := Some("net.imagej.Main"),
    fork in run := true,
    javaOptions ++= Seq("-Xmx12G"),//, "-Dscijava.log.level=debug"),
    test in assembly := {},
    assemblyJarName in assembly := s"llsm-ij_${scalaVersion.value}.jar",
    assemblyOption in assembly := (assemblyOption in assembly).value
      .copy(includeScala = false),
    run in Compile <<= Defaults.runTask(fullClasspath in Compile,
                                        mainClass in (Compile, run),
                                        runner in (Compile, run))
  )

lazy val tests = project
  .in(file("tests"))
  .disablePlugins(AssemblyPlugin)
  .dependsOn(core, api)
  .settings(moduleName := "llsm-tests")
  .settings(llsmSettings: _*)
  .settings(noPublishSettings: _*)
  .settings(
    libraryDependencies ++= Seq(
      "org.scalacheck" %% "scalacheck"           % "1.13.4",
      "org.scalatest"  %% "scalatest"            % "3.0.1",
      "net.imglib2"    % "imglib2"               % "3.2.1",
      "net.imglib2"    % "imglib2-realtransform" % "2.0.0-beta-34",
      "io.scif"        % "scifio"                % "0.31.1",
      "io.scif"        % "scifio-ome-xml"        % "0.14.2",
      "sc.fiji"        % "bigdataviewer-core"    % "3.0.3"
    )
  )

lazy val benchmark = project
  .in(file("benchmark"))
  .disablePlugins(AssemblyPlugin)
  .enablePlugins(JmhPlugin)
  .settings(llsmSettings: _*)
  .settings(noPublishSettings: _*)
  .dependsOn(core, api)
  .settings(
    libraryDependencies ++= Seq(
      "net.imglib2"   %   "imglib2"                 % "3.2.1",
      "net.imglib2"   %   "imglib2-realtransform"   % "2.0.0-beta-34",
      "net.imagej"    %   "imagej"                  % "2.0.0-rc-59",
      "io.scif"       %   "scifio"                  % "0.31.1",
      "io.scif"       %   "scifio-ome-xml"          % "0.14.2",
      "sc.fiji"       %   "bigdataviewer-core"      % "3.0.3",
      "io.monix"      %%  "monix-eval"              % "2.2.2",
      "io.monix"      %%  "monix-cats"              % "2.2.2"
    )
  )

def scalaVersionFlags(version: String): List[String] =
  CrossVersion.partialVersion(version) match {
    case Some((2, 11)) => List("-Ywarn-unused-import")
    case Some((2, 12)) => List("-Ypartial-unification")
    // case Some((2, 12)) => List("-Ypartial-unification", "-Ywarn-unused-import")
    case _             => List.empty
  }

def scalaVersionDeps(version: String): List[ModuleID] =
  CrossVersion.partialVersion(version) match {
    case Some((2, 11)) =>
      List(
        compilerPlugin("com.milessabin" % "si2712fix-plugin_2.11.8" % "1.2.0"))
    case Some((2, 12)) => List.empty
    case _             => List.empty
  }


def coreVersionDeps(version: String): List[ModuleID] =
  CrossVersion.partialVersion(version) match {
    case Some((2, 11)) =>
      List("org.typelevel" %% "cats-core"       % "0.9.0")
    case Some((2, 12)) => List.empty
    case _             => List.empty
  }
