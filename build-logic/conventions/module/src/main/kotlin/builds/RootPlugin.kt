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

package builds

import builds.artifacts.ArtifactsPlugin
import com.rickbusarow.kgx.checkProjectIsRoot
import com.rickbusarow.kgx.extras
import com.rickbusarow.kgx.isRealRootProject
import com.rickbusarow.kgx.resolveInParent
import org.gradle.api.Plugin
import org.gradle.api.Project
import java.util.Properties

/** Applied to the real project root and the root project of any included build except this one. */
abstract class RootPlugin : Plugin<Project> {
  override fun apply(target: Project) {

    target.checkProjectIsRoot()

    if (!target.isRealRootProject()) {
      target.rootDir.resolveInParent("gradle.properties")
        .inputStream()
        .use { Properties().apply { load(it) } }
        .forEach { key, value ->
          target.extras.set(key.toString(), value.toString())
        }
    }

    target.plugins.apply("com.autonomousapps.dependency-analysis")

    target.plugins.apply(ModulePlugin::class.java)

    target.plugins.apply(ArtifactsPlugin::class.java)
    target.plugins.apply(BenManesVersionsPlugin::class.java)
    target.plugins.apply(GitHubReleasePlugin::class.java)
    target.plugins.apply(KnitConventionPlugin::class.java)
    target.plugins.apply(SpotlessConventionPlugin::class.java)

    // Hack for ensuring that when 'publishToMavenLocal' is invoked from the root project,
    // all subprojects are published.  This is used in plugin tests.
    target.tasks.register("publishToMavenLocal", BuildLogicTask::class.java) {
      target.subprojects.forEach { sub ->
        it.dependsOn(sub.tasks.matching { it.name == "publishToMavenLocal" })
      }
    }
    target.tasks.register("publishToMavenLocalNoDokka", BuildLogicTask::class.java) {
      target.subprojects.forEach { sub ->
        it.dependsOn(sub.tasks.matching { it.name == "publishToMavenLocalNoDokka" })
      }
    }

    if (target.gradle.includedBuilds.isNotEmpty()) {
      target.plugins.apply("composite")
    }

    if (inCI() && target.isRealRootProject()) {
      target.logger.lifecycle("CI environment detected.")
    }
  }
}
