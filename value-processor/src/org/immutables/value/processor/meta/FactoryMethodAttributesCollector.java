/*
    Copyright 2014 Immutables Authors and Contributors

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

import com.google.common.base.Functions;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import java.util.List;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import org.immutables.value.processor.meta.Proto.Protoclass;

final class FactoryMethodAttributesCollector {
  private final Protoclass protoclass;
  private final ValueType type;
  private final List<ValueAttribute> attributes = Lists.newArrayList();
  private final Styles styles;
  private final Reporter reporter;

  FactoryMethodAttributesCollector(Protoclass protoclass, ValueType type) {
    this.protoclass = protoclass;
    this.styles = protoclass.styles();
    this.type = type;
    this.reporter = protoclass.report();
  }

  void collect() {
    ExecutableElement factoryMethodElement = (ExecutableElement) protoclass.sourceElement();

    for (VariableElement parameter : factoryMethodElement.getParameters()) {
      TypeMirror returnType = parameter.asType();

      ValueAttribute attribute = new ValueAttribute();
      attribute.isGenerateAbstract = true;
      attribute.reporter = reporter;
      attribute.returnType = returnType;

      attribute.element = parameter;
      String parameterName = parameter.getSimpleName().toString();
      attribute.names = styles.forAccessorWithRaw(parameterName, parameterName);

      attribute.containingType = type;
      attributes.add(attribute);
    }

    for (ValueAttribute attribute : attributes) {
      attribute.initAndValidate();
    }

    type.attributes.addAll(attributes);
    type.throwing = extractThrowsClause(factoryMethodElement);
  }

  private static ImmutableList<String> extractThrowsClause(ExecutableElement factoryMethodElement) {
    return FluentIterable.from(factoryMethodElement.getThrownTypes())
        .transform(Functions.toStringFunction())
        .toList();
  }
}
