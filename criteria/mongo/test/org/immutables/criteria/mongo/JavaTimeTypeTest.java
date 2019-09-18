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

package org.immutables.criteria.mongo;

import com.mongodb.reactivestreams.client.MongoDatabase;
import io.reactivex.Flowable;
import org.bson.BsonDateTime;
import org.bson.BsonDocument;
import org.bson.BsonNull;
import org.immutables.criteria.backend.Backend;
import org.immutables.criteria.typemodel.ImmutableInstantHolder;
import org.immutables.criteria.typemodel.ImmutableLocalDateHolder;
import org.immutables.criteria.typemodel.ImmutableLocalDateTimeHolder;
import org.immutables.criteria.typemodel.InstantHolderRepository;
import org.immutables.criteria.typemodel.LocalDateHolderRepository;
import org.immutables.criteria.typemodel.LocalDateTimeHolderRepository;
import org.immutables.criteria.typemodel.TypeHolder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.immutables.check.Checkers.check;

/**
 * Check correct serialization of {@code java.time.*} types like {@link LocalDate}
 */
@ExtendWith(MongoExtension.class)
class JavaTimeTypeTest {

  private final MongoDatabase database;
  private final Backend backend;

  JavaTimeTypeTest(MongoDatabase database) {
    this.database = database;
    this.backend = new BackendResource(database).backend();
  }

  @Test
  void instant() {
    InstantHolderRepository repository = new InstantHolderRepository(backend);
    Instant value = Instant.now();
    ImmutableInstantHolder holder = TypeHolder.InstantHolder.generator().get().withValue(value).withOptional(value).withNullable(null);
    repository.insert(holder);
    BsonDocument doc = fetch();

    BsonDateTime expected = new BsonDateTime(value.toEpochMilli());
    check(doc.get("value")).is(expected);
    check(doc.get("optional")).is(expected);
    if (doc.containsKey("nullable")) {
      check(doc.get("nullable")).is(BsonNull.VALUE);
    }
  }

  @Test
  void localDateTime() {
    LocalDateTimeHolderRepository repository = new LocalDateTimeHolderRepository(backend);
    LocalDateTime value = LocalDateTime.now();
    ImmutableLocalDateTimeHolder holder = TypeHolder.LocalDateTimeHolder.generator().get().withValue(value).withOptional(value).withNullable(null);
    repository.insert(holder);
    BsonDocument doc = fetch();

    BsonDateTime expected = new BsonDateTime(value.toInstant(ZoneOffset.UTC).toEpochMilli());
    check(doc.get("value")).is(expected);
    check(doc.get("optional")).is(expected);
    if (doc.containsKey("nullable")) {
      check(doc.get("nullable")).is(BsonNull.VALUE);
    }
  }

  @Test
  void localDate() {
    LocalDateHolderRepository repository = new LocalDateHolderRepository(backend);
    LocalDate value = LocalDate.now();
    ImmutableLocalDateHolder holder = TypeHolder.LocalDateHolder.generator().get().withValue(value).withOptional(value).withNullable(null);
    repository.insert(holder);
    BsonDocument doc = fetch();

    BsonDateTime expected = new BsonDateTime(value.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli());
    check(doc.get("value")).is(expected);
    check(doc.get("optional")).is(expected);
    if (doc.containsKey("nullable")) {
      check(doc.get("nullable")).is(BsonNull.VALUE);
    }
  }


  private BsonDocument fetch() {
    List<String> names = Flowable.fromPublisher(database.listCollectionNames()).toList().blockingGet();
    if (names.size() != 1) {
      throw new IllegalStateException(String.format("Expected single(1) collection but got %d: %s", names.size(), names));
    }

    List<BsonDocument> docs = Flowable.fromPublisher(database.getCollection(names.get(0))
            .withDocumentClass(BsonDocument.class).find()).toList().blockingGet();

    if (docs.size() != 1) {
      throw new IllegalStateException(String.format("Expected single(1) document but got %d: %s", docs.size(), docs));
    }

    return docs.get(0);
  }

}
