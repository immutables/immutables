/*
   Copyright 2013-2015 Immutables Authors and Contributors

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
package org.immutables.mongo.repository.internal;

import com.google.common.base.Function;
import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import com.google.common.collect.BoundType;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Range;
import com.mongodb.QueryOperators;
import org.bson.BsonDocument;
import org.bson.BsonDocumentWriter;
import org.bson.BsonValue;
import org.bson.Document;
import org.bson.codecs.Encoder;
import org.bson.codecs.EncoderContext;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.conversions.Bson;
import org.immutables.mongo.repository.Repositories;
import org.immutables.mongo.repository.internal.Constraints.ConstraintVisitor;

import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Routines and classes used by generated code and bridging code in {@link Repositories}
 */
public final class Support {
  private Support() {}

  /**
   * Given a query / ordering clause will convert it to Bson representation.
   */
  public static Bson convertToBson(final Constraints.ConstraintHost fields) {
    if (fields instanceof JsonQuery) {
      return (Bson) fields;
    }
    return fields.accept(new ConstraintBuilder("")).asDocument();
  }

  public static String stringify(final Constraints.ConstraintHost constraints) {
    if (constraints instanceof JsonQuery) {
      return ((JsonQuery) constraints).toString();
    }
    return convertToBson(constraints).toString();
  }

  @NotThreadSafe
  public static class ConstraintBuilder implements Constraints.ConstraintVisitor<ConstraintBuilder> {

    private final String keyPrefix;
    private Document constraints;
    private List<Document> disjunction;

    ConstraintBuilder(String keyPrefix) {
      this(keyPrefix, new Document());
    }

    private ConstraintBuilder(String keyPrefix, Document constraints) {
      this.keyPrefix = keyPrefix;
      this.constraints = constraints;
    }

    private ConstraintBuilder newBuilderForKey(String key) {
      return new ConstraintBuilder(keyPrefix + "." + key);
    }

    private void addContraint(String name, Object constraint) {
      String path = keyPrefix.concat(name);
      @Nullable Object existingConstraint = constraints.get(path);
      if (existingConstraint != null) {
        constraints.put(path, mergeConstraints(path, constraint, existingConstraint));
      } else {
        constraints.put(path, constraint);
      }
    }

    @Override
    public ConstraintBuilder in(String name, boolean negate, Iterable<?> values) {
      addContraint(name,
          new Document(
              negate ? QueryOperators.NIN : QueryOperators.IN,
              ImmutableSet.copyOf(unwrapBsonableIterable(values))));
      return this;
    }

    @Override
    public ConstraintBuilder equal(String name, boolean negate, @Nullable Object value) {
      addContraint(name, negate ? new Document(QueryOperators.NE, unwrapBsonable(value)) : unwrapBsonable(value));
      return this;
    }

    @Override
    public ConstraintBuilder range(String name, boolean negate, Range<?> range) {

      if (range.hasLowerBound() && range.hasUpperBound()) {
        if (range.lowerEndpoint().equals(range.upperEndpoint()) && !range.isEmpty()) {
          equal(name, negate, range.lowerEndpoint());
        } else {
          Document rangeObject = new Document()
              .append(boundToOperator(true, false, range.lowerBoundType()), unwrapBsonable(range.lowerEndpoint()))
              .append(boundToOperator(false, false, range.upperBoundType()), unwrapBsonable(range.upperEndpoint()));

          addContraint(name, negateConstraint(negate, rangeObject));
        }

      } else if (range.hasLowerBound()) {
        Document rangeObject =
            new Document(
                boundToOperator(true, negate, range.lowerBoundType()),
                unwrapBsonable(range.lowerEndpoint()));

        addContraint(name, rangeObject);

      } else if (range.hasUpperBound()) {
        Document rangeObject =
            new Document(
                boundToOperator(false, negate, range.upperBoundType()),
                unwrapBsonable(range.upperEndpoint()));

        addContraint(name, rangeObject);
      }
      return this;
    }

    private String boundToOperator(boolean lower, boolean negate, BoundType lowerBoundType) {
      boolean closedBound = lowerBoundType == BoundType.CLOSED;
      return comparisonOperators[lower ^ negate ? 1 : 0][closedBound ^ negate ? 1 : 0];
    }

    private static final String[][] comparisonOperators = {
        {QueryOperators.LT, QueryOperators.LTE},
        {QueryOperators.GT, QueryOperators.GTE}
    };

    private Object negateConstraint(boolean negate, Object constraint) {
      return negate ? new Document(QueryOperators.NOT, constraint) : constraint;
    }

    public Document asDocument() {
      if (disjunction != null) {
        return new Document(QueryOperators.OR, disjunction);
      }
      return constraints;
    }

    @Override
    public ConstraintBuilder size(String name, boolean negate, int size) {
      addContraint(name, negateConstraint(negate, new Document(QueryOperators.SIZE, size)));
      return this;
    }

    @Override
    public ConstraintBuilder present(String name, boolean negate) {
      addContraint(name, new Document(QueryOperators.EXISTS, !negate));
      return this;
    }

    @Override
    public ConstraintBuilder match(String name, boolean negate, Pattern pattern) {
      addContraint(name, negateConstraint(negate, pattern));
      return this;
    }

    @Override
    public ConstraintBuilder nested(String name, Constraints.ConstraintHost nestedConstraints) {
      constraints.putAll(nestedConstraints.accept(newBuilderForKey(name)).asDocument());
      return this;
    }

    @Override
    public ConstraintBuilder disjunction() {
      if (disjunction == null) {
        disjunction = new ArrayList<>(4);
        disjunction.add(constraints);
      }
      constraints = new Document();
      disjunction.add(constraints);
      return this;
    }

    private Object mergeConstraints(String path, Object constraint, Object existingConstraint) {
      Preconditions.checkState(false,
          "Cannot add another contraint on '%s': %s. Existing: %s",
          path,
          constraint,
          existingConstraint);
      return constraint;
    }
  }

  public static Constraints.ConstraintHost jsonQuery(String query) {
    return new JsonQuery(Document.parse(query));
  }

  public static <T> Object writable(Encoder<T> encoder, T value) {
    return new Adapted<>(encoder, value);
  }

  public static Object writable(Object value) {
    return value;
  }

  @SuppressWarnings("unchecked")
  public static <T extends Comparable<? super T>> Range<Comparable<Object>> writable(Encoder<T> encoder, Range<T> range) {
    if (range.hasLowerBound() && range.hasUpperBound()) {
      return Range.range(
          (Comparable<Object>) writable(encoder, range.lowerEndpoint()),
          range.lowerBoundType(),
          (Comparable<Object>) writable(encoder, range.upperEndpoint()),
          range.upperBoundType());
    } else if (range.hasLowerBound()) {
      return Range.downTo(
          (Comparable<Object>) writable(encoder, range.lowerEndpoint()),
          range.lowerBoundType());
    } else if (range.hasUpperBound()) {
      return Range.upTo(
          (Comparable<Object>) writable(encoder, range.upperEndpoint()),
          range.upperBoundType());
    }
    throw new AssertionError();
  }

  @SuppressWarnings("unchecked")
  public static <T extends Comparable<? super T>> Range<Comparable<Object>> writable(Range<T> range) {
    return (Range<Comparable<Object>>) writable((Object) range);
  }

  public static Object unwrapBsonable(Object value) {
    if (value instanceof Document) {
      for (Entry<String, Object> entry : ((Document) value).entrySet()) {
        entry.setValue(unwrapBsonable(entry.getValue()));
      }
      return value;
    }

    if (value instanceof Iterable<?>) {
      return ImmutableList.copyOf(unwrapBsonableIterable((Iterable<?>) value));
    }

    if (value instanceof Constraints.ConstraintHost) {
      return convertToBson((Constraints.ConstraintHost) value);
    }

    if (value == null
        || value instanceof Number
        || value instanceof Boolean
        || value instanceof String) {
      return value;
    }

    if (value instanceof Adapted<?>) {
      return ((Adapted<?>) value).toBson();
    }

    return String.valueOf(value);
  }

  private static class JsonQuery implements Constraints.ConstraintHost, Bson {
    private final Document value;

    JsonQuery(Document value) {
      this.value = checkNotNull(value, "value");
    }

    @Override
    public String toString() {
      return value.toJson();
    }

    @Override
    public <V extends ConstraintVisitor<V>> V accept(V visitor) {
      throw new UnsupportedOperationException(
          "Satisfied ConstraintSupport.ConstraintHost only for technical reasons and don't implements accept");
    }

    @Override
    public <TDocument> BsonDocument toBsonDocument(Class<TDocument> tDocumentClass, CodecRegistry codecRegistry) {
      return value.toBsonDocument(tDocumentClass, codecRegistry);
    }
  }

  static final class Adapted<T> implements Comparable<Adapted<T>> {
    final T value;
    final Encoder<T> encoder;

    Adapted(Encoder<T> encoder, T value) {
      this.encoder = encoder;
      this.value = value;
    }

    BsonValue toBson() {
      BsonDocument bson = new BsonDocument();
      org.bson.BsonWriter writer = new BsonDocumentWriter(bson);
      // Bson doesn't allow to write directly scalars / primitives, they have to be embedded in a document.
      writer.writeStartDocument();
      writer.writeName("$");
      encoder.encode(writer, value, EncoderContext.builder().build());
      writer.writeEndDocument();
      writer.flush();
      return bson.get("$");
    }

    @SuppressWarnings("unchecked")
    @Override
    public int compareTo(Adapted<T> o) {
      return ((Comparable<T>) value).compareTo(o.value);
    }

    @Override
    public String toString() {
      return MoreObjects.toStringHelper(this)
          .addValue(value)
          .toString();
    }
  }

  static Iterable<?> unwrapBsonableIterable(Iterable<?> values) {
    return Iterables.transform(values, new Function<Object, Object>() {
      @Override
      public Object apply(Object input) {
        return unwrapBsonable(input);
      }
    });
  }

  public static Object emptyBsonObject() {
    return new Document();
  }

  public static Object bsonObjectAttribute(String name, Object value) {
    return new Document(name, value);
  }
}
