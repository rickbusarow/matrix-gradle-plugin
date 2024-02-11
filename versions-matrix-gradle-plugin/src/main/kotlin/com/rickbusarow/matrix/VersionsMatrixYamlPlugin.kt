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

package com.rickbusarow.matrix

import com.rickbusarow.kgx.checkProjectIsRoot
import com.rickbusarow.kgx.dependsOn
import com.rickbusarow.matrix.internal.capitalize
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.language.base.plugins.LifecycleBasePlugin

public abstract class VersionsMatrixYamlPlugin : Plugin<Project> {
  override fun apply(target: Project) {

    target.checkProjectIsRoot()
    target.plugins.apply("base")

    val ciFile = target.file(".github/workflows/ci.yml")

    require(ciFile.exists()) {
      "Could not resolve '$ciFile'.  Only add the ci/yaml matrix tasks to the root project."
    }

    val extension = target.extensions.create("matrices", MatricesExtension::class.java)

    extension.matrices.configureEach { matrixExtension ->

      val taskNameStart = "matrixYaml${matrixExtension.name.capitalize()}"

      val versionsMatrixYamlUpdate = target.tasks.register(
        "${taskNameStart}Update",
        VersionsMatrixYamlGenerateTask::class.java
      ) { task ->
        task.yamlFile.set(ciFile)
        task.matrix.set(matrixExtension.matrix())
      }

      val versionsMatrixYamlCheck = target.tasks.register(
        "${taskNameStart}Check",
        VersionsMatrixYamlCheckTask::class.java
      ) { task ->
        task.yamlFile.set(ciFile)
        task.matrix.set(matrixExtension.matrix())
        task.mustRunAfter(versionsMatrixYamlUpdate)
      }

      // Automatically run `versionsMatrixYamlCheck` when running `check`
      target.tasks.named(LifecycleBasePlugin.CHECK_TASK_NAME).dependsOn(versionsMatrixYamlCheck)
    }
  }
}
