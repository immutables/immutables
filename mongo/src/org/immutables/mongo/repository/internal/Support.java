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
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonWriter;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.QueryOperators;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.regex.Pattern;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import org.bson.BSONObject;
import org.immutables.mongo.repository.Repositories;
import org.immutables.mongo.repository.internal.Constraints.ConstraintVisitor;

/**
 * Routines and classes used by generated code and bridging code in {@link Repositories}
 */
public final class Support {
  private Support() {}

  public static DBObject extractDbObject(final Constraints.ConstraintHost fields) {
    if (fields instanceof JsonQuery) {
      return BsonEncoding.unwrapJsonable(((JsonQuery) fields).value);
    }
    return fields.accept(new ConstraintBuilder("")).asDbObject();
  }

  public static String stringify(final Constraints.ConstraintHost constraints) {
    if (constraints instanceof JsonQuery) {
      return ((JsonQuery) constraints).value;
    }
    return extractDbObject(constraints).toString();
  }

  @NotThreadSafe
  public static class ConstraintBuilder implements Constraints.ConstraintVisitor<ConstraintBuilder> {

    private final String keyPrefix;
    private BasicDBObject constraints;
    private List<BasicDBObject> disjunction;

    public ConstraintBuilder(String keyPrefix) {
      this(keyPrefix, new BasicDBObject());
    }

    private ConstraintBuilder(String keyPrefix, BasicDBObject constraints) {
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
          new BasicDBObject(
              negate ? QueryOperators.NIN : QueryOperators.IN,
              ImmutableSet.copyOf(unwrapBsonableIterable(values))));
      return this;
    }

    @Override
    public ConstraintBuilder equal(String name, boolean negate, @Nullable Object value) {
      addContraint(name, negate ? new BasicDBObject(QueryOperators.NE, unwrapBsonable(value)) : unwrapBsonable(value));
      return this;
    }

    @Override
    public ConstraintBuilder range(String name, boolean negate, Range<?> range) {

      if (range.hasLowerBound() && range.hasUpperBound()) {
        if (range.lowerEndpoint().equals(range.upperEndpoint()) && !range.isEmpty()) {
          equal(name, negate, range.lowerEndpoint());
        } else {
          BasicDBObject rangeObject = new BasicDBObject(2)
              .append(boundToOperator(true, false, range.lowerBoundType()), unwrapBsonable(range.lowerEndpoint()))
              .append(boundToOperator(false, false, range.upperBoundType()), unwrapBsonable(range.upperEndpoint()));

          addContraint(name, negateConstraint(negate, rangeObject));
        }

      } else if (range.hasLowerBound()) {
        BasicDBObject rangeObject =
            new BasicDBObject(
                boundToOperator(true, negate, range.lowerBoundType()),
                unwrapBsonable(range.lowerEndpoint()));

        addContraint(name, rangeObject);

      } else if (range.hasUpperBound()) {
        BasicDBObject rangeObject =
            new BasicDBObject(
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
      return negate ? new BasicDBObject(QueryOperators.NOT, constraint) : constraint;
    }

    public BasicDBObject asDbObject() {
      if (disjunction != null) {
        return new BasicDBObject(1).append(QueryOperators.OR, disjunction);
      }
      return constraints;
    }

    @Override
    public ConstraintBuilder size(String name, boolean negate, int size) {
      addContraint(name, negateConstraint(negate, new BasicDBObject(QueryOperators.SIZE, size)));
      return this;
    }

    @Override
    public ConstraintBuilder present(String name, boolean negate) {
      addContraint(name, new BasicDBObject(QueryOperators.EXISTS, !negate));
      return this;
    }

    @Override
    public ConstraintBuilder match(String name, boolean negate, Pattern pattern) {
      addContraint(name, negateConstraint(negate, pattern));
      return this;
    }

    @Override
    public ConstraintBuilder nested(String name, Constraints.ConstraintHost nestedConstraints) {
      constraints.putAll((BSONObject) nestedConstraints.accept(newBuilderForKey(name)).asDbObject());
      return this;
    }

    @Override
    public ConstraintBuilder disjunction() {
      if (disjunction == null) {
        disjunction = new ArrayList<>(4);
        disjunction.add(constraints);
      }
      constraints = new BasicDBObject();
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
    return new JsonQuery(query);
  }

  public static <T> Object writable(TypeAdapter<T> adapter, T value) {
    return new Adapted<>(adapter, value);
  }

  public static Object writable(Object value) {
    return value;
  }

  @SuppressWarnings("unchecked")
  public static <T extends Comparable<? super T>> Range<Comparable<Object>> writable(TypeAdapter<T> adapter, Range<T> range) {
    if (range.hasLowerBound() && range.hasUpperBound()) {
      return Range.range(
          (Comparable<Object>) writable(adapter, range.lowerEndpoint()),
          range.lowerBoundType(),
          (Comparable<Object>) writable(adapter, range.upperEndpoint()),
          range.upperBoundType());
    } else if (range.hasLowerBound()) {
      return Range.downTo(
          (Comparable<Object>) writable(adapter, range.lowerEndpoint()),
          range.lowerBoundType());
    } else if (range.hasUpperBound()) {
      return Range.upTo(
          (Comparable<Object>) writable(adapter, range.upperEndpoint()),
          range.upperBoundType());
    }
    throw new AssertionError();
  }

  @SuppressWarnings("unchecked")
  public static <T extends Comparable<? super T>> Range<Comparable<Object>> writable(Range<T> range) {
    return (Range<Comparable<Object>>) writable((Object) range);
  }

  public static Object unwrapBsonable(Object value) {
    if (value instanceof BasicDBObject) {
      for (Entry<String, Object> entry : ((BasicDBObject) value).entrySet()) {
        entry.setValue(unwrapBsonable(entry.getValue()));
      }
      return value;
    }

    if (value instanceof Iterable<?>) {
      return ImmutableList.copyOf(unwrapBsonableIterable((Iterable<?>) value));
    }

    if (value instanceof Constraints.ConstraintHost) {
      return extractDbObject((Constraints.ConstraintHost) value);
    }

    if (value == null
        || value instanceof Number
        || value instanceof Boolean
        || value instanceof String) {
      return value;
    }

    if (value instanceof Adapted<?>) {
      return BsonEncoding.unwrapBsonable((Adapted<?>) value);
    }

    return String.valueOf(value);
  }

  private static class JsonQuery implements Constraints.ConstraintHost {
    private final String value;

    JsonQuery(String value) {
      this.value = value;
    }

    @Override
    public String toString() {
      return value;
    }

    @Override
    public <V extends ConstraintVisitor<V>> V accept(V visitor) {
      throw new UnsupportedOperationException(
          "Satisfied ConstraintSupport.ConstraintHost only for technical reasons and don't implements accept");
    }
  }

  static final class Adapted<T> implements Comparable<Adapted<T>> {
    private final T value;
    private final TypeAdapter<T> adapter;

    Adapted(TypeAdapter<T> adapter, T value) {
      this.adapter = adapter;
      this.value = value;
    }

    void write(JsonWriter writer) throws IOException {
      adapter.write(writer, value);
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
    return new BasicDBObject();
  }

  public static Object bsonObjectAttribute(String name, Object value) {
    return new BasicDBObject(name, value);
  }
}
