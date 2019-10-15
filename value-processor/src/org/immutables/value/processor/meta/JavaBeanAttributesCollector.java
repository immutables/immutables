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
import javax.annotation.processing.ProcessingEnvironment;
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
 * Collects attributes by scanning for getters/setters/fields in a class definition.
 *
 * <p>Current logic is as follows:
 * <ol>
 *   <li>Collect getters (no-arg, non-static, non-void methods). Infer attribute names using JavaBean spec.</li>
 *   <li>Collect setters (single-arg, non-static, void methods). Infer attribute names using JavaBean spec.</li>
 *   <li>Collect (non-static) fields including from parent classes.
 *   Add alternative field names using primitive heuristics like underscore '_' removal or first letter decapitalization.
 *   </li>
 * </ol>
 * Intersection between getter / setter and field names is final attribute set. Created {@link ValueAttribute} will
 * point to field element. For now we don't allow "derived" attributes and require fields to be present.
 *
 * <p>It is important that attributes are serialized otherwise using criteria on non-marshalable attribute(s) doesn't
 * make sense (we may provide attribute renaming strategies in future).
 *
 * For each {@code getFoo} method (without parameters) create attribute {@code foo}.
 * @see AccessorAttributesCollector
 * @see <a href="https://www.oracle.com/technetwork/java/javase/documentation/spec-136004.html">JavaBeans spec</a>
 */
final class JavaBeanAttributesCollector {

  private final ValueType type;
  private final Proto.Protoclass protoclass;
  private final Fields fields;
  private final Getters getters;
  private final Setters setters;
  private final Styles styles;

  JavaBeanAttributesCollector(Proto.Protoclass protoclass, ValueType type) {
    this.type = Preconditions.checkNotNull(type, "type");
    this.protoclass = Preconditions.checkNotNull(protoclass, "protoclass");
    this.styles = new Styles(ImmutableStyleInfo.copyOf(protoclass.styles().style()).withGet("is*", "get*").withSet("set*"));
    this.fields = new Fields();
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
          for (String alt: alternativeNamesFor(field)) {
            map.put(alt, field);
          }
        }
      }

      this.fields = ImmutableMap.copyOf(map);
    }

    /**
     * List alternative names for a field
     */
    private Set<String> alternativeNamesFor(VariableElement element) {
      String name = element.getSimpleName().toString();
      Set<String> names = new LinkedHashSet<>();
      // add alternative field names for legacy code-generators
      if (name.length() > 1 && Character.isUpperCase(name.charAt(0)) && Character.isLowerCase(name.charAt(1))) {
        // replace Foo with foo
        names.add(Character.toLowerCase(name.charAt(0)) + name.substring(1));
      }

      if (name.length() > 1 && name.charAt(0) == '_') {
        // replace _foo with foo
        String altName = name.substring(1);
        names.add(altName);
      }

      if (name.length() == 1 && Character.isUpperCase(name.charAt(0))) {
        names.add(name.toLowerCase());
      }

      return names;
    }

    VariableElement field(String name) {
      Preconditions.checkArgument(names().contains(name), "Field by name %s not found in %s", name, type.name());
      return fields.get(name);
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
    private final TypeMirror boxedBooleanType;

    private Getters(Iterable<? extends ExecutableElement> methods) {
      ProcessingEnvironment processing = protoclass.environment().processing();
      this.boxedBooleanType = processing.getElementUtils().getTypeElement(Boolean.class.getCanonicalName()).asType();
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
      Preconditions.checkArgument(names().contains(name), "Getter by name %s not found in %s", name, type.name());
      return getters.get(name);
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

      // methods starting with "is" have to have boolean return type to be considered a valid JavaBean getter
      if (name.startsWith("is") && name.length() > "is".length() && !isBoolean(executable.getReturnType())) {
        return false;
      }

      return executable.getParameters().isEmpty()
              && executable.getReturnType().getKind() != TypeKind.VOID
              && executable.getModifiers().contains(Modifier.PUBLIC)
              && !executable.getModifiers().contains(Modifier.STATIC)
              && !executable.getModifiers().contains(Modifier.ABSTRACT);
    }

    private boolean isBoolean(TypeMirror type) {
      return type.getKind() == TypeKind.BOOLEAN || boxedBooleanType.equals(type);
    }

  }

  void collect() {
    List<ValueAttribute> attributes = new ArrayList<>();
    for (String name: Sets.intersection(fields.names(), Sets.intersection(getters.names(), setters.names()))) {
      attributes.add(toAttribute(name, fields.field(name)));
    }

    type.attributes.addAll(attributes);
  }

  /**
   * Create attribute from JavaBean getter
   */
  private ValueAttribute toAttribute(String name, Element element) {
    // expect field or method
    TypeMirror returnType = element.getKind().isField() ? element.asType() : MoreElements.asExecutable(element).getReturnType();
    ValueAttribute attribute = new ValueAttribute();
    attribute.reporter = protoclass.report();
    attribute.returnType = returnType;
    attribute.names = styles.forAccessorWithRaw(element.getSimpleName().toString(), name);
    attribute.element = element;
    attribute.containingType = type;
    attribute.isGenerateAbstract = true; // to be visible as marshalling attribute
    attribute.initAndValidate(null);
    return attribute;
  }

}
