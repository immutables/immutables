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

import javax.annotation.Nullable;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;

final class SuppressedWarnings {
  private static final String ALL = "all";
  private static final String IMMUTABLES = "immutables";
  private static final String GENERATED = "generated";
  private static final String RAWTYPES = "rawtypes";

  final boolean all;
  final boolean immutables;
  final boolean generated;
  final boolean rawtypes;

  private SuppressedWarnings(
      boolean all,
      boolean immutables,
      boolean generated,
      boolean rawtypes) {
    this.all = all;
    this.immutables = immutables;
    this.generated = generated;
    this.rawtypes = rawtypes;
  }

  static SuppressedWarnings forElement(Element element) {
    boolean all = false;
    boolean immutables = false;
    boolean generated = false;
    boolean rawtypes = false;

    outer: for (Element e = element; e.getKind() != ElementKind.PACKAGE; e = e.getEnclosingElement()) {
      @Nullable SuppressWarnings suppressWarnings = e.getAnnotation(SuppressWarnings.class);
      if (suppressWarnings != null) {
        for (String w : suppressWarnings.value()) {
          switch (w) {
          case ALL:
            all = true;
            immutables = true;
            generated = true;
            rawtypes = true;
            break outer;
          case IMMUTABLES:
            immutables = true;
            break;
          case GENERATED:
            generated = true;
            break;
          case RAWTYPES:
            rawtypes = true;
            break;
          default:
            break;
          }
        }
      }
    }
    return new SuppressedWarnings(all, immutables, generated, rawtypes);
  }
}
