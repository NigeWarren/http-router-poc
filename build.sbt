name := "http-router-POC"

version := "0.0.1"

scalaVersion := "2.12.4"

libraryDependencies ++= Seq(
  "ch.qos.logback" % "logback-classic" % "1.2.3",
  "io.netty" % "netty-all" % "4.1.23.Final",

  "io.vertx" % "vertx-web" % "3.4.2",
  "io.vertx" % "vertx-core" % "3.4.2",
  "io.vertx" % "vertx-auth-common" % "3.4.2",

)


