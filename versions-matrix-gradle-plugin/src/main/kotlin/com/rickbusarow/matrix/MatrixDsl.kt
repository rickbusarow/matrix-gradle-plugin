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

import com.rickbusarow.kgx.property
import com.rickbusarow.matrix.internal.mapToSet
import dev.drewhamilton.poko.Poko
import org.gradle.api.Action
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.NamedDomainObjectProvider
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.provider.ProviderFactory
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import java.io.Serializable
import java.util.concurrent.Callable
import javax.inject.Inject
import kotlin.LazyThreadSafetyMode.NONE

@DslMarker
public annotation class MatrixDsl

@MatrixDsl
public abstract class MatricesExtension @Inject constructor(
  objects: ObjectFactory
) {

  public val matrices: NamedDomainObjectContainer<MatrixExtension> = objects
    .domainObjectContainer(MatrixExtension::class.java)

  public fun matrix(
    name: String,
    action: Action<MatrixExtension>
  ): NamedDomainObjectProvider<MatrixExtension> = matrices.register(name, action)
}

@MatrixDsl
public abstract class MatrixExtension @Inject constructor(
  @get:Input
  public val name: String,
  private val providerFactory: ProviderFactory
) : Serializable {

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

  public fun exclude(listNameToValue: Collection<Pair<NamedParamGroup, String>>) {

    exclusions.add(
      providerFactory.provider {
        MatrixExclusion(listNameToValue.mapToSet { (group, value) ->
          NamedParamValue(paramNames = group.paramNames, value = value)
        })
      }
    )
  }

  private fun List<ParamCombination>.requireNotEmpty(): List<ParamCombination> {
    return apply {
      require(isNotEmpty()) {
        "There are no valid version combinations to be made from the provided arguments."
      }
    }
  }

  private fun List<MatrixExclusion>.requireNoDuplicates(): List<MatrixExclusion> {
    return also { exclusions ->
      require(exclusions.toSet().size == exclusions.size) {
        val duplicates = exclusions.filter { target ->
          exclusions.filter { it == target }.size > 1
        }
          .distinct()

        "There are duplicate (identical) exclusions (this list shows one of each type):\n" +
          duplicates.joinToString("\n\t")
      }
    }
  }
}

@Poko
public class ParamCombination(public val list: Set<NamedParamValue>) : Serializable {
  init {
    require(list.distinctBy { it.paramNames }.size == list.size) {

      """
      |########################################
      |${list.joinToString("\n")}
      |########################################
      """.trimMargin()
    }
  }
}

@Poko
public class ParamNames constructor(
  public val name: String,
  public val yamlName: String = name,
  public val buildConfigName: String = name,
  public val catalogAliasName: String = name
) : Serializable {
  override fun toString(): String =
    "ParamId(name: $name | yamlName: $yamlName | buildConfigName: $buildConfigName | catalogAliasName: $catalogAliasName)"
}

public interface HasParamNames {
  public val paramNames: ParamNames
}

@Poko
public class NamedParamValue(
  public override val paramNames: ParamNames,
  public val value: String
) : Serializable, HasParamNames {
  override fun toString(): String = "$paramNames: $value"
}

public abstract class NamedParamGroup @Inject constructor(
  @get:Input public val name: String,
  objects: ObjectFactory
) : Serializable, HasParamNames {

  @get:Input
  public val yamlName: Property<String> = objects.property<String>(name)

  @get:Input
  public val buildConfigName: Property<String> = objects.property<String>(name)

  @get:Input
  public val catalogAliasName: Property<String> = objects.property<String>(name)

  @get:Internal
  override val paramNames: ParamNames by lazy(NONE) {
    ParamNames(
      name = name,
      yamlName = yamlName.getAndFinalize(),
      buildConfigName = buildConfigName.getAndFinalize(),
      catalogAliasName = catalogAliasName.getAndFinalize()
    )
  }

  @get:Input
  public abstract val paramValues: ListProperty<String>

  @get:Input
  public val defaultValue: Property<String> =
    objects.property<String>(paramValues.map { it.last() })
}

internal fun <T> Property<T>.getAndFinalize(): T {
  finalizeValue()
  return get()
}

@Poko
public class MatrixExclusion(
  @get:Input public val values: Set<NamedParamValue>
) : Serializable {
  internal fun excludes(set: ParamCombination): Boolean {
    return values.all { set.list.contains(it) }
  }
}
