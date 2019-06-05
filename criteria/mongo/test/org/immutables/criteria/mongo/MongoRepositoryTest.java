package org.immutables.criteria.mongo;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.mongodb.reactivestreams.client.MongoClient;
import com.mongodb.reactivestreams.client.MongoClients;
import com.mongodb.reactivestreams.client.MongoCollection;
import de.bwaldvogel.mongo.MongoServer;
import de.bwaldvogel.mongo.backend.memory.MemoryBackend;
import io.reactivex.Flowable;
import org.immutables.criteria.DocumentCriteria;
import org.immutables.criteria.mongo.bson4jackson.JacksonCodecs;
import org.immutables.criteria.personmodel.Person;
import org.immutables.criteria.personmodel.PersonCriteria;
import org.immutables.criteria.personmodel.PersonGenerator;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

/**
 * Basic tests of mongo adapter
 */
public class MongoRepositoryTest {


  private final MongoServer server = new MongoServer(new MemoryBackend());
  private final MongoClient client = MongoClients.create(String.format("mongodb://localhost:%d", server.bind().getPort()));

  private MongoCollection<Person> collection;

  private MongoRepository<Person> repository;

  @Before
  public void setUp() throws Exception {
    final ObjectMapper mapper = new ObjectMapper()
            .registerModule(new JavaTimeModule()) // need to support native BSON codecs
            .registerModule(new GuavaModule())
            .registerModule(new Jdk8Module());

    Flowable.fromPublisher(client.getDatabase("test").createCollection("test"))
            .test()
            .awaitDone(1, TimeUnit.SECONDS)
            .assertComplete();


    this.collection = client.getDatabase("test").getCollection("test")
            .withDocumentClass(Person.class)
            .withCodecRegistry(JacksonCodecs.registryFromMapper(mapper));

    this.repository = new MongoRepository<>(this.collection);

    Flowable.fromPublisher(repository.insert(PersonGenerator.of("test")))
            .test()
            .awaitDone(1, TimeUnit.SECONDS)
            .assertComplete();

  }

  @After
  public void tearDown() throws Exception {
    client.close();
    server.shutdownNow();
  }

  @Test
  public void basic() {
    execute(PersonCriteria.create().fullName.isEqualTo("test"), 1);
    execute(PersonCriteria.create().fullName.isEqualTo("test")
            .age.isNotEqualTo(1), 1);
    execute(PersonCriteria.create().fullName.isEqualTo("_MISSING_"), 0);
  }

  private void execute(DocumentCriteria<Person> expr, int count) {

    Flowable.fromPublisher(repository.find(expr).fetch())
            .test()
            .awaitDone(1, TimeUnit.SECONDS)
            .assertValueCount(count);

  }
}
