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

import com.mongodb.reactivestreams.client.MongoCollection;
import com.mongodb.reactivestreams.client.MongoDatabase;
import io.reactivex.Flowable;
import org.bson.BsonDocument;
import org.immutables.check.Checkers;
import org.immutables.criteria.typemodel.ImmutableStringHolder;
import org.immutables.criteria.typemodel.StringHolderCriteria;
import org.immutables.criteria.typemodel.StringHolderRepository;
import org.immutables.criteria.typemodel.TypeHolder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Check correct serialization of {@code _id} attribute
 */
@ExtendWith(MongoExtension.class)
class IdAttributeTest {

  private final BackendResource resource;

  IdAttributeTest(MongoDatabase database) {
    this.resource = new BackendResource(database);
  }

  /**
   * Test that {@code _id} attribute is persisted instead of {@code id}
   */
  @Test
  void idAttribute() {
    StringHolderRepository repository = new StringHolderRepository(this.resource.backend());
    ImmutableStringHolder holder = TypeHolder.StringHolder.generator().get().withId("id1");
    repository.insertAll(Arrays.asList(holder, holder.withId("id2")));

    MongoCollection<BsonDocument> collection = this.resource.collection(TypeHolder.StringHolder.class)
            .withDocumentClass(BsonDocument.class);


    List<BsonDocument> docs = Flowable.fromPublisher(collection.find()).toList().blockingGet();

    Checkers.check(docs).hasSize(2);

    // has _id attribute
    Checkers.check(docs.stream().map(BsonDocument::keySet).flatMap(Collection::stream).collect(Collectors.toSet())).has("_id");
    // does not have 'id' attribute only '_id' (with underscore which is mongo specific) in collected documents
    Checkers.check(docs.stream().map(BsonDocument::keySet).flatMap(Collection::stream).collect(Collectors.toSet())).not().has("id");
    Checkers.check(docs.stream().map(d -> d.get("_id").asString().getValue()).collect(Collectors.toList())).hasContentInAnyOrder("id1", "id2");

    // using repository
    Checkers.check(repository.findAll().fetch().stream().map(TypeHolder.StringHolder::id).collect(Collectors.toList())).hasContentInAnyOrder("id1", "id2");
  }

  /**
   * Tests the sorting by {@code _id}.
   */
  @Test
  void sortById() {
    final StringHolderRepository repository = new StringHolderRepository(this.resource.backend());

    final List<TypeHolder.StringHolder> stringHolders =
        Stream.generate(TypeHolder.StringHolder.generator()).limit(100).collect(Collectors.toList());
    Collections.shuffle(stringHolders);
    repository.insertAll(stringHolders);

    final List<String> expectedIdsInOrder =
        stringHolders.stream().map(TypeHolder.StringHolder::id).sorted().collect(Collectors.toList());

    final List<String> actualIds = repository.findAll().orderBy(StringHolderCriteria.stringHolder.id.asc()).fetch()
        .stream().map(TypeHolder.StringHolder::id).collect(Collectors.toList());

    Checkers.check(actualIds).is(expectedIdsInOrder);
  }

}
