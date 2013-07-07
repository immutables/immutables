package org.immutables.common.repository;

import com.fasterxml.jackson.core.JsonGenerator;
import com.google.common.base.Function;
import com.google.common.base.Objects;
import com.google.common.base.Optional;
import com.google.common.collect.BoundType;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Range;
import com.google.common.primitives.Ints;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.QueryOperators;
import com.mongodb.WriteResult;
import java.io.IOException;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.Callable;
import java.util.regex.Pattern;
import javax.annotation.Nonnegative;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import javax.annotation.concurrent.ThreadSafe;
import org.bson.BSONObject;
import org.immutables.common.marshal.Marshaler;
import org.immutables.common.repository.ConstraintSupport.Constraint;
import org.immutables.common.repository.ConstraintSupport.ConstraintHost;
import org.immutables.common.repository.ConstraintSupport.ConstraintVisitor;
import static com.google.common.base.Preconditions.*;

public final class RepositorySupport {
  private static final int LARGE_BATCH_SIZE = 2000;
  private static final int DEFAULT_EXPECTED_RESULT_SIZE = 500;

  private RepositorySupport() {
  }

  @ThreadSafe
  public static abstract class Repository<T> {

    private final RepositoryConfiguration configuration;
    private final String collectionName;
    private final Marshaler<T> marshaler;

    protected Repository(
        RepositoryConfiguration configuration,
        String collectionName,
        Marshaler<T> marshaler) {
      this.configuration = configuration;
      this.collectionName = collectionName;
      this.marshaler = marshaler;
    }

    private DBCollection collection() {
      return configuration.database.getCollection(collectionName);
    }

    protected final ListenableFuture<Void> doIndex(final Constraint fields, final Constraint options) {
      return configuration.executor.submit(new Callable<Void>() {
        @Override
        public Void call() {
          collection().ensureIndex(
              extractDbObject(fields),
              extractDbObject(options));
          return null;
        }
      });
    }

    protected final ListenableFuture<Integer> doInsert(final ImmutableList<T> documents) {
      return configuration.executor.submit(new Callable<Integer>() {
        @Override
        public Integer call() {
          DBCollection collection = collection();

          WriteResult result = collection.insert(
              BsonEncoding.wrapInsertObjectList(documents, marshaler),
              collection.getWriteConcern(),
              BsonEncoding.encoder());

          return result.getN();
        }
      });
    }

    protected final ListenableFuture<Optional<T>> doModify(
        final ConstraintHost criteria,
        final Constraint ordering,
        final Constraint exclusion,
        final Constraint update,
        final boolean upsert,
        final boolean newOrOld,
        final boolean remove) {
      checkArgument(!upsert || !remove);
      checkArgument(!remove || !newOrOld);
      return configuration.executor.submit(new Callable<Optional<T>>() {
        @Override
        public Optional<T> call() throws Exception {
          DBCollection collection = collection();

          @Nullable
          DBObject result = collection.findAndModify(
              extractDbObject(criteria),
              extractDbObject(exclusion),
              extractDbObject(ordering),
              remove,
              extractDbObject(update),
              newOrOld,
              upsert);

          if (result != null) {
            return Optional.of(BsonEncoding.unmarshalDbObject(result, marshaler));
          }

          return Optional.absent();
        }
      });
    }

    protected final ListenableFuture<Integer> doUpdate(
        final ConstraintHost criteria,
        final ConstraintSupport.Constraint update,
        final boolean upsert,
        final boolean multiple) {
      checkArgument(!multiple || !upsert);
      return configuration.executor.submit(new Callable<Integer>() {
        @Override
        public Integer call() {
          DBCollection collection = collection();

          WriteResult result = collection.update(
              extractDbObject(criteria),
              extractDbObject(update),
              upsert,
              multiple,
              collection.getWriteConcern(),
              BsonEncoding.encoder());

          return result.getN();
        }
      });
    }

    protected final ListenableFuture<Integer> doDelete(
        final ConstraintHost criteria) {
      return configuration.executor.submit(new Callable<Integer>() {
        @Override
        public Integer call() {
          DBCollection collection = collection();
          WriteResult result = collection.remove(
              extractDbObject(criteria),
              collection.getWriteConcern());

          return result.getN();
        }
      });
    }

    protected final ListenableFuture<Void> doUpsert(
        final ConstraintHost criteria,
        final T document) {
      return configuration.executor.submit(new Callable<Void>() {
        @Override
        public Void call() {
          DBCollection collection = collection();

          WriteResult result = collection.update(
              extractDbObject(criteria),
              BsonEncoding.wrapUpdateObject(document, marshaler),
              true,
              false,
              collection.getWriteConcern(),
              BsonEncoding.encoder());

          result.getN();
          return null;
        }
      });
    }

    protected final ListenableFuture<List<T>> doFetch(
        final @Nullable ConstraintHost criteria,
        final ConstraintSupport.Constraint ordering,
        final ConstraintSupport.Constraint exclusion,
        final @Nonnegative int skip,
        final @Nonnegative int limit) {
      return configuration.executor.submit(new Callable<List<T>>() {
        @Override
        public List<T> call() throws Exception {
          DBCollection collection = collection();

          @Nullable
          DBObject query = criteria != null ? extractDbObject(criteria) : null;
          @Nullable
          DBObject keys = !exclusion.isNil() ? extractDbObject(exclusion) : null;

          DBCursor cursor = collection.find(query, keys);

          if (!ordering.isNil()) {
            cursor.sort(extractDbObject(exclusion));
          }

          cursor.skip(skip);

          int expectedSize = DEFAULT_EXPECTED_RESULT_SIZE;

          if (limit != 0) {
            cursor.limit(limit);
            expectedSize = Math.min(limit, expectedSize);
            if (limit <= LARGE_BATCH_SIZE) {
              // if limit specified and is smaller than resonable large (but ok) batch size
              // then we force batch size to be the same as limit,
              // but negative, to force cursor close
              cursor.batchSize(-limit);
            }
          }

          cursor.setDecoderFactory(BsonEncoding.newResultDecoderFor(marshaler, expectedSize));

          List<DBObject> array = cursor.toArray();

          return BsonEncoding.unwrapResultObjectList(array);
        }
      });
    }
  }

  @NotThreadSafe
  public static abstract class Operation<T> {
    protected final Repository<T> repository;

    protected Operation(Repository<T> repository) {
      this.repository = repository;
    }
  }

  @NotThreadSafe
  public static abstract class UpdatatingOperation<T> extends Operation<T> {
    @Nullable
    protected ConstraintSupport.ConstraintHost criteria;
    protected ConstraintSupport.Constraint setFields = ConstraintSupport.nilConstraint();
    protected ConstraintSupport.Constraint setOnInsertFields = ConstraintSupport.nilConstraint();
    protected ConstraintSupport.Constraint incrementFields = ConstraintSupport.nilConstraint();
    protected ConstraintSupport.Constraint addToSetFields = ConstraintSupport.nilConstraint();
    protected ConstraintSupport.Constraint pushFields = ConstraintSupport.nilConstraint();
    protected ConstraintSupport.Constraint pullFields = ConstraintSupport.nilConstraint();
    protected ConstraintSupport.Constraint unsetFields = ConstraintSupport.nilConstraint();

    protected UpdatatingOperation(Repository<T> repository) {
      super(repository);
    }

    protected ConstraintSupport.Constraint collectRequiredUpdate() {
      ConstraintSupport.Constraint update = collectUpdate();
      checkState(!update.isNil());
      return update;
    }

    protected ConstraintSupport.Constraint collectUpdate() {
      ConstraintSupport.Constraint update = ConstraintSupport.nilConstraint();
      update = appendFields(update, "$set", setFields);
      update = appendFields(update, "$setOnInsert", setOnInsertFields);
      update = appendFields(update, "$inc", incrementFields);
      update = appendFields(update, "$addToSet", addToSetFields);
      update = appendFields(update, "$push", pushFields);
      update = appendFields(update, "$pull", pullFields);
      update = appendFields(update, "$unset", unsetFields);
      return update;
    }

    private ConstraintSupport.Constraint appendFields(
        ConstraintSupport.Constraint fields, String name, Constraint setOfFields) {
      return !setOfFields.isNil() ? fields.equal(name, false, setOfFields) : fields;
    }
  }

  @NotThreadSafe
  public static abstract class Updater<T> extends UpdatatingOperation<T> {
    protected Updater(Repository<T> repository) {
      super(repository);
    }

    public ListenableFuture<Integer> upsert() {
      return repository.doUpdate(criteria, collectRequiredUpdate(), true, false);
    }

    public ListenableFuture<Integer> updateFirst() {
      return repository.doUpdate(criteria, collectRequiredUpdate(), false, false);
    }

    public ListenableFuture<Integer> updateAll() {
      return repository.doUpdate(criteria, collectRequiredUpdate(), false, true);
    }
  }

  // We expect M to be a self type
  @SuppressWarnings("unchecked")
  @NotThreadSafe
  public static abstract class Modifier<T, M extends Modifier<T, M>> extends UpdatatingOperation<T> {
    protected ConstraintSupport.Constraint ordering = ConstraintSupport.nilConstraint();
    protected ConstraintSupport.Constraint exclusion = ConstraintSupport.nilConstraint();

    private boolean returnNewOrOld;

    protected Modifier(Repository<T> repository) {
      super(repository);
    }

    public M returnOld() {
      returnNewOrOld = false;
      return (M) this;
    }

    public M returnNew() {
      returnNewOrOld = true;
      return (M) this;
    }

    public ListenableFuture<Optional<T>> upsert() {
      return repository.doModify(criteria, ordering, exclusion, collectRequiredUpdate(), true, returnNewOrOld, false);
    }

    public ListenableFuture<Optional<T>> update() {
      return repository.doModify(criteria, ordering, exclusion, collectRequiredUpdate(), false, returnNewOrOld, false);
    }

    public ListenableFuture<Optional<T>> update(FutureCallback<T> callback) {
      ListenableFuture<Optional<T>> future = update();
      Futures.addCallback(future, new OptionalDereferencingFutureCallback<>(callback));
      return future;
    }

    public ListenableFuture<Optional<T>> upsert(FutureCallback<T> callback) {
      ListenableFuture<Optional<T>> future = upsert();
      Futures.addCallback(future, new OptionalDereferencingFutureCallback<>(callback));
      return future;
    }
  }

  // We expect I to be a self type
  @SuppressWarnings("unchecked")
  @NotThreadSafe
  public static abstract class Indexer<T, I extends Indexer<T, I>> extends Operation<T> {
    protected ConstraintSupport.Constraint fields = ConstraintSupport.nilConstraint();
    protected ConstraintSupport.Constraint options = ConstraintSupport.nilConstraint();

    protected Indexer(Repository<T> repository) {
      super(repository);
    }

    public final I named(String indexName) {
      options = options.equal("name", false, indexName);
      return (I) this;
    }

    public final I unique() {
      options = options.equal("unique", false, true);
      return (I) this;
    }

    public final I expireAfterSeconds(long ttlSeconds) {
      options = options.equal("expireAfterSeconds", false, Ints.checkedCast(ttlSeconds));
      return (I) this;
    }

    public final ListenableFuture<Void> ensure() {
      return repository.doIndex(fields, options);
    }
  }

  private static final class OptionalDereferencingFutureCallback<T> implements FutureCallback<Optional<T>> {
    private final FutureCallback<T> callback;

    OptionalDereferencingFutureCallback(FutureCallback<T> callback) {
      this.callback = callback;
    }

    @Override
    public void onSuccess(Optional<T> result) {
      try {
        callback.onSuccess(result.get());
      } catch (IllegalStateException ex) {
        onFailure(ex);
      }
    }

    @Override
    public void onFailure(Throwable t) {
      callback.onFailure(t);
    }
  }

  @NotThreadSafe
  public static abstract class Fetcher<T, F extends Fetcher<T, F>> extends Operation<T> {
    private int numberToSkip;

    @Nullable
    protected ConstraintSupport.ConstraintHost criteria;
    protected ConstraintSupport.Constraint ordering = ConstraintSupport.nilConstraint();
    protected ConstraintSupport.Constraint exclusion = ConstraintSupport.nilConstraint();

    protected Fetcher(Repository<T> repository) {
      super(repository);
    }

    // extenders should always substitute F with self type
    @SuppressWarnings("unchecked")
    public F skip(@Nonnegative int numberToSkip) {
      checkArgument(numberToSkip >= 0, "number to skip cannot be negative");
      this.numberToSkip = numberToSkip;
      return (F) this;
    }

    public final ListenableFuture<List<T>> fetchWithLimit(@Nonnegative int limit) {
      checkArgument(limit >= 0, "limit cannot be negative");
      return repository.doFetch(criteria, ordering, exclusion, numberToSkip, limit);
    }

    public final ListenableFuture<List<T>> fetchAll() {
      return fetchWithLimit(0);
    }

    public final ListenableFuture<Optional<T>> fetchFirst() {
      return Futures.transform(fetchWithLimit(1), new Function<List<T>, Optional<T>>() {
        @Override
        public Optional<T> apply(List<T> input) {
          return FluentIterable.from(input).first();
        }
      });
    }

    public final ListenableFuture<List<T>> fetchAll(FutureCallback<List<T>> callback) {
      ListenableFuture<List<T>> future = fetchAll();
      Futures.addCallback(future, callback);
      return future;
    }

    public final ListenableFuture<Optional<T>> fetchFirst(final FutureCallback<T> callback) {
      ListenableFuture<Optional<T>> future = fetchFirst();
      Futures.addCallback(future, new OptionalDereferencingFutureCallback<>(callback));
      return future;
    }

    public final Optional<T> getFirstUnchecked() {
      return Futures.getUnchecked(fetchFirst());
    }

    public final List<T> getAllUnchecked() {
      return Futures.getUnchecked(fetchAll());
    }

    public ListenableFuture<Optional<T>> deleteFirst() {
      checkState(numberToSkip == 0, "Cannot use skip() with .removeFirst()");
      return repository.doModify(
          criteria, ordering, exclusion, ConstraintSupport.nilConstraint(), false, false, true);
    }

    public ListenableFuture<Optional<T>> deleteFirst(FutureCallback<T> callback) {
      ListenableFuture<Optional<T>> future = deleteFirst();
      Futures.addCallback(future, new OptionalDereferencingFutureCallback<>(callback));
      return future;
    }
  }

  private static BasicDBObject extractDbObject(final ConstraintHost fields) {
    BasicDBObject asDbObject = fields.accept(new ConstraintBuilder("")).asDbObject();
    return asDbObject;
  }

  @NotThreadSafe
  public static class ConstraintBuilder implements ConstraintVisitor<ConstraintBuilder> {

    private final String keyPrefix;
    private BasicDBObject constraints = new BasicDBObject();

    public ConstraintBuilder(String keyPrefix) {
      this.keyPrefix = keyPrefix;
    }

    private ConstraintBuilder newBuilderForKey(String key) {
      return new ConstraintBuilder(keyPrefix + "." + key);
    }

    private void addContraint(String name, Object constraint) {
      String path = keyPrefix.concat(name);
      @Nullable
      Object existingConstraint = constraints.get(path);
      if (existingConstraint != null) {
        constraints.put(path, mergeConstraints(constraint, existingConstraint));
      } else {
        constraints.put(path, constraint);
      }
    }

    /**
     * Merge constraints.
     * @param constraint the constraint
     * @param existingConstraint the existing constraint
     * @return the object
     */
    private Object mergeConstraints(Object constraint, Object existingConstraint) {
      // TODO implement
      throw new UnsupportedOperationException();
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
    public ConstraintBuilder nested(String name, ConstraintHost nestedConstraints) {
      constraints.putAll((BSONObject) nestedConstraints.accept(newBuilderForKey(name)).asDbObject());
      return this;
    }

    @Override
    public ConstraintBuilder disjunction() {
      // TODO implement
      throw new UnsupportedOperationException();
    }
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

    if (value instanceof ConstraintHost) {
      return extractDbObject((ConstraintHost) value);
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
