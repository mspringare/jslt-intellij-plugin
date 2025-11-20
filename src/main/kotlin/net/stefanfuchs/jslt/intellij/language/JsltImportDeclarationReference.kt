package net.stefanfuchs.jslt.intellij.language

import com.intellij.lang.injection.InjectedLanguageManager
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

        val resourcesRoot = findResourcesRoot(containingFile?.virtualFile)
        val referencedFile = resourcesRoot?.findFileByRelativePath(refFilename)

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

}