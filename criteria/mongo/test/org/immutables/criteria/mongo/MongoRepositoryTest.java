package org.immutables.criteria.mongo;

import com.mongodb.reactivestreams.client.MongoClient;
import com.mongodb.reactivestreams.client.MongoClients;
import com.mongodb.reactivestreams.client.MongoCollection;
import de.bwaldvogel.mongo.MongoServer;
import de.bwaldvogel.mongo.backend.memory.MemoryBackend;
import io.reactivex.Flowable;
import org.immutables.criteria.Person;
import org.immutables.criteria.PersonCriteria;
import org.immutables.criteria.constraints.Expressional;
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

    Flowable.fromPublisher(client.getDatabase("test").createCollection("test"))
            .test()
            .awaitTerminalEvent();

    this.collection = client.getDatabase("test").getCollection("test").withDocumentClass(Person.class);
    this.repository = new MongoRepository<>(this.collection);
  }

  @After
  public void tearDown() throws Exception {
    client.close();
    server.shutdownNow();
  }

  @Test
  public void basic() {
    execute(PersonCriteria.create().firstName.isEqualTo("aaa"));
    execute(PersonCriteria.create().firstName.isEqualTo("aaa")
            .age.isNotEqualTo(22));

  }

  private void execute(Expressional<Person> expr) {

    Flowable.fromPublisher(repository.query(expr))
            .test()
            .awaitDone(1, TimeUnit.SECONDS)
            .assertValueCount(0);

  }
}