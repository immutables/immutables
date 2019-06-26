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

package org.immutables.criteria.geode;

import io.reactivex.disposables.Disposable;
import org.apache.geode.cache.query.CqException;
import org.apache.geode.cache.query.CqQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

/**
 * Disposable (rxjava interface) wrapper for {@link CqQuery}
 */
class CqDisposable implements Disposable  {

  private static final Logger logger = LoggerFactory.getLogger(CqDisposable.class);

  private final CqQuery cqQuery;

  CqDisposable(CqQuery cqQuery) {
    this.cqQuery = Objects.requireNonNull(cqQuery, "cqQuery");
  }

  @Override
  public void dispose() {
    try {
      cqQuery.close();
    } catch (CqException e) {
      logger.error("Failed to close {} [{}]", cqQuery.getName(), cqQuery.getQueryString(), e);
    }
  }

  @Override
  public boolean isDisposed() {
    return cqQuery.isClosed();
  }
}
