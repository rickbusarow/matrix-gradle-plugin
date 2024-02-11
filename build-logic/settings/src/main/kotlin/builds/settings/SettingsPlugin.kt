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

package builds.settings

import org.gradle.api.Plugin
import org.gradle.api.initialization.Settings
import org.gradle.api.internal.file.FileOperations
import org.gradle.api.invocation.Gradle
import java.io.File
import javax.inject.Inject

abstract class SettingsPlugin @Inject constructor(
  private val fileOperations: FileOperations,
) : Plugin<Settings> {

  override fun apply(target: Settings) {

    val maybeFile = target.rootDir.resolveInParents("gradle/libs.versions.toml")

    target.dependencyResolutionManagement.versionCatalogs { container ->

      val catalogBuilder = container.maybeCreate("libs")


      if (maybeFile != target.rootDir.resolve("gradle/libs.versions.toml")) {
        catalogBuilder.from(fileOperations.immutableFiles(maybeFile))
      }
    }

    @Suppress("UnstableApiUsage")
    target.dependencyResolutionManagement.repositories { repos ->
      repos.mavenCentral()
      repos.gradlePluginPortal()
      repos.google()
    }
  }

  private fun Gradle.parents(): Sequence<Gradle> = generateSequence(this) { it.parent }
    .drop(1)

  private fun File.resolveInParents(relativePath: String): File {
    return resolve(relativePath).takeIf { it.exists() }
      ?: parentFile?.resolveInParents(relativePath)
      ?: error("File $relativePath not found")
  }
}
