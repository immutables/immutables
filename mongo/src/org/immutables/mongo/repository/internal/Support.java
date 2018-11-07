/*
   Copyright 2013-2018 Immutables Authors and Contributors

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
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
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
import java.util.Collection;
import java.util.List;
import java.util.Map;
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

  /**
   * Builds bson with index definition. The difference with
   * {@link #convertToBson(Constraints.ConstraintHost)} is that this method fails on duplicate
   * fields. Currently only one index is allowed per field.
   * @see <a href="https://docs.mongodb.com/manual/indexes/">Mongo Indexes</a>
   */
  public static Bson convertToIndex(final Constraints.ConstraintHost fields) {
    final Document document = new Document();
    fields.accept(new ConvertToIndex(document));
    return document;
  }

  public static String stringify(final Constraints.ConstraintHost constraints) {
    if (constraints instanceof JsonQuery) {
      return constraints.toString();
    }
    return convertToBson(constraints).toString();
  }

  private static final class ConvertToIndex
      extends Constraints.AbstractConstraintVisitor<ConvertToIndex> {
    private final Document document;

    private ConvertToIndex(Document document) {
      this.document = document;
    }

    @Override
    public ConvertToIndex equal(String name, boolean negate, @Nullable Object value) {
      if (document.containsKey(name)) {
        throw new IllegalArgumentException(
            String.format("Attribute %s is not unique: %s", name, document.get(name)));
      }
      document.put(name, value);
      return this;
    }
  }

  @NotThreadSafe
  public static class ConstraintBuilder extends Constraints.AbstractConstraintVisitor<ConstraintBuilder> {

    private final String keyPrefix;

    private List<Document> conjunctions; // $and
    private final List<Document> disjunctions; // $or

    private ConstraintBuilder(String keyPrefix) {
      this.keyPrefix = Preconditions.checkNotNull(keyPrefix, "keyPrefix");
      this.conjunctions = new ArrayList<>();
      this.disjunctions = new ArrayList<>();
    }

    private ConstraintBuilder newBuilderForKey(String key) {
      return new ConstraintBuilder(keyPrefix + "." + key);
    }

    private void addContraint(String name, Object constraint) {
      final String path = keyPrefix.concat(name);
      conjunctions.add(new Document(path, constraint));
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
      final List<Document> result = new ArrayList<>(disjunctions);
      result.add(mergeConjunctions(conjunctions));

      return result.size() == 1 ? result.get(0) : new Document(QueryOperators.OR, result);

    }

    private Document mergeConjunctions(List<Document> conjunctions) {
      final Multimap<String, Document> merged = LinkedHashMultimap.create();

      for (Document doc : conjunctions) {
        Preconditions.checkState(doc.keySet().size() == 1, "Invalid constraint %s", doc);
        final String key = doc.keySet().iterator().next();
        merged.put(key, doc);
      }

      final Document result = new Document();

      for (Map.Entry<String, Collection<Document>> entry : merged.asMap().entrySet()) {
        Preconditions.checkState(!entry.getValue().isEmpty(), "empty constraint: %s", entry);

        if (entry.getValue().size() == 1) {
          result.putAll(entry.getValue().iterator().next());
        } else {
          result.putAll(new Document(QueryOperators.AND, entry.getValue()));
        }
      }

      return result;
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
      conjunctions.add(nestedConstraints.accept(newBuilderForKey(name)).asDocument());
      return this;
    }

    @Override
    public ConstraintBuilder disjunction() {
      disjunctions.add(mergeConjunctions(conjunctions));
      conjunctions = new ArrayList<>();
      return this;
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
  public static <T extends Comparable<? super T>> Range<Comparable<Object>> writable(
      Encoder<T> encoder,
      Range<T> range) {
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
      // Bson doesn't allow to write directly scalars / primitives, they have to be embedded in a
      // document.
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
