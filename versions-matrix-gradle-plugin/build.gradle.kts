/*
 * Copyright (C) 2024 Rick Busarow
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import builds.VERSION_NAME
import com.github.gmazzo.buildconfig.BuildConfigTask
import com.rickbusarow.kgx.dependsOn
import com.rickbusarow.kgx.isRealRootProject

plugins {
  id("module")
  id("java-gradle-plugin")
  id("com.gradle.plugin-publish")
  `jvm-test-suite`
  alias(libs.plugins.buildconfig)
  alias(libs.plugins.poko)
}

val pluginId = "com.rickbusarow.versions-matrix"
val pluginArtifactId = "versions-matrix-gradle-plugin"
val moduleDescription =
  "Syncs a versions matrix between versions catalog, GitHub workflows, and BuildConfig."

version = VERSION_NAME

val pluginDeclaration: NamedDomainObjectProvider<PluginDeclaration> =
  gradlePlugin.plugins
    .register(pluginArtifactId) pd@{
      val declaration = this@pd
      declaration.id = pluginId
      declaration.displayName = "Test Matrix"
      declaration.implementationClass = "com.rickbusarow.matrix.TestMatrixPlugin"
      declaration.description = moduleDescription
      @Suppress("UnstableApiUsage")
      declaration.tags.set(listOf())
    }

module {

  published(
    artifactId = pluginArtifactId,
    pomDescription = moduleDescription
  )

  publishedPlugin(pluginDeclaration = pluginDeclaration)
}

val deps = mutableSetOf<String>()
val ktlintDeps = mutableSetOf<String>()

buildConfig {

  this@buildConfig.sourceSets.named("main") {

    packageName(builds.GROUP)
    className("BuildConfig")

    buildConfigField("pluginId", pluginId)
    buildConfigField("version", VERSION_NAME)
    buildConfigField("kotlinVersion", libs.versions.kotlin.get())
    buildConfigField(
      name = "deps",
      value = provider {
        if (deps.isEmpty()) {
          throw GradleException(
            "There are no dependencies to pass along to the Gradle Worker's classpath.  " +
              "Is there a race condition?"
          )
        }
        deps
      }
    )
  }
}

rootProject.tasks.named("prepareKotlinBuildScriptModel")
  .dependsOn(tasks.withType(BuildConfigTask::class.java))

@Suppress("UnstableApiUsage")
testing {
  suites {

    val gradleTest by registering(JvmTestSuite::class) {

      useJUnitJupiter()

      testType.set(TestSuiteType.INTEGRATION_TEST)

      dependencies {
        implementation(project())
      }

      targets {
        configureEach {

          testTask.configure {
            dependsOn("publishToMavenLocalNoDokka")
          }
        }
      }
    }

    tasks.check.dependsOn(gradleTest)
    // tasks.named("check") {
    //   dependsOn(testing.suites.named("gradleTest"))
    // }
  }
}

val gradleTestSourceSet by sourceSets.named("gradleTest", SourceSet::class)

gradlePlugin {
  @Suppress("UnstableApiUsage")
  testSourceSet(gradleTestSourceSet)
}

kotlin {
  val compilations = target.compilations
  compilations.named("gradleTest") {
    associateWith(compilations.getByName("main"))
  }
}

val mainConfig: Configuration = when {
  rootProject.isRealRootProject() -> configurations.compileOnly.get()
  else -> configurations.getByName("implementation")
}

fun DependencyHandlerScope.worker(dependencyNotation: Any) {
  mainConfig(dependencyNotation)

  val dependency = when (dependencyNotation) {
    is org.gradle.api.internal.provider.TransformBackedProvider<*, *> -> {
      dependencyNotation.get() as ExternalDependency
    }

    is ProviderConvertible<*> -> {
      dependencyNotation.asProvider().get() as ExternalDependency
    }

    else -> error("unsupported dependency type -- ${dependencyNotation::class.java.canonicalName}")
  }

  if (dependency.group == libs.ktlint.core.get().group) {
    ktlintDeps.add(dependency.module.toString())
  } else {
    deps.add(dependency.toString())
  }
}

dependencies {

  api(libs.java.diff.utils)

  compileOnly(gradleApi())

  compileOnly(libs.buildconfig)

  implementation(libs.rickBusarow.kgx)

  testImplementation(libs.jetbrains.markdown)
  testImplementation(libs.junit.jupiter)
  testImplementation(libs.junit.jupiter.api)
  testImplementation(libs.junit.jupiter.engine)
  testImplementation(libs.junit.jupiter.params)
  testImplementation(libs.kotest.assertions.api)
  testImplementation(libs.kotest.assertions.core.jvm)
  testImplementation(libs.kotest.assertions.shared)
  testImplementation(libs.kotest.common)
  testImplementation(libs.kotest.extensions)
  testImplementation(libs.kotest.property.jvm)
  testImplementation(libs.kotlin.gradle.plugin)
  testImplementation(libs.ktlint.ruleset.standard)
  testImplementation(libs.ktlint.test)

  worker(libs.ec4j.core)
  worker(libs.kotlin.gradle.plugin)
  worker(libs.kotlin.gradle.plugin.api)
  worker(libs.ktlint.cli.ruleset.core)
  worker(libs.ktlint.rule.engine)
  worker(libs.ktlint.rule.engine.core)
  worker(libs.ktlint.ruleset.standard)
}
