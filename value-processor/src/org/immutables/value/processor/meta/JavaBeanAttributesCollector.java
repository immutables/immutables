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

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import org.immutables.generator.Naming;

import javax.annotation.Nullable;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.Elements;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Collects attributes by searching for getters in a class definition.
 *
 * For each {@code getFoo} method (without parameters) create attribute {@code foo}.
 * @see AccessorAttributesCollector
 */
final class JavaBeanAttributesCollector {

  private final ValueType type;
  private final Proto.Protoclass protoclass;
  private final Getters getters;
  private final Fields fields;
  private final Styles styles;

  JavaBeanAttributesCollector(Proto.Protoclass protoclass, ValueType type) {
    this.type = Preconditions.checkNotNull(type, "type");
    this.protoclass = Preconditions.checkNotNull(protoclass, "protoclass");
    this.styles = new Styles(ImmutableStyleInfo.copyOf(protoclass.styles().style()).withGet("is*", "get*"));
    this.getters = new Getters();
    this.fields = new Fields();
  }


  private TypeElement getCachedTypeElement() {
    return CachingElements.getDelegate(MoreElements.asType(type.element));
  }

  /**
   * Collects and caches list of fields for current type
   */
  private class Fields {
    private final Map<String, VariableElement> fields;

    private Fields() {
      Map<String, VariableElement> map = new LinkedHashMap<>();
      for (VariableElement field: collectFields(getCachedTypeElement(), new LinkedHashSet<VariableElement>())) {
        if (!field.getModifiers().contains(Modifier.STATIC)) {
          map.put(field.getSimpleName().toString(), field);
        }
      }

      this.fields = ImmutableMap.copyOf(map);
    }

    /**
     * For some reason {@link Elements#getAllMembers(TypeElement)} does not
     * return fields from parent class. Collecting them manually in this method.
     */
    private <C extends Collection<VariableElement>> C collectFields(@Nullable Element element, C collection) {
      if (element == null || !element.getKind().isClass() || element.getKind() == ElementKind.ENUM) {
        return collection;
      }

      collection.addAll(ElementFilter.fieldsIn(element.getEnclosedElements()));
      TypeMirror parent = MoreElements.asType(element).getSuperclass();
      if (parent.getKind() != TypeKind.NONE) {
        collectFields(MoreTypes.asDeclared(parent).asElement(), collection);
      }
      return collection;
    }

    public Set<String> names() {
      return fields.keySet();
    }

  }

  /**
   * Collects and caches list of getters for current type
   */
  private class Getters {
    private final Map<String, ExecutableElement> getters;

    private Getters() {
      List<? extends Element> members = protoclass.processing().getElementUtils().getAllMembers(getCachedTypeElement());
      Map<String, ExecutableElement> map = new LinkedHashMap<>();
      for (ExecutableElement executable: ElementFilter.methodsIn(members)) {
        if (isGetter(executable)) {
          String name = styles.forAccessor(executable.getSimpleName().toString()).raw;
          map.put(name, executable);
        }
      }

      this.getters = ImmutableMap.copyOf(map);
    }

    private ExecutableElement getter(String name) {
      ExecutableElement element = getters.get(name);
      if (element == null) {
        throw new IllegalArgumentException(String.format("Getter by name %s not found in %s", name, type.name()));
      }
      return element;
    }

    public Set<String> names() {
      return getters.keySet();
    }

    private boolean isGetter(ExecutableElement executable) {
      if (isJavaLangObject(executable.getEnclosingElement())) {
        return false;
      }

      String name = executable.getSimpleName().toString();
      boolean getterName = false;
      for (Naming naming: styles.scheme().get) {
        if (!naming.detect(name).isEmpty()) {
          getterName = true;
          break;
        }
      }
      boolean noArgs = executable.getParameters().isEmpty() && executable.getReturnType().getKind() != TypeKind.VOID;
      return getterName && noArgs;
    }

    private boolean isJavaLangObject(Element element) {
      return element instanceof TypeElement && ((TypeElement) element).getQualifiedName().contentEquals(Object.class.getName());
    }

  }

  void collect() {
    List<ValueAttribute> attributes = new ArrayList<>();
    // filter on common fields and getters (by name)
    for (String name: Sets.intersection(getters.names(), fields.names())) {
      attributes.add(toAttribute(getters.getter(name)));
    }

    type.attributes.addAll(attributes);
  }

  /**
   * Create attribute from JavaBean getter
   */
  private ValueAttribute toAttribute(ExecutableElement getter) {
    ValueAttribute attribute = new ValueAttribute();
    attribute.reporter = protoclass.report();
    attribute.returnType = getter.getReturnType();
    attribute.names = styles.forAccessor(getter.getSimpleName().toString());
    attribute.element = getter;
    attribute.containingType = type;
    attribute.isGenerateAbstract = true; // to be visible as marshalling attribute
    attribute.initAndValidate(null);
    return attribute;
  }


}
