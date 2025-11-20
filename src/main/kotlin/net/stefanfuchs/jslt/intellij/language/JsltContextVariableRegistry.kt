package net.stefanfuchs.jslt.intellij.language

import com.intellij.openapi.project.Project
import com.intellij.psi.*
import com.intellij.psi.search.FilenameIndex
import com.intellij.psi.search.GlobalSearchScope
import java.util.concurrent.ConcurrentHashMap

/**
 * Scans Java code to find context variables passed to Expression.apply() calls.
 * Looks for Map.of() and Map.entry() patterns to extract variable names.
 */
object JsltContextVariableRegistry {
    private val cache = ConcurrentHashMap<Project, Map<String, PsiElement>>()

    /**
     * Searches the project for Java files that call Expression.apply() with context variables.
     * Extracts variable names from Map.of(), Map.entry(), and similar patterns.
     * Returns a map of variable names to their PsiElement (the string literal where they're defined).
     */
    fun getContextVariables(project: Project): Map<String, PsiElement> {
        return cache.getOrPut(project) {
            val variables = mutableMapOf<String, PsiElement>()
            val scope = GlobalSearchScope.projectScope(project)
            val psiManager = PsiManager.getInstance(project)

            // Find all .java files
            val allFiles = FilenameIndex.getAllFilesByExt(project, "java", scope)

            allFiles.forEach { virtualFile ->
                try {
                    val psiFile = psiManager.findFile(virtualFile) as? PsiJavaFile ?: return@forEach

                    // Visit all elements in the file looking for Map construction patterns
                    psiFile.accept(object : JavaRecursiveElementVisitor() {
                        override fun visitMethodCallExpression(expression: PsiMethodCallExpression) {
                            super.visitMethodCallExpression(expression)

                            // Look for Map.of(...) or ImmutableMap.of(...) calls
                            val methodName = expression.methodExpression.referenceName
                            if (methodName == "of") {
                                val qualifierType = expression.methodExpression.qualifierExpression?.type
                                val qualifierText = qualifierType?.canonicalText ?: ""

                                // Check if this is a Map.of() call
                                if (qualifierText.contains("Map") ||
                                    qualifierText.contains("java.util.Map") ||
                                    expression.methodExpression.qualifierExpression?.text?.contains("Map") == true) {

                                    // Extract string literal keys (at positions 0, 2, 4, 6...)
                                    expression.argumentList.expressions.forEachIndexed { index, arg ->
                                        if (index % 2 == 0 && arg is PsiLiteralExpression) {
                                            val value = arg.value
                                            if (value is String && isValidVariableName(value)) {
                                                // Store the variable name mapped to the PsiLiteralExpression
                                                variables[value] = arg
                                            }
                                        }
                                    }
                                }
                            }

                            // Look for Map.entry("key", value) calls
                            if (methodName == "entry") {
                                val args = expression.argumentList.expressions
                                if (args.isNotEmpty() && args[0] is PsiLiteralExpression) {
                                    val literal = args[0] as PsiLiteralExpression
                                    val value = literal.value
                                    if (value is String && isValidVariableName(value)) {
                                        // Store the variable name mapped to the PsiLiteralExpression
                                        variables[value] = literal
                                    }
                                }
                            }
                        }
                    })
                } catch (e: Exception) {
                    // Skip files that can't be read or parsed
                }
            }

            variables
        }
    }

    /**
     * Validates that a string is a reasonable variable name
     * Filters out things like URLs, paths, etc.
     */
    private fun isValidVariableName(name: String): Boolean {
        // Variable names should:
        // - Not be empty
        // - Not contain dots (likely JSON paths or package names)
        // - Not contain slashes (likely file paths)
        // - Not contain special characters that wouldn't be in a JSLT variable
        // - Start with a letter or underscore (common variable convention)
        return name.isNotEmpty() &&
               !name.contains('.') &&
               !name.contains('/') &&
               !name.contains('\\') &&
               !name.contains(':') &&
               !name.contains(' ') &&
               name.matches(Regex("^[a-zA-Z_][a-zA-Z0-9_]*$"))
    }

    /**
     * Clears the cache for a project (useful when files change)
     */
    fun clearCache(project: Project) {
        cache.remove(project)
    }

    /**
     * Clears the entire cache
     */
    fun clearAllCaches() {
        cache.clear()
    }
}
