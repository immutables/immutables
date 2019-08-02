Mongo Adapter. Uses [reactive streams](https://mongodb.github.io/mongo-java-driver-reactivestreams/) driver.

## Integration with Jackson
[Jackson](https://github.com/FasterXML/jackson) is a popular java JSON library and criteria API provides easy integration
between [BSON](http://bsonspec.org/) and Jackson. This allows (for example) reuse of jackson annotations in rest (web) and mongo context. 

User can equally leverage existing codecs like [bson-codecs-jsr310](https://github.com/cbartosiak/bson-codecs-jsr310) to serialize
values in native BSON format (eg. [Decimal128](https://github.com/mongodb/specifications/blob/master/source/bson-decimal128/decimal128.rst))    

The integration is done at the lowest level between 
[BsonReader / BsonWriter](https://mongodb.github.io/mongo-java-driver/3.10/bson/readers-and-writers/) 
and [JsonParser](https://fasterxml.github.io/jackson-core/javadoc/2.8/com/fasterxml/jackson/core/JsonParser.html) so 
they don't incur additional transformation cost (eg. `BSON -> String -> Object`).

For more details look at [JacksonCodecs](https://github.com/immutables/immutables/blob/master/criteria/mongo/src/org/immutables/criteria/mongo/bson4jackson/JacksonCodecs.java) class 
 in `bson4jackon` package (nothing in common with another [bson4jackson](https://github.com/michel-kraemer/bson4jackson) library).

### Example
Snippet from mongo integration tests.
 
```java
// parameters Jackson serializer (it will be exposed as CodecRegistry)
final ObjectMapper mapper = new ObjectMapper()
          // allow java.time.* classes to be serialized in BSON native types: date / timestamp
         .registerModule(JacksonCodecs.module(new Jsr310CodecProvider())) 
         .registerModule(new GuavaModule()) // support Immutable* collections
         .registerModule(new Jdk8Module()) // support Optional (and others)
         .registerModule(new IdAnnotationModule()); // auto-detect @Criteria.Id annotation and persist is as _id

// connect to database
MongoDatabase database = ...
MongoCollection<Person> collection = database.getCollection("persons")
                     .withDocumentClass(Person.class)
                     .withCodecRegistry(JacksonCodecs.registryFromMapper(mapper));

PersonRepository repository = new PersonRepository(new MongoBackend(collection));
// insert an object
repository.insert(ImmutablePerson.builder().id("aaa").fullName("John Smith").age(22).build());
// read it
Publisher<Person> found = repository.findAll().fetch();
```  

Don't forget to add Jackson annotations to the model:
```java
@Value.Immutable
@Criteria
@Criteria.Repository
@JsonSerialize(as = ImmutablePerson.class) // serialize using jackson
@JsonDeserialize(as = ImmutablePerson.class) // deserializer using jackson
public interface Person 
{
  @Criteria.Id // will be serialized as _id
  String id();
  // ...
}
```
