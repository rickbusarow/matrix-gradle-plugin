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

import com.rickbusarow.matrix.internal.mapToSet
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.NamedDomainObjectProvider
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Provider
import org.gradle.api.provider.ProviderFactory
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import java.io.Serial
import java.io.Serializable
import java.util.concurrent.Callable
import javax.inject.Inject

@MatrixDsl
public abstract class MatrixExtension @Inject constructor(
  @get:Input
  public val name: String,
  private val providerFactory: ProviderFactory
) : Serializable {

  @get:InputFiles
  public abstract val workflowFiles: ConfigurableFileCollection

  @get:Input
  public abstract val paramGroups: NamedDomainObjectContainer<NamedParamGroup>

  @get:Input
  public abstract val exclusions: ListProperty<MatrixExclusion>

  internal fun matrix(): Matrix = Matrix(
    name = name,
    paramGroups = paramGroups.toList(),
    exclusions = exclusions.get()
  )

  public fun paramGroup(
    name: String,
    versions: Collection<String>
  ): NamedDomainObjectProvider<NamedParamGroup> {
    return paramGroups.register(name) { it.paramValues.set(versions) }
  }

  public fun paramGroup(
    name: String,
    versions: Provider<Collection<String>>
  ): NamedDomainObjectProvider<NamedParamGroup> {
    return paramGroups.register(name) { it.paramValues.set(versions) }
  }

  public fun paramGroup(
    name: String,
    versions: Callable<Collection<String>>
  ): NamedDomainObjectProvider<NamedParamGroup> {
    return paramGroup(name, providerFactory.provider(versions))
  }

  public fun exclude(vararg nameToValue: Pair<NamedParamGroup, String>) {
    exclude(nameToValue.asList())
  }

  public fun exclude(nameToValue: Collection<Pair<NamedParamGroup, String>>) {

    exclusions.add(
      providerFactory.provider {
        MatrixExclusion(
          nameToValue.mapToSet { (group, value) ->
            NamedParamValue(paramNames = group.paramNames, value = value)
          }
        )
      }
    )
  }

  private companion object {
    @Serial private const val serialVersionUID: Long = 8579711066864604696L
  }
}
