package io.gitlab.arturbosch.detekt.rules.bugs

import com.intellij.psi.PsiElement
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.Entity
import io.gitlab.arturbosch.detekt.api.Finding
import io.gitlab.arturbosch.detekt.api.RequiresFullAnalysis
import io.gitlab.arturbosch.detekt.api.Rule
import io.gitlab.arturbosch.detekt.rules.isCalling
import io.gitlab.arturbosch.detekt.rules.isNullable
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtSimpleNameExpression
import org.jetbrains.kotlin.psi.KtStringTemplateEntry
import org.jetbrains.kotlin.psi.psiUtil.getQualifiedExpressionForSelector

/**
 * Reports `toString()` calls with a nullable receiver that may return the string "null".
 *
 * <noncompliant>
 * fun foo(a: Any?): String {
 *     return a.toString()
 * }
 *
 * fun bar(a: Any?): String {
 *     return "$a"
 * }
 * </noncompliant>
 *
 * <compliant>
 * fun foo(a: Any?): String {
 *     return a?.toString() ?: "-"
 * }
 *
 * fun bar(a: Any?): String {
 *     return "${a ?: "-"}"
 * }
 * </compliant>
 */
class NullableToStringCall(config: Config) :
    Rule(
        config,
        "`toString()` on nullable receiver may return the string \"null\""
    ),
    RequiresFullAnalysis {

    override fun visitSimpleNameExpression(expression: KtSimpleNameExpression) {
        super.visitSimpleNameExpression(expression)

        val callExpression = expression.parent as? KtCallExpression
        val qualifiedExpression = (callExpression ?: expression).getQualifiedExpressionForSelector()
        val stringTemplateEntry = (qualifiedExpression ?: callExpression ?: expression).parent as? KtStringTemplateEntry

        when {
            callExpression?.isCalling(toString, bindingContext) == true -> report(qualifiedExpression ?: callExpression)
            stringTemplateEntry?.hasNullableExpression() == true -> report(stringTemplateEntry)
        }
    }

    private fun KtStringTemplateEntry.hasNullableExpression(): Boolean {
        val expression = this.expression ?: return false
        return expression.isNullable(
            bindingContext,
            compilerResources.languageVersionSettings,
            compilerResources.dataFlowValueFactory,
            shouldConsiderPlatformTypeAsNullable = false,
        )
    }

    private fun report(element: PsiElement) {
        val finding = Finding(
            Entity.from(element),
            "This call '${element.text}' may return the string \"null\"."
        )
        report(finding)
    }

    companion object {
        val toString = FqName("kotlin.toString")
    }
}
