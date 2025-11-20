package net.stefanfuchs.jslt.intellij.language

import com.intellij.lang.injection.InjectedLanguageManager
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiReferenceBase

class JsltImportDeclarationReference(element: PsiElement, textRange: TextRange) :
    PsiReferenceBase<PsiElement?>(element, textRange) {

    private val refFilename = element.text.substring(textRange.startOffset, textRange.endOffset)

    override fun resolve(): PsiElement? {
        val containingFile =
            if (InjectedLanguageManager
                    .getInstance(element.project)
                    .isInjectedFragment(element.containingFile)
            ) {
                InjectedLanguageManager
                    .getInstance(element.project)
                    .getInjectionHost(element)
                    ?.containingFile
            } else {
                element.containingFile
            }

        // Try local resources root first
        val localResourcesRoot = findResourcesRoot(containingFile?.virtualFile)
        val localFile = localResourcesRoot?.findFileByRelativePath(refFilename)
        if (localFile != null) {
            return PsiManager.getInstance(element.project).findFile(localFile)
        }

        // Search all src/main/resources directories in the project
        val referencedFile = findInAllResourcesRoots()
        return if (referencedFile != null) {
            PsiManager.getInstance(element.project).findFile(referencedFile)
        } else {
            null
        }
    }

    private fun findResourcesRoot(virtualFile: com.intellij.openapi.vfs.VirtualFile?): com.intellij.openapi.vfs.VirtualFile? {
        var current = virtualFile?.parent
        while (current != null) {
            if (current.name == "resources") {
                return current
            }
            current = current.parent
        }
        return null
    }

    private fun findInAllResourcesRoots(): com.intellij.openapi.vfs.VirtualFile? {
        val moduleManager = ModuleManager.getInstance(element.project)

        for (module in moduleManager.modules) {
            val moduleRootManager = ModuleRootManager.getInstance(module)

            // Get all source roots for this module
            for (sourceRoot in moduleRootManager.sourceRoots) {
                // Check if this is a resources directory (src/main/resources)
                if (isMainResourcesRoot(sourceRoot)) {
                    val file = sourceRoot.findFileByRelativePath(refFilename)
                    if (file != null) {
                        return file
                    }
                }
            }
        }
        return null
    }

    private fun isMainResourcesRoot(root: com.intellij.openapi.vfs.VirtualFile): Boolean {
        // Check if path matches src/main/resources pattern
        return root.name == "resources" &&
                root.parent?.name == "main" &&
                root.parent?.parent?.name == "src"
    }

}