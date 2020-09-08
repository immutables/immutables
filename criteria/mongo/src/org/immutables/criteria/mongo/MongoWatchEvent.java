/*
 * Copyright 2020 Immutables Authors and Contributors
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

package org.immutables.criteria.mongo;

import com.mongodb.client.model.changestream.ChangeStreamDocument;
import com.mongodb.client.model.changestream.OperationType;
import org.bson.BsonDocument;
import org.bson.BsonValue;
import org.immutables.criteria.backend.WatchEvent;

import java.util.Optional;

class MongoWatchEvent<T> implements WatchEvent<T> {

  private final Object key;
  private final T newValue;
  private final Operation operation;

  MongoWatchEvent(Object key, T newValue, Operation operation) {
    this.key = key;
    this.newValue = newValue;
    this.operation = operation;
  }

  @Override
  public Object key() {
    return key;
  }

  @Override
  public Optional<T> newValue() {
    return Optional.ofNullable(newValue);
  }

  @Override
  public Operation operation() {
    return operation;
  }

  static <T> WatchEvent<T> fromChangeStream(ChangeStreamDocument<T> doc) {
    Object key = toInternalKey(doc.getDocumentKey());
    T newValue = doc.getFullDocument();
    OperationType type = doc.getOperationType();
    Operation op;
    switch (type) {
      case INSERT:
        op =  Operation.INSERT;
        break;
      case REPLACE:
        op = Operation.REPLACE;
        break;
      case DELETE:
        op = Operation.DELETE;
        break;
      case UPDATE:
      default:
        op = Operation.UPDATE;
    }

    return new MongoWatchEvent<>(key, newValue, op);
  }

  /**
   * Convert document key (BSON) to entity ID. Currently only strings are supported.
   * This code needs to leverage existing CodecRegistry to correctly convert BSON value
   * into ID type.
   */
  private static Object toInternalKey(BsonDocument key) {
    if (key == null) {
      return null;
    }

    BsonValue value = key.get(Mongos.ID_FIELD_NAME);

    if (value == null) {
      return null;
    }

    if (value.isString()) {
      return value.asString().getValue();
    } else if (value.isSymbol()) {
      return value.asSymbol().getSymbol();
    } else if (value.isObjectId()) {
      return value.asObjectId().getValue();
    }

    // don't know what to do with this value
    // TODO reuse CodecRegistry
    return null;
  }
}
