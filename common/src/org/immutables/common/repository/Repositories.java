/*
    Copyright 2013 Immutables.org authors

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
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.primitives.Ints;
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
import static com.google.common.base.Preconditions.*;
import static org.immutables.common.repository.internal.RepositorySupport.*;

public final class Repositories {
  private static final int LARGE_BATCH_SIZE = 2000;
  private static final int DEFAULT_EXPECTED_RESULT_SIZE = 500;

  private Repositories() {
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

    private <V> FluentFuture<V> submit(Callable<V> callable) {
      return FluentFutures.from(configuration.executor.submit(callable));
    }

    protected final FluentFuture<Void> doIndex(
        final ConstraintSupport.Constraint fields,
        final ConstraintSupport.Constraint options) {
      return submit(new Callable<Void>() {
        @Override
        public Void call() {
          collection().ensureIndex(
              extractDbObject(fields),
              extractDbObject(options));
          return null;
        }
      });
    }

    protected final FluentFuture<Integer> doInsert(final ImmutableList<T> documents) {
      return submit(new Callable<Integer>() {
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
      return submit(new Callable<Optional<T>>() {
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

    protected final FluentFuture<Integer> doUpdate(
        final ConstraintSupport.ConstraintHost criteria,
        final ConstraintSupport.Constraint update,
        final boolean upsert,
        final boolean multiple) {
      checkArgument(!multiple || !upsert);
      return submit(new Callable<Integer>() {
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

    protected final FluentFuture<Integer> doDelete(
        final ConstraintSupport.ConstraintHost criteria) {
      return submit(new Callable<Integer>() {
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

    protected final FluentFuture<Void> doUpsert(
        final ConstraintSupport.ConstraintHost criteria,
        final T document) {
      return submit(new Callable<Void>() {
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

    protected final FluentFuture<List<T>> doFetch(
        final @Nullable ConstraintSupport.ConstraintHost criteria,
        final ConstraintSupport.Constraint ordering,
        final ConstraintSupport.Constraint exclusion,
        final @Nonnegative int skip,
        final @Nonnegative int limit) {
      return submit(new Callable<List<T>>() {
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
        ConstraintSupport.Constraint fields,
        String name,
        ConstraintSupport.Constraint setOfFields) {
      return !setOfFields.isNil() ? fields.equal(name, false, setOfFields) : fields;
    }
  }

  @NotThreadSafe
  public static abstract class Updater<T> extends UpdatatingOperation<T> {
    protected Updater(Repository<T> repository) {
      super(repository);
    }

    public FluentFuture<Integer> upsert() {
      return repository.doUpdate(criteria, collectRequiredUpdate(), true, false);
    }

    public FluentFuture<Integer> updateFirst() {
      return repository.doUpdate(criteria, collectRequiredUpdate(), false, false);
    }

    public FluentFuture<Integer> updateAll() {
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

    public FluentFuture<Optional<T>> upsert() {
      return repository.doModify(criteria, ordering, exclusion, collectRequiredUpdate(), true, returnNewOrOld, false);
    }

    public FluentFuture<Optional<T>> update() {
      return repository.doModify(criteria, ordering, exclusion, collectRequiredUpdate(), false, returnNewOrOld, false);
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

    public final FluentFuture<Void> ensure() {
      return repository.doIndex(fields, options);
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

    public final FluentFuture<List<T>> fetchWithLimit(@Nonnegative int limit) {
      checkArgument(limit >= 0, "limit cannot be negative");
      return repository.doFetch(criteria, ordering, exclusion, numberToSkip, limit);
    }

    public final FluentFuture<List<T>> fetchAll() {
      return fetchWithLimit(0);
    }

    public final FluentFuture<Optional<T>> fetchFirst() {
      return fetchWithLimit(1).transform(new Function<List<T>, Optional<T>>() {
        @Override
        public Optional<T> apply(List<T> input) {
          return FluentIterable.from(input).first();
        }
      });
    }

    public FluentFuture<Optional<T>> deleteFirst() {
      checkState(numberToSkip == 0, "Cannot use skip() with .removeFirst()");
      return repository.doModify(
          criteria, ordering, exclusion, ConstraintSupport.nilConstraint(), false, false, true);
    }
  }

}
