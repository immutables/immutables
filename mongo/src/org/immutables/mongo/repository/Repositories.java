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
package org.immutables.mongo.repository;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.util.concurrent.Futures;
import com.google.gson.Gson;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.model.*;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nonnegative;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import javax.annotation.concurrent.ThreadSafe;
import org.bson.Document;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.conversions.Bson;
import org.immutables.mongo.bson4gson.GsonCodecs;
import org.immutables.mongo.concurrent.FluentFuture;
import org.immutables.mongo.concurrent.FluentFutures;
import org.immutables.mongo.repository.internal.Constraints;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static com.mongodb.client.model.Projections.fields;
import static com.mongodb.client.model.Projections.include;
import static org.immutables.mongo.repository.internal.Support.convertToBson;
import static org.immutables.mongo.repository.internal.Support.convertToIndex;

/**
 * Umbrella class which contains abstract super-types of repository and operation objects that
 * inherited by generated repositories. These base classes performs bridging to underlying MongoDB
 * java driver.
 */
public final class Repositories {
  private static final int LARGE_BATCH_SIZE = 2000;

  private Repositories() {}

  /**
   * Base abstract class for repositories.
   * @param <T> type of document
   */
  @ThreadSafe
  public static abstract class Repository<T> {

    private final RepositorySetup configuration;
    private final MongoCollection<T> collection;
    private final RepositorySetup.FieldNamingStrategy fieldNamingStrategy;

    protected Repository(
        RepositorySetup configuration,
        String collectionName,
        Class<T> type) {

      this.configuration = checkNotNull(configuration, "configuration");
      checkNotNull(collectionName, "collectionName");
      checkNotNull(type, "type");

      MongoCollection<T> collection =
          configuration.database.getCollection(collectionName, type);

      this.collection = collection.withCodecRegistry(configuration.codecRegistry);

      this.fieldNamingStrategy = configuration.fieldNamingStrategy;
    }

    /**
     * Codec used for current collection type.
     * @return registry
     */
    protected final CodecRegistry codecRegistry() {
      return collection.getCodecRegistry();
    }

    protected final RepositorySetup.FieldNamingStrategy fieldNamingStrategy() {
      return fieldNamingStrategy;
    }

    protected final MongoCollection<T> collection() {
      return collection;
    }

    private <V> FluentFuture<V> submit(Callable<V> callable) {
      return FluentFutures.from(configuration.executor.submit(callable));
    }

    protected final FluentFuture<Void> doIndex(
        final Constraints.Constraint fields,
        final IndexOptions options) {

      return submit(new Callable<Void>() {
        @Override
        public Void call() {
          collection().createIndex(convertToIndex(fields), options);
          return null;
        }
      });
    }

    protected final FluentFuture<Integer> doInsert(final ImmutableList<T> documents) {
      if (documents.isEmpty()) {
        return FluentFutures.from(Futures.immediateFuture(0));
      }
      return submit(new Callable<Integer>() {
        @Override
        public Integer call() {
          collection().insertMany(documents);
          return 0; // java driver for mongo returns 0 anyway
        }
      });
    }

    protected final FluentFuture<Optional<T>> doReplace(
            final Constraints.ConstraintHost criteria,
            final T document,
            final FindOneAndReplaceOptions options) {

      checkNotNull(criteria, "criteria");
      checkNotNull(document, "document");
      checkNotNull(options, "options");

      return submit(new Callable<Optional<T>>() {
        @Override
        public Optional<T> call() throws Exception {
          @Nullable T result = collection().findOneAndReplace(
              convertToBson(criteria), // query
              document,
              options);

          return Optional.fromNullable(result);
        }
      });

    }

    protected final FluentFuture<Optional<T>> doModify(
            final Constraints.ConstraintHost criteria,
            final Constraints.Constraint update,
            final FindOneAndUpdateOptions options) {

      checkNotNull(criteria, "criteria");
      checkNotNull(update, "update");

      return submit(new Callable<Optional<T>>() {
        @Override
        public Optional<T> call() throws Exception {
          @Nullable T result = collection().findOneAndUpdate(
                  convertToBson(criteria),
                  convertToBson(update),
                  options);

          return Optional.fromNullable(result);
        }
      });
    }

    protected final FluentFuture<Optional<T>> doFindOneAndDelete(
            final Constraints.ConstraintHost criteria,
            final FindOneAndDeleteOptions options) {

      checkNotNull(criteria, "criteria");
      checkNotNull(options, "options");

      return submit(new Callable<Optional<T>>() {
        @Override
        public Optional<T> call() throws Exception {
          @Nullable T result = collection().findOneAndDelete(convertToBson(criteria), options);
          return Optional.fromNullable(result);
        }
      });
    }

    protected final FluentFuture<Integer> doUpdateFirst(
            final Constraints.ConstraintHost criteria,
            final Constraints.Constraint update,
            final FindOneAndUpdateOptions options
    ) {
      checkNotNull(criteria, "criteria");
      checkNotNull(update, "update");
      checkNotNull(options, "options");

      return submit(new Callable<Integer>() {
        @Override
        public Integer call() {
          T result = collection().findOneAndUpdate(
                  convertToBson(criteria),
                  convertToBson(update),
                  options);

          return result == null ? 0 : 1;
        }
      });
    }

    protected final FluentFuture<Integer> doUpdate(
            final Constraints.ConstraintHost criteria,
            final Constraints.Constraint update,
            final UpdateOptions options) {

      checkNotNull(criteria, "criteria");
      checkNotNull(update, "update");
      checkNotNull(options, "options");

      return submit(new Callable<UpdateResult>() {
        @Override
        public UpdateResult call() {
          return collection()
              .updateMany(
              convertToBson(criteria),
              convertToBson(update),
              options);
        }
      }).lazyTransform(new Function<UpdateResult, Integer>() {
        @Override
        public Integer apply(UpdateResult input) {
          return (int) input.getModifiedCount();
        }
      });
    }

    protected final FluentFuture<Integer> doDelete(
        final Constraints.ConstraintHost criteria) {
      checkNotNull(criteria);
      return submit(new Callable<DeleteResult>() {
        @Override
        public DeleteResult call() {
          return collection().deleteMany(convertToBson(criteria));
        }
      }).lazyTransform(new Function<DeleteResult, Integer>() {
        @Override
        public Integer apply(DeleteResult input) {
          return (int) input.getDeletedCount();
        }
      });
    }


    protected final FluentFuture<Integer> doUpsert(
        final Constraints.ConstraintHost criteria,
        final T document) {
      checkNotNull(criteria, "criteria");
      checkNotNull(document, "document");
      return submit(new Callable<Integer>() {
        @Override
        public Integer call() {
          collection().replaceOne(convertToBson(criteria), document, new UpdateOptions().upsert(true));
          // upsert will always return 1:
          // if document doesn't exists, it will be inserted (modCount == 1)
          // if document exists, it will be updated (modCount == 1)

          return 1;
        }
      });
    }

    protected final FluentFuture<List<T>> doFetch(
        final @Nullable Constraints.ConstraintHost criteria,
        final Constraints.Constraint ordering,
        final Constraints.Constraint exclusion,
        final @Nonnegative int skip,
        final @Nonnegative int limit) {
      return submit(new Callable<List<T>>() {
        @SuppressWarnings("resource")
        @Override
        public List<T> call() throws Exception {
          @Nullable Bson query = criteria != null ? convertToBson(criteria) : null;

          FindIterable<T> cursor = collection().find(query);

          if (!exclusion.isNil()) {
            cursor.projection(convertToBson(exclusion));
          }

          if (!ordering.isNil()) {
            cursor.sort(convertToBson(ordering));
          }

          cursor.skip(skip);

          if (limit != 0) {
            cursor.limit(limit);
            if (limit <= LARGE_BATCH_SIZE) {
              // if limit specified and is smaller than reasonable large batch size
              // then we force batch size to be the same as limit,
              // but negative, this force cursor to close right after result is sent
              cursor.batchSize(-limit);
            }
          }

          // close properly
          try (MongoCursor<T> iterator = cursor.iterator()) {
            return ImmutableList.copyOf(iterator);
          }
        }
      });
    }

    protected final <U> FluentFuture<List<U>> doFetch(
        final @Nullable Constraints.ConstraintHost criteria,
        final Constraints.Constraint ordering,
        final Projection<U> projection,
        final @Nonnegative int skip,
        final @Nonnegative int limit) {
      return submit(new Callable<List<U>>() {
        @SuppressWarnings("resource")
        @Override
        public List<U> call() throws Exception {
          @Nullable Bson query = criteria != null ? convertToBson(criteria) : null;

          MongoCollection<T> collection = collection();

          if(projection.gson() != null) {
            CodecRegistry registry = CodecRegistries.fromRegistries(GsonCodecs.codecRegistryFromGson(projection.gson()), codecRegistry());
            collection = collection().withCodecRegistry(registry);
          }

          FindIterable<U> cursor = collection.withDocumentClass(projection.resultType()).find(query);

          if(projection.fields() != null) {
            cursor.projection(projection.fields());
          }

          if (!ordering.isNil()) {
            cursor.sort(convertToBson(ordering));
          }

          cursor.skip(skip);

          if (limit != 0) {
            cursor.limit(limit);
            if (limit <= LARGE_BATCH_SIZE) {
              // if limit specified and is smaller than reasonable large batch size
              // then we force batch size to be the same as limit,
              // but negative, this force cursor to close right after result is sent
              cursor.batchSize(-limit);
            }
          }

          // close properly
          try (MongoCursor<U> iterator = cursor.iterator()) {
            return ImmutableList.copyOf(iterator);
          }
        }
      });
    }
  }

  /**
   * Specifies the fields to include in the resulting documents that match the query filter.
   * @param <T> result type
   */
  public static abstract class Projection<T> {

    public static <T> Projection<T> of(final Class<T> resultType, String... fieldsToInclude) {
      List<String> fields = fieldsToInclude == null
              ? ImmutableList.<String> of()
              : ImmutableList.copyOf(fieldsToInclude);
      return of(resultType, fields);
    }

    public static <T> Projection<T> of(final Class<T> resultType, List<String> fieldsToInclude) {
      final Bson fields = fieldsToInclude.isEmpty()
              ? null
              : Projections.fields(Projections.excludeId(), Projections.include(fieldsToInclude));
      return of(resultType, fields);
    }

    public static <T> Projection<T> of(final Class<T> resultType, @Nullable final Bson projection) {
      checkNotNull(resultType);
      return new Projection<T>() {
        @Nullable
        @Override
        protected Gson gson() {
            return null;
        }

        @Nullable
        @Override
        protected Bson fields() {
          return projection;
        }

        @Override
        protected Class<T> resultType() {
          return resultType;
        }
      };
    }

    /**
     * @return optional {@link Gson} instance to use to be able to to decode to the specified result documents
     */
    @Nullable
    protected abstract Gson gson();

    /**
     * @return fields to include in the result documents
     */
    @Nullable
    protected abstract Bson fields();

    /**
     * @return type of the result documents
     */
    protected abstract Class<T> resultType();

  }

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
  @ThreadSafe
  public static abstract class Criteria {
    /**
     * Returns chained criteria handle used to "OR" new constraint set to form logical DNF.
     * @return disjunction separated criteria handle
     */
    public abstract Criteria or();
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
    protected Constraints.ConstraintHost criteria;
    protected Constraints.Constraint setFields = Constraints.nilConstraint();
    protected Constraints.Constraint setOnInsertFields = Constraints.nilConstraint();
    protected Constraints.Constraint incrementFields = Constraints.nilConstraint();
    protected Constraints.Constraint addToSetFields = Constraints.nilConstraint();
    protected Constraints.Constraint pushFields = Constraints.nilConstraint();
    protected Constraints.Constraint pullFields = Constraints.nilConstraint();
    protected Constraints.Constraint unsetFields = Constraints.nilConstraint();

    protected UpdatatingOperation(Repository<T> repository) {
      super(repository);
    }

    protected Constraints.Constraint collectRequiredUpdate() {
      Constraints.Constraint update = collectUpdate();
      checkState(!update.isNil());
      return update;
    }

    protected Constraints.Constraint collectUpdate() {
      Constraints.Constraint update = Constraints.nilConstraint();
      update = appendFields(update, "$set", setFields);
      update = appendFields(update, "$setOnInsert", setOnInsertFields);
      update = appendFields(update, "$inc", incrementFields);
      update = appendFields(update, "$addToSet", addToSetFields);
      update = appendFields(update, "$push", pushFields);
      update = appendFields(update, "$pull", pullFields);
      update = appendFields(update, "$unset", unsetFields);
      return update;
    }

    private Constraints.Constraint appendFields(
        Constraints.Constraint fields,
        String name,
        Constraints.Constraint setOfFields) {
      return !setOfFields.isNil() ? fields.equal(name, false, setOfFields) : fields;
    }
  }

  /**
   * Base class which handles update operations (like {@code upsert}, {@code updateAll}, {@code updateFirst} etc.)
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
      UpdateOptions options = new UpdateOptions();
      options.upsert(true);
      return repository.doUpdate(criteria, collectRequiredUpdate(), options);
    }

    /**
     * Updates a single document that matches.
     * @return number of updated documents. 0 or 1
     */
    public FluentFuture<Integer> updateFirst() {
      return repository.doUpdateFirst(criteria, collectRequiredUpdate(), new FindOneAndUpdateOptions());
    }

    /**
     * Updates all matching document.
     * @return future of number of updated document
     */
    public FluentFuture<Integer> updateAll() {
      return repository.doUpdate(criteria, collectRequiredUpdate(), new UpdateOptions());
    }
  }

  /**
   * Provides base configuration methods and action methods to perform 'modify' step in
   * 'findAndModify' operation.
   *
   * @param <T> document type
   * @param <M> a self type of extended modifier class
   */
  @NotThreadSafe
  public static abstract class Modifier<T, M extends Modifier<T, M>> extends UpdatatingOperation<T> {
    protected Constraints.Constraint ordering = Constraints.nilConstraint();
    protected Constraints.Constraint exclusion = Constraints.nilConstraint();
    private final FindOneAndUpdateOptions options = new FindOneAndUpdateOptions();

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
      options.returnDocument(ReturnDocument.BEFORE);
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
      options.returnDocument(ReturnDocument.AFTER);
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
     *
     */
    public final FluentFuture<Optional<T>> upsert() {
      options.upsert(true);
      options.sort(convertToBson(ordering));
      // TODO exclusion / projection
      return repository.doModify(criteria, collectRequiredUpdate(), options);
    }

    /**
     * Performs an update. If query will match a document, then it will be modified and old or new
     * version of document returned (depending if {@link #returningNew()} was configured). When
     * there
     * isn't any matching document, {@link Optional#absent()} will be result of the operation.
     * @return future of optional document (present if matching document would be found)
     */
    public final FluentFuture<Optional<T>> update() {
      options.sort(convertToBson(ordering));
      // TODO exlusion / projection
      return repository.doModify(criteria, collectRequiredUpdate(), options);
    }
  }

  /**
   * Base class for handling replace operations on a mongo document given a criteria.
   */
  @NotThreadSafe
  public static abstract class Replacer<T, M extends Replacer<T, M>> extends UpdatatingOperation<T> {

    private final FindOneAndReplaceOptions options;
    private final T document;
    private final Constraints.ConstraintHost criteria;
    private final Constraints.Constraint ordering;

    protected Replacer(
        Repository<T> repository,
        T document,
        Constraints.ConstraintHost criteria,
        Constraints.Constraint ordering) {
      super(repository);
      this.document = checkNotNull(document, "document");
      this.criteria = checkNotNull(criteria, "criteria");
      this.ordering = checkNotNull(ordering, "ordering");
      this.options = new FindOneAndReplaceOptions();
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
      options.returnDocument(ReturnDocument.BEFORE);
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
      options.returnDocument(ReturnDocument.AFTER);
      return (M) this;
    }

    public final FluentFuture<Optional<T>> upsert() {
      options.upsert(true);
      options.sort(convertToBson(ordering));
      return repository.doReplace(criteria, document, options);
    }

    public final FluentFuture<Optional<T>> update() {
      options.sort(convertToBson(ordering));
      return repository.doReplace(criteria, document, options);
    }

  }

  /**
   * Base class for the indexer objects. Allows to create indexes for mongo documents at runtime.
   * @param <T> document type
   * @param <I> a self type of extended indexer class
   */
  @NotThreadSafe
  public static abstract class Indexer<T, I extends Indexer<T, I>> extends Operation<T> {
    protected Constraints.Constraint fields = Constraints.nilConstraint();
    private final IndexOptions options = new IndexOptions();

    protected Indexer(Repository<T> repository) {
      super(repository);
    }

    /**
     * Configures name for an index, that is otherwise will be auto-named by index fields.
     * @param indexName explicitly provided index name
     * @return {@code this} indexer for chained invocation
     */
    // safe unchecked: we expect I to be a self type
    @SuppressWarnings("unchecked")
    public final I named(String indexName) {
      options.name(indexName);
      return (I) this;
    }

    /**
     * Makes an index to enforce unique constraint.
     * @return {@code this} indexer for chained invocation
     */
    // safe unchecked: we expect I to be a self type
    @SuppressWarnings("unchecked")
    public final I unique() {
      options.unique(true);
      return (I) this;
    }

    /**
     * Configures and TTL for an index. Creates an index on a time field, and document will be
     * removed when TTL will expire.
     * <p>
     * <em>Note: Care should be taken to configure TTL only on single time instant field</em>
     * @param timeToLiveSeconds time to live for an object, non-zero time in seconds required.
     * @return {@code this} indexer for chained invocation
     */
    // safe unchecked: we expect I to be a self type
    @SuppressWarnings("unchecked")
    public final I expireAfterSeconds(int timeToLiveSeconds) {
      options.expireAfter((long) timeToLiveSeconds, TimeUnit.SECONDS);
      return (I) this;
    }

    /**
     * Creates configured index on a set of fields, if one does not already exist.
     * @see MongoCollection#createIndex(Bson, IndexOptions)
     * @return future of indexing operation, future value is insignificant ({@code null} typed as
     *         {@link Void})
     */
    public final FluentFuture<Void> ensure() {
      return repository.doIndex(fields, options);
    }
  }

  /**
   * Fetcher class which adds delete functionality to the base class {@link Finder}.
   *
   * @param <T> document type
   * @param <F> a self type of extended finder class
   */
  @NotThreadSafe
  public static abstract class FinderWithDelete<T, F extends Finder<T, F>> extends Finder<T, F> {
    protected FinderWithDelete(Repository<T> repository) {
      super(repository);
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
      FindOneAndDeleteOptions options = new FindOneAndDeleteOptions();
      options.sort(convertToBson(ordering));
      return repository.doFindOneAndDelete(criteria, options);
    }

  }

  /**
   * Base class for the finder objects. Fetcher objects are used to configure query.
   * @param <T> document type
   * @param <F> a self type of extended finder class
   */
  @NotThreadSafe
  public static abstract class Finder<T, F extends Finder<T, F>> extends Operation<T> {
    int numberToSkip;

    @Nullable
    protected Constraints.ConstraintHost criteria;
    protected Constraints.Constraint ordering = Constraints.nilConstraint();
    protected Constraints.Constraint exclusion = Constraints.nilConstraint();

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
     * Fetches result list with at most as {@code limitSize} matching documents. It could
     * be used together with {@link #skip(int)} to paginate results.
     * <p>
     * Zero limit ({@code fetchWithLimit(0)}) is equivalent to {@link #fetchAll()}.
     * <p>
     * As an performance optimization, when limit is "not so large", then batch size will be set to
     * a negative limit: this forces a MongoDB to sent results in a single batch and immediately
     * closes cursor.
     * @param limitSize specify limit on the number of document in result.
     * @param projection specify which fields to project onto a result type to be specified
     * @return future of matching document list
     */
    public final <U> FluentFuture<List<U>> fetchWithLimit(@Nonnegative int limitSize, Projection<U> projection) {
      checkArgument(limitSize >= 0, "limit cannot be negative");
      return repository.doFetch(criteria, ordering, projection, numberToSkip, limitSize);
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
     * Fetches all matching documents list.
     * <p>
     * If number or results could be very large, then prefer to use {@link #fetchWithLimit(int, Projection)} to
     * always limit result to some large but reasonable size.
     * @param projection specify which fields to project onto a result type to be specified
     * @return future of matching document list
     */
    public final <U> FluentFuture<List<U>> fetchAll(Projection<U> projection) {
      return fetchWithLimit(0, projection);
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
     * Fetches first matching document. If none of the documents matches, then
     * {@link Optional#absent()} will be returned.
     * @param projection specify which fields to project onto a result type to be specified
     * @return future of optional matching document
     */
    public final <U> FluentFuture<Optional<U>> fetchFirst(Projection<U> projection) {
      return fetchWithLimit(1, projection).transform(new Function<List<U>, Optional<U>>() {
        @Override
        public Optional<U> apply(List<U> input) {
          return FluentIterable.from(input).first();
        }
      });
    }

  }
}
