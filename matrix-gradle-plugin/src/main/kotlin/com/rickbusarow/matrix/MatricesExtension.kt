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

import org.gradle.api.Action
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.NamedDomainObjectProvider
import org.gradle.api.model.ObjectFactory
import javax.inject.Inject

/** */
@MatrixDsl
@Suppress("IdentifierGrammar")
public abstract class MatricesExtension @Inject constructor(
  private val taskFactory: MatrixTaskFactory,
  private val buildConfigConfigurator: BuildConfigConfigurator,
  objects: ObjectFactory
) {

  public val matrices: NamedDomainObjectContainer<MatrixExtension> = objects
    .domainObjectContainer(MatrixExtension::class.java)

  public fun matrix(
    name: String,
    action: Action<MatrixExtension>
  ): NamedDomainObjectProvider<MatrixExtension> {

    val matrixExtension = matrices.register(name, action)
    taskFactory.create(matrixExtension)
    buildConfigConfigurator.buildConfig("main", matrixExtension)

    return matrixExtension
  }
}
