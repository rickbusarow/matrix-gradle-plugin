/*
 * Copyright (C) 2025 Rick Busarow
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

import com.github.gmazzo.buildconfig.BuildConfigExtension
import com.rickbusarow.kgx.checkProjectIsRoot
import com.rickbusarow.kgx.dependsOn
import com.rickbusarow.kgx.newInstance
import com.rickbusarow.matrix.internal.capitalize
import org.gradle.api.NamedDomainObjectProvider
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.language.base.plugins.LifecycleBasePlugin
import java.io.Serializable
import javax.inject.Inject

/** */
public abstract class MatrixPlugin : Plugin<Project> {

  override fun apply(target: Project) {

    target.checkProjectIsRoot()
    target.plugins.apply("base")

    val ciFile = target.file(".github/workflows/ci.yml")

    require(ciFile.exists()) {
      "Could not resolve '$ciFile'.  Only add the ci/yaml matrix tasks to the root project."
    }

    val buildConfigExtension = target.extensions
      .getByType(BuildConfigExtension::class.java)

    target.extensions.create(
      "matrices",
      MatricesExtension::class.java,
      MatrixTaskFactory(target),
      target.objects.newInstance<BuildConfigConfigurator>(buildConfigExtension)
    )
  }
}

/** */
public class MatrixTaskFactory @Inject constructor(
  private val target: Project
) : Serializable {

  internal fun create(matrixExtensionProvider: NamedDomainObjectProvider<MatrixExtension>) {
    val matrixName = matrixExtensionProvider.name

    val taskNameStart = "matrixYaml${matrixName.capitalize()}"

    val ciFile = target.file(".github/workflows/ci.yml")

    val matrixProvider = matrixExtensionProvider.map { it.matrix() }

    val versionsMatrixYamlUpdate = target.tasks.register(
      "${taskNameStart}Update",
      MatrixYamlGenerateTask::class.java
    ) { task ->
      task.group = "Matrix"

      task.yamlFile.set(ciFile)
      task.matrix.set(matrixProvider)
    }

    val versionsMatrixYamlCheck = target.tasks.register(
      "${taskNameStart}Check",
      MatrixYamlCheckTask::class.java
    ) { task ->
      task.group = "Matrix"

      task.yamlFile.set(ciFile)
      task.matrix.set(matrixProvider)
      task.mustRunAfter(versionsMatrixYamlUpdate)
    }

    // Automatically run `matrixYamlCheck` when running `check`
    target.tasks.named(LifecycleBasePlugin.CHECK_TASK_NAME).dependsOn(versionsMatrixYamlCheck)
  }
}
