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

internal class MatrixYamlGenerator {

  fun generate(matrix: Matrix, indentSize: Int): String {
    var currentIndent = " ".repeat(indentSize)

    fun StringBuilder.indent(content: StringBuilder.() -> Unit) {
      currentIndent += "  "
      content()
      currentIndent = currentIndent.removeSuffix("  ")
    }

    fun StringBuilder.line(content: String) {
      appendLine(currentIndent + content)
    }

    return buildString {
      line("matrix:")

      indent {

        for (list in matrix.paramGroups) {
          line("${list.yamlName.get()}: ${list.paramValues.get().asYamlList()}")
        }

        if (matrix.exclusions.isEmpty()) {
          line("exclude: [ ]")
        } else {
          line("exclude:")

          indent {
            for (exclude in matrix.exclusions) {
              for (line in exclude.asYamlLines()) {
                line(line)
              }
            }
          }
        }
      }
    }
  }

  private fun List<String>.asYamlList() = joinToString(", ", "[ ", " ]") { it }

  private fun MatrixExclusion.asYamlLines(): List<String> {

    return values.mapIndexed { index, namedValue ->

      val label = namedValue.paramNames.yamlName
      val value = namedValue.value
      if (index == 0) {
        "- $label: $value"
      } else {
        "  $label: $value"
      }
    }
  }
}
