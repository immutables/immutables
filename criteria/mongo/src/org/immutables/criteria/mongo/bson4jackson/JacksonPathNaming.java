/*
 * Copyright 2023 Immutables Authors and Contributors
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

package org.immutables.criteria.mongo.bson4jackson;

import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.introspect.BeanPropertyDefinition;
import com.fasterxml.jackson.databind.util.Annotations;
import com.google.common.annotations.Beta;
import org.immutables.criteria.backend.PathNaming;
import org.immutables.criteria.expression.Path;

import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Leverages Jackson Introspection API (like {@link com.fasterxml.jackson.databind.SerializationConfig}) to
 * derive correct names for each path element be it field or method.
 *
 * <p>This feature is experimental. The location of this class might change in future.</p>
 */
@Beta
public class JacksonPathNaming implements PathNaming {
  private final ObjectMapper mapper;

  public JacksonPathNaming(ObjectMapper mapper) {
    this.mapper = Objects.requireNonNull(mapper, "mapper");
  }

  @Override
  public String name(Path path) {
    List<String> names = new ArrayList<>();
    Type enclosingType = (Class<?>) path.root().element();
    for (Member member : path.members()) {
      JavaType javaType = mapper.getTypeFactory().constructType(enclosingType);
      if (javaType.isCollectionLikeType() || javaType.isTypeOrSubTypeOf(Optional.class)) {
        // Get element type (T) from Optional<T> or List<T>
        javaType = javaType.getBindings().getTypeParameters().get(0);
      }

      BeanDescription description = mapper.getSerializationConfig().introspect(javaType);
      if (description.findProperties().isEmpty()) {
        description = findImmutableClassDefinition(javaType);
      }
      Optional<BeanPropertyDefinition> def = Stream.of(description)
              .flatMap(x -> x.findProperties().stream())
              .filter(x -> x.getPrimaryMember() != null && member.equals(x.getPrimaryMember().getMember()))
              .findAny();

      String name = def.map(BeanPropertyDefinition::getName).orElse(member.getName());
      names.add(name);
      if (member instanceof Field) {
        enclosingType = ((Field) member).getGenericType();
      } else if (member instanceof Method) {
        enclosingType = ((Method) member).getGenericReturnType();
      } else {
        enclosingType = member.getDeclaringClass();
      }
    }

    return String.join(".", names);
  }

  /**
   * Uses {@link JsonSerialize} annotation to find actual immutable class implementation.
   */
  BeanDescription findImmutableClassDefinition(JavaType type) {
    Annotations annot = mapper.getSerializationConfig().introspect(type).getClassAnnotations();
    JsonSerialize ser = annot.get(JsonSerialize.class);
    JavaType newType = mapper.getTypeFactory().constructType(ser.as());
    return mapper.getSerializationConfig().introspect(newType);
  }
}
