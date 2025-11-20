package net.stefanfuchs.jslt.intellij.language

import com.intellij.openapi.project.Project
import com.intellij.psi.*
import com.intellij.psi.search.FilenameIndex
import com.intellij.psi.search.GlobalSearchScope
import java.util.concurrent.ConcurrentHashMap

object JsltCustomFunctionRegistry {
    private val cache = ConcurrentHashMap<Project, Map<String, PsiClass>>()

    /**
     * Searches the project for Java classes that extend AbstractFunction
     * and extracts function names from the super() constructor call
     * Returns a map of function name to the PsiClass that defines it
     */
    fun getCustomFunctions(project: Project): Map<String, PsiClass> {
        return cache.getOrPut(project) {
            val functions = mutableMapOf<String, PsiClass>()
            val scope = GlobalSearchScope.projectScope(project)
            val psiManager = PsiManager.getInstance(project)

            // Find all .java files
            val allFiles = FilenameIndex.getAllFilesByExt(project, "java", scope)

            allFiles.forEach { virtualFile ->
                try {
                    val psiFile = psiManager.findFile(virtualFile) as? PsiJavaFile ?: return@forEach

                    // Recursively find all classes (including inner/nested classes)
                    fun processClass(psiClass: PsiClass) {
                        // Check if the class extends AbstractFunction
                        val superClass = psiClass.superClass
                        if (superClass?.qualifiedName == "com.schibsted.spt.data.jslt.impl.AbstractFunction") {
                            // Find the constructor
                            psiClass.constructors.forEach { constructor ->
                                // Look for super() call
                                constructor.body?.statements?.forEach { statement ->
                                    if (statement is PsiExpressionStatement) {
                                        val expr = statement.expression
                                        if (expr is PsiMethodCallExpression) {
                                            val methodExpr = expr.methodExpression
                                            if (methodExpr.referenceName == "super") {
                                                // Extract the first argument (function name)
                                                val args = expr.argumentList.expressions
                                                if (args.isNotEmpty() && args[0] is PsiLiteralExpression) {
                                                    val literal = args[0] as PsiLiteralExpression
                                                    val functionName = literal.value as? String
                                                    if (functionName != null) {
                                                        functions[functionName] = psiClass
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Recursively process inner classes
                        psiClass.innerClasses.forEach { innerClass ->
                            processClass(innerClass)
                        }
                    }

                    // Process all top-level classes
                    psiFile.classes.forEach { psiClass ->
                        processClass(psiClass)
                    }
                } catch (e: Exception) {
                    // Skip files that can't be read or parsed
                }
            }

            functions
        }
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