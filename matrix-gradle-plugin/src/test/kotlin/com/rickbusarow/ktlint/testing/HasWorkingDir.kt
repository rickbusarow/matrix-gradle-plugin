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

package com.rickbusarow.ktlint.testing

import com.rickbusarow.ktlint.internal.div
import com.rickbusarow.ktlint.internal.segments
import java.io.File

/**
 * @param workingDir the directory containing all source and generated files to be used in this test
 */
@Suppress("UnnecessaryAbstractClass")
abstract class HasWorkingDir(workingDir: File) {

  /** the directory containing all source and generated files to be used in this test */
  val workingDir: File by lazy {
    checkInWorkingDir(workingDir)
    workingDir
  }

  override fun toString(): String {
    return "workingDir=$workingDir\n" + getSourceReport()
  }

  protected fun getSourceReport(): String {

    val grouped = workingDir
      .walkBottomUp()
      .filter { it.isFile }
      .groupBy { generated ->
        generated.relativeTo(workingDir).segments().first()
      }
      .toList()
      .sortedBy { it.first }

    return buildString {
      appendLine("====")
      grouped.forEach { (type, files) ->
        appendLine("## $type")
        files.forEach { path ->
          appendLine("file://$path")
        }
      }
      appendLine("----")
    }
  }

  companion object {

    private val allWorkingDirs = mutableSetOf<File>()

    @PublishedApi
    internal fun checkInWorkingDir(workingDir: File) {
      synchronized(allWorkingDirs) {
        require(allWorkingDirs.add(workingDir)) {
          val annotation = "${SkipInStackTrace::class.simpleName}"
          """
          A working directory with this path has already been registered during this test run,
          meaning it would have multiple tests modifying it concurrently.

          This probably means you need to annotate a test factory function with `@$annotation`, like:

            @$annotation
            fun myTestFactory(
              @Language("proto")
              vararg content: String,
              /* ... /*
            ) = ...

          This is the working directory which would be duplicated: $workingDir
          """.trimIndent()
        }
      }
    }

    /**
     * Determines an appropriate working directory based upon the current class under
     * test, the languages being generated by Wire, and the current test function.
     *
     * @param testStackFrame the StackFrame which captures the actual test function, so
     *   that we can get the test name. This must be grabbed as soon as possible, since
     *   default functions, inline functions, sequences, and iterators all redirect
     *   things and have a chance of hiding the original calling function completely.
     * @param testVariantName additional subdirectories underneath the test
     *   function's name, such as the names of the languages being generated
     * @return a File directory corresponding to the root of the working directory for this test
     */
    @SkipInStackTrace
    fun createWorkingDir(
      testStackFrame: StackTraceElement,
      vararg testVariantName: String
    ): File {

      val actualClass = testStackFrame.declaringClass()
        // trim off all the stuff like "$$inlined$$execute$1""
        .firstNonSyntheticClass()

      val declaringPackage = actualClass.canonicalName.split('.')
        .takeWhile { it.first().isLowerCase() }
        .joinToString(".")

      // nested classes and functions have the java `$` delimiter
      // ex: "com.example.MyTest$nested class$my test"
      val segments: String.() -> List<String> = {
        split(".", "$")
          .filter { it.isNotBlank() }
      }

      val allSegments = actualClass.name.segments()

      val packageLength = declaringPackage.count { it == '.' }

      val classSimpleNames = actualClass.canonicalName.segments().drop(packageLength)

      val testFunctionName = allSegments
        .drop(packageLength + classSimpleNames.size)
        .firstOrNull()
        ?.cleanForDir()
        ?: testStackFrame.methodName.cleanForDir()

      val testClassName = classSimpleNames
        // "MyTest/nested class"
        .joinToString(File.separator)
        // "MyTest/nested_class"
        .replace("[^a-zA-Z\\d/]".toRegex(), "_")

      val classDir = File("build") / "tests" / testClassName

      val working = classDir / testFunctionName / testVariantName.joinToString(File.separator)

      return working.absoluteFile
    }

    @PublishedApi
    internal fun String.cleanForDir(): String = replace("[^a-zA-Z\\d]".toRegex(), "_")
      .replace("_{2,}".toRegex(), "_")
      .removeSuffix("_")

    /**
     * Finds the stack trace element corresponding to the invoking test
     * function. This should be called as close as possible to the test function.
     */
    @SkipInStackTrace
    fun testStackTraceElement(): StackTraceElement {

      return Thread.currentThread()
        .stackTrace
        // skip the first since it's this function and not the calling test
        .drop(1)
        .first { !it.isSkipped() }
    }
  }
}
