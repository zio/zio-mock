import BuildHelper._
import MimaSettings.mimaSettings

inThisBuild(
  List(
    organization  := "dev.zio",
    homepage      := Some(url("https://zio.dev/zio-mock/")),
    licenses      := List("Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0")),
    developers    := List(
      Developer(
        "jdegoes",
        "John De Goes",
        "john@degoes.net",
        url("http://degoes.net")
      )
    ),
    pgpPassphrase := sys.env.get("PGP_PASSWORD").map(_.toArray),
    pgpPublicRing := file("/tmp/public.asc"),
    pgpSecretRing := file("/tmp/secret.asc")
  )
)

addCommandAlias("fmt", "all scalafmtSbt scalafmt test:scalafmt")
addCommandAlias("fix", "; mockJVM/compile:scalafix; mockJVM/test:scalafix; mockJVM/scalafmtSbt; mockJVM/scalafmtAll")
addCommandAlias(
  "check",
  "; scalafmtSbtCheck; mockJVM/scalafmtCheckAll; mockJVM/compile:scalafix --check; mockJVM/test:scalafix --check"
)

addCommandAlias(
  "testJVM",
  ";mockTestsJVM/test"
)
addCommandAlias(
  "testJS",
  ";mockTestsJS/test"
)
addCommandAlias(
  "testNative",
  ";mockNative/compile"
)

val zioVersion = "2.0.12"

lazy val root = (project in file("."))
  .aggregate(
    mockJVM,
    mockJS,
    mockNative,
    mockTestsJVM,
    mockTestsJS,
    examplesJVM,
    docs
  )
  .settings(
    crossScalaVersions := Nil,
    publish / skip     := true
  )

lazy val mock = crossProject(JSPlatform, JVMPlatform, NativePlatform)
  .in(file("mock"))
  .settings(
    resolvers +=
      "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots",
    libraryDependencies ++= Seq(
      "dev.zio" %%% "zio"         % zioVersion,
      "dev.zio" %%% "zio-streams" % zioVersion,
      "dev.zio" %%% "zio-test"    % zioVersion
    )
  )
  .settings(stdSettings("zio-mock"))
  .settings(crossProjectSettings)
  .settings(macroDefinitionSettings)
  .settings(macroExpansionSettings)
  .settings(buildInfoSettings("zio.mock"))
  .settings(
    scalacOptions ++= {
      if (scalaVersion.value == Scala3)
        Seq.empty
      else
        Seq("-P:silencer:globalFilters=[zio.stacktracer.TracingImplicits.disableAutoTrace]")
    }
  )
  .enablePlugins(BuildInfoPlugin)

lazy val mockJVM = mock.jvm
  .settings(dottySettings)
  // No bincompat on zio-test yet
  .settings(mimaSettings(failOnProblem = false))

lazy val mockJS = mock.js
  .settings(jsSettings)
  .settings(dottySettings)

lazy val mockNative = mock.native
  .settings(nativeSettings)
  .settings(libraryDependencies += "org.ekrich" %%% "sjavatime" % "1.1.9")

lazy val mockTests = crossProject(JSPlatform, JVMPlatform)
  .in(file("mock-tests"))
  .dependsOn(mock)
  .settings(
    resolvers +=
      "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots",
    libraryDependencies ++= Seq(
      "dev.zio" %%% "zio-test"     % zioVersion % Test,
      "dev.zio" %%% "zio-test-sbt" % zioVersion % Test
    )
  )
  .settings(stdSettings("zio-mock-tests"))
  .settings(crossProjectSettings)
  .settings(semanticdbEnabled := false) // NOTE: disabled because it failed on MockableSpec.scala
  .settings(testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework"))
  .settings(publish / skip := true)
  .settings(macroExpansionSettings)

lazy val mockTestsJVM = mockTests.jvm
  .settings(dottySettings)

lazy val mockTestsJS = mockTests.js
  .settings(jsSettings)
  .settings(dottySettings)

lazy val examples = crossProject(JVMPlatform)
  .in(file("examples"))
  .dependsOn(mock)
  .settings(
    libraryDependencies ++= Seq(
      "dev.zio" %% "zio-test-junit" % zioVersion
    )
  )
  .settings(stdSettings("zio-mock-tests"))
  .settings(crossProjectSettings)
  .settings(macroExpansionSettings)
  .settings(publish / skip := true)

lazy val examplesJVM = examples.jvm.settings(dottySettings)

lazy val docs = project
  .in(file("zio-mock-docs"))
  .settings(stdSettings("zio-mock"))
  .settings(macroDefinitionSettings)
  .settings(macroExpansionSettings)
  .settings(
    scalaVersion                               := Scala213,
    moduleName                                 := "zio-mock-docs",
    scalacOptions -= "-Yno-imports",
    scalacOptions -= "-Xfatal-warnings",
    projectName                                := "ZIO Mock",
    mainModuleName                             := (mockJVM / moduleName).value,
    projectStage                               := ProjectStage.Development,
    ScalaUnidoc / unidoc / unidocProjectFilter := inProjects(mockJVM),
    docsPublishBranch                          := "master"
  )
  .dependsOn(mockJVM)
  .enablePlugins(WebsitePlugin)
