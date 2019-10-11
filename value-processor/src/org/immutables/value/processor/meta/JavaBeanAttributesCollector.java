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
    this.styles = new Styles(ImmutableStyleInfo.copyOf(protoclass.styles().style()).withGet("is*", "get*").withSet("set*"));
    this.getters = new Getters();
    this.fields = new Fields();
  }

  private static boolean isJavaLangObject(Element element) {
    return MoreElements.isType(element) && MoreElements.asType(element).getQualifiedName().contentEquals(Object.class.getName());
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
          String name = field.getSimpleName().toString();
          map.put(name, field);

          // add alternative field names for legacy code-generators
          if (Character.isUpperCase(name.charAt(0))) {
            // private String Foo (instead of lowercase foo)
            String altName = Character.toLowerCase(name.charAt(0)) + name.substring(1);
            map.put(altName, field);
          }

          if (name.charAt(0) == '_' && name.length() > 1) {
            // private String _foo
            String altName = name.substring(1);
            map.put(altName, field);
          }
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
          String name = javaBeanAttributeName(executable.getSimpleName().toString());
          map.put(name, executable);
        }
      }

      this.getters = ImmutableMap.copyOf(map);
    }

    /**
     * Handle special case when first 2 characters are upper case.
     *
     * <p>See 8.8 Capitalization of inferred names in
     * <a href="https://download.oracle.com/otndocs/jcp/7224-javabeans-1.01-fr-spec-oth-JSpec/">javabean spec</a>
     * <pre>
     * Thus when we extract a property or event name from the middle of an existing Java name, we
     * normally convert the first character to lower case. However to support the occasional use of all
     * upper-case names, we check if the first two characters of the name are both upper case and if
     * so leave it alone. So for example,
     * "FooBah" becomes "fooBah"
     * "Z" becomes "z"
     * "URL" becomes "URL"
     *</pre>
     */
    private String javaBeanAttributeName(String raw) {
      Preconditions.checkArgument(raw.startsWith("get") || raw.startsWith("is"), "%s doesn't start with 'get' or 'is'", raw);
      String name = raw.startsWith("get") ? raw.substring(3) : raw.substring(2);
      // for Java Beans if there are more than 2 uppercase letters attribute remains unchanged
      // "FooBah" becomes "fooBah" and "X" becomes "x", but "URL" stays as "URL"
      boolean allUpperCase = name.length() > 1 && Character.isUpperCase(name.charAt(0)) && Character.isUpperCase(name.charAt(1));
      if (allUpperCase) {
        return name;
      } else {
        return styles.forAccessor(raw).raw;
      }
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
    Sets.SetView<String> names = Sets.intersection(getters.names(), fields.names());
    for (String name: names) {
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
