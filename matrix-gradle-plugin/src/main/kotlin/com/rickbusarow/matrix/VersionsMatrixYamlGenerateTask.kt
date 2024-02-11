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

import com.rickbusarow.matrix.internal.diffString
import org.gradle.api.model.ObjectFactory
import org.gradle.api.tasks.TaskAction
import org.gradle.internal.logging.text.StyledTextOutput
import javax.inject.Inject

public abstract class VersionsMatrixYamlGenerateTask @Inject constructor(
  objectFactory: ObjectFactory
) : BaseYamlMatrixTask(objectFactory) {

  @TaskAction
  public fun execute() {
    val ciFile = requireCiFile()

    val existingText = ciFile.readText()

    val newText = createNewText(existingText)

    if (existingText != newText) {

      ciFile.writeText(newText)

      val message = "Updated the versions matrix in the CI file." +
        "\n\tfile://${yamlFile.get()}"

      createStyledOutput()
        .withStyle(StyledTextOutput.Style.Description)
        .println(message)

      println()
      println(diffString(existingText, newText))
      println()
    }
  }
}
