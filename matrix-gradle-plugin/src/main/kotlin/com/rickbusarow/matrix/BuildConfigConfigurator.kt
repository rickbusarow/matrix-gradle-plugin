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

import com.github.gmazzo.buildconfig.BuildConfigExtension
import com.github.gmazzo.buildconfig.BuildConfigSourceSet
import com.github.gmazzo.buildconfig.generators.BuildConfigGenerator
import com.github.gmazzo.buildconfig.generators.BuildConfigGeneratorSpec
import com.github.gmazzo.buildconfig.generators.BuildConfigKotlinGenerator
import org.gradle.api.NamedDomainObjectProvider
import org.gradle.kotlin.dsl.buildConfigField
import javax.inject.Inject

public class BuildConfigConfigurator @Inject constructor(
  private val buildConfigExtension: BuildConfigExtension
) {

  public fun buildConfig(
    name: String,
    matrixExtension: NamedDomainObjectProvider<MatrixExtension>
  ): NamedDomainObjectProvider<out BuildConfigSourceSet> {

    return buildConfigExtension.sourceSets.named(name) { sourceSet ->

      sourceSet.generator(object : BuildConfigGenerator {
        override fun execute(spec: BuildConfigGeneratorSpec) {

          BuildConfigKotlinGenerator().execute(spec)

          // FileSpec.builder(spec.packageName, "MatrixExclusion")
          //   .addType(
          //     matrixExtension.get().exclusions().generateTypeSpec()
          //   )
        }
      })

      sourceSet.forClass("com.rickbusarow.matrix", "Matrix") { clazz ->

        val matrix = matrixExtension.get().matrix()

        val cartesian = matrix.cartesian().map { combination ->
          // combination.list.map { "\"${ it.paramNames.name}\"" to "\"${it.value}\"" }
          combination.list
            .map { it.value }
          // .joinToString(", ", "listOf(", ")") {
          //   "\"${it.value}\""
          // }
        }

        clazz.buildConfigField("combinations", cartesian)

        matrix.paramGroups.forEach { group ->
          clazz.buildConfigField(
            name = group.buildConfigName.getAndFinalize(),
            value = group.paramValues
          )
        }
      }
    }
  }
}
