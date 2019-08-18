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

import org.bson.BsonDocument;
import org.bson.BsonDocumentReader;
import org.bson.BsonNull;
import org.bson.BsonReader;
import org.bson.BsonValue;
import org.bson.BsonWriter;
import org.bson.codecs.Codec;
import org.bson.codecs.Decoder;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import org.bson.codecs.configuration.CodecProvider;
import org.bson.codecs.configuration.CodecRegistry;
import org.immutables.criteria.backend.ProjectedTuple;
import org.immutables.criteria.expression.Expressions;
import org.immutables.criteria.expression.Path;
import org.immutables.criteria.expression.Query;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Provider for {@link ProjectedTuple}
 */
class TupleCodecProvider implements CodecProvider {

  private final Query query;

  TupleCodecProvider(Query query) {
    this.query = Objects.requireNonNull(query, "query");
  }

  @SuppressWarnings("unchecked")
  @Override
  public <T> Codec<T> get(Class<T> clazz, CodecRegistry registry) {
    if (clazz == ProjectedTuple.class) {
      return (Codec<T>) new TupleCodec(registry, query);
    }
    return null;
  }


  private static class TupleCodec implements Codec<ProjectedTuple> {
    private final CodecRegistry registry;
    private final Query query;
    private final List<Path> projections;
    private final List<Type> types;
    private final List<String> fields;
    private final Map<String, Decoder<?>> decoderMap;

    private TupleCodec(CodecRegistry registry, Query query) {
      this.query = query;
      final List<Path> projections = query.projections().stream().map(e -> (Path) e).collect(Collectors.toList());
      if (projections.isEmpty()) {
        throw new IllegalArgumentException(String.format("No projections defined in query %s", query));
      }
      this.registry = Objects.requireNonNull(registry, "registry");
      this.projections = projections;
      this.types = projections.stream().map(Expressions::returnType).collect(Collectors.toList());
      this.fields = projections.stream().map(Mongos::toMongoFieldName).collect(Collectors.toList());
      // ensure no generic currently
      for (int i = 0; i < projections.size(); i++) {
        Type type = types.get(i);
        String field = fields.get(i);
        if (!(type instanceof Class)) {
          throw new UnsupportedOperationException(String.format("Can't project field %s of %s because of generic type %s. " +
                  "Currently only non-generic types (eg. String, LocalDate etc.) are supported in projections (not List<T> / Optional<T>).",
                  field,
                  query.entityPath().annotatedElement().getName(),
                  type));
        }
      }
      final Function<Path, Decoder<?>> decoderFn = path -> registry.get((Class<?>)  Expressions.returnType(path));
      this.decoderMap = projections.stream()
              .collect(Collectors.toMap(Mongos::toMongoFieldName, decoderFn));
    }

    @Override
    public ProjectedTuple decode(BsonReader reader, DecoderContext decoderContext) {
      BsonDocument doc = registry.get(BsonDocument.class).decode(reader, decoderContext);
      List<Object> values = new ArrayList<>();
      for (int i = 0; i < projections.size(); i++) {
        String path = Mongos.toMongoFieldName(projections.get(i));
        List<String> paths = Arrays.asList(path.split("\\."));
        BsonValue bson = resolveOrNull(doc, paths);
        Decoder<?> decoder = decoderMap.get(path);
        if (decoder == null) {
          throw new AssertionError("No decoder found for path " + path);
        }
        final Object value;
        if (!bson.isDocument()) {
          value = decoder.decode(new BsonValueReader(bson), decoderContext);
        } else {
          value = decoder.decode(new BsonDocumentReader(bson.asDocument()), decoderContext);
        }
        values.add(value);
      }

      return ProjectedTuple.of(query.projections(), values);
    }

    private static BsonValue resolveOrNull(BsonValue value, List<String> paths) {
      if (paths.isEmpty()) {
        return value;
      }

      if (!value.isDocument()) {
        return BsonNull.VALUE;
      }

      BsonDocument document = value.asDocument();
      final String first = paths.get(0);
      if (!document.containsKey(first)) {
        return BsonNull.VALUE;
      }

      return resolveOrNull(document.get(first), paths.subList(1, paths.size()));
    }

    @Override
    public void encode(BsonWriter writer, ProjectedTuple value, EncoderContext encoderContext) {
      throw new UnsupportedOperationException(String.format("%s can't encode %s (only decode)", getClass().getSimpleName(), getEncoderClass().getName()));
    }

    @Override
    public Class<ProjectedTuple> getEncoderClass() {
      return ProjectedTuple.class;
    }

  }

  /**
   * Simulates {@link BsonReader} but for a simple (scalar) {@link BsonValue} not {@link BsonDocument}
   */
  private static class BsonValueReader extends BsonDocumentReader {

    private BsonValueReader(BsonValue value) {
      super(fromValue(value));
      readStartDocument();
      String name = readName(); // value
      if (!name.equals("value")) {
        throw new IllegalStateException(String.format("Expected 'value' got %s", name));
      }
      // now marker should be at BsonValue
    }

    private static BsonDocument fromValue(BsonValue value) {
      return value.isDocument() ? value.asDocument() : new BsonDocument("value", value);
    }
  }


}
