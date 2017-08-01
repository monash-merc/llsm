import ReleaseTransformations._
import sbtunidoc.Plugin.UnidocKeys._
import com.typesafe.tools.mima.plugin.MimaPlugin.mimaDefaultSettings

scalaVersion in ThisBuild := "2.11.8"

addCommandAlias(
  "gitSnapshots",
  ";set version in ThisBuild := git.gitDescribedVersion.value.get + \"-SNAPSHOT\"")

/**
 * Common dep versions
 */
lazy val imglib2Version     = "4.2.1"
lazy val imglib2RTVersion   = "2.0.0-beta-37"
lazy val scifioVersion      = "0.32.0"
lazy val scifioOMEVersion   = "0.14.3"
lazy val bdvCoreVersion     = "4.1.0"
lazy val imagejVersion      = "2.0.0-rc-61"
lazy val ijLegacyVersion    = "0.25.0"
lazy val ijScalaVersion     = "0.2.1"

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
    "com.lihaoyi" % "ammonite" % "1.0.0" % "test" cross CrossVersion.full),
  sourceGenerators in Test += Def.task {
      val file = (sourceManaged in Test).value / "amm.scala"
      IO.write(file, """object amm extends App { ammonite.Main().run()  }""")
      Seq(file)
  }.taskValue,
  (fullClasspath in Test) ++= {
    (updateClassifiers in Test).value
      .configurations
      .find(_.configuration == Test.name)
      .get
      .modules
      .flatMap(_.artifacts)
      .collect{case (a, f) if a.classifier == Some("sources") => f}
  },
  addCompilerPlugin(
    "org.spire-math" % "kind-projector" % "0.9.4" cross CrossVersion.binary)
)

lazy val publishSettings = List(
  publishTo := Some(Resolver.file("file",  new File(Path.userHome.absolutePath+"/.m2/repository"))),
  // publishTo := {
  //   val nexus = "https://oss.sonatype.org/"
  //   if (isSnapshot.value)
  //     Some("snapshots" at nexus + "content/repositories/snapshots")
  //   else
  //     Some("releases" at nexus + "service/local/staging/deploy/maven2")
  // },
  publishMavenStyle := true,
  publishArtifact in Test := false,
  useGpg := true,
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
        <url>https://platforms.monash.edu/mmi/</url>
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
    // ReleaseStep(action = Command.process("sonatypeReleaseAll", _)),
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

lazy val llsmSettings = buildSettings ++ commonSettings ++ publishSettings ++ scoverageSettings

lazy val docMappingsApiDir =
  settingKey[String]("Subdirectory in site target directory for API docs")

lazy val docSettings = Seq(
  micrositeName := "LLSM",
  micrositeDescription := "Lattice Light-Sheet Microscopy",
  micrositeAuthor := "Keith Schulze",
  micrositeHighlightTheme := "atom-one-light",
  micrositeBaseUrl := "llsm",
  micrositeDocumentationUrl := "/llsm/docs/",
  micrositeGithubOwner := "keithschulze",
  micrositeGithubRepo := "llsm",
  micrositePalette := Map(
    "brand-primary"   -> "#43C5E4",
    "brand-secondary" -> "#006CAB",
    "brand-tertiary"  -> "#009FD8",
    "gray-dark"       -> "#231F20",
    "gray"            -> "#423F40",
    "gray-light"      -> "#939598",
    "gray-lighter"    -> "#F7F8F8",
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
  .settings(docSettings)
  .settings(
    scalacOptions in Tut ~= (_.filterNot(
    Set("-Ywarn-unused-import", "-Ywarn-dead-code")))
  )
  .dependsOn(core, api)

lazy val llsm = project
  .in(file("."))
  .disablePlugins(AssemblyPlugin)
  .settings(llsmSettings)
  .settings(noPublishSettings)
  .aggregate(core, api, ij, cli, tests, docs)
  .dependsOn(core,
             api,
             ij,
             cli,
             tests     % "compile;test-internal -> test",
             benchmark % "compile-internal;test-internal -> test")

lazy val core = project
  .in(file("core"))
  .disablePlugins(AssemblyPlugin)
  .settings(moduleName := "llsm-core")
  .settings(llsmSettings)
  .settings(
    exportJars := true,
    libraryDependencies ++= Seq(
      "net.imglib2" % "imglib2"               % imglib2Version    % "provided",
      "net.imglib2" % "imglib2-algorithm"     % "0.8.0"           % "provided",
      "net.imglib2" % "imglib2-realtransform" % imglib2RTVersion  % "provided",
      "io.scif"     % "scifio"                % scifioVersion     % "provided"
    ) ++ coreVersionDeps(scalaVersion.value)
  )

lazy val api = project
  .in(file("api"))
  .disablePlugins(AssemblyPlugin)
  .dependsOn(core)
  .settings(moduleName := "llsm-api")
  .settings(llsmSettings)
  .settings(
    exportJars := true,
    libraryDependencies ++= Seq(
      "org.scala-lang.modules"  %% "scala-xml"            % "1.0.6",
      "org.typelevel"           %% "cats-free"            % "0.9.0",
      "net.imglib2"             % "imglib2"               % imglib2Version    % "provided",
      "net.imglib2"             % "imglib2-realtransform" % imglib2RTVersion  % "provided",
      "io.scif"                 % "scifio"                % scifioVersion     % "provided",
      "io.scif"                 % "scifio-ome-xml"        % scifioOMEVersion  % "provided",
      "sc.fiji"                 % "bigdataviewer-core"    % bdvCoreVersion    % "provided"
    )
  )

lazy val cli = project
  .in(file("cli"))
  .disablePlugins(AssemblyPlugin)
  .enablePlugins(JavaAppPackaging)
  .dependsOn(core, api)
  .settings(moduleName := "llsm-cli")
  .settings(llsmSettings)
  .settings(noPublishSettings)
  .settings(
    libraryDependencies ++= Seq(
      "com.github.scopt"  %% "scopt"                % "3.6.0",
      "net.imglib2"       % "imglib2"               % imglib2Version,
      "net.imglib2"       % "imglib2-realtransform" % imglib2RTVersion,
      "io.scif"           % "scifio"                % scifioVersion,
      "io.scif"           % "scifio-ome-xml"        % scifioOMEVersion,
      "sc.fiji"           % "bigdataviewer-core"    % bdvCoreVersion
      ),
    name := "llsmc",
    mainClass in Compile := Some("llsm.cli.LLSMC")
  )


lazy val ij = project
  .in(file("ij"))
  .dependsOn(core, api)
  .settings(moduleName := "llsm-ij")
  .settings(llsmSettings)
  .settings(noPublishSettings)
  .settings(
    libraryDependencies ++= Seq(
      "io.scif"       % "scifio-ome-xml"        % scifioOMEVersion  % "provided",
      "net.imagej"    % "imagej"                % imagejVersion     % "provided",
      "net.imagej"    % "imagej-legacy"         % ijLegacyVersion   % "provided",
      "org.scijava"   % "scripting-scala"       % ijScalaVersion    % "provided",
      "sc.fiji"       % "bigdataviewer-core"    % bdvCoreVersion    % "provided"
    ),
    mainClass in (Compile, run) := Some("net.imagej.Main"),
    fork in run := true,
    javaOptions ++= Seq("-Xmx12G"),//, "-Dscijava.log.level=debug"),
    test in assembly := {},
    assemblyJarName in assembly := s"llsm-ij_${scalaVersion.value}.jar",
    assemblyOption in assembly := (assemblyOption in assembly).value
      .copy(includeScala = false, cacheOutput = false),
    run in Compile := Defaults.runTask(fullClasspath in Compile,
                                       mainClass in (Compile, run),
                                       runner in (Compile, run)).evaluated
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
      "net.imglib2"    % "imglib2"               % imglib2Version,
      "net.imglib2"    % "imglib2-realtransform" % imglib2RTVersion,
      "io.scif"        % "scifio"                % scifioVersion,
      "io.scif"        % "scifio-ome-xml"        % scifioOMEVersion,
      "sc.fiji"        % "bigdataviewer-core"    % bdvCoreVersion
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
      "net.imglib2"   %   "imglib2"                 % imglib2Version,
      "net.imglib2"   %   "imglib2-realtransform"   % imglib2RTVersion,
      "net.imagej"    %   "imagej"                  % imagejVersion,
      "io.scif"       %   "scifio"                  % scifioVersion,
      "io.scif"       %   "scifio-ome-xml"          % scifioOMEVersion,
      "sc.fiji"       %   "bigdataviewer-core"      % bdvCoreVersion,
      "io.monix"      %%  "monix-eval"              % "2.3.0",
      "io.monix"      %%  "monix-cats"              % "2.3.0"
    )
  )

def scalaVersionFlags(version: String): List[String] =
  CrossVersion.partialVersion(version) match {
    case Some((2, 11)) => List("-Ywarn-unused-import")
    case Some((2, 12)) => List("-Ypartial-unification", "-Ywarn-unused-import")
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
