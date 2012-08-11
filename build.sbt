sbtPlugin := true

name := "sbt-jelastic-deploy"

organization := "com.github.casualjim"

version := "0.1.0"

libraryDependencies ++= Seq(
  "net.databinder.dispatch" %% "core" % "0.9.0",
  "com.fasterxml.jackson.core" % "jackson-databind" % "2.0.5",
  "com.fasterxml.jackson.module" % "jackson-module-scala" % "2.0.2",
  "io.backchat.inflector" %% "scala-inflector" % "1.3.4",
  "org.specs2" %% "specs2" % "1.12" % "test"
)

libraryDependencies <+= sbtVersion(v => v match {
  case "0.11.0" => "com.github.siasia" %% "xsbt-web-plugin" % "0.11.0-0.2.8"
  case "0.11.1" => "com.github.siasia" %% "xsbt-web-plugin" % "0.11.1-0.2.10"
  case "0.11.2" => "com.github.siasia" %% "xsbt-web-plugin" % "0.11.2-0.2.11"
  case "0.11.3" => "com.github.siasia" %% "xsbt-web-plugin" % "0.11.3-0.2.11.1"
  case x if (x.startsWith("0.12")) => "com.github.siasia" %% "xsbt-web-plugin" % "0.12.0-0.2.11.1"
})

publishMavenStyle := false

publishTo <<= (version) { version: String =>
   val scalasbt = "http://scalasbt.artifactoryonline.com/scalasbt/"
   val (name, url) = if (version.contains("-SNAPSHOT"))
                       ("sbt-plugin-snapshots", scalasbt+"sbt-plugin-snapshots")
                     else
                       ("sbt-plugin-releases", scalasbt+"sbt-plugin-releases")
   Some(Resolver.url(name, new URL(url))(Resolver.ivyStylePatterns))
}

publishArtifact in Test := false

pomIncludeRepository := { x => false }

packageOptions <<= (packageOptions, name, version, organization) map {
  (opts, title, version, vendor) =>
     opts :+ Package.ManifestAttributes(
      "Created-By" -> "Simple Build Tool",
      "Built-By" -> System.getProperty("user.name"),
      "Build-Jdk" -> System.getProperty("java.version"),
      "Specification-Title" -> title,
      "Specification-Vendor" -> "Scalatra",
      "Specification-Version" -> version,
      "Implementation-Title" -> title,
      "Implementation-Version" -> version,
      "Implementation-Vendor-Id" -> vendor,
      "Implementation-Vendor" -> "Scalatra",
      "Implementation-Url" -> "https://github.com/scalatra/sbt-jelastic-deploy"
     )
}

homepage := Some(url("https://github.com/scalatra/sbt-jelastic-deploy"))

startYear := Some(2012)

licenses := Seq(("MIT", url("http://raw.github.com/casualjim/sbt-jelastic-deploy/master/LICENSE")))

javacOptions := Seq("-Xlint:deprecation")

scalacOptions := Seq("-optimise", "-deprecation")