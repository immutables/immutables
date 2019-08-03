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
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import org.immutables.value.Value;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Repository model and builder. Traverses {@code javax.lang.model} API to generate
 * repository interfaces and methods and expose them in template engine.
 *
 * TODO: This code is currently a mess and needs cleanup.
 */
public class RepositoryModel {

  private static final String BACKEND = "org.immutables.criteria.backend.Backend";

  private final ValueType type;
  private final Element element;

  // processor utils
  private final Types types;
  private final Elements elements;

  private List<Facet> cachedFacets;

  RepositoryModel(ValueType type) {
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
        final FacetConsumer facetConsumer = new FacetConsumer(facets, typed);
        for (TypeMirror iface: typed.getInterfaces()) {
          facetConsumer.consume(iface);
        }
      }
    }
    cachedFacets = ImmutableList.copyOf(facets);
    return cachedFacets;
  }

  private interface Consumer<T> {
    void consume(T value);
  }

  /**
   * Traverses current type and build a facet definition
   */
  private class FacetConsumer implements Consumer<TypeMirror> {
    private final List<Facet> facets;
    private final TypeElement typed;

    private FacetConsumer(List<Facet> facets, TypeElement typed) {
      this.facets = facets;
      this.typed = typed;
    }

    private String nextName(TypeElement element) {
      final String ifaceName = element.getQualifiedName().toString();
      String name;
      if (ifaceName.contains(".")) {
        name = ifaceName.substring(ifaceName.lastIndexOf('.') + 1);
      } else {
        name = ifaceName;
      }
      name = CaseFormat.UPPER_CAMEL.converterTo(CaseFormat.LOWER_CAMEL).convert(name);

      return name;
    }

    @Override
    public void consume(TypeMirror iface) {
      if (!isFacet(iface)) {
        return;
      }

      final TypeElement ifaceElement = MoreElements.asType(types.asElement(iface));
      final String name = nextName(ifaceElement);
      // convert Writable<T> into Writable<Person>
      final DeclaredType fieldType = types.getDeclaredType(typed, RepositoryModel.this.element.asType());

      final List<ExecutableElement> constructors = ElementFilter.constructorsIn(elements.getAllMembers(typed));
      final CodeBlock block;
      if (!constructors.isEmpty()) {
        final List<String> params = new ArrayList<>();
        // only one constructor expected
        ExecutableElement ctor = constructors.get(0);
        for(VariableElement variable:ctor.getParameters()) {
          TypeMirror varType = variable.asType();
          if (types.isSubtype(varType, types.erasure(elements.getTypeElement(Class.class.getCanonicalName()).asType()))) {
            // inject entity class
            params.add(RepositoryModel.this.type.typeDocument().toString() + ".class");
          } else if (types.isSubtype(varType, elements.getTypeElement(BACKEND).asType())) {
            // inject backend
            params.add("backend");
          }
        }

        block = new CodeBlock(String.format("this.%s = new %s(%s)", name, fieldType, Joiner.on(", ").join(params)));
      } else {
        block = new CodeBlock("");
      }

      ImmutableFacet.Builder facet = ImmutableFacet.builder();
      // figure out list of interface methods to implement from eg. Writable
      final DeclaredType interfaceType = types.getDeclaredType(ifaceElement, RepositoryModel.this.element.asType());
      final List<ExecutableElement> ifaceMethods = ElementFilter.methodsIn(elements.getAllMembers(MoreElements.asType(interfaceType.asElement())));
      final List<ExecutableElement> objectMethods = ElementFilter.methodsIn(elements.getAllMembers(MoreElements.asType(elements.getTypeElement(Object.class.getCanonicalName()))));
      ifaceMethods.removeAll(objectMethods);

      for (ExecutableElement exec: ifaceMethods) {
        // figure out implemented method signature
        ExecutableType type1 = MoreTypes.asExecutable(types.asMemberOf(interfaceType, exec));
        facet.addMethods(new DelegateMethod(type1, exec, name));
      }

      facet.name(name)
              .interfaceType(interfaceType)
              .fieldType(fieldType)
              .constructor(block)
              .build();

      facets.add(facet.build());
    }
  }

  private boolean isFacet(TypeMirror mirror) {
    final Element element = types.asElement(mirror);
    if (element == null || !MoreElements.isType(element)) {
      return false;
    }

    final TypeElement typed = MoreElements.asType(element);
    final TypeMirror facet = elements.getTypeElement("org.immutables.criteria.repository.Facet").asType();
    if (types.isSubtype(mirror, facet)) {
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

  public boolean isGenerateRepository() {
    return type.constitution.protoclass().criteriaRepository().isPresent();
  }

  /**
   * Used in templates to generate repository source code
   */
  @Value.Immutable
  public interface Facet {
    String name();
    TypeMirror interfaceType();
    TypeMirror fieldType();
    CodeBlock constructor();
    List<DelegateMethod> methods();
  }

  public static class CodeBlock {
    public final String code;

    private CodeBlock(String code) {
      this.code = Objects.requireNonNull(code, "code");
    }

    public boolean isEmpty() {
      return code.isEmpty();
    }

  }

  public static class DelegateMethod {
    private final ExecutableType type;
    private final ExecutableElement element;
    private final String body;

    private DelegateMethod(ExecutableType type, ExecutableElement element, String delegateName) {
      this.type = Objects.requireNonNull(type, "type");
      this.element = element;
      this.body = String.format("return %s.%s(%s);", delegateName, element.getSimpleName().toString(), Joiner.on(", ").join(element.getParameters()));
    }

    public String name() {
      return element.getSimpleName().toString();
    }

    public String parameters() {
      List<String> params = new ArrayList<>();
      for (int i = 0; i < type.getParameterTypes().size(); i++) {
        final TypeMirror paramType = type.getParameterTypes().get(i);
        final String name = element.getParameters().get(i).getSimpleName().toString();
        if (element.isVarArgs()) {
          // special case for varargs
          final TypeMirror component = MoreTypes.asArray(paramType).getComponentType();
          params.add(component + " ... " + name);
        } else {
          params.add(paramType + " " + name);
        }
      }
      return Joiner.on(", ").join(params);
    }

    public TypeMirror returnType() {
      return type.getReturnType();
    }

    public String body() {
      return body;
    }
  }

}
