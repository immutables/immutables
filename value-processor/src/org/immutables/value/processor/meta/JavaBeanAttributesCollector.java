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

import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.util.ElementFilter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
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
  private final Setters setters;
  private final Styles styles;

  JavaBeanAttributesCollector(Proto.Protoclass protoclass, ValueType type) {
    this.type = Preconditions.checkNotNull(type, "type");
    this.protoclass = Preconditions.checkNotNull(protoclass, "protoclass");
    this.styles = new Styles(ImmutableStyleInfo.copyOf(protoclass.styles().style()).withGet("is*", "get*").withSet("set*"));
    List<? extends ExecutableElement> members = ElementFilter.methodsIn(protoclass.processing().getElementUtils().getAllMembers(getCachedTypeElement()));
    this.getters = new Getters(members);
    this.setters = new Setters(members);
  }

  /**
   * Checks if current element is {@link Object}. Usually no processing is necessary
   * for top-level {@code Object} class.
   */
  private static boolean isJavaLangObject(Element element) {
    return MoreElements.isType(element) && MoreElements.asType(element).getQualifiedName().contentEquals(Object.class.getName());
  }

  private TypeElement getCachedTypeElement() {
    return CachingElements.getDelegate(MoreElements.asType(type.element));
  }

  private class Setters {
    private final Map<String, ExecutableElement> setters;

    private Setters(Iterable<? extends ExecutableElement> methods) {
      Map<String, ExecutableElement> map = new LinkedHashMap<>();
      for (ExecutableElement executable: methods) {
        if (isSetter(executable)) {
          String name = styles.scheme().set.requireJavaBeanConvention().detect(executable.getSimpleName().toString());
          map.put(name, executable);
        }
      }

      this.setters = ImmutableMap.copyOf(map);
    }

    Set<String> names() {
      return setters.keySet();
    }

    private boolean isSetter(ExecutableElement executable) {
      if (isJavaLangObject(executable.getEnclosingElement())) {
        return false;
      }

      Naming set = styles.scheme().set;
      boolean notASetter = set.detect(executable.getSimpleName().toString()).isEmpty();
      if (notASetter) {
        return false;
      }

      return !executable.getModifiers().contains(Modifier.STATIC)
             && executable.getModifiers().contains(Modifier.PUBLIC)
             && executable.getParameters().size() == 1
             && executable.getReturnType().getKind() == TypeKind.VOID;
    }
  }

  /**
   * Collects and caches list of getters for current type
   */
  private class Getters {
    private final Map<String, ExecutableElement> getters;

    private Getters(Iterable<? extends ExecutableElement> methods) {
      Map<String, ExecutableElement> map = new LinkedHashMap<>();
      for (ExecutableElement executable: methods) {
        if (isGetter(executable)) {
          String name = javaBeanAttributeName(executable.getSimpleName().toString());
          map.put(name, executable);
        }
      }

      this.getters = ImmutableMap.copyOf(map);
    }

    /**
     * Get attribute name from java bean getter. Some examples:
     * <pre>
     *   getA -> a
     *   getAB -> AB (fist 2 chars are uppercase)
     *   getABC -> ABC (fist 2 chars are uppercase)
     *   getAb -> ab
     *   getAbc -> abc
     *   getFoo -> foo
     * </pre>
     *
     * <p>See 8.8 Capitalization of inferred names in
     * <a href="https://download.oracle.com/otndocs/jcp/7224-javabeans-1.01-fr-spec-oth-JSpec/">javabean spec</a>
     */
    private String javaBeanAttributeName(String raw) {
      for (Naming naming: styles.scheme().get) {
        String detected = naming.requireJavaBeanConvention().detect(raw);
        if (!detected.isEmpty()) {
          return detected;
        }
      }

      throw new IllegalArgumentException(String.format("%s it not a getter in %s", raw, type.name()));
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

    /**
     * Checks if {@code executable} follows JavaBean convention for getter methods (like {@code getFoo})
     */
    private boolean isGetter(ExecutableElement executable) {
      if (isJavaLangObject(executable.getEnclosingElement())) {
        return false;
      }

      String name = executable.getSimpleName().toString();
      boolean isGetterName = false;
      for (Naming naming: styles.scheme().get) {
        if (!naming.detect(name).isEmpty()) {
          isGetterName = true;
          break;
        }
      }

      if (!isGetterName) {
        return false;
      }

      boolean isGetterSignature = executable.getParameters().isEmpty()
              && executable.getReturnType().getKind() != TypeKind.VOID
              && executable.getModifiers().contains(Modifier.PUBLIC)
              && !executable.getModifiers().contains(Modifier.STATIC)
              && !executable.getModifiers().contains(Modifier.ABSTRACT);

      return isGetterSignature;
    }

  }

  void collect() {
    List<ValueAttribute> attributes = new ArrayList<>();
    for (String name: Sets.intersection(getters.names(), setters.names())) {
      attributes.add(toAttribute(name, getters.getter(name)));
    }

    type.attributes.addAll(attributes);
  }

  /**
   * Create attribute from JavaBean getter
   */
  private ValueAttribute toAttribute(String name, ExecutableElement getter) {
    ValueAttribute attribute = new ValueAttribute();
    attribute.reporter = protoclass.report();
    attribute.returnType = getter.getReturnType();
    attribute.names = styles.forAccessorWithRaw(getter.getSimpleName().toString(), name);
    attribute.element = getter;
    attribute.containingType = type;
    attribute.isGenerateAbstract = true; // to be visible as marshalling attribute
    attribute.initAndValidate(null);
    return attribute;
  }

}
