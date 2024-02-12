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

import org.gradle.api.Project
import java.io.Serializable
import javax.inject.Inject

public open class VersionsMatrixExtension @Inject constructor(target: Project) : Serializable {

  // public val versions = Versions(target.objects, target.libs)

  // public fun Project.versionsMatrix(sourceSetName: String, packageName: String) {
  //   setUpGeneration(
  //     sourceSetName = sourceSetName,
  //     packageName = packageName,
  //     versions = versions
  //   )
  // }
}

// private fun Project.setUpGeneration(
//   sourceSetName: String,
//   packageName: String,
//   versions: Versions
// ) {
//
//   // val generatedDirPath = layout.buildDirectory.dir(
//   //   "generated/sources/versionsMatrix/kotlin/main"
//   // )
//   //
//   // requireInSyncWithToml(versions)
//   //
//   // plugins.apply(libsCatalog.pluginId("buildconfig"))
//   //
//   // extensions.configure(BuildConfigExtension::class.java) { extension ->
//   //   extension.sourceSets.named(sourceSetName) { sourceSet ->
//   //     sourceSet.forClass(packageName, "Versions") { clazz ->
//   //       clazz.buildConfigField("agpList", versions.agpList.get())
//   //       clazz.buildConfigField("daggerList", versions.daggerList.get())
//   //       clazz.buildConfigField("gradleList", versions.gradleList.get())
//   //       clazz.buildConfigField("kotlinList", versions.kotlinList.get())
//   //     }
//   //   }
//   // }
//   //
//   // extensions.configure(KotlinJvmProjectExtension::class.java) { extension ->
//   //   extension.sourceSets.named("main") { kotlinSourceSet ->
//   //     kotlinSourceSet.kotlin.srcDir(generatedDirPath)
//   //   }
//   // }
// }
//
// private fun Project.requireInSyncWithToml(versions: Versions) {
//
//   // val simpleName = Versions::class.simpleName
//   // val versionsMatrixRelativePath = Versions::class.qualifiedName!!
//   //   .replace('.', File.separatorChar)
//   //   .let { "$it.kt" }
//
//   // val versionMatrixFile = rootDir
//   //   .resolve("build-logic/conventions")
//   //   .resolve("src/main/kotlin")
//   //   .resolve(versionsMatrixRelativePath)
//   //
//   // require(versionMatrixFile.exists()) {
//   //   "Could not resolve the $simpleName file: $versionMatrixFile"
//   // }
//
//   // VersionsMatrix(
//   //   gradleList = versions.gradleList.get(),
//   //   agpList = versions.agpList.get(),
//   //   daggerList = versions.daggerList.get(),
//   //   kotlinList = versions.kotlinList.get(),
//   //   exclusions = versions.exclusions.get()
//   // ).run {
//   //
//   //   sequenceOf(
//   //     Triple(agpList, "agpList", "androidTools"),
//   //     Triple(daggerList, "daggerList", "dagger"),
//   //     Triple(kotlinList, "kotlinList", "kotlin")
//   //   )
//   //     .forEach { (list, listName, alias) ->
//   //       require(list.contains(libsCatalog.version(alias))) {
//   //         "The versions catalog version for '$alias' is ${libsCatalog.version(alias)}.  " +
//   //           "Update the $simpleName list '$listName' to include this new version.\n" +
//   //           "\tfile://$versionMatrixFile"
//   //       }
//   //     }
//   //
//   //   require(gradleList.contains(gradle.gradleVersion)) {
//   //     "The Gradle version is ${gradle.gradleVersion}.  " +
//   //       "Update the $simpleName list 'gradleList' to include this new version.\n" +
//   //       "\tfile://$versionMatrixFile"
//   //   }
//   // }
// }
