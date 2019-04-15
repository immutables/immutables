package org.immutables.mongo.fixture;

import com.google.common.util.concurrent.MoreExecutors;
import com.google.gson.GsonBuilder;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.conversions.Bson;
import org.immutables.mongo.repository.RepositorySetup;
import org.junit.Before;
import org.junit.Test;
import static org.immutables.check.Checkers.check;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Set of tests to ensure mongo resources are properly closed.
 *
 * @see MongoCursor#close()
 * @see <a href="https://docs.mongodb.com/v3.4/reference/method/cursor.close/">cursor.close</a>
 */
public class MongoCursorIsClosedTest {
  private EntityRepository repository;
  private MongoCursor<Entity> cursor;

  @Before
  @SuppressWarnings("unchecked")
  public void setUp() throws Exception {
    MongoDatabase db = mock(MongoDatabase.class);
    MongoCollection<Entity> collection = mock(MongoCollection.class);

    when(db.getCollection(anyString(), any(Class.class))).thenReturn(collection);
    when(collection.getDocumentClass()).thenReturn(Entity.class);
    when(collection.withCodecRegistry(any(CodecRegistry.class))).thenReturn(collection);
    when(collection.withDocumentClass(any(Class.class))).thenReturn(collection);

    RepositorySetup setup = RepositorySetup.builder().database(db)
            .executor(MoreExecutors.newDirectExecutorService())
            .gson(new GsonBuilder().registerTypeAdapterFactory(new GsonAdaptersEntity()).create())
            .build();

    this.repository = new EntityRepository(setup);

    FindIterable<Entity> iterable = mock(FindIterable.class);
    when(collection.find(any(Bson.class))).thenReturn(iterable);
    MongoCursor<Entity> cursor = mock(MongoCursor.class);
    when(iterable.iterator()).thenReturn(cursor);

    this.cursor = cursor;
  }

  @Test
  public void emptyResult() throws Exception {
    when(cursor.hasNext()).thenReturn(false);
    check(repository.findAll().fetchAll().getUnchecked()).isEmpty();

    verify(cursor, times(1)).close();
  }

  @Test
  public void singleResult() throws Exception {
    when(cursor.hasNext()).thenReturn(true).thenReturn(false);
    when(cursor.next()).thenReturn(ImmutableEntity.of("foo"));

    check(repository.findAll().fetchAll().getUnchecked()).has(ImmutableEntity.of("foo"));

    verify(cursor, times(1)).close();
  }

  @Test
  public void onException() throws Exception {
    when(cursor.hasNext()).thenReturn(true);
    when(cursor.next()).thenThrow(new IllegalStateException("boom"));

    try {
      repository.findAll().fetchAll().getUnchecked();
      fail("Shouldn't get here");
    } catch (Exception ignore) {
      // expected
    }

    verify(cursor, times(1)).close();
  }
}
