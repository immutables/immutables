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

package org.immutables.criteria.jackson;

import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.introspect.BeanPropertyDefinition;
import org.immutables.criteria.backend.PathNaming;
import org.immutables.criteria.expression.Path;

import java.lang.reflect.Member;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

class JacksonPathNaming implements PathNaming {
  private final ObjectMapper mapper;

  public JacksonPathNaming(ObjectMapper mapper) {
    this.mapper = Objects.requireNonNull(mapper, "mapper");
  }

  @Override
  public String name(Path path) {
    List<String> names = new ArrayList<>();
    Type enclosing = (Class<?>) path.root().element();
    for (Member member : path.members()) {
      JavaType javaType = mapper.getTypeFactory().constructType(enclosing);

      BeanPropertyDefinition def = Stream.of(mapper.getSerializationConfig().introspect(javaType))
              .flatMap(x->((BeanDescription) x).findProperties().stream())
              .filter(x -> x.getPrimaryMember() != null && member.equals(x.getPrimaryMember().getMember()))
              .findAny()
              .orElseThrow(() ->new IllegalStateException(String.format("No jackson bean property found for member %s in path %s", member, path)));

      names.add(def.getName());
      enclosing = member.getDeclaringClass();
    }

    return String.join(".", names);
  }
}
