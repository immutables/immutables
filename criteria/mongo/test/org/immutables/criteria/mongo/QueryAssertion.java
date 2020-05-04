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

import com.mongodb.MongoClientSettings;
import org.bson.BsonDocument;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.json.JsonWriterSettings;
import org.immutables.criteria.backend.KeyExtractor;
import org.immutables.criteria.backend.PathNaming;
import org.immutables.criteria.expression.Path;
import org.immutables.criteria.expression.Query;
import org.immutables.criteria.expression.Visitors;
import org.junit.jupiter.api.Assertions;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

class QueryAssertion {

  private final Query query;
  private final List<BsonDocument> actual;

  QueryAssertion(Query query, boolean pipeline) {
    this.query = Objects.requireNonNull(query, "query");
    Path idPath = Visitors.toPath(KeyExtractor.defaultFactory().create(query.entityClass()).metadata().keys().get(0));
    PathNaming pathNaming = new MongoPathNaming(idPath, PathNaming.defaultNaming());
    FindVisitor visitor = new FindVisitor(pathNaming);
    CodecRegistry codecRegistry = MongoClientSettings.getDefaultCodecRegistry();
    if (query.hasAggregations() || pipeline) {
      AggregationQuery agg = new AggregationQuery(query, pathNaming);
      this.actual =  agg.toPipeline().stream()
              .map(b -> b.toBsonDocument(BsonDocument.class, MongoClientSettings.getDefaultCodecRegistry()))
              .collect(Collectors.toList());
    } else {
      BsonDocument actual = query.filter().map(f -> f.accept(visitor)).orElseGet(BsonDocument::new)
              .toBsonDocument(BsonDocument.class, codecRegistry);
      this.actual = Collections.singletonList(actual);
    }
  }

  void matches(String ... linesAsSingleDoc) {
    BsonDocument expected = BsonDocument.parse(String.join("\n", linesAsSingleDoc));
    assertEquals(actual, Collections.singletonList(expected));
  }

  void matchesMulti(String ... lines) {
    List<BsonDocument> expected = Arrays.stream(lines).map(BsonDocument::parse).collect(Collectors.toList());
    assertEquals(actual, expected);
  }

  private static void assertEquals(List<BsonDocument> actual, List<BsonDocument> expected) {
    if (!actual.equals(expected)) {
      final JsonWriterSettings settings = JsonWriterSettings.builder().indent(true).build();
      // outputs Bson in pretty Json format (with new lines)
      // so output is human friendly in IDE diff tool
      final Function<List<BsonDocument>, String> prettyFn = bsons -> bsons.stream()
              .map(b -> b.toJson(settings)).collect(Collectors.joining("\n"));

      // used to pretty print Assertion error
      Assertions.assertEquals(
              prettyFn.apply(expected),
              prettyFn.apply(actual),
              "expected and actual Mongo pipelines do not match");

      Assertions.fail("Should have failed previously because expected != actual is known to be true");
    }
  }

  static QueryAssertion of(Query query) {
    return new QueryAssertion(query, false);
  }

  /**
   * Force assertions of pipeline (aggregation) query
   */
  static QueryAssertion ofPipeline(Query query) {
    return new QueryAssertion(query, true);
  }

}
