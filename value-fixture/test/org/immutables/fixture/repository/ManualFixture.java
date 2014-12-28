/*
    Copyright 2013-2014 Immutables Authors and Contributors

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
package org.immutables.fixture.repository;

import org.immutables.common.repository.Id;
import org.immutables.common.concurrent.FluentFuture;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
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
import org.immutables.fixture.ImmutableSillyEntity;
import org.immutables.fixture.ImmutableSillyEntitySecond;
import org.immutables.fixture.SillyEntity;
import org.immutables.fixture.SillyEntityMarshaler;
import org.immutables.fixture.SillyEntityRepository;
import org.immutables.fixture.SillyEntitySecond;
import org.immutables.fixture.SillyEntitySecondRepository;
import org.immutables.fixture.SillyStructureWithId;
import org.immutables.fixture.SillyStructureWithIdMarshaler;
import org.immutables.fixture.SillyStructureWithIdRepository;

@SuppressWarnings({"unused", "resource"})
public final class ManualFixture {

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

  public static void main999(String... args) {

    RepositorySetup setup = RepositorySetup.forUri("mongodb://localhost/test");

    SillyEntitySecondRepository repository = new SillyEntitySecondRepository(setup);

    repository.findAll().deleteAll().getUnchecked();

    ImmutableSillyEntitySecond build = ImmutableSillyEntitySecond.builder()
        .build();

    // repository.insert(build).getUnchecked();

    repository.insert(build).getUnchecked();
    repository.insert(ImmutableSillyEntitySecond.builder()
        .build()).getUnchecked();

//    repository.upsert(ImmutableSillyEntitySecond.builder().build()).getUnchecked();
//
//    Optional<SillyEntitySecond> unchecked = repository.find("{_id: %s}", "{$ne:null}")
//        .fetchFirst()
//        .getUnchecked();
//
//    System.out.println(unchecked);
  }

  public static void main(String... args) throws Exception {
    ListeningExecutorService executor = MoreExecutors.listeningDecorator(Executors.newCachedThreadPool());

    MongoClient mongo = new MongoClient();

    SillyEntityRepository repository = new SillyEntityRepository(
        RepositorySetup.builder()
            .database(mongo.getDB("test"))
            .executor(executor)
            .build());

    repository.index().withId().withInts().ensure().getUnchecked();

    repository.index().withId().withDerDesceding().named("Index222").ensure().getUnchecked();

    repository.findAll().deleteAll().getUnchecked();

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
        .returningNew()
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

  public static void main334477(String... args) throws Exception {
    ListeningExecutorService executor = MoreExecutors.listeningDecorator(Executors.newCachedThreadPool());

    MongoClient mongo = new MongoClient();

    SillyEntityRepository repository = new SillyEntityRepository(
        RepositorySetup.builder()
            .database(mongo.getDB("test"))
            .executor(executor)
            .build());

    repository.findAll().deleteAll().get();

    repository.insert(
        ImmutableSillyEntity.builder()
            .id(7777)
            .val("22222")
            .putPayload("AA", 9)
            .putPayload("BB", 9)
            .build())
        .get();

    repository.insert(
        ImmutableSillyEntity.builder()
            .id(3454)
            .val("43535")
            .putPayload("AA", 9)
            .putPayload("BB", 9)
            .build())
        .get();

    repository.insert(
        ImmutableSillyEntity.builder()
            .id(4656457)
            .val("dsfghfhd")
            .build())
        .get();

    List<SillyEntity> list = repository.findAll().fetchAll().get();

    System.out.println("!!!! " + list);

    // System.out.println();

//    repository.index()
//        .withIdDesceding()
//        .withPayload()
//        .named("myindex")
//        .ensure().get();
//
    repository.insert(ImmutableList.of(
        ImmutableSillyEntity.builder()
            .id(4)
            .val("11")
            .putPayload("AA", 9)
            .putPayload("BB", 9)
            .build(),
        ImmutableSillyEntity.builder()
            .id(2)
            .val("11")
            .putPayload("AA", 9)
            .putPayload("BB", 9)
            .build())).get();

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
            .build())).getUnchecked();

    List<SillyEntity> entities = repository.findAll()
        .fetchAll()
        .getUnchecked();

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
