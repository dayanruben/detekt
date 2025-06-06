package io.gitlab.arturbosch.detekt.rules.coroutines

import io.gitlab.arturbosch.detekt.api.Alias
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.Entity
import io.gitlab.arturbosch.detekt.api.Finding
import io.gitlab.arturbosch.detekt.api.RequiresFullAnalysis
import io.gitlab.arturbosch.detekt.api.Rule
import io.gitlab.arturbosch.detekt.rules.coroutines.utils.isCoroutineScope
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.resolve.BindingContext

/**
 * Suspend functions that use `CoroutineScope` as receiver should not be marked as `suspend`.
 * A `CoroutineScope` provides structured concurrency via its `coroutineContext`. A `suspend`
 * function also has its own `coroutineContext`, which is now ambiguous and mixed with the
 * receiver`s.
 *
 * See https://kotlinlang.org/docs/coroutines-basics.html#scope-builder-and-concurrency
 *
 * <noncompliant>
 * suspend fun CoroutineScope.foo() {
 *     launch {
 *         delay(1.seconds)
 *     }
 * }
 * </noncompliant>
 *
 * <compliant>
 * fun CoroutineScope.foo() {
 *     launch {
 *         delay(1.seconds)
 *     }
 * }
 *
 * // Alternative
 * suspend fun foo() = coroutineScope {
 *     launch {
 *         delay(1.seconds)
 *     }
 * }
 * </compliant>
 *
 */
@Alias("SuspendFunctionOnCoroutineScope")
class SuspendFunWithCoroutineScopeReceiver(config: Config) :
    Rule(
        config,
        "The `suspend` modifier should not be used for functions that use a CoroutinesScope as receiver. You should " +
            "use suspend functions without the receiver or use plain functions and use coroutineScope { } instead."
    ),
    RequiresFullAnalysis {

    override fun visitNamedFunction(function: KtNamedFunction) {
        checkReceiver(function)
    }

    private fun checkReceiver(function: KtNamedFunction) {
        val suspendModifier = function.modifierList?.getModifier(KtTokens.SUSPEND_KEYWORD) ?: return
        val receiver = bindingContext[BindingContext.FUNCTION, function]
            ?.extensionReceiverParameter
            ?.value
            ?.type
            ?: return
        if (receiver.isCoroutineScope()) {
            report(
                Finding(
                    entity = Entity.from(suspendModifier),
                    message = "`suspend` function uses CoroutineScope as receiver."
                )
            )
        }
    }
}
