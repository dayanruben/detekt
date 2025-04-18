package io.github.detekt.test.utils

import com.intellij.openapi.Disposable
import com.intellij.openapi.util.Disposer
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import java.io.File

/**
 * Make sure to always call [dispose] or use a [use] block when working with [KotlinCoreEnvironment]s.
 */
class KotlinCoreEnvironmentWrapper(
    private var environment: KotlinCoreEnvironment?,
    private val disposable: Disposable,
) {

    val env: KotlinCoreEnvironment
        get() = checkNotNull(environment) { "Environment already disposed." }

    fun dispose() {
        Disposer.dispose(disposable)
        environment = null
    }
}

/**
 * Create a {@link KotlinCoreEnvironmentWrapper} used for test.
 *
 * @param additionalRootPaths the optional JVM classpath roots list.
 */
fun createEnvironment(
    additionalRootPaths: List<File> = emptyList(),
    additionalJavaSourceRootPaths: List<File> = emptyList(),
): KotlinCoreEnvironmentWrapper = KtTestCompiler.createEnvironment(additionalRootPaths, additionalJavaSourceRootPaths)
