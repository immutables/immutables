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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import org.bson.BsonArray;
import org.bson.BsonDateTime;
import org.bson.BsonDocument;
import org.bson.BsonDocumentReader;
import org.bson.BsonInt32;
import org.bson.BsonNull;
import org.bson.BsonString;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.configuration.CodecRegistry;
import org.immutables.criteria.backend.KeyExtractor;
import org.immutables.criteria.backend.ProjectedTuple;
import org.immutables.criteria.expression.Path;
import org.immutables.criteria.expression.Query;
import org.immutables.criteria.expression.Visitors;
import org.immutables.criteria.matcher.Matchers;
import org.immutables.criteria.mongo.bson4jackson.BsonModule;
import org.immutables.criteria.mongo.bson4jackson.IdAnnotationModule;
import org.immutables.criteria.mongo.bson4jackson.JacksonCodecs;
import org.immutables.criteria.personmodel.Person;
import org.immutables.criteria.personmodel.PersonCriteria;
import org.immutables.criteria.typemodel.LocalDateHolderCriteria;
import org.immutables.criteria.typemodel.TypeHolder;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.immutables.check.Checkers.check;

public class TupleCodecProviderTest {

  private final ObjectMapper mapper = new ObjectMapper()
          .registerModule(new BsonModule())
          .registerModule(new GuavaModule())
          .registerModule(new Jdk8Module())
          .registerModule(new IdAnnotationModule());

  private final CodecRegistry registry = JacksonCodecs.registryFromMapper(mapper);

  @Test
  public void age() {
    Query query = Query.of(Person.class).addProjections(Matchers.toExpression(PersonCriteria.person.age));
    Path idPath = Visitors.toPath(KeyExtractor.defaultFactory().create(Person.class).metadata().keys().get(0));
    TupleCodecProvider provider = new TupleCodecProvider(query, new MongoPathNaming(idPath).toExpression());
    Codec<ProjectedTuple> codec = provider.get(ProjectedTuple.class, registry);

    ProjectedTuple tuple = codec.decode(new BsonDocumentReader(new BsonDocument("age", new BsonInt32(10))), DecoderContext.builder().build());

    check(tuple.values()).hasSize(1);
    check(tuple.values().get(0)).asString().is("10");
  }

  @Test
  void localDate() {
    LocalDateHolderCriteria criteria = LocalDateHolderCriteria.localDateHolder;

    Query query = Query.of(TypeHolder.LocalDateHolder.class)
            .addProjections(Matchers.toExpression(criteria.value),  Matchers.toExpression(criteria.nullable), Matchers.toExpression(criteria.optional));

    Path idPath = Visitors.toPath(KeyExtractor.defaultFactory().create(TypeHolder.LocalDateHolder.class).metadata().keys().get(0));
    TupleCodecProvider provider = new TupleCodecProvider(query, new MongoPathNaming(idPath).toExpression());
    Codec<ProjectedTuple> codec = provider.get(ProjectedTuple.class, registry);

    LocalDate now = LocalDate.now();
    final long millisEpoch = now.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli();

    BsonDocument doc = new BsonDocument()
            .append("id", new BsonString("id1"))
            .append("value", new BsonDateTime(millisEpoch))
            .append("nullable", BsonNull.VALUE)
            .append("optional", BsonNull.VALUE)
            .append("array", new BsonArray())
            .append("list", new BsonArray());

    ProjectedTuple tuple = codec.decode(new BsonDocumentReader(doc), DecoderContext.builder().build());

    check(tuple.get(Matchers.toExpression(criteria.value))).is(now);
    check(tuple.get(Matchers.toExpression(criteria.nullable))).isNull();
    check(tuple.get(Matchers.toExpression(criteria.optional))).is(Optional.empty());
  }

  /**
   * Projection of an optional attribute
   */
  @Test
  @SuppressWarnings("unchecked")
  public void optionalAttribute_nickname() {
    Query query = Query.of(Person.class).addProjections(Matchers.toExpression(PersonCriteria.person.nickName));
    Path idPath = Visitors.toPath(KeyExtractor.defaultFactory().create(Person.class).metadata().keys().get(0));
    TupleCodecProvider provider = new TupleCodecProvider(query, new MongoPathNaming(idPath).toExpression());
    Codec<ProjectedTuple> codec = provider.get(ProjectedTuple.class, registry);

    ProjectedTuple tuple1 = codec.decode(new BsonDocumentReader(new BsonDocument("nickName", new BsonString("aaa"))), DecoderContext.builder().build());
    check(tuple1.values()).hasSize(1);
    check((Optional<String>) tuple1.values().get(0)).is(Optional.of("aaa"));

    ProjectedTuple tuple2 = codec.decode(new BsonDocumentReader(new BsonDocument()), DecoderContext.builder().build());
    check(tuple2.values()).hasSize(1);
    check((Optional<String>) tuple2.values().get(0)).is(Optional.empty());
  }
}