resolvers += Resolver.url(
  "typelevel-sbt-plugins",
  url("http://dl.bintray.com/content/typelevel/sbt-plugins"))(
  Resolver.ivyStylePatterns)

addSbtPlugin("com.eed3si9n" % "sbt-unidoc" % "0.3.3")
addSbtPlugin("com.github.gseitz" % "sbt-release" % "1.0.3")
addSbtPlugin("com.jsuereth" % "sbt-pgp" % "1.0.0")
addSbtPlugin("org.xerial.sbt" % "sbt-sonatype" % "1.1")
addSbtPlugin("com.typesafe" % "sbt-mima-plugin" % "0.1.12")
addSbtPlugin("com.typesafe.sbt" % "sbt-ghpages" % "0.5.3")
addSbtPlugin("pl.project13.scala" % "sbt-jmh" % "0.2.20")
addSbtPlugin("org.scalastyle" % "scalastyle-sbt-plugin" % "0.8.0")
addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "0.14.3")
addSbtPlugin("org.scoverage" % "sbt-scoverage" % "1.5.0")
addSbtPlugin("com.fortysevendeg" % "sbt-microsites" % "0.4.0")
addSbtPlugin("com.geirsson" % "sbt-scalafmt" % "0.5.1")
