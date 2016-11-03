resolvers += Resolver.url(
  "typelevel-sbt-plugins",
   url("http://dl.bintray.com/content/typelevel/sbt-plugins"))(
      Resolver.ivyStylePatterns)

addSbtPlugin("org.typelevel"        %   "sbt-catalysts"               %   "0.1.12")
addSbtPlugin("com.eed3si9n"         %   "sbt-doge"                    %   "0.1.5")
addSbtPlugin("pl.project13.scala"   %   "sbt-jmh"                     %   "0.2.12")
addSbtPlugin("org.scalastyle"       %%  "scalastyle-sbt-plugin"       %   "0.8.0")
addSbtPlugin("com.eed3si9n"         %   "sbt-assembly"                %   "0.14.3")
// addSbtPlugin("net.virtual-void"     %   "sbt-dependency-graph"        %   "0.8.2")
addSbtPlugin("org.scoverage"        %   "sbt-scoverage"               %   "1.5.0-RC2")
