/*
 * Copyright 2022 Immutables Authors and Contributors
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
package org.immutables.criteria.sql.generator;

import org.immutables.criteria.sql.SQL;
import org.immutables.generator.AbstractTemplate;
import org.immutables.generator.Generator;
import org.immutables.value.Value;

import javax.lang.model.element.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Generator.Template
@Generator.Import({
    TypeElement.class,
    PackageElement.class
})
public class Sql extends AbstractTemplate {
  static String tableName(TypeElement e) {
    final SQL.Table annotation = e.getAnnotation(SQL.Table.class);
    if (annotation == null || annotation.value().isEmpty()) {
      throw new UnsupportedOperationException(String.format("%s.name annotation is not defined on %s",
          SQL.Table.class.getSimpleName(), e.getSimpleName()));
    }
    return annotation.value();

  }

  static Set<ColumnMapping> columns(TypeElement e) {
    return e.getEnclosedElements().stream()
        .filter(x -> x instanceof ExecutableElement)
        .map(x -> (ExecutableElement) x)
        .filter(x -> !(x.getReturnType().toString().equals("Void") || x.getParameters().size() > 0))
        .map(Sql::column)
        .collect(Collectors.toSet());
  }

  static ColumnMapping column(ExecutableElement e) {
    String name = e.getSimpleName().toString();
    String property_type = e.getReturnType().toString();
    String column_type = e.getReturnType().toString();
    AnnotationMirror column = getAnnotation(e, SQL.Column.class);
    if (column != null) {
      AnnotationValue alias = value(column, "name");
      AnnotationValue cast = value(column, "type");
      name = alias != null ? String.valueOf(alias.getValue()) : name;
      column_type = cast != null ? String.valueOf(cast.getValue()) : e.getReturnType().toString();
    }
    return ImmutableColumnMapping.builder()
        .element(e)
        .method(e.getSimpleName().toString())
        .column(name)
        .property_type(property_type)
        .column_type(column_type)
        .build();
  }

  static AnnotationValue value(AnnotationMirror a, String name) {
    for (ExecutableElement e : a.getElementValues().keySet()) {
      if (e.getSimpleName().toString().equals(name)) {
        return a.getElementValues().get(e);
      }
    }
    return null;
  }

  static AnnotationMirror getAnnotation(Element e, Class type) {
    for (AnnotationMirror m : e.getAnnotationMirrors()) {
      if (m.getAnnotationType().asElement().getSimpleName().toString().equals(type.getSimpleName()))
        return m;
    }
    return null;
  }

  public List<ClassMapping> mappings() {
    List<ClassMapping> ret = new ArrayList<>();
    for (TypeElement e : elements()) {
      ret.add(ImmutableClassMapping.builder()
          .table(tableName(e))
          .element(e)
          .columns(columns(e))
          .build());
    }
    return ret;
  }

  public Set<TypeElement> elements() {
    return round().getElementsAnnotatedWith(SQL.Table.class)
        .stream()
        .filter(e -> e instanceof TypeElement)
        .map(e -> (TypeElement) e)
        .collect(Collectors.toSet());
  }

  @Value.Immutable
  public interface ClassMapping {
    String table();

    default String mappingClassName() {
      return element().getSimpleName().toString();
    }

    TypeElement element();

    Set<ColumnMapping> columns();
  }

  @Value.Immutable
  public interface ColumnMapping {
    ExecutableElement element();

    String column();

    String method();

    String property_type();

    String column_type();
  }
}
