package io.gitlab.arturbosch.detekt.rules.style

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import io.github.detekt.psi.absolutePath
import io.gitlab.arturbosch.detekt.api.ActiveByDefault
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.Configuration
import io.gitlab.arturbosch.detekt.api.Entity
import io.gitlab.arturbosch.detekt.api.Finding
import io.gitlab.arturbosch.detekt.api.Location
import io.gitlab.arturbosch.detekt.api.Rule
import io.gitlab.arturbosch.detekt.api.SourceLocation
import io.gitlab.arturbosch.detekt.api.TextLocation
import io.gitlab.arturbosch.detekt.api.config
import io.gitlab.arturbosch.detekt.rules.lastArgumentMatchesKotlinReferenceUrlSyntax
import io.gitlab.arturbosch.detekt.rules.lastArgumentMatchesMarkdownUrlSyntax
import io.gitlab.arturbosch.detekt.rules.lastArgumentMatchesUrl
import org.jetbrains.kotlin.KtPsiSourceFileLinesMapping
import org.jetbrains.kotlin.diagnostics.DiagnosticUtils.getLineAndColumnRangeInPsiFile
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtStringTemplateExpression
import org.jetbrains.kotlin.psi.psiUtil.getNonStrictParentOfType

/**
 * This rule reports lines of code which exceed a defined maximum line length.
 *
 * Long lines might be hard to read on smaller screens or printouts. Additionally, having a maximum line length
 * in the codebase will help make the code more uniform.
 */
@ActiveByDefault(since = "1.0.0")
class MaxLineLength(config: Config) : Rule(
    config,
    "Line detected, which is longer than the defined maximum line length in the code style."
) {

    @Suppress("MemberNameEqualsClassName")
    @Configuration("maximum line length")
    private val maxLineLength: Int by config(DEFAULT_IDEA_LINE_LENGTH)

    @Configuration("if package statements should be ignored")
    private val excludePackageStatements: Boolean by config(true)

    @Configuration("if import statements should be ignored")
    private val excludeImportStatements: Boolean by config(true)

    @Configuration("if comment statements should be ignored")
    private val excludeCommentStatements: Boolean by config(false)

    @Configuration("if raw strings should be ignored")
    private val excludeRawStrings: Boolean by config(true)

    override fun visitKtFile(file: KtFile) {
        super.visitKtFile(file)

        val sourceFileLinesMapping = KtPsiSourceFileLinesMapping(file)

        file.text.lineSequence().withIndex()
            .filterNot { (index, line) ->
                isValidLine(file, { sourceFileLinesMapping.getLineStartOffset(index) }, line)
            }
            .forEach { (index, line) ->
                val offset = sourceFileLinesMapping.getLineStartOffset(index)
                val ktElement = findFirstMeaningfulKtElementInParents(file, offset, line) ?: file
                val textRange = TextRange(offset, offset + line.length)
                val lineAndColumnRange = getLineAndColumnRangeInPsiFile(file, textRange)
                val location = Location(
                    source = SourceLocation(lineAndColumnRange.start.line, lineAndColumnRange.start.column),
                    endSource = SourceLocation(lineAndColumnRange.end.line, lineAndColumnRange.end.column),
                    text = TextLocation(offset, offset + line.length),
                    path = file.absolutePath(),
                )
                report(Finding(Entity.from(ktElement, location), description))
            }
    }

    private fun isValidLine(file: KtFile, offset: () -> Int, line: String) =
        line.length <= maxLineLength ||
            isIgnoredStatement(file, offset, line) ||
            line.lastArgumentMatchesUrl() ||
            line.lastArgumentMatchesMarkdownUrlSyntax() ||
            line.lastArgumentMatchesKotlinReferenceUrlSyntax()

    private fun isIgnoredStatement(file: KtFile, offset: () -> Int, line: String): Boolean =
        containsIgnoredPackageStatement(line) ||
            containsIgnoredImportStatement(line) ||
            containsIgnoredCommentStatement(line) ||
            containsIgnoredRawString(file, offset, line)

    private fun containsIgnoredRawString(file: KtFile, offset: () -> Int, line: String): Boolean {
        if (!excludeRawStrings) return false

        return findKtElementInParents(file, offset(), line)
            .sortedBy { it.textOffset }
            .lastOrNull()
            ?.isInsideRawString() == true
    }

    private fun containsIgnoredPackageStatement(line: String): Boolean {
        if (!excludePackageStatements) return false

        return line.trimStart().startsWith("package ")
    }

    private fun containsIgnoredImportStatement(line: String): Boolean {
        if (!excludeImportStatements) return false

        return line.trimStart().startsWith("import ")
    }

    private fun containsIgnoredCommentStatement(line: String): Boolean {
        if (!excludeCommentStatements) return false

        return line.trimStart().startsWith("//") ||
            line.trimStart().startsWith("/*") ||
            line.trimStart().startsWith("*")
    }

    companion object {
        private const val DEFAULT_IDEA_LINE_LENGTH = 120
        private val BLANK_OR_QUOTES = """[\s"]*""".toRegex()

        private fun findFirstMeaningfulKtElementInParents(file: KtFile, offset: Int, line: String): PsiElement? =
            findKtElementInParents(file, offset, line).firstOrNull { !BLANK_OR_QUOTES.matches(it.text) }
    }
}

private fun PsiElement.isInsideRawString(): Boolean =
    this is KtStringTemplateExpression || getNonStrictParentOfType<KtStringTemplateExpression>() != null
