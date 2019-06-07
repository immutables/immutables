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

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.immutables.criteria.expression.Expression;
import org.immutables.criteria.expression.ExpressionConverter;
import org.immutables.criteria.expression.Expressions;

import java.io.IOException;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.util.Objects;

final class Elasticsearch {

  private  Elasticsearch() {}

  static ExpressionConverter<String> converter(ObjectMapper mapper) {
    Objects.requireNonNull(mapper, "expression");

    return expression -> {
      if (Expressions.isNil(expression)) {
        return mapper.createObjectNode().toString();
      }
      ElasticsearchQueryVisitor visitor = new ElasticsearchQueryVisitor();
      final QueryBuilders.QueryBuilder builder = expression.accept(visitor);

      try (StringWriter writer = new StringWriter();
           JsonGenerator generator = mapper.getFactory().createGenerator(writer)) {
        generator.writeStartObject();
        generator.writeFieldName("query");
        QueryBuilders.constantScoreQuery(builder).writeJson(generator);
        generator.writeEndObject();
        generator.flush();
        return writer.toString();
      } catch (IOException e) {
        throw new UncheckedIOException(e);
      }
    };
  }

}
