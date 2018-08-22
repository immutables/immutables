/*
   Copyright 2015 Immutables Authors and Contributors

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

import java.util.LinkedHashSet;
import java.util.Set;
import javax.annotation.Nullable;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;

final class SuppressedWarnings {
  private static final String ALL = "all";
  private static final String IMMUTABLES = "immutables";
  private static final String GENERATED = "generated";
  private static final String RAWTYPES = "rawtypes";
  private static final String DEPRECATION = "deprecation";
  private static final String IMMUTABLES_FROM = "immutables:from";
  private static final String IMMUTABLES_SUBTYPE = "immutables:subtype";
  private static final String IMMUTABLES_UNTYPE = "immutables:untype";
  private static final String IMMUTABLES_INCOMPAT = "immutables:incompat";

  final boolean all;
  final boolean immutables;
  final boolean generated;
  final boolean rawtypes;
  final boolean from;
  final boolean subtype;
  final boolean untype;
  final boolean incompat;
  final Set<String> generatedSuppressions;

  private SuppressedWarnings(
      boolean all,
      boolean immutables,
      boolean generated,
      boolean rawtypes,
      boolean from,
      boolean subtype,
      boolean untype,
      boolean incompat,
      Set<String> generatedSuppressions) {
    this.all = all;
    this.immutables = immutables;
    this.generated = generated;
    this.rawtypes = rawtypes;
    this.generatedSuppressions = generatedSuppressions;
    this.from = from;
    this.subtype = subtype;
    this.untype = untype;
    this.incompat = incompat;
  }

  static SuppressedWarnings forElement(
      Element element,
      boolean generateSuppressAllWarning,
      boolean hasDeprecatedMembers) {
    boolean all = false;
    boolean immutables = false;
    boolean generated = generateSuppressAllWarning;
    boolean rawtypes = false;
    boolean deprecated = hasDeprecatedMembers;
    boolean from = false;
    boolean subtype = false;
    boolean untype = false;
    boolean incompat = false;

    Set<String> generatedSuppressions = new LinkedHashSet<>();

    for (Element e = element; e.getKind() != ElementKind.PACKAGE; e = e.getEnclosingElement()) {
      @Nullable SuppressWarnings suppressWarnings = e.getAnnotation(SuppressWarnings.class);
      if (suppressWarnings != null) {
        for (String w : suppressWarnings.value()) {
          switch (w) {
          case ALL:
            all = true;
            from = true;
            immutables = true;
            generated = true;
            rawtypes = true;
            from = true;
            subtype = true;
            untype = true;
            incompat = true;
            break;
          case IMMUTABLES:
            immutables = true;
            from = true;
            subtype = true;
            untype = true;
            incompat = true;
            break;
          case IMMUTABLES_FROM:
            from = true;
            break;
          case IMMUTABLES_SUBTYPE:
            subtype = true;
            break;
          case IMMUTABLES_UNTYPE:
            untype = true;
            break;
          case IMMUTABLES_INCOMPAT:
            incompat = true;
            break;
          case GENERATED:
            generated = true;
            break;
          case RAWTYPES:
            rawtypes = true;
            generatedSuppressions.add(w);
            break;
          case DEPRECATION:
            deprecated = true;
            break;
          default:
            generatedSuppressions.add(w);
            break;
          }
        }
      }
    }

    if (generated) {
      generatedSuppressions.add(ALL);
    }
    if (deprecated) {
      generatedSuppressions.add(DEPRECATION);
    }
    return new SuppressedWarnings(
        all,
        immutables,
        generated,
        rawtypes,
        from,
        subtype,
        untype,
        incompat,
        generatedSuppressions);
  }
}
