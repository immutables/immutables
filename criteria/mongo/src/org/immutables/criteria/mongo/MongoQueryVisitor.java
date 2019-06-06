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

package org.immutables.criteria.mongo;

import com.google.common.base.Preconditions;
import org.bson.BsonArray;
import org.bson.BsonDocument;
import org.bson.BsonDocumentWriter;
import org.bson.BsonNull;
import org.bson.BsonString;
import org.bson.BsonValue;
import org.bson.BsonWriter;
import org.bson.codecs.Encoder;
import org.bson.codecs.EncoderContext;
import org.bson.codecs.configuration.CodecRegistry;
import org.immutables.criteria.expression.Call;
import org.immutables.criteria.expression.Constant;
import org.immutables.criteria.expression.Expression;
import org.immutables.criteria.expression.ExpressionVisitor;
import org.immutables.criteria.expression.Operator;
import org.immutables.criteria.expression.Operators;
import org.immutables.criteria.expression.Path;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Generates mongo find using visitor API.
 */
class MongoQueryVisitor implements ExpressionVisitor<BsonValue> {

  private final CodecRegistry registry;

  MongoQueryVisitor(CodecRegistry registry) {
    this.registry = Objects.requireNonNull(registry, "registry");
  }

  @Override
  public BsonValue visit(Call call) {
    final Operator op = call.operator();
    final List<Expression> args = call.arguments();

    if (op == Operators.EQUAL || op == Operators.NOT_EQUAL) {
      Preconditions.checkArgument(args.size() == 2, "Size should be 2 for %s but was %s", op, args.size());
      Preconditions.checkArgument(args.get(0) instanceof Path, "first argument should be path access");
      Preconditions.checkArgument(args.get(1) instanceof Constant, "second argument should be constant");

      BsonValue field = args.get(0).accept(this);
      BsonValue value = args.get(1).accept(this);
      Preconditions.checkNotNull(field, "field");
      Preconditions.checkNotNull(value, "value");

      BsonDocument doc = new BsonDocument();
      doc.put(field.asString().getValue(), op == Operators.NOT_EQUAL ? new BsonDocument("$ne", value) : value);

      return doc;
    }

    if (op == Operators.AND || op == Operators.OR) {
      final BsonDocument doc = new BsonDocument();

      final BsonArray array = call.arguments().stream()
              .map(a -> a.accept(this))
              .collect(Collectors.toCollection(BsonArray::new));

      doc.put(op == Operators.AND ? "$and" : "$or", array);

      return doc;
    }

    throw new UnsupportedOperationException(String.format("Not yet supported (%s): %s", call.operator(), call));
  }

  @Override
  public BsonValue visit(Constant constant) {
    final Object value = constant.value();

    if (value == null) {
      return BsonNull.VALUE;
    }


    @SuppressWarnings("unchecked")
    final Encoder<Object> encoder = (Encoder<Object>) registry.get(value.getClass());

    final BsonDocument bson = new BsonDocument();
    final BsonWriter writer = new BsonDocumentWriter(bson);
    // Bson doesn't allow to write directly scalars / primitives, they have to be embedded in a
    // document.
    writer.writeStartDocument();
    writer.writeName("$");
    encoder.encode(writer, value, EncoderContext.builder().build());
    writer.writeEndDocument();
    writer.flush();
    return bson.get("$");
  }

  @Override
  public BsonString visit(Path path) {
    return new BsonString(path.toStringPath());
  }
}
