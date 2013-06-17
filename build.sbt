import sbtrelease._

/** Project */
name := "Achilles"

organization := "org.cakesolutions"

scalaVersion := "2.10.1"

/** Shell */
shellPrompt := { state => System.getProperty("user.name") + "> " }

shellPrompt in ThisBuild := { state => Project.extract(state).currentRef.project + "> " }

/** Dependencies */
resolvers += "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/"

resolvers += "Sonatype OSS Releases" at "http://oss.sonatype.org/content/repositories/releases/"

publishTo <<= version { v: String =>
  val nexus = "https://oss.sonatype.org/"
  if (v.trim.endsWith("SNAPSHOT")) Some("snapshots" at nexus + "content/repositories/snapshots")
  else                             Some("releases" at nexus + "service/local/staging/deploy/maven2")
}

publishMavenStyle := true

publishArtifact in Test := false

pomIncludeRepository := { x => false }

pomExtra := (
  <url>http://www.cakesolutions.net</url>
  <licenses>
    <license>
      <name>BSD-style</name>
      <url>http://www.opensource.org/licenses/bsd-license.php</url>
      <distribution>repo</distribution>
    </license>
  </licenses>
  <scm>
    <url>git@github.com:adinapoli/achilles.git</url>
    <connection>scm:git:git@github.com:adinapoli/achilles.git</connection>
  </scm>
  <developers>
    <developer>
      <id>adinapoli</id>
      <name>Alfredo Di Napoli</name>
      <url>http://www.alfredodinapoli.com</url>
    </developer>
  </developers>
)

libraryDependencies <<= scalaVersion { scala_version => 
    val scalazVersion = "7.0.0-M8"
    Seq(
        "org.specs2"             %% "specs2"             % "1.13"   % "test",
        "org.scalacheck"         %% "scalacheck"         % "1.10.0" % "test",
        "com.datastax.cassandra"  % "cassandra-driver-core" % "1.0.0",
        "org.scalaz"             %% "scalaz-effect"         % scalazVersion,
        "org.scalaz"             %% "scalaz-core"           % scalazVersion
    )
}

/** Compilation */
javacOptions ++= Seq("-Xmx1812m", "-Xms512m", "-Xss6m")

javaOptions += "-Xmx2G"

scalacOptions ++= Seq("-deprecation", "-unchecked", "-feature")

maxErrors := 20 

pollInterval := 1000

logBuffered := false

cancelable := true

testOptions := Seq(Tests.Filter(s =>
  Seq("Spec", "Suite", "Test", "Unit", "all").exists(s.endsWith(_)) &&
    !s.endsWith("FeaturesSpec") ||
    s.contains("UserGuide") || 
    s.contains("index") ||
    s.matches("org.specs2.guide.*")))

/** Console */
initialCommands in console := "import org.cakesolutions.achilles._"
