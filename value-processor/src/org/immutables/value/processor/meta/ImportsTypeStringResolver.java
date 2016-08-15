/*
   Copyright 2016 Immutables Authors and Contributors

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */
package org.immutables.value.processor.meta;

import com.google.common.base.Ascii;
import com.google.common.base.Function;
import javax.annotation.Nullable;
import org.immutables.generator.SourceExtraction;
import org.immutables.value.processor.meta.Proto.DeclaringType;

class ImportsTypeStringResolver implements Function<String, String> {
  boolean unresolved;
  private final @Nullable DeclaringType usingType;
  private final @Nullable DeclaringType originType;

  ImportsTypeStringResolver(@Nullable DeclaringType usingType, @Nullable DeclaringType originType) {
    this.usingType = usingType == null ? null : usingType.associatedTopLevel();
    this.originType = originType == null ? null : originType.associatedTopLevel();
  }

  @Override
  public String apply(String input) {
    boolean assumedUnqualified = Ascii.isUpperCase(input.charAt(0));
    if (assumedUnqualified) {
      input = qualifyImportedIfPossible(input, false);
    }
    return input;
  }

  public String resolveTopForAttribute(String input) {
    boolean assumedUnqualified = Ascii.isUpperCase(input.charAt(0));
    if (assumedUnqualified) {
      input = qualifyImportedIfPossible(input, true);
    }
    return input;
  }

  @Nullable
  private String getFromSourceImports(String resolvable, boolean notTypeArgument) {
    SourceExtraction.Imports[] importsSet = usingType == null && originType == null
        ? new SourceExtraction.Imports[] {}
        : usingType == null
            ? new SourceExtraction.Imports[] {originType.sourceImports()}
            : originType == null || usingType == originType
                ? new SourceExtraction.Imports[] {usingType.sourceImports()}
                : new SourceExtraction.Imports[] {originType.sourceImports(), usingType.sourceImports()};

    for (SourceExtraction.Imports imports : importsSet) {
      @Nullable String resolved = imports.classes.get(resolvable);
      if (resolved != null) {
        return resolved;
      }
    }

    // where types are present and are different
    if (notTypeArgument && originType != null) {
      if (resolvable.equals("ImmutableDelta")) {
        System.out.println("Origin " + originType + "\nUsing " + usingType);
      }
      if (!hasStarImports(importsSet)) {
        // Strongly assuming it comes from originating type's package
        return originType.packageOf().name() + "." + resolvable;
      }
    }

    return null;
  }

  private boolean hasStarImports(SourceExtraction.Imports... importsSet) {
    for (SourceExtraction.Imports imports : importsSet) {
      for (String statement : imports.all) {
        if (statement.endsWith(".*")) {
          return true;
        }
      }
    }
    return false;
  }

  private String qualifyImportedIfPossible(String typeName, boolean notTypeArgument) {
    int nestedTypeDotIndex = typeName.indexOf('.');

    String resolvable = nestedTypeDotIndex > 0
        ? typeName.substring(0, nestedTypeDotIndex)
        : typeName;

    @Nullable String resolvedImported = getFromSourceImports(resolvable, notTypeArgument);
    if (resolvedImported != null) {
      return nestedTypeDotIndex > 0
          ? resolvedImported + typeName.substring(nestedTypeDotIndex)
          : resolvedImported;
    }

    unresolved = true;
    return typeName;
  }
}
