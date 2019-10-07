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
import org.immutables.generator.Naming;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.util.ElementFilter;
import java.util.ArrayList;
import java.util.List;

/**
 * Collects attributes by searching for getters in a class definition.
 *
 * For each {@code getFoo} method (without parameters) create attribute {@code foo}.
 * @see AccessorAttributesCollector
 */
final class JavaBeanAttributesCollector {

  private final ValueType type;
  private final Reporter reporter;
  private final ProcessingEnvironment processing;
  private final Styles styles;

  JavaBeanAttributesCollector(Proto.Protoclass protoclass, ValueType type) {
    this.type = Preconditions.checkNotNull(type, "type");
    this.reporter = protoclass.report();
    this.styles = new Styles(ImmutableStyleInfo.copyOf(protoclass.styles().style()).withGet("is*", "get*"));
    this.processing = protoclass.processing();
  }

  private TypeElement getTypeElement() {
    return (TypeElement) type.element;
  }

  void collect() {
    TypeElement originalType = CachingElements.getDelegate(getTypeElement());
    List<ValueAttribute> attributes = new ArrayList<>();
    List<ExecutableElement> methods = ElementFilter.methodsIn(processing.getElementUtils().getAllMembers(originalType));
    for (ExecutableElement method : methods) {
      if (isGetter(method)) {
        ValueAttribute attribute = toAttribute(method);
        attributes.add(attribute);
      }
    }

    type.attributes.addAll(attributes);
  }

  /**
   * Create attribute from JavaBean getter
   */
  private ValueAttribute toAttribute(ExecutableElement getter) {
    ValueAttribute attribute = new ValueAttribute();
    attribute.reporter = reporter;
    attribute.returnType = getter.getReturnType();
    attribute.names = styles.forAccessor(getter.getSimpleName().toString());
    attribute.element = getter;
    attribute.containingType = type;
    attribute.isGenerateAbstract = true; // to be visible as marshalling attribute
    attribute.initAndValidate(null);
    return attribute;
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

  private static boolean isJavaLangObject(Element element) {
    return element instanceof TypeElement && ((TypeElement) element).getQualifiedName().contentEquals(Object.class.getName());
  }

}
