lazy val appName = "api-platform-delete-api-key-lambda"
lazy val appDependencies: Seq[ModuleID] = compileDependencies ++ testDependencies

lazy val compileDependencies = Seq(
  "uk.gov.hmrc" %% "api-platform-manage-api" % "0.49.0"
)

lazy val testDependencies = Seq(
  "org.scalatest"     %% "scalatest"      % "3.2.18",
  "org.mockito"       %  "mockito-core"   % "5.18.0",
  "org.scalatestplus" %% "mockito-5-18"   % "3.2.19.0"
).map(_ % Test)

lazy val plugins: Seq[Plugins] = Seq()

lazy val lambda = (project in file("."))
  .enablePlugins(plugins: _*)
  .settings(
    name := appName,
    scalaVersion := "2.13.16",
    libraryDependencies ++= appDependencies,
    Test / parallelExecution := false,
    Test / fork := false,
  )
  .settings(
    resolvers += "hmrc-releases" at "https://artefacts.tax.service.gov.uk/artifactory/hmrc-releases/",
    resolvers += Resolver.jcenterRepo
  )
  .settings(
   assembly / assemblyOutputPath := file(s"./$appName.zip"),
    assembly / assemblyMergeStrategy := {
      case PathList("module-info.class") => MergeStrategy.first
      case PathList("META-INF", "versions", "9", "module-info.class") => MergeStrategy.last
      case PathList("META-INF", xs @ _*) => MergeStrategy.first
      case path if path.endsWith("io.netty.versions.properties") => MergeStrategy.discard
      case path if path.endsWith("BuildInfo$.class") => MergeStrategy.discard
      case path =>
        val oldStrategy = (assembly / assemblyMergeStrategy).value
        oldStrategy(path)
    }
  )

// Coverage configuration
coverageMinimumStmtTotal := 80
coverageFailOnMinimum := true
coverageExcludedPackages := "<empty>"
