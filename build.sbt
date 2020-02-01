name := "socializer-chat"

version := "0.1"

scalaVersion := "2.13.1"

val http4sVersion = "0.21.0-RC2"
val circeVersion = "0.13.0-RC1"
val logbackVersion = "1.2.3"
val zioVersion = "1.0.0-RC17"
val zioInteropCatsVersion = "2.0.0.0-RC10"

libraryDependencies ++= Seq(
  "org.http4s"      %% "http4s-blaze-server" % http4sVersion,
  "org.http4s"      %% "http4s-blaze-client" % http4sVersion,
  "org.http4s"      %% "http4s-circe"        % http4sVersion,
  "org.http4s"      %% "http4s-dsl"          % http4sVersion,
  "io.circe"        %% "circe-generic"       % circeVersion,
  "ch.qos.logback"  %  "logback-classic"     % logbackVersion,
  "dev.zio" %% "zio" % zioVersion,
  "dev.zio" %% "zio-interop-cats" % zioInteropCatsVersion
)

addCompilerPlugin("org.typelevel" %% "kind-projector"     % "0.10.3")
addCompilerPlugin("com.olegpy"    %% "better-monadic-for" % "0.3.0")

scalacOptions ++= Seq(
  "-deprecation",
  "-encoding", "UTF-8",
  "-language:higherKinds",
  "-language:postfixOps",
  "-feature"
)
