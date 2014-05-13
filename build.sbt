name := "InteractiveEmailClient"

version := "0.1"

scalaVersion := "2.10.3"

resolvers += "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/"

// needs to be in sync with whatever Sarah2 uses
libraryDependencies ++= Seq("com.typesafe.akka" %% "akka-actor" % "2.3.2")


