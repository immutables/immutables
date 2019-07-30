/*
 * Copyright 2019 Immutables Authors and Contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.immutables.value.processor.meta;

import com.google.common.base.CaseFormat;
import org.immutables.value.Value;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Helper methods for criteria repository generation. Used to get list of facets
 * and their implementations.
 */
public class ValueTypeRepository {

  private final ValueType type;
  private final Element element;

  // processor utils
  private final Types types;
  private final Elements elements;

  private List<Facet> cachedFacets;

  ValueTypeRepository(ValueType type) {
    this.type = Objects.requireNonNull(type, "type");
    this.element = type.element;
    final ProcessingEnvironment env = type.constitution.protoclass().environment().processing();
    this.types = env.getTypeUtils();
    this.elements = env.getElementUtils();
  }

  public List<Facet> facets() {
    if (cachedFacets != null) {
      return cachedFacets;
    }

    final CriteriaRepositoryMirror mirror = annotation();
    final List<Facet> facets = new ArrayList<>();

    for (TypeMirror type: mirror.facetsMirror()) {
      final Element element = types.asElement(type);
      if (MoreElements.isType(element)) {
        final TypeElement typed = MoreElements.asType(element);

        for (TypeMirror iface: typed.getInterfaces()) {
          if (isFacet(iface)) {
            final TypeElement ifaceElement = MoreElements.asType(types.asElement(iface));
            final String ifaceName = ifaceElement.getQualifiedName().toString();
            final String name;
            if (ifaceName.contains(".")) {
              name = ifaceName.substring(ifaceName.lastIndexOf('.') + 1);
            } else {
              name = ifaceName;
            }
            final Facet facet = ImmutableFacet.builder()
                    .name(CaseFormat.UPPER_CAMEL.converterTo(CaseFormat.LOWER_CAMEL).convert(name))
                    .interfaceType(types.getDeclaredType(ifaceElement, this.element.asType()))
                    .fieldType(types.getDeclaredType(typed, this.element.asType()))
                    .build();
            facets.add(facet);
          }
        }
      }
    }
    cachedFacets = facets;
    return cachedFacets;
  }

  private boolean isFacet(TypeMirror mirror) {
    final Element element = types.asElement(mirror);
    if (element == null) {
      return false;
    }

    if (!MoreElements.isType(element)) {
      return false;
    }

    final TypeElement typed = MoreElements.asType(types.asElement(mirror));

    // TODO interface mirror ?
    if (typed.getQualifiedName().contentEquals("org.immutables.criteria.repository.Facet")) {
      return true;
    }

    for (TypeMirror iface: typed.getInterfaces()) {
      if (isFacet(iface)) {
        return true;
      }
    }
    return false;
  }

  private CriteriaRepositoryMirror annotation() {
    return type.constitution.protoclass().criteriaRepository().get();
  }

  public boolean isReadonly() {
    return annotation().readonly();
  }

  public boolean isWatch() {
    return annotation().watch();
  }

  public boolean isGenerateRepository() {
    return type.constitution.protoclass().criteriaRepository().isPresent();
  }

  @Value.Immutable
  public interface Facet {
    String name();
    TypeMirror interfaceType();
    TypeMirror fieldType();
  }

  public static class FacetMethod {
    // name
    // codeblock
    // return type
    // parameters
  }

}
