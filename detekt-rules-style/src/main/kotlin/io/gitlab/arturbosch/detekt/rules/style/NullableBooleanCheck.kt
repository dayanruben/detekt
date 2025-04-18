package io.gitlab.arturbosch.detekt.rules.style

import com.intellij.psi.PsiElement
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.Entity
import io.gitlab.arturbosch.detekt.api.Finding
import io.gitlab.arturbosch.detekt.api.RequiresFullAnalysis
import io.gitlab.arturbosch.detekt.api.Rule
import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtBinaryExpression
import org.jetbrains.kotlin.resolve.calls.util.getType
import org.jetbrains.kotlin.types.typeUtil.isBooleanOrNullableBoolean

/**
 * Detects nullable boolean checks which use an elvis expression `?:` rather than equals `==`.
 *
 * Per the [Kotlin coding conventions](https://kotlinlang.org/docs/coding-conventions.html#nullable-boolean-values-in-conditions)
 * converting a nullable boolean property to non-null should be done via `!= false` or `== true`
 * rather than `?: true` or `?: false` (respectively).
 *
 * <noncompliant>
 * value ?: true
 * value ?: false
 * </noncompliant>
 *
 * <compliant>
 * value != false
 * value == true
 * </compliant>
 */
class NullableBooleanCheck(config: Config) :
    Rule(
        config,
        "Nullable boolean check should use `==` rather than `?:`"
    ),
    RequiresFullAnalysis {

    override fun visitBinaryExpression(expression: KtBinaryExpression) {
        if (expression.operationToken == KtTokens.ELVIS &&
            expression.right?.isBooleanConstant() == true &&
            expression.left?.getType(bindingContext)?.isBooleanOrNullableBoolean() == true
        ) {
            val messageSuffix =
                if (expression.right?.text == "true") {
                    "`!= false` rather than `?: true`"
                } else {
                    "`== true` rather than `?: false`"
                }
            report(
                Finding(
                    entity = Entity.from(expression),
                    message = "The nullable boolean check `${expression.text}` should use $messageSuffix",
                )
            )
        }

        super.visitBinaryExpression(expression)
    }

    private fun PsiElement.isBooleanConstant() = node.elementType == KtNodeTypes.BOOLEAN_CONSTANT
}
