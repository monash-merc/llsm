resolvers += Resolver.url(
  "typelevel-sbt-plugins",
   url("http://dl.bintray.com/content/typelevel/sbt-plugins"))(
      Resolver.ivyStylePatterns)

addSbtPlugin("org.typelevel"      %   "sbt-catalysts"         % "0.1.12")
addSbtPlugin("pl.project13.scala" %   "sbt-jmh"               % "0.2.12")
addSbtPlugin("org.scalastyle"     %%  "scalastyle-sbt-plugin" % "0.8.0")
