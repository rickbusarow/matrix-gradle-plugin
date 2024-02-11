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

import jdk.javadoc.internal.Versions
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.internal.logging.text.StyledTextOutput
import org.gradle.internal.logging.text.StyledTextOutputFactory
import java.io.File
import javax.inject.Inject

public abstract class BaseYamlMatrixTask @Inject constructor(
  objectFactory: ObjectFactory
) : DefaultTask() {

  @get:InputFile
  @get:PathSensitive(PathSensitivity.RELATIVE)
  public val yamlFile: RegularFileProperty = objectFactory.fileProperty()

  @get:Input
  public abstract val startTag: Property<String>

  @get:Input
  public abstract val endTag: Property<String>

  @get:Internal
  protected val matrixSectionRegex: Regex by lazy(LazyThreadSafetyMode.NONE) {

    val startTagEscaped = Regex.escape(startTag.get())
    val endTagEscaped = Regex.escape(endTag.get())

    Regex("""( *)(.*$startTagEscaped.*\n)([\s\S]+?)(.*$endTagEscaped)""")
  }

  protected fun getYamlSections(ciText: String): Sequence<MatchResult> = matrixSectionRegex
    .findAll(ciText)
    .also { matches ->

      if (!matches.iterator().hasNext()) {
        val message =
          "Couldn't find any `$startTag`/`$endTag` sections in the CI file:" +
            "\tfile://${yamlFile.get()}\n\n" +
            "\tSurround the matrix section with the comments '$startTag' and `$endTag':\n\n" +
            "\t    strategy:\n" +
            "\t      ### $startTag\n" +
            "\t      matrix:\n" +
            "\t        [ ... ]\n" +
            "\t      ### $endTag\n"

        createStyledOutput()
          .withStyle(StyledTextOutput.Style.Description)
          .println(message)

        require(false)
      }
    }

  protected fun createNewText(ciText: String): String {

    return getYamlSections(ciText)
      .joinToString("") { match ->

        val (indent, startTag, _, closingLine) = match.destructured

        val newContent = createYaml(indent.length)

        "$indent$startTag$newContent$closingLine"
      }
  }

  private fun createYaml(indentSize: Int): String {
    val versionsMatrix = VersionsMatrix(
      gradleList = Versions.gradleListDefault,
      agpList = Versions.agpListDefault,
      anvilList = Versions.anvilListDefault,
      kotlinList = Versions.kotlinListDefault
    )

    return VersionsMatrixYamlGenerator()
      .generate(
        matrix = versionsMatrix,
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
}
