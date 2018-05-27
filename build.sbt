import microsites._
import ReleaseTransformations._
import com.typesafe.tools.mima.plugin.MimaPlugin.mimaDefaultSettings

inThisBuild(List(
  organization := "edu.monash",
  scalaVersion := "2.12.6",
  crossScalaVersions := Seq("2.11.12", "2.12.6")
))

addCommandAlias(
  "gitSnapshots",
  ";set version in ThisBuild := git.gitDescribedVersion.value.get + \"-SNAPSHOT\"")

/**
 * Common dep versions
 */
lazy val imglib2Version     = "5.2.0"
lazy val imglib2RTVersion   = "2.0.0"
lazy val scifioVersion      = "0.36.0"
lazy val scifioOMEVersion   = "0.14.3"
lazy val bdvCoreVersion     = "5.0.0"
lazy val imagejVersion      = "2.0.0-rc-66"
lazy val ijLegacyVersion    = "0.30.0"
lazy val ijScalaVersion     = "0.2.1"

/**
  * Settings
  */
val disabledReplOptions = Set(
  "-Ywarn-unused-import",
  "-Ywarn-unused:import",
  "-Xfatal-warnings"
)

lazy val buildSettings = Seq(
  scalacOptions ++= orgScalacOptions(scalaOrganization.value),
  libraryDependencies ++= scalaVersionDeps(scalaVersion.value)
)

lazy val commonSettings = List(
  licenses ++= Seq(
    ("MIT", url("http://opensource.org/licenses/MIT"))
  ),
  scalacOptions ++= Seq(
    "-deprecation",                      // Emit warning and location for usages of deprecated APIs.
    "-encoding", "utf-8",                // Specify character encoding used by source files.
    "-explaintypes",                     // Explain type errors in more detail.
    "-feature",                          // Emit warning and location for usages of features that should be imported explicitly.
    "-language:existentials",            // Existential types (besides wildcard types) can be written and inferred
    "-language:experimental.macros",     // Allow macro definition (besides implementation and application)
    "-language:higherKinds",             // Allow higher-kinded types
    "-language:implicitConversions",     // Allow definition of implicit functions called views
    "-unchecked",                        // Enable additional warnings where generated code depends on assumptions.
    "-Xcheckinit",                       // Wrap field accessors to throw an exception on uninitialized access.
    "-Xfatal-warnings",                  // Fail the compilation if there are any warnings.
    "-Xfuture",                          // Turn on future language features.
    "-Xlint:adapted-args",               // Warn if an argument list is modified to match the receiver.
    "-Xlint:by-name-right-associative",  // By-name parameter of right associative operator.
    "-Xlint:delayedinit-select",         // Selecting member of DelayedInit.
    "-Xlint:doc-detached",               // A Scaladoc comment appears to be detached from its element.
    "-Xlint:inaccessible",               // Warn about inaccessible types in method signatures.
    "-Xlint:infer-any",                  // Warn when a type argument is inferred to be `Any`.
    "-Xlint:missing-interpolator",       // A string literal appears to be missing an interpolator id.
    "-Xlint:nullary-override",           // Warn when non-nullary `def f()' overrides nullary `def f'.
    "-Xlint:nullary-unit",               // Warn when nullary methods return Unit.
    "-Xlint:option-implicit",            // Option.apply used implicit view.
    "-Xlint:package-object-classes",     // Class or object defined in package object.
    "-Xlint:poly-implicit-overload",     // Parameterized overloaded implicit methods are not visible as view bounds.
    "-Xlint:private-shadow",             // A private field (or class parameter) shadows a superclass field.
    "-Xlint:stars-align",                // Pattern sequence wildcard must align with sequence component.
    "-Xlint:type-parameter-shadow",      // A local type parameter shadows a type already in scope.
    "-Xlint:unsound-match",              // Pattern match may not be typesafe.
    "-Yno-adapted-args",                 // Do not adapt an argument list (either by inserting () or creating a tuple) to match the receiver.
    "-Ypartial-unification",             // Enable partial unification in type constructor inference
    "-Ywarn-dead-code",                  // Warn when dead code is identified.
    "-Ywarn-inaccessible",               // Warn about inaccessible types in method signatures.
    "-Ywarn-infer-any",                  // Warn when a type argument is inferred to be `Any`.
    "-Ywarn-nullary-override",           // Warn when non-nullary `def f()' overrides nullary `def f'.
    "-Ywarn-nullary-unit",               // Warn when nullary methods return Unit.
    "-Ywarn-numeric-widen",              // Warn when numerics are widened.
    "-Ywarn-value-discard"               // Warn when non-Unit expression results are unused.
  ) ++ scalaVersionFlags(scalaVersion.value),
  resolvers ++= Seq(
    "imagej.public" at "http://maven.imagej.net/content/groups/public"
  ),
  addCompilerPlugin(
    "org.spire-math" % "kind-projector" % "0.9.4" cross CrossVersion.binary
  ),
  scalacOptions in (Compile, console) := (scalacOptions in (Compile, console)).value.filterNot(disabledReplOptions.contains(_)),
  scalacOptions in (Test, console) := (scalacOptions in (Compile, console)).value
)

lazy val coreApiSettings = List(
  wartremoverWarnings in (Compile, compile) ++= Warts.unsafe
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
  homepage := Some(url("https://github.com/monash-merc/llsm")),
  pomIncludeRepository := Function.const(false),
  pomExtra := (
    <scm>
      <url>git@github.com:monash-merc/llsm.git</url>
      <connection>scm:git:git@github.com:monash-merc/llsm.git</connection>
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
    releaseStepCommand("package"),
    setReleaseVersion,
    commitReleaseVersion,
    tagRelease,
    releaseStepCommand("publishSigned"),
    setNextVersion,
    commitNextVersion,
    // ReleaseStep(action = Command.process("sonatypeReleaseAll", _)),
    pushChanges
  )
)

lazy val noPublishSettings = List(
  publish := {},
  publishLocal := {},
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
  micrositeDocumentationUrl := "/llsm/api/",
  micrositeGithubOwner := "monash-merc",
  micrositeGithubRepo := "llsm",
  micrositePalette := Map(
    "brand-primary"   -> "#009FD8",
    "brand-secondary" -> "#006CAB",
    "brand-tertiary"  -> "#004D7B",
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
  git.remoteRepo := "git@github.com:monash-merc/llsm.git",
  ghpagesNoJekyll := false,
  fork in tut := true,
  fork in (ScalaUnidoc, unidoc) := true,
  scalacOptions in (ScalaUnidoc, unidoc) ++= Seq(
    "-doc-source-url",
    "https://github.com/monash-merc/llsm/tree/master€{FILE_PATH}.scala",
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
  .enablePlugins(ScalaUnidocPlugin)
  .enablePlugins(MicrositesPlugin)
  .settings(moduleName := "llsm-docs")
  .settings(llsmSettings)
  .settings(noPublishSettings)
  .settings(docSettings)
  .settings(
    scalacOptions in Tut ~= (_.filterNot(
      Set(
        "-Ywarn-unused-import",
        "-Ywarn-dead-code",
        "-Ywarn-unused:imports"
      )
    )),
    libraryDependencies ++= Seq(
      "net.imglib2" % "imglib2"               % imglib2Version    % "tut",
      "net.imglib2" % "imglib2-algorithm"     % "0.9.0"           % "tut",
      "net.imglib2" % "imglib2-realtransform" % imglib2RTVersion  % "tut",
      "io.scif"     % "scifio"                % scifioVersion     % "tut",
      "io.scif"     % "scifio-ome-xml"        % scifioOMEVersion  % "tut",
      "sc.fiji"     % "bigdataviewer-core"    % bdvCoreVersion    % "tut"
    )
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
  .settings(coreApiSettings)
  .settings(
    exportJars := true,
    libraryDependencies ++= Seq(
      "net.imglib2" % "imglib2"               % imglib2Version    % "provided",
      "net.imglib2" % "imglib2-algorithm"     % "0.9.0"           % "provided",
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
  .settings(coreApiSettings)
  .settings(
    exportJars := true,
    libraryDependencies ++= Seq(
      "org.scala-lang.modules"  %% "scala-xml"            % "1.1.0",
      "org.typelevel"           %% "cats-free"            % "1.1.0",
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
      "com.github.scopt"  %% "scopt"                % "3.7.0",
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
      "io.scif"       % "scifio-ome-xml"            % scifioOMEVersion  % "provided",
      "net.imagej"    % "imagej"                    % imagejVersion     % "provided",
      "net.imagej"    % "imagej-legacy"             % ijLegacyVersion   % "provided",
      "org.scijava"   % "scripting-scala"           % ijScalaVersion    % "provided",
      "net.imglib2"             % "imglib2"               % imglib2Version    % "provided",
      "net.imglib2"   % "imglib2-cache"             % "1.0.0-beta-9"     % "provided",
      "net.imglib2"             % "imglib2-realtransform" % imglib2RTVersion  % "provided",
      "net.imglib2"   % "imglib2-ui"                % "2.0.0"   % "provided",
      "sc.fiji"       % "bigdataviewer-core"        % bdvCoreVersion    % "provided",
      "sc.fiji"       % "bigdataviewer-vistools"    % "1.0.0-beta-11"    % "provided"
    ),
    scalacOptions ~= (_ filterNot(_ == "-Xcheckinit")), // disable checkinit because ImageJ does a lot of runtime injection
    mainClass in (Compile, run) := Some("net.imagej.Main"),
    fork in run := true,
    javaOptions ++= Seq("-Xmx12G"),//, "-Dscijava.log.level=debug"),
    test in assembly := {},
    assemblyJarName in assembly := s"llsm-ij-${version.value}.jar",
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
  .settings(llsmSettings)
  .settings(coreApiSettings)
  .settings(noPublishSettings: _*)
  .settings(
    libraryDependencies ++= Seq(
      "org.scalacheck" %% "scalacheck"           % "1.14.0",
      "org.scalatest"  %% "scalatest"            % "3.0.5",
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
      "io.monix"      %%  "monix"              % "3.0.0-RC1",
    )
  )

def scalaVersionFlags(version: String): List[String] =
  CrossVersion.partialVersion(version) match {
    case Some((2, 11)) => List(
      "-Ywarn-unused-import"
      )
    case _             => List(
      "-Ywarn-extra-implicit",             // Warn when more than one implicit parameter section is defined.
      "-Ywarn-unused:implicits",           // Warn if an implicit parameter is unused.
      "-Ywarn-unused:imports",             // Warn if an import selector is not referenced.
      "-Ywarn-unused:locals",              // Warn if a local definition is unused.
      "-Ywarn-unused:params",              // Warn if a value parameter is unused.
      "-Ywarn-unused:patvars",             // Warn if a variable bound in a pattern is unused.
      "-Ywarn-unused:privates",            // Warn if a private member is unused.
      "-Xlint:constant")
  }

def orgScalacOptions(version: String): List[String] =
  version match {
    case "org.typelevel" => List(
      "-Yinduction-heuristics",       // speeds up the compilation of inductive implicit resolution
      "-Ykind-polymorphism",          // type and method definitions with type parameters of arbitrary kinds
      "-Yliteral-types",              // literals can appear in type position
      "-Xstrict-patmat-analysis",     // more accurate reporting of failures of match exhaustivity
      "-Xlint:strict-unsealed-patmat" // warn on inexhaustive matches against unsealed traits
      )
    case _             => List.empty
  }

def scalaVersionDeps(version: String): List[ModuleID] = {
  CrossVersion.partialVersion(version) match {
    case Some((2, 11)) => List.empty
    case Some((2, 12)) => List.empty
    case _             => List.empty
  }
}

// Cats required to support Right-biased Either in scala 2.11
def coreVersionDeps(version: String): List[ModuleID] =
  CrossVersion.partialVersion(version) match {
    case Some((2, 11)) =>
      List("org.typelevel" %% "cats-core"       % "1.1.0")
    case Some((2, 12)) => List.empty
    case _             => List.empty
  }
