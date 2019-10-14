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

package org.immutables.criteria.repository;

import com.google.common.collect.ImmutableList;
import org.immutables.criteria.Criterion;

import java.util.Arrays;

/**
 * Declares repository as writable. Means documents can be inserted / updated / deleted.
 *
 * @param <T> entity type
 * @param <R> write-result type
 */
public interface Writable<T, R> extends Facet {

  default R insert(T ... docs) {
    return insertAll(Arrays.asList(docs));
  }

  /**
   * Insert list of documents. If one or more documents already exists exception will
   * be thrown.
   * @param docs list of documents to insert
   * @return some wrapper around {@link org.immutables.criteria.backend.WriteResult}
   */
  R insertAll(Iterable<? extends T> docs);

  /**
   * Delete documents matching a filter
   * @param criteria element filter
   */
  R delete(Criterion<T> criteria);

  /**
   * Update or Insert a single document. If supported by the backend, this operation
   * is atomic.
   */
  default R upsert(T doc) {
    return upsertAll(ImmutableList.of(doc));
  }

  /**
   * Update or Insert list of documents. If one of the document does not exists
   * it will be inserted. Document is identified by {@code ID} attribute. If supported by the
   * backend, this operation is atomic.
   *
   * @param docs list of documents to insert or update
   * @return some wrapper around {@link org.immutables.criteria.backend.WriteResult}
   */
  R upsertAll(Iterable<? extends T> docs);

  /**
   * Update single document
   */
  default R update(T doc) {
    return updateAll(ImmutableList.of(doc));
  }

  /**
   * Update list of <strong>existing</strong> documents. Only matching (as identified by {@code ID} attribute)
   * documents are updated. If one of the documents does not exists it will be omitted (ignored).
   * Document is identified by {@code ID} attribute.
   * @param docs list of documents to update.
   * @return some wrapper around {@link org.immutables.criteria.backend.WriteResult}
   */
  R updateAll(Iterable<? extends T> docs);

  /**
   * Update documents matching a filter
   *
   * @param criterion filter to apply updates on
   * @return update DSL
   */
  Updater<T, R> update(Criterion<T> criterion);

}
