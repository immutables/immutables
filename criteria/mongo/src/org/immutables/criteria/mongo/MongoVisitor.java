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
import org.immutables.criteria.constraints.Call;
import org.immutables.criteria.constraints.Expression;
import org.immutables.criteria.constraints.ExpressionVisitor;
import org.immutables.criteria.constraints.Literal;
import org.immutables.criteria.constraints.Operator;
import org.immutables.criteria.constraints.Operators;
import org.immutables.criteria.constraints.Path;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Generates mongo query using visitor API.
 */
class MongoVisitor implements ExpressionVisitor<BsonValue, Void> {

  private final CodecRegistry registry;

  MongoVisitor(CodecRegistry registry) {
    this.registry = Objects.requireNonNull(registry, "registry");
  }

  @Override
  public BsonValue visit(Call call, @Nullable Void context) {
    final Operator op = call.getOperator();
    final List<Expression> args = call.getArguments();

    if (op == Operators.EQUAL || op == Operators.NOT_EQUAL) {
      Preconditions.checkArgument(args.size() == 2, "Size should be 2 for %s but was %s", op, args.size());
      Preconditions.checkArgument(args.get(0) instanceof Path, "first argument should be path access");
      Preconditions.checkArgument(args.get(1) instanceof Literal, "second argument should be literal");

      BsonValue field = args.get(0).accept(this, null);
      BsonValue value = args.get(1).accept(this, null);
      Preconditions.checkNotNull(field, "field");
      Preconditions.checkNotNull(value, "value");

      BsonDocument doc = new BsonDocument();
      doc.put(field.asString().getValue(), op == Operators.NOT_EQUAL ? new BsonDocument("$ne", value) : value);

      return doc;
    }

    if (op == Operators.AND || op == Operators.OR) {
      final BsonDocument doc = new BsonDocument();

      final BsonArray array = call.getArguments().stream()
              .map(a -> a.accept(this, null))
              .collect(Collectors.toCollection(BsonArray::new));

      doc.put(op == Operators.AND ? "$and" : "$or", array);

      return doc;
    }

    throw new UnsupportedOperationException(String.format("Not yet supported (%s): %s", call.getOperator(), call));
  }

  @Override
  public BsonValue visit(Literal literal, @Nullable Void context) {
    final Object value = literal.value();

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
  public BsonString visit(Path path, @Nullable Void context) {
    return new BsonString(path.path());
  }
}
