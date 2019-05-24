package org.immutables.criteria.elasticsearch;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.immutables.criteria.constraints.Expression;
import org.immutables.criteria.constraints.Expressions;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Objects;

final class Elasticsearch {

  private  Elasticsearch() {}


  static String toQuery(ObjectMapper mapper, Expression expression) throws IOException  {
    Objects.requireNonNull(expression, "expression");
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
    }
  }

}
