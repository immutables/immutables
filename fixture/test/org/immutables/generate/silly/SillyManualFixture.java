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
package org.immutables.generate.silly;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.util.TokenBuffer;
import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.base.Splitter;
import com.google.common.base.Stopwatch;
import com.google.common.collect.ImmutableList;
import com.google.common.io.BaseEncoding;
import com.google.common.primitives.UnsignedInteger;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.LazyDBDecoder;
import com.mongodb.LazyDBObject;
import com.mongodb.MongoClient;
import de.undercouch.bson4jackson.BsonFactory;
import de.undercouch.bson4jackson.BsonGenerator;
import de.undercouch.bson4jackson.BsonParser;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executors;
import org.bson.LazyBSONCallback;
import org.immutables.common.repository.RepositorySetup;
import org.immutables.common.time.TimeMeasure;
import org.immutables.generate.silly.repository.SillyEntityRepository;
import org.immutables.generate.silly.repository.SillyEntitySecondRepository;
import org.immutables.generate.silly.repository.SillyStructureWithIdRepository;
import static org.immutables.check.Checkers.*;

@SuppressWarnings("unused")
public final class SillyManualFixture {

  static BsonFactory bsonFactory = new BsonFactory();

  static JsonFactory jsonFactory = new JsonFactory()
      .enable(JsonParser.Feature.ALLOW_SINGLE_QUOTES)
      .enable(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES)
      .disable(JsonGenerator.Feature.QUOTE_FIELD_NAMES);

  private static SillyStructureWithId fromJson(String string) throws IOException {
    SillyStructureWithIdRepository.where()
        .attr1Not("111")
        .idIn("ddd", "zzz")
        .subs6Empty()
        .subs6Size(23)
        .flag2(true)
        .or();

    JsonParser jsonParser = jsonFactory.createParser(string);
    jsonParser.nextToken();
    return SillyStructureWithIdMarshaler.unmarshal(jsonParser, null, SillyStructureWithId.class);
  }

  private static byte[] toBson(SillyStructureWithId structure) throws IOException {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    BsonGenerator bsonGen = bsonFactory.createJsonGenerator(baos);
    SillyStructureWithIdMarshaler.marshal(bsonGen, structure);
    bsonGen.flush();
    bsonGen.close();
    return baos.toByteArray();
  }

  public static void main(String... args) {

    RepositorySetup setup = RepositorySetup.forUri("mongodb://localhost/test");
    SillyEntitySecondRepository repository = SillyEntitySecondRepository.create(setup);

    repository.upsert(ImmutableSillyEntitySecond.builder().build()).getUnchecked();

  }

  public static void main3(String... args) throws Exception {
    JsonParser parser = jsonFactory.createParser("{a:1,b:2}");
    TokenBuffer tokenBuffer = new TokenBuffer(new ObjectMapper());

    JsonToken t = parser.nextToken();
    check(t).is(JsonToken.START_OBJECT);

    tokenBuffer.copyCurrentStructure(parser);
    System.out.println(tokenBuffer.asParser());
    tokenBuffer.close();
  }

  public static void main44(String... args) throws Exception {
    ListeningExecutorService executor = MoreExecutors.listeningDecorator(Executors.newCachedThreadPool());

    MongoClient mongo = new MongoClient();

    SillyEntityRepository repository = SillyEntityRepository.create(
        RepositorySetup.builder()
            .database(mongo.getDB("test"))
            .executor(executor)
            .build());

    repository.upsert(ImmutableSillyEntity.builder()
        .id(11)
        .val("aa")
        .build());

    repository.upsert(ImmutableSillyEntity.builder()
        .id(11)
        .val("bbb")
        .build()).get();

    repository.update(SillyEntityRepository.where().id(11))
        .setVal("yyy").updateAll().get();

    repository.update(SillyEntityRepository.where().id(133))
        .setVal("UUu")
        .putPayload("ff", 1)
        .addAllInts(Arrays.asList(1, 2, 2))
        .upsert().get();

    repository.update(SillyEntityRepository.where().id(133))
        .setVal("UUu").putPayload("ff", 1).removeInts(1).upsert().get();

    repository.find(SillyEntityRepository.where().id(133))
        .andModifyFirst()
        .setVal("UU1")
        .putPayload("ff", 888)
        .removeInts(2)
        .returnNew()
        .update()
        .addCallback(new FutureCallback<Optional<SillyEntity>>() {
          @Override
          public void onSuccess(Optional<SillyEntity> result) {
            System.out.println(result);
          }

          @Override
          public void onFailure(Throwable t) {}
        })
        .get();

    executor.shutdown();

    TimeMeasure.seconds(2).sleep();
  }

  public static void main33(String... args) throws Exception {
    ListeningExecutorService executor = MoreExecutors.listeningDecorator(Executors.newCachedThreadPool());

    MongoClient mongo = new MongoClient();

    SillyEntityRepository repository = SillyEntityRepository.create(
        RepositorySetup.builder()
            .database(mongo.getDB("test"))
            .executor(executor)
            .build());

    repository.index()
        .withIdDesceding()
        .withPayload()
        .named("myindex")
        .ensure().get();

    repository.insert(ImmutableList.of(
        ImmutableSillyEntity.builder()
            .id(9)
            .val("11")
            .putPayload("AA", 9)
            .putPayload("BB", 9)
            .build(),
        ImmutableSillyEntity.builder()
            .id(5)
            .val("455")
            .putPayload("CC", 5)
            .putPayload("DD", 5)
            .build()));

    TimeMeasure.seconds(1).sleep();
    List<SillyEntity> unchecked =
        repository.find(
            SillyEntityRepository.where()
                .idNot(15)
                .valStartsWith("1")
                .derAtLeast(UnsignedInteger.valueOf(2)))
            .orderByIdDesceding()
            .fetchAll()
            .getUnchecked();

    System.out.println(unchecked);

    System.out.println(repository.findById(5).fetchFirst().getUnchecked().get().val());

    System.out.println();

    for (int i = 0; i < 1; i++) {
      List<SillyEntity> readIt = readIt(repository);
      System.out.println(readIt);
    }

    executor.shutdown();

    TimeMeasure.seconds(2).sleep();
  }

  private static List<SillyEntity> readIt(SillyEntityRepository repository) {
    Stopwatch w = Stopwatch.createStarted();

    List<SillyEntity> unchecked =
        repository.find(SillyEntityRepository.where())
            .orderByIdDesceding()
            .fetchAll()
            .getUnchecked();

    System.out.println(w.stop());
    return unchecked;
  }

  public static void main1(String... args) throws Exception {

    byte[] data =
        BaseEncoding.base16()
            .decode(Joiner.on("")
                .join(Splitter.on(' ')
                    .split("26 00 00 00 10 5F 69 64 00 05 00 00 00 03 70 00 15 00 00 00 10 43 43 00 05 00 00 00 10 44 44 00 05 00 00 00 00 00")));

    BsonParser p = new BsonFactory().createParser(new ByteArrayInputStream(data));
    p.nextToken();

    SillyEntity unmarshal = SillyEntityMarshaler.instance().unmarshalInstance(p);

    System.out.println(unmarshal);

    MongoClient mongo = new MongoClient();
    DB db = mongo.getDB("test");
    DBCollection cl = db.getCollection("silly");

    cl.setDBDecoderFactory(LazyDBDecoder.FACTORY);

    SillyStructureWithId structure =
        fromJson("{_id:'zzz2',attr1:'x', flag2:false,opt3:1, very4:33, wet5:555.55, subs6:null,"
            + " nest7:{ set2:'METHOD', set3: [1,2,4],floats4:[333.11] },"
            + "int9:0, tup3: [1212.441, null, [true,true,false]]}");

    LazyDBObject dbObject = new LazyDBObject(toBson(structure), new LazyBSONCallback());
    cl.insert(dbObject);

    DBCursor find = cl.find();

    List<DBObject> array = find.toArray();

    System.out.println(array.size());

  }

}
