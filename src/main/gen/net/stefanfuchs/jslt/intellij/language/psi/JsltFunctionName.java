// This is a generated file. Not intended for manual editing.
package net.stefanfuchs.jslt.intellij.language.psi;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;

public interface JsltFunctionName extends JsltFunctionNameElement {

  @Nullable String getName();

  @NotNull PsiElement setName(@NotNull String newName);

  @Nullable PsiElement getNameIdentifier();

  @Nullable String getImportAlias();

  //WARNING: setImportAlias(...) is skipped
  //matching setImportAlias(JsltFunctionName, ...)
  //methods are not found in JsltPsiImplUtil

  @NotNull PsiReference getReference();

}
