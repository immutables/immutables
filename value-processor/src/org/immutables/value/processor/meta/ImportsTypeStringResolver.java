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
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import javax.annotation.Nullable;
import javax.lang.model.element.TypeElement;
import org.immutables.generator.SourceExtraction;
import org.immutables.value.processor.meta.Proto.DeclaringType;

class ImportsTypeStringResolver implements Function<String, String> {
  boolean unresolved;
  private final @Nullable DeclaringType usingType;
  private final @Nullable DeclaringType originType;
  private @Nullable ImmutableList<TypeElement> extendedClasses;
  private @Nullable ImmutableSet<TypeElement> implementedInterfaces;
  private ImmutableSet<String> unresolvedYetArguments = ImmutableSet.of();
  private Round round;

  ImportsTypeStringResolver(@Nullable DeclaringType usingType, @Nullable DeclaringType originType) {
    this.usingType = usingType == null ? null : usingType.associatedTopLevel();
    this.originType = originType == null ? null : originType.associatedTopLevel();
  }

  void hierarchyTraversalForUnresolvedTypes(
      Round round,
      ImmutableList<TypeElement> extendedClasses,
      ImmutableSet<TypeElement> implementedInterfaces,
      ImmutableSet<String> unresolvedYetArguments) {
    this.round = round;
    this.extendedClasses = extendedClasses;
    this.implementedInterfaces = implementedInterfaces;
    this.unresolvedYetArguments = unresolvedYetArguments;
  }

  @Override
  public String apply(String input) {
    unresolved = false;
    boolean assumedUnqualified = Ascii.isUpperCase(input.charAt(0));
    if (assumedUnqualified) {
      input = qualifyImportedIfPossible(input, false);
    }
    return input;
  }

  String resolveTopForAttribute(String input) {
    unresolved = false;
    boolean assumedUnqualified = Ascii.isUpperCase(input.charAt(0));
    if (assumedUnqualified) {
      input = qualifyImportedIfPossible(input, !unresolvedYetArguments.contains(input));
    }
    return input;
  }

  @Nullable
  private String getFromSourceImports(String resolvable, boolean notTypeArgument) {
    SourceExtraction.Imports[] importsSet = takeImportSets();

    for (SourceExtraction.Imports imports : importsSet) {
      @Nullable String resolved = imports.classes.get(resolvable);
      if (resolved != null) {
        return resolved;
      }
    }

    if (extendedClasses != null)
      for (TypeElement type : extendedClasses) {
        DeclaringType top = round.declaringTypeFrom(type).associatedTopLevel();
        @Nullable String resolved = top.sourceImports().classes.get(resolvable);
        if (resolved != null) {
          return resolved;
        }
      }

    if (implementedInterfaces != null) {
      for (TypeElement type : implementedInterfaces) {
        DeclaringType top = round.declaringTypeFrom(type).associatedTopLevel();
        @Nullable String resolved = top.sourceImports().classes.get(resolvable);
        if (resolved != null) {
          return resolved;
        }
      }
    }

    // where types are present and are different
    if (notTypeArgument && originType != null) {
      if (!hasStarImports(importsSet)) {
        // Strongly assuming it comes from originating type's package
        return originType.packageOf().name() + "." + resolvable;
      }
    }

    return null;
  }

  private SourceExtraction.Imports[] takeImportSets() {
    if (usingType == null && originType == null) {
      return new SourceExtraction.Imports[] {};
    }
    if (usingType == null) {
      return new SourceExtraction.Imports[] {
          originType.sourceImports()
      };
    }
    if (originType == null || usingType == originType) {
      return new SourceExtraction.Imports[] {
          usingType.sourceImports()
      };
    }
    return new SourceExtraction.Imports[] {
        originType.sourceImports(),
        usingType.sourceImports()
    };
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
