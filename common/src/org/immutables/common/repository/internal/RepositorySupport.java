/*
    Copyright 2013-2014 Immutables.org authors

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
package org.immutables.common.repository.internal;

import com.fasterxml.jackson.core.JsonGenerator;
import com.google.common.base.Function;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.collect.BoundType;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Range;
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
import org.immutables.common.repository.Repositories;
import org.immutables.common.repository.internal.ConstraintSupport.ConstraintVisitor;

/**
 * Routines and classes used by generated code and bridging code in {@link Repositories}
 */
public final class RepositorySupport {
  private RepositorySupport() {}

  public static DBObject extractDbObject(final ConstraintSupport.ConstraintHost fields) {
    if (fields instanceof UnmarshalableWrapper) {
      return BsonEncoding.unwrapJsonable((UnmarshalableWrapper) fields);
    }
    BasicDBObject asDbObject = fields.accept(new ConstraintBuilder("")).asDbObject();
    return asDbObject;
  }

  public static String stringify(final ConstraintSupport.ConstraintHost constraints) {
    return extractDbObject(constraints).toString();
  }

  @NotThreadSafe
  public static class ConstraintBuilder implements ConstraintSupport.ConstraintVisitor<ConstraintBuilder> {

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
      @Nullable
      Object existingConstraint = constraints.get(path);
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
        { QueryOperators.LT, QueryOperators.LTE },
        { QueryOperators.GT, QueryOperators.GTE }
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
    public ConstraintBuilder nested(String name, ConstraintSupport.ConstraintHost nestedConstraints) {
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

  public static Object wrapString(String query, Object... parameters) {
    return new UnmarshalableWrapper(query, parameters);
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

    if (value instanceof ConstraintSupport.ConstraintHost) {
      return extractDbObject((ConstraintSupport.ConstraintHost) value);
    }

    if (value == null
        || value instanceof Number
        || value instanceof Boolean
        || value instanceof String) {
      return value;
    }

    if (value instanceof MarshalableWrapper) {
      return BsonEncoding.unwrapBsonable((MarshalableWrapper) value);
    }

    return String.valueOf(value);
  }

  public static class UnmarshalableWrapper implements ConstraintSupport.ConstraintHost {
    private final String value;
    private final Object[] parameters;

    public UnmarshalableWrapper(String value, Object[] parameters) {
      this.value = value;
      this.parameters = parameters;
    }

    @Override
    public String toString() {
      return parameters.length == 0
          ? value
          : String.format(value, parameters);
    }

    @Override
    public <V extends ConstraintVisitor<V>> V accept(V visitor) {
      throw new UnsupportedOperationException();
    }
  }

  public static abstract class MarshalableWrapper implements Comparable<MarshalableWrapper> {
    private final Object value;

    protected MarshalableWrapper(Object value) {
      this.value = value;
    }

    protected abstract void marshalWrapped(JsonGenerator generator) throws IOException;

    @SuppressWarnings("unchecked")
    @Override
    public int compareTo(MarshalableWrapper o) {
      return ((Comparable<Object>) value).compareTo(o.value);
    }

    @Override
    public String toString() {
      return Objects.toStringHelper("MarshalableWrapper")
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
