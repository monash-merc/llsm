resolvers += Resolver.url(
  "typelevel-sbt-plugins",
  url("http://dl.bintray.com/content/typelevel/sbt-plugins"))(
  Resolver.ivyStylePatterns)

addSbtPlugin("com.eed3si9n" % "sbt-unidoc" % "0.3.3")
addSbtPlugin("com.github.gseitz" % "sbt-release" % "1.0.5")
addSbtPlugin("com.jsuereth" % "sbt-pgp" % "1.0.0")
addSbtPlugin("org.xerial.sbt" % "sbt-sonatype" % "1.1")
addSbtPlugin("com.typesafe" % "sbt-mima-plugin" % "0.1.14")
addSbtPlugin("com.typesafe.sbt" % "sbt-ghpages" % "0.6.0")
addSbtPlugin("pl.project13.scala" % "sbt-jmh" % "0.2.24")
addSbtPlugin("org.scalastyle" % "scalastyle-sbt-plugin" % "0.9.0")
addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "0.14.5")
addSbtPlugin("org.scoverage" % "sbt-scoverage" % "1.5.0")
addSbtPlugin("com.47deg" % "sbt-microsites" % "0.6.1")
addSbtPlugin("com.lucidchart" % "sbt-scalafmt" % "1.8")
addSbtPlugin("com.typesafe.sbt" % "sbt-native-packager" % "1.2.0")
