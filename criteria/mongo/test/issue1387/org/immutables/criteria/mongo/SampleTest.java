package issue1387.org.immutables.criteria.mongo;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.mongodb.reactivestreams.client.MongoClient;
import com.mongodb.reactivestreams.client.MongoClients;
import com.mongodb.reactivestreams.client.MongoDatabase;
import de.bwaldvogel.mongo.MongoServer;
import de.bwaldvogel.mongo.backend.memory.MemoryBackend;
import org.bson.codecs.configuration.CodecRegistry;
import org.immutables.criteria.mongo.MongoBackend;
import org.immutables.criteria.mongo.MongoSetup;
import org.immutables.criteria.mongo.bson4jackson.BsonModule;
import org.immutables.criteria.mongo.bson4jackson.IdAnnotationModule;
import org.immutables.criteria.mongo.bson4jackson.JacksonCodecs;
import org.immutables.criteria.repository.sync.SyncReader;
import org.junit.*;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.Optional;

/**
 * This test is created to in the context of https://github.com/immutables/immutables/issues/1387 .
 * Specifically it tests that @<code>*Repository.find(Criterion<T>)</code> can work using fields specified with getters.
 * E.g. getName() and getSurname() work with the update
 *
 * @author Spyros Koukas
 */
public class SampleTest {

    private final String databaseName = "test";


    private String mongoConnectionString;
    private MongoServer mongoServer;
    private MongoClient mongoClient;
    private SampleRepository sampleRepository;

    @Before
    public void beforeTest() {
        try {
            this.mongoServer = new MongoServer(new MemoryBackend());
            this.mongoServer.bind();
            final InetSocketAddress socketAddress = this.mongoServer.getLocalAddress();
            this.mongoConnectionString = "mongodb://" + socketAddress.getHostString() + ":" + socketAddress.getPort();

        } catch (final Exception exception) {
            Assume.assumeNoException("Exception while creating Mongo Server for tests", exception);
        }
        try {
            this.mongoClient = MongoClients.create(this.mongoConnectionString);

        } catch (final Exception exception) {
            Assume.assumeNoException("Exception while creating Mongo Client for tests", exception);
        }

        try {
            final CodecRegistry registry = JacksonCodecs.registryFromMapper(new ObjectMapper().registerModule(new BsonModule())  // register default codecs like Jsr310, BsonValueCodec, ValueCodecProvider etc.
                    .registerModule(new GuavaModule()) // for Immutable* classes from Guava (eg. ImmutableList)
                    .registerModule(new Jdk8Module()) // used for java 8 types like Optional / OptionalDouble etc.
                    .registerModule(new IdAnnotationModule())); // used for Criteria.Id to '_id' attribute mapping);
            final MongoDatabase mongoDatabase = this.mongoClient.getDatabase(this.databaseName).withCodecRegistry(registry);

            final MongoSetup setup = MongoSetup.of(mongoDatabase);
            final MongoBackend mongoBackend = new MongoBackend(setup);
            this.sampleRepository = new SampleRepository(mongoBackend);
            for (int i = 0; i < 10; i++) {
                sampleRepository.insert(Sample.createRandom());
            }
        } catch (final Exception exception) {
            Assume.assumeNoException("Exception while creating random samples", exception);
        }
    }

    @After
    public void afterTest() {
        try {
            if (this.mongoClient != null) {
                this.mongoClient.close();
                this.mongoClient = null;
            }
        } catch (final Exception exception) {
            //Silent
        }
        try {
            if (this.mongoServer != null) {
                this.mongoServer.shutdown();
                this.mongoServer = null;
            }
        } catch (final Exception exception) {
            //Silent
        }
        this.sampleRepository = null;
    }


    @Test
    public void getSampleByIdMongoBackend() {


        try {

            final Optional<Sample> sampleOptional = this.sampleRepository.findAll().fetch().stream().findAny();
            Assume.assumeTrue(sampleOptional.isPresent());
            final Sample sample = sampleOptional.get();
            final String sampleId = sample.getId();

            final SyncReader<Sample> syncReader = this.sampleRepository.find(SampleCriteria.sample.id.is(sampleId));
            final List<Sample> samples = syncReader.fetch();
            Assert.assertTrue("Could not fetch any elements by id:[" + sampleId + "]", !samples.isEmpty());
        } catch (final Exception exception) {
            Assume.assumeNoException(exception);
            throw new RuntimeException(exception);
        }
    }


    @Test
    public void getSampleByNameMongoBackend() {
        try {
            final Optional<Sample> sampleOptional = this.sampleRepository.findAll().fetch().stream().findAny();
            Assume.assumeTrue(sampleOptional.isPresent());
            final Sample sample = sampleOptional.get();
            final String sampleName = sample.getName();
            final SampleCriteria criterion = SampleCriteria.sample.name.is(sampleName);
            final SyncReader<Sample> syncReader = this.sampleRepository.find(criterion);
            final List<Sample> samples = syncReader.fetch();
            Assert.assertTrue("Could not fetch any elements by sampleName:[" + sampleName + "]", !samples.isEmpty());
        } catch (final Exception exception) {
            Assume.assumeNoException(exception);
            throw new RuntimeException(exception);
        }
    }


    @Test
    public void getSampleBySurnameMongoBackend() {
        try {

            final Optional<Sample> sampleOptional = this.sampleRepository.findAll().fetch().stream().findAny();
            Assume.assumeTrue(sampleOptional.isPresent());
            final Sample sample = sampleOptional.get();
            final String sampleSurname = sample.surname();
            final SampleCriteria criterion = SampleCriteria.sample.surname.is(sampleSurname);
            final SyncReader<Sample> syncReader = this.sampleRepository.find(criterion);
            final List<Sample> samples = syncReader.fetch();
            Assert.assertTrue("Could not fetch any elements by sampleSurname:[" + sampleSurname + "]", !samples.isEmpty());
        } catch (final Exception exception) {
            Assume.assumeNoException(exception);
            throw new RuntimeException(exception);
        }
    }

}
