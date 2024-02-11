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
import dev.drewhamilton.poko.Poko
import java.io.Serializable

@Poko
public class Matrix(
  public val name: String,
  public val paramGroups: List<NamedParamGroup>,
  public val exclusions: List<MatrixExclusion>
) : Serializable {

  internal fun cartesian(): List<ParamCombination> {
    return paramGroups.fold(listOf(emptySet<NamedParamValue>())) { acc, list ->

      acc.flatMap { existingList ->
        list.values().mapToSet { existingList + it }
      }
    }
      .map { ParamCombination(it) }
      .filterNot { set ->
        exclusions.any { it.excludes(set) }
      }
  }

  private fun NamedParamGroup.values(): List<NamedParamValue> {
    return paramValues.get().map { value ->
      NamedParamValue(
        paramNames = paramNames,
        value = value
      )
    }
  }
}
