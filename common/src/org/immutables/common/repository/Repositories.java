/*
   Copyright 2013-2014 Immutables Authors and Contributors

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
package org.immutables.common.repository;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.primitives.Ints;
import com.google.common.util.concurrent.Futures;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.WriteResult;
import java.util.List;
import java.util.concurrent.Callable;
import javax.annotation.Nonnegative;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import javax.annotation.concurrent.ThreadSafe;
import org.immutables.common.concurrent.FluentFuture;
import org.immutables.common.concurrent.FluentFutures;
import org.immutables.common.marshal.Marshaler;
import org.immutables.common.repository.internal.BsonEncoding;
import org.immutables.common.repository.internal.ConstraintSupport;
import org.immutables.common.time.TimeMeasure;
import static com.google.common.base.Preconditions.*;
import static org.immutables.common.repository.internal.RepositorySupport.*;

/**
 * Umbrella class which contains abstract super-types of repository and operation objects that
 * inherited by generated repositories. These base classes performs bridging to underlying MongoDB
 * java driver.
 */
public final class Repositories {
  private static final int LARGE_BATCH_SIZE = 2000;
  private static final int DEFAULT_EXPECTED_RESULT_SIZE = 500;

  private Repositories() {}

  /**
   * Call methods on Criteria to add constraint for search query.
   * As each constraint that is added, new immutable criteria created and returned. {@code Criteria}
   * objects are immutable so they can be passed along when needed in situations
   * such as when you need to separate how you choose documents from how you process them.
   * <p>
   * Constraints keeps adding up as using 'AND' condition and could be separated in 'OR' blocks
   * using {@link #or()} method, and ultimately, these blocks form a so called disjunctive normal
   * form. Such approach (DNF) was taken to achieve fine power/expressiveness balance in criteria
   * DSL embedded into Java language.
   */
  public static abstract class Criteria {
    /**
     * Returns chained criteria handle used to "OR" new constraint set to form logical DNF.
     * @return disjunction separated criteria handle
     */
    public abstract Criteria or();
  }

  /**
   * Base abstract class for repositories.
   * @param <T> type of document
   */
  @ThreadSafe
  public static abstract class Repository<T> {

    private final RepositorySetup configuration;
    private final String collectionName;
    private final Marshaler<T> marshaler;

    protected Repository(
        RepositorySetup configuration,
        String collectionName,
        Marshaler<T> marshaler) {
      this.configuration = checkNotNull(configuration);
      this.collectionName = checkNotNull(collectionName);
      this.marshaler = checkNotNull(marshaler);
    }

    private DBCollection collection() {
      return configuration.database.getCollection(collectionName);
    }

    private <V> FluentFuture<V> submit(Callable<V> callable) {
      return FluentFutures.from(configuration.executor.submit(callable));
    }

    private enum GetN implements Function<WriteResult, Integer> {
      FUNCTION;
      @Override
      public Integer apply(WriteResult input) {
        return input.getN();
      }
    }

    protected final FluentFuture<Void> doIndex(
        final ConstraintSupport.Constraint fields,
        final ConstraintSupport.Constraint options) {
      return submit(new Callable<Void>() {
        @Override
        public Void call() {
          collection().createIndex(
              extractDbObject(fields),
              extractDbObject(options));
          return null;
        }
      });
    }

    protected final FluentFuture<Integer> doInsert(final ImmutableList<T> documents) {
      if (documents.isEmpty()) {
        return FluentFutures.from(Futures.immediateFuture(0));
      }
      return submit(new Callable<WriteResult>() {
        @Override
        public WriteResult call() {
          DBCollection collection = collection();
          return collection.insert(
              BsonEncoding.wrapInsertObjectList(documents, marshaler),
              collection.getWriteConcern(),
              BsonEncoding.encoder());
        }
      }).lazyTransform(GetN.FUNCTION);
    }

    protected final FluentFuture<Optional<T>> doModify(
        final ConstraintSupport.ConstraintHost criteria,
        final ConstraintSupport.Constraint ordering,
        final ConstraintSupport.Constraint exclusion,
        final ConstraintSupport.Constraint update,
        final boolean upsert,
        final boolean newOrOld,
        final boolean remove) {
      checkArgument(!upsert || !remove);
      checkArgument(!remove || !newOrOld);
      checkNotNull(criteria);
      return submit(new Callable<Optional<T>>() {
        @Override
        public Optional<T> call() throws Exception {
          DBCollection collection = collection();

          @Nullable DBObject result = collection.findAndModify(
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

    protected final FluentFuture<Integer> doUpdate(
        final ConstraintSupport.ConstraintHost criteria,
        final ConstraintSupport.Constraint update,
        final boolean upsert,
        final boolean multiple) {
      checkArgument(!multiple || !upsert);
      checkNotNull(criteria);
      return submit(new Callable<WriteResult>() {
        @Override
        public WriteResult call() {
          DBCollection collection = collection();
          return collection.update(
              extractDbObject(criteria),
              extractDbObject(update),
              upsert,
              multiple,
              collection.getWriteConcern(),
              BsonEncoding.encoder());
        }
      }).lazyTransform(GetN.FUNCTION);
    }

    protected final FluentFuture<Integer> doDelete(
        final ConstraintSupport.ConstraintHost criteria) {
      checkNotNull(criteria);
      return submit(new Callable<WriteResult>() {
        @Override
        public WriteResult call() {
          DBCollection collection = collection();
          return collection.remove(
              extractDbObject(criteria),
              collection.getWriteConcern());
        }
      }).lazyTransform(GetN.FUNCTION);
    }

    protected final FluentFuture<Integer> doUpsert(
        final ConstraintSupport.ConstraintHost criteria,
        final T document) {
      checkNotNull(criteria);
      checkNotNull(document);
      return submit(new Callable<WriteResult>() {
        @Override
        public WriteResult call() {
          DBCollection collection = collection();
          return collection.update(
              extractDbObject(criteria),
              BsonEncoding.wrapUpdateObject(document, marshaler),
              true,
              false,
              collection.getWriteConcern(),
              BsonEncoding.encoder());
        }
      }).lazyTransform(GetN.FUNCTION);
    }

    protected final FluentFuture<List<T>> doFetch(
        final @Nullable ConstraintSupport.ConstraintHost criteria,
        final ConstraintSupport.Constraint ordering,
        final ConstraintSupport.Constraint exclusion,
        final @Nonnegative int skip,
        final @Nonnegative int limit) {
      return submit(new Callable<List<T>>() {
        @SuppressWarnings("resource")
        @Override
        public List<T> call() throws Exception {
          DBCollection collection = collection();

          @Nullable DBObject query = criteria != null ? extractDbObject(criteria) : null;
          @Nullable DBObject keys = !exclusion.isNil() ? extractDbObject(exclusion) : null;

          DBCursor cursor = collection.find(query, keys);

          if (!ordering.isNil()) {
            cursor.sort(extractDbObject(ordering));
          }

          cursor.skip(skip);

          int expectedSize = DEFAULT_EXPECTED_RESULT_SIZE;

          if (limit != 0) {
            cursor.limit(limit);
            expectedSize = Math.min(limit, expectedSize);
            if (limit <= LARGE_BATCH_SIZE) {
              // if limit specified and is smaller than reasonable large batch size
              // then we force batch size to be the same as limit,
              // but negative, this force cursor to close right after result is sent
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
  static abstract class Operation<T> {
    protected final Repository<T> repository;

    protected Operation(Repository<T> repository) {
      this.repository = repository;
    }
  }

  @NotThreadSafe
  static abstract class UpdatatingOperation<T> extends Operation<T> {
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
        ConstraintSupport.Constraint fields,
        String name,
        ConstraintSupport.Constraint setOfFields) {
      return !setOfFields.isNil() ? fields.equal(name, false, setOfFields) : fields;
    }
  }

  /**
   * Base updater.
   * @param <T> document type
   */
  @NotThreadSafe
  public static abstract class Updater<T> extends UpdatatingOperation<T> {
    protected Updater(Repository<T> repository) {
      super(repository);
    }

    /**
     * Perform upsert: update single element or inserts a new one if none of the document matches.
     * <p>
     * <em>Note: Upsert operation requires special care to set or init all required attributes in case of insertion
     * (including but not limited to '_id'), so that valid document could be inserted into collection.
     * </em>
     * @return future of number of processed document (expected to be 1)
     */
    public FluentFuture<Integer> upsert() {
      return repository.doUpdate(criteria, collectRequiredUpdate(), true, false);
    }

    /**
     * Updates a single document that matches.
     * @return number of updated documents. 0 or 1
     */
    public FluentFuture<Integer> updateFirst() {
      return repository.doUpdate(criteria, collectRequiredUpdate(), false, false);
    }

    /**
     * Updates all matching document.
     * @return future of number of updated document
     */
    public FluentFuture<Integer> updateAll() {
      return repository.doUpdate(criteria, collectRequiredUpdate(), false, true);
    }
  }

  /**
   * Provides base configuration methods and action methods to perform 'modify' step in
   * 'findAndModify' operation.
   * @see DBCollection#findAndModify(DBObject, DBObject, DBObject, boolean, DBObject, boolean,
   *      boolean)
   * @param <T> document type
   * @param <M> a self type of extended modifier class
   */
  @NotThreadSafe
  public static abstract class Modifier<T, M extends Modifier<T, M>> extends UpdatatingOperation<T> {
    protected ConstraintSupport.Constraint ordering = ConstraintSupport.nilConstraint();
    protected ConstraintSupport.Constraint exclusion = ConstraintSupport.nilConstraint();

    private boolean returnNewOrOld;

    protected Modifier(Repository<T> repository) {
      super(repository);
    }

    /**
     * Configures this modifier so that old (not updated) version of document will be returned in
     * case of successful update.
     * This is default behavior so it may be called only for explanatory reasons.
     * @see #returningNew()
     * @return {@code this} modifier for chained invocation
     */
    // safe unchecked: we expect I to be a self type
    @SuppressWarnings("unchecked")
    public final M returningOld() {
      returnNewOrOld = false;
      return (M) this;
    }

    /**
     * Configures this modifier so that new (updated) version of document will be returned in
     * case of successful update.
     * @see #returningOld()
     * @return {@code this} modifier for chained invocation
     */
    // safe unchecked: we expect I to be a self type
    @SuppressWarnings("unchecked")
    public final M returningNew() {
      returnNewOrOld = true;
      return (M) this;
    }

    /**
     * Performs an upsert. If query will match a document, then it will be modified and old or new
     * version of document returned (depending if {@link #returningNew()} was configured). When
     * there
     * isn't any such matching document, a new one will be created and returned if
     * {@link #returningNew()} was configured.
     * <p>
     * <em>Note: Upsert operation requires special care to set or init all required attributes
     * (including but not limited to '_id'), so that valid document could be inserted into collection.
     * </em>
     * @return future of optional document.
     * @see DBCollection#findAndModify(DBObject, DBObject, DBObject, boolean, DBObject, boolean,
     *      boolean)
     */
    public final FluentFuture<Optional<T>> upsert() {
      return repository.doModify(criteria, ordering, exclusion, collectRequiredUpdate(), true, returnNewOrOld, false);
    }

    /**
     * Performs an update. If query will match a document, then it will be modified and old or new
     * version of document returned (depending if {@link #returningNew()} was configured). When
     * there
     * isn't any matching document, {@link Optional#absent()} will be result of the operation.
     * @return future of optional document (present if matching document would be found)
     * @see DBCollection#findAndModify(DBObject, DBObject, DBObject, boolean, DBObject, boolean,
     *      boolean)
     */
    public final FluentFuture<Optional<T>> update() {
      return repository.doModify(criteria, ordering, exclusion, collectRequiredUpdate(), false, returnNewOrOld, false);
    }
  }

  /**
   * Base class for the indexer objects.
   * @param <T> document type
   * @param <I> a self type of extended indexer class
   */
  @NotThreadSafe
  public static abstract class Indexer<T, I extends Indexer<T, I>> extends Operation<T> {
    protected ConstraintSupport.Constraint fields = ConstraintSupport.nilConstraint();
    protected ConstraintSupport.Constraint options = ConstraintSupport.nilConstraint();

    protected Indexer(Repository<T> repository) {
      super(repository);
    }

    /**
     * Configures name for an index, that is otherwise will be auto-named by index fields.
     * @param indexName explicitly provided index name
     * @return {@code this} indexer for chained invocation indexer
     */
    // safe unchecked: we expect I to be a self type
    @SuppressWarnings("unchecked")
    public final I named(String indexName) {
      options = options.equal("name", false, indexName);
      return (I) this;
    }

    /**
     * Makes an index to enforce unique constraint.
     * @return {@code this} indexer for chained invocation
     */
    // safe unchecked: we expect I to be a self type
    @SuppressWarnings("unchecked")
    public final I unique() {
      options = options.equal("unique", false, true);
      return (I) this;
    }

    /**
     * Configures and TTL for an index. Creates an index on a time field, and document will be
     * removed when TTL will expire.
     * <p>
     * <em>Note: Care should be taken to configure TTL only on single time instant field</em>
     * @param timeToLive time to live for an object, non-zero time in seconds required.
     * @return {@code this} indexer for chained invocation
     */
    // safe unchecked: we expect I to be a self type
    @SuppressWarnings("unchecked")
    public final I expireAfterSeconds(TimeMeasure timeToLive) {
      Preconditions.checkArgument(timeToLive.toSeconds() < 1,
          "unsupported precision for TTL, non-zero time in seconds required");
      options = options.equal("expireAfterSeconds", false, Ints.checkedCast(timeToLive.toSeconds()));
      return (I) this;
    }

    /**
     * Creates configured index on a set of fields, if one does not already exist.
     * @see DBCollection#ensureIndex(DBObject, DBObject)
     * @return future of indexing operation, future value is insignificant ({@code null} typed as
     *         {@link Void})
     */
    public final FluentFuture<Void> ensure() {
      return repository.doIndex(fields, options);
    }
  }

  /**
   * Base class for the finder objects. Fetcher objects are used to configure query.
   * @param <T> document type
   * @param <F> a self type of extended finder class
   */
  @NotThreadSafe
  public static abstract class Finder<T, F extends Finder<T, F>> extends Operation<T> {
    private int numberToSkip;

    @Nullable
    protected ConstraintSupport.ConstraintHost criteria;
    protected ConstraintSupport.Constraint ordering = ConstraintSupport.nilConstraint();
    protected ConstraintSupport.Constraint exclusion = ConstraintSupport.nilConstraint();

    protected Finder(Repository<T> repository) {
      super(repository);
    }

    /**
     * Configures finder to skip a number of document. Useful for results pagination in
     * conjunction with {@link #fetchWithLimit(int) limiting}
     * @param numberToSkip number of documents to skip.
     * @return {@code this} finder for chained invocation
     */
    // safe unchecked: we expect F to be a self type
    @SuppressWarnings("unchecked")
    public F skip(@Nonnegative int numberToSkip) {
      checkArgument(numberToSkip >= 0, "number to skip cannot be negative");
      this.numberToSkip = numberToSkip;
      return (F) this;
    }

    /**
     * Fetches result list with at most as {@code limitSize} matching documents. It could
     * be used together with {@link #skip(int)} to paginate results.
     * <p>
     * Zero limit ({@code fetchWithLimit(0)}) is equivalent to {@link #fetchAll()}.
     * <p>
     * As an performance optimization, when limit is "not so large", then batch size will be set to
     * a negative limit: this forces a MongoDB to sent results in a single batch and immediately
     * closes cursor.
     * @param limitSize specify limit on the number of document in result.
     * @return future of matching document list
     */
    public final FluentFuture<List<T>> fetchWithLimit(@Nonnegative int limitSize) {
      checkArgument(limitSize >= 0, "limit cannot be negative");
      return repository.doFetch(criteria, ordering, exclusion, numberToSkip, limitSize);
    }

    /**
     * Fetches all matching documents list.
     * <p>
     * If number or results could be very large, then prefer to use {@link #fetchWithLimit(int)} to
     * always limit result to some large but reasonable size.
     * @return future of matching document list
     */
    public final FluentFuture<List<T>> fetchAll() {
      return fetchWithLimit(0);
    }

    /**
     * Fetches first matching document. If none of the documents matches, then
     * {@link Optional#absent()} will be returned.
     * @return future of optional matching document
     */
    public final FluentFuture<Optional<T>> fetchFirst() {
      return fetchWithLimit(1).transform(new Function<List<T>, Optional<T>>() {
        @Override
        public Optional<T> apply(List<T> input) {
          return FluentIterable.from(input).first();
        }
      });
    }

    /**
     * Delete all matching documents from the collection if they matches {@link Criteria}.
     * @return future of number of deleted documents if WriteConcern allows.
     */
    public FluentFuture<Integer> deleteAll() {
      checkState(numberToSkip == 0, "Cannot use .skip() with .deleteAll()");
      return repository.doDelete(criteria);
    }

    /**
     * Deletes and returns first matching document. Returns {@link Optional#absent()} if none
     * documents matches.
     * @return future of optional matching deleted document.
     */
    public FluentFuture<Optional<T>> deleteFirst() {
      checkState(numberToSkip == 0, "Cannot use .skip() with .deleteFirst()");
      return repository.doModify(
          criteria, ordering, exclusion, ConstraintSupport.nilConstraint(), false, false, true);
    }
  }
}
