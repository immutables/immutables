package org.immutables.mongo.fixture;

import com.google.common.util.concurrent.MoreExecutors;
import com.mongodb.MongoClientSettings;
import org.bson.BsonReader;
import org.bson.BsonWriter;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import org.bson.codecs.configuration.CodecConfigurationException;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.types.ObjectId;
import org.immutables.gson.Gson;
import org.immutables.mongo.Mongo;
import org.immutables.mongo.repository.RepositorySetup;
import org.immutables.value.Value;
import org.junit.Rule;
import org.junit.Test;

import java.lang.reflect.Member;
import java.util.Date;

import static org.immutables.check.Checkers.check;

/**
 * Tests repositories which are using directly with Bson codecs (not {@code Gson} adapter).
 */
@Gson.TypeAdapters
public class BsonCodecRepoTest {

  @Rule
  public final MongoContext context = MongoContext.create();

  @Test
  public void basic() {
    LocalCodec codecAndStrategy = new LocalCodec();

    CodecRegistry registry = CodecRegistries.fromRegistries(codecAndStrategy,
            MongoClientSettings.getDefaultCodecRegistry());


    RepositorySetup.FieldNamingStrategy strategy = new RepositorySetup.FieldNamingStrategy() {
      @Override
      public String translateName(Member member) {
        return "date".equals(member.getName()) ? "dateChanged" : member.getName();
      }
    };

    RepositorySetup setup = RepositorySetup.builder()
            .executor(MoreExecutors.newDirectExecutorService())
            .database(context.database())
            .codecRegistry(registry, strategy)
            .build();

    SomebodyRepository repository = new SomebodyRepository(setup);

    ImmutableSomebody somebody = ImmutableSomebody.builder().id(new ObjectId()).date(new Date()).prop1("prop1").build();
    repository.insert(somebody)
            .getUnchecked();

    check(repository.findById(somebody.id()).fetchAll().getUnchecked()).hasAll(somebody);
    check(repository.find(repository.criteria().prop1(somebody.prop1())).fetchAll().getUnchecked()).hasAll(somebody);
    check(repository.find(repository.criteria().prop1("unknown")).fetchAll().getUnchecked()).isEmpty();
    check(repository.find(repository.criteria().date(somebody.date())).fetchAll().getUnchecked()).hasAll(somebody);
    check(repository.find(repository.criteria().date(new Date(somebody.date().getTime() + 1)))
            .fetchAll().getUnchecked()).isEmpty();
  }

  /**
   * Manually reads and writes all attributes
   */
  private static class LocalCodec implements Codec<Somebody>, CodecRegistry  {

    @Override
    public Somebody decode(BsonReader reader, DecoderContext decoderContext) {
      reader.readStartDocument();
      ObjectId id = reader.readObjectId("_id");
      String prop1 = reader.readString("prop1");
      Date date = new Date(reader.readDateTime("dateChanged"));
      reader.readEndDocument();
      return ImmutableSomebody.builder().id(id).prop1(prop1).date(date).build();
    }

    @Override
    public void encode(BsonWriter writer, Somebody value, EncoderContext encoderContext) {
      writer.writeStartDocument();
      writer.writeObjectId("_id", value.id());
      writer.writeString("prop1", value.prop1());
      writer.writeDateTime("dateChanged", value.date().getTime());
      writer.writeEndDocument();
    }

    @Override
    public Class<Somebody> getEncoderClass() {
      return Somebody.class;
    }

    @SuppressWarnings("unchecked") // hopefully correct
    @Override
    public <T> Codec<T> get(Class<T> clazz) {
      if (!Somebody.class.isAssignableFrom(clazz)) {
        throw new CodecConfigurationException("Not supported " + clazz);
      }
      return (Codec<T>) this;
    }

    @Override
    public <T> Codec<T> get(Class<T> clazz, CodecRegistry registry) {
      try {
        return get(clazz);
      } catch (CodecConfigurationException e) {
        return null;
      }
    }
  }

  @Value.Immutable
  @Mongo.Repository
  interface Somebody {

    @Mongo.Id
    ObjectId id();

    String prop1();

    // field name changed to 'dateChanged' when serialized
    Date date();

  }
}
