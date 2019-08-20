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

package org.immutables.criteria.elasticsearch;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.NullNode;
import com.google.common.base.Preconditions;
import org.immutables.criteria.backend.ProjectedTuple;
import org.immutables.criteria.expression.Expression;
import org.immutables.criteria.expression.Expressions;
import org.immutables.criteria.expression.Path;
import org.immutables.criteria.expression.Query;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Parse result of a projection
 */
class ToTupleConverter implements JsonConverter<ProjectedTuple>  {

  private final Query query;
  private final List<ProjectedField> fields;

  ToTupleConverter(Query query, ObjectMapper mapper) {
    Preconditions.checkArgument(!query.projections().isEmpty(), "no projections defined");
    this.query = query;
    this.fields = query.projections().stream().map(e -> new ProjectedField(e, mapper)).collect(Collectors.toList());
  }

  @Override
  public ProjectedTuple convert(JsonNode node) {
    List<Object> values = new ArrayList<>();
    for (ProjectedField info: fields) {
      JsonNode value = node.path(info.pathAsString);
      if (value.isMissingNode()) {
        value = NullNode.getInstance();
      }

      values.add(info.convert(value));
    }

    return ProjectedTuple.of(query.projections(), values);
  }

  private static class ProjectedField {
    private final Path path;
    private final String pathAsString;
    private final JavaType javaType;
    private final ObjectMapper mapper;

    private ProjectedField(Expression expr, ObjectMapper mapper) {
      Path path = (Path) expr;
      this.path =  path;
      this.mapper = mapper;
      this.pathAsString = path.toStringPath();
      this.javaType = mapper.getTypeFactory().constructType(Expressions.returnType(path));
    }

    Object convert(JsonNode json) {
      return mapper.convertValue(json, javaType);
    }
  }

}
