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

package org.immutables.mongo.bson4gson;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import org.bson.*;
import org.bson.codecs.DateCodec;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Date;

import static org.immutables.check.Checkers.check;

/**
 * Tests that {@code java.time.*} and {@link Date} BSON codecs are properly working with Gson type adapters.
 */
public class JavaTimeTest {

  private Gson gson;

  @Before
  public void setUp() {
    TypeAdapter<Date> dateTypeAdapter = GsonCodecs.typeAdapterFromCodec(new DateCodec());
    gson = GsonCodecs.newGsonWithBsonSupport(
            new GsonBuilder().registerTypeAdapter(Date.class, dateTypeAdapter).create()
    );
  }

  @Test
  public void localDate() throws IOException {
    long epoch = System.currentTimeMillis();
    LocalDate now = Instant.ofEpochMilli(epoch).atOffset(ZoneOffset.UTC).toLocalDate();

    TypeAdapter<LocalDate> adapter = gson.getAdapter(LocalDate.class);

    // read
    LocalDate date = adapter.read(Jsons.readerAt(new BsonDateTime(epoch)));
    check(date).is(now);

    // write
    BsonValue bson = writeAndReadBson(now);
    check(bson.getBsonType()).is(BsonType.DATE_TIME);
    check(Instant.ofEpochMilli(bson.asDateTime().getValue()).atOffset(ZoneOffset.UTC).toLocalDate()).is(now);
  }

  @Test
  public void localDateTime() throws IOException {
    LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);
    long epoch = now.toInstant(ZoneOffset.UTC).toEpochMilli();

    TypeAdapter<LocalDateTime> adapter = gson.getAdapter(LocalDateTime.class);

    // read
    LocalDateTime date = adapter.read(Jsons.readerAt(new BsonDateTime(epoch)));
    LocalDateTime valueRead = Instant.ofEpochMilli(epoch).atOffset(ZoneOffset.UTC).toLocalDateTime();
    check(date).is(now);

    // write
    BsonValue bson = writeAndReadBson(valueRead);
    check(bson.getBsonType()).is(BsonType.DATE_TIME);
    check(Instant.ofEpochMilli(bson.asDateTime().getValue()).atOffset(ZoneOffset.UTC).toLocalDateTime()).is(valueRead);
  }

  @Test
  public void instant() throws IOException {
    Instant now = Instant.now().truncatedTo(ChronoUnit.MILLIS);
    long epoch = now.toEpochMilli();

    TypeAdapter<Instant> adapter = gson.getAdapter(Instant.class);

    // read
    Instant date = adapter.read(Jsons.readerAt(new BsonDateTime(epoch)));
    check(date).is(now);

    // write
    BsonValue bson = writeAndReadBson(now);
    check(bson.getBsonType()).is(BsonType.DATE_TIME);
    check(Instant.ofEpochMilli(bson.asDateTime().getValue())).is(now);
  }

  @Test
  public void javaUtilDate() throws IOException {
    Date now = new Date();
    long epoch = now.getTime();

    TypeAdapter<Date> adapter = gson.getAdapter(Date.class);

    // read
    Date date = adapter.read(Jsons.readerAt(new BsonDateTime(epoch)));
    check(date).is(now);

    // write
    BsonValue bson = writeAndReadBson(now);
    check(bson.getBsonType()).is(BsonType.DATE_TIME);
    check(new Date(bson.asDateTime().getValue())).is(now);
  }

  @Test
  public void javaUtilDateUsingGson() throws IOException {

    Gson gsonWithInternalDateTypeAdapter = GsonCodecs.newGsonWithBsonSupport(new Gson());

    TypeAdapter<Date> adapter = gsonWithInternalDateTypeAdapter.getAdapter(Date.class);

    Date now = Date.from(Instant.now().truncatedTo(ChronoUnit.SECONDS));

    // write
    BsonValue bson = writeAndReadBsonUsingGson(now, gsonWithInternalDateTypeAdapter);
    check(bson.getBsonType()).is(BsonType.STRING);
    String dateAsString = bson.asString().getValue();
    check(dateAsString).isNonEmpty();

    // read
    Date date = adapter.read(Jsons.readerAt(new BsonString(dateAsString)));
    check(date).is(now);
  }

  private <T> BsonValue writeAndReadBson(T value) throws IOException {
    return writeAndReadBsonUsingGson(value, gson);
  }

  private static <T> BsonValue writeAndReadBsonUsingGson(T value, Gson gson) throws IOException {
    TypeAdapter<T> adapter = gson.getAdapter((Class<T>) value.getClass());
    BsonDocumentWriter writer = new BsonDocumentWriter(new BsonDocument());
    writer.writeStartDocument();
    writer.writeName("value");
    adapter.write(Jsons.asGsonWriter(writer), value);
    writer.writeEndDocument();

    BsonValue bson = writer.getDocument().get("value");
    check(bson).notNull();
    return bson;
  }

}
