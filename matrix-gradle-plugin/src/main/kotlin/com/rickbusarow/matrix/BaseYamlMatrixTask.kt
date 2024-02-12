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

import com.rickbusarow.matrix.internal.Color.Companion.colorized
import com.rickbusarow.matrix.internal.Color.LIGHT_YELLOW
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.internal.logging.text.StyledTextOutput
import org.gradle.internal.logging.text.StyledTextOutputFactory
import org.intellij.lang.annotations.Language
import java.io.File
import javax.inject.Inject

public abstract class BaseYamlMatrixTask @Inject constructor(
  objectFactory: ObjectFactory
) : DefaultTask() {

  @get:InputFile
  @get:PathSensitive(PathSensitivity.RELATIVE)
  public val yamlFile: RegularFileProperty = objectFactory.fileProperty()

  @get:Input
  public abstract val matrix: Property<Matrix>

  @get:Internal
  protected val startTag: Provider<String>
    get() = matrix.map { "### <start-matrix-${it.name}>" }

  @get:Internal
  protected val endTag: Provider<String>
    get() = matrix.map { "### <end-matrix-${it.name}>" }

  @get:Internal
  protected val matrixSectionRegex: Regex by lazy(LazyThreadSafetyMode.NONE) {

    val startTagEscaped = Regex.escape(startTag.get())
    val endTagEscaped = Regex.escape(endTag.get())

    buildRegex {

      // Match and capture everything before the line with the start tag
      appendRegex("""((?:.*\R)*?)""")
      // Match and capture all whitespaces before the start tag
      appendRegex("""( *)""")
      // Match and capture the line with the start tag
      appendRegex("""(.*$startTagEscaped.*\R)""")
      // Match and capture everything before the end tag
      appendRegex("""([\s\S]+?)""")
      // Match and capture the line with the end tag
      appendRegex("""(.*$endTagEscaped)""")
    }
  }

  private fun getYamlSections(ciText: String): Sequence<MatchResult> = matrixSectionRegex
    .findAll(ciText)
    .also { matches ->

      require(matches.iterator().hasNext()) {
        val start = startTag.get()
        val end = endTag.get()
        val yaml = yamlFile.get()

        """
          |Couldn't find any `$start`/`$end` sections in the CI file.
          |  file://$yaml
          |
          |Surround the matrix section with the comments '$start' and `$end':
          |
          |    strategy:
          |      $start
          |      matrix:
          |      $end
        """.trimMargin()
          .colorized(LIGHT_YELLOW)
      }
    }

  protected fun createNewText(ciText: String): String {

    val matchResults = getYamlSections(ciText)
      // Don't loop over a sequence, or the first match will just become an infinite loop.
      .toList()

    if (matchResults.none()) {
      return ciText
    }

    return matchResults.joinToString(
      separator = "",
      postfix = ciText.substring(matchResults.last().range.last + 1)
    ) { match ->

      val (prefix, indent, startTag, _, closingLine) = match.destructured

      val newContent = createYaml(indent.length)

      "$prefix$indent$startTag$newContent$closingLine"
    }
  }

  private fun createYaml(indentSize: Int): String {

    return MatrixYamlGenerator().generate(
      matrix = matrix.get(),
      indentSize = indentSize
    )
  }

  protected fun createStyledOutput(): StyledTextOutput = services
    .get(StyledTextOutputFactory::class.java)
    .create("versions-matrix")

  protected fun requireCiFile(): File {
    val ciFile = yamlFile.get().asFile

    require(ciFile.exists()) {
      "Could not resolve file: file://$ciFile"
    }

    return ciFile
  }

  private fun buildRegex(builder: RegexBuilder.() -> Unit): Regex {
    return RegexBuilder(StringBuilder())
      .apply { builder() }
      .build()
  }

  private class RegexBuilder(private val stringBuilder: StringBuilder) {

    fun appendRegex(@Language("regexp") str: String): RegexBuilder = apply {
      stringBuilder.append(str)
    }

    fun build(): Regex = Regex(stringBuilder.toString())
  }
}
