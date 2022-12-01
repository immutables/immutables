package issue1387.org.immutables.criteria.mongo;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.mongodb.reactivestreams.client.MongoClient;
import com.mongodb.reactivestreams.client.MongoClients;
import com.mongodb.reactivestreams.client.MongoDatabase;
import org.bson.codecs.configuration.CodecRegistry;
import org.immutables.criteria.mongo.MongoBackend;
import org.immutables.criteria.mongo.MongoSetup;
import org.immutables.criteria.mongo.bson4jackson.BsonModule;
import org.immutables.criteria.mongo.bson4jackson.IdAnnotationModule;
import org.immutables.criteria.mongo.bson4jackson.JacksonCodecs;
import org.immutables.criteria.repository.sync.SyncReader;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;

import java.util.List;
import java.util.Optional;

public class SampleTest {


    @Test
    public void getSampleByIdMongoBackend() {
        final String databaseName="test";
        ObjectMapper mapper = new ObjectMapper()
                .registerModule(new BsonModule())  // register default codecs like Jsr310, BsonValueCodec, ValueCodecProvider etc.
                .registerModule(new GuavaModule()) // for Immutable* classes from Guava (eg. ImmutableList)
                .registerModule(new Jdk8Module()) // used for java 8 types like Optional / OptionalDouble etc.
                .registerModule(new IdAnnotationModule()); // used for Criteria.Id to '_id' attribute mapping
//        mapper.getPropertyNamingStrategy().nameForGetterMethod()
        final CodecRegistry registry = JacksonCodecs.registryFromMapper(mapper);


        final MongoClient client = MongoClients.create("mongodb://127.0.0.1/"+databaseName);

        final MongoDatabase mongoDatabase = client.getDatabase(databaseName).withCodecRegistry(registry);
        final MongoSetup setup = MongoSetup.of(mongoDatabase);
        final MongoBackend mongoBackend = new MongoBackend(setup);
        final SampleRepository sampleRepository = new SampleRepository(mongoBackend);
        for (int i = 0; i < 10; i++) {
            sampleRepository.insert(Sample.createRandom());
        }
        final Optional<Sample> sampleOptional = sampleRepository.findAll().fetch().stream().findAny();
        Assume.assumeTrue(sampleOptional.isPresent());
        final Sample sample = sampleOptional.get();
        final String sampleId = sample.getId();

        final SyncReader<Sample> syncReader = sampleRepository.find(SampleCriteria.sample.id.is(sampleId));
        final List<Sample> samples = syncReader.fetch();
        Assert.assertTrue("Could not fetch any elements by id:[" + sampleId+"]", !samples.isEmpty());
    }


    @Test
    public void getSampleByNameMongoBackend() {
        final String databaseName="test";
        ObjectMapper mapper = new ObjectMapper()
                .registerModule(new BsonModule())  // register default codecs like Jsr310, BsonValueCodec, ValueCodecProvider etc.
                .registerModule(new GuavaModule()) // for Immutable* classes from Guava (eg. ImmutableList)
                .registerModule(new Jdk8Module()) // used for java 8 types like Optional / OptionalDouble etc.
                .registerModule(new IdAnnotationModule()); // used for Criteria.Id to '_id' attribute mapping
        final CodecRegistry registry = JacksonCodecs.registryFromMapper(mapper);


        final MongoClient client = MongoClients.create("mongodb://127.0.0.1/"+databaseName);

        final MongoDatabase mongoDatabase = client.getDatabase(databaseName).withCodecRegistry(registry);
        final MongoSetup setup = MongoSetup.of(mongoDatabase);
        final MongoBackend mongoBackend = new MongoBackend(setup);
        final SampleRepository sampleRepository = new SampleRepository(mongoBackend);
        for (int i = 0; i < 1; i++) {
            sampleRepository.insert(Sample.createRandom());
        }
        final Optional<Sample> sampleOptional = sampleRepository.findAll().fetch().stream().findAny();
        Assume.assumeTrue(sampleOptional.isPresent());
        final Sample sample = sampleOptional.get();
        final String sampleName = sample.getName();
        final SampleCriteria criterion=SampleCriteria.sample.name.is(sampleName);
        final SyncReader<Sample> syncReader = sampleRepository.find(criterion);
        final List<Sample> samples = syncReader.fetch();
        Assert.assertTrue("Could not fetch any elements by sampleName:[" + sampleName+"]", !samples.isEmpty());
    }


    @Test
    public void getSampleBySurnameMongoBackend() {
        final String databaseName="test";
        ObjectMapper mapper = new ObjectMapper()
                .registerModule(new BsonModule())  // register default codecs like Jsr310, BsonValueCodec, ValueCodecProvider etc.
                .registerModule(new GuavaModule()) // for Immutable* classes from Guava (eg. ImmutableList)
                .registerModule(new Jdk8Module()) // used for java 8 types like Optional / OptionalDouble etc.
                .registerModule(new IdAnnotationModule()); // used for Criteria.Id to '_id' attribute mapping
        final CodecRegistry registry = JacksonCodecs.registryFromMapper(mapper);


        final MongoClient client = MongoClients.create("mongodb://127.0.0.1/"+databaseName);

        final MongoDatabase mongoDatabase = client.getDatabase(databaseName).withCodecRegistry(registry);
        final MongoSetup setup = MongoSetup.of(mongoDatabase);
        final MongoBackend mongoBackend = new MongoBackend(setup);
        final SampleRepository sampleRepository = new SampleRepository(mongoBackend);
        for (int i = 0; i < 1; i++) {
            sampleRepository.insert(Sample.createRandom());
        }
        final Optional<Sample> sampleOptional = sampleRepository.findAll().fetch().stream().findAny();
        Assume.assumeTrue(sampleOptional.isPresent());
        final Sample sample = sampleOptional.get();
        final String sampleSurname = sample.surname();
        final SampleCriteria criterion=SampleCriteria.sample.surname.is(sampleSurname);
        final SyncReader<Sample> syncReader = sampleRepository.find(criterion);
        final List<Sample> samples = syncReader.fetch();
        Assert.assertTrue("Could not fetch any elements by sampleSurname:[" + sampleSurname+"]", !samples.isEmpty());
    }

}
