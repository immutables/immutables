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

import java.util.Collections;
import com.google.common.base.Optional;
import com.google.common.base.Ascii;
import com.google.common.base.Function;
import com.google.common.collect.Lists;
import java.util.Collection;
import java.util.List;
import javax.annotation.Nullable;
import org.immutables.generator.SourceExtraction;
import org.immutables.generator.SourceExtraction.Imports;
import org.immutables.value.processor.meta.Proto.DeclaringType;

class ImportsTypeStringResolver implements Function<String, String> {
  private final Collection<DeclaringType> declaringTypes;
  private List<SourceExtraction.Imports> sourceClassesImports;

  boolean unresolved;

  ImportsTypeStringResolver(Collection<DeclaringType> declaringTypes) {
    this.declaringTypes = declaringTypes;
  }

  @Override
  public String apply(String input) {
    boolean assumedUnqualified = Ascii.isUpperCase(input.charAt(0));
    if (assumedUnqualified) {
      input = qualifyImportedIfPossible(input);
    }
    return input;
  }

  @Nullable
  private String getFromSourceImports(String resolvable) {
    if (sourceClassesImports == null) {
      sourceClassesImports = Lists.newArrayList();
      for (DeclaringType t : declaringTypes) {
        Imports imports = t.associatedTopLevel().sourceImports();
        if (!sourceClassesImports.contains(imports)) {
          sourceClassesImports.add(imports);
        }
      }
    }
    for (SourceExtraction.Imports imports : sourceClassesImports) {
      @Nullable String resolved = imports.classes.get(resolvable);
      if (resolved != null) {
        return resolved;
      }
    }
    return null;
  }

  private String qualifyImportedIfPossible(String typeName) {
    int nestedTypeDotIndex = typeName.indexOf('.');

    String resolvable = nestedTypeDotIndex > 0
        ? typeName.substring(0, nestedTypeDotIndex)
        : typeName;

    @Nullable String resolvedImported = getFromSourceImports(resolvable);
    if (resolvedImported != null) {
      return nestedTypeDotIndex > 0
          ? resolvedImported + typeName.substring(nestedTypeDotIndex)
          : resolvedImported;
    }

    unresolved = true;
    return typeName;
  }

  public static Function<String, String> from(Optional<DeclaringType> optional) {
    return new ImportsTypeStringResolver(optional.asSet());
  }

  public static Function<String, String> from(Collection<DeclaringType> types) {
    return new ImportsTypeStringResolver(types);
  }

  public static Function<String, String> from(DeclaringType type) {
    return new ImportsTypeStringResolver(Collections.singleton(type));
  }
}
