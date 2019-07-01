The goal of Criteria module is to provide fluent java API (DSL) to query heterogeneous data-sources.

#### Main Features

1. **Expressive and type-safe API** Compile-type validation of the query.
2. **Dynamic** Combine predicates at runtime based on some logic
3. **Data-source agnostic** Define criteria once and apply to different data-sources (Map, JDBC, Mongo, Elastic etc.)
4. **Blocking / asynchronous operations** Generated repositories allow you to query data in blocking, non-blocking and [reactive](https://www.reactive-streams.org/) fashion

### Example

#### Define Model
Define your model using immutables interfaces
```java
@Value.Immutable
@Criteria // means generate criteria
@Criteria.Repository // means generate repository
interface Person {
    @Criteria.Id
    String id();
   
    String fullName();
  
    Optional<String> nickName();
  
    int age();
    
    Set<String> interests();
  
    List<Pet> pets();
}
```

#### Write query
Now you can write the following predicates:
```java
PersonCriteria.person.id.isIn("id1", "id2", "id3");

PersonCriteria.person
    .fullName.startsWith("John")
    .nickName.isAbsent()
    .or()
    .age.isGreaterThan(21)
    .nickName.value().startsWith("Adam");

PersonCriteria.person
    .pets.none().type.isEqualTo(Pet.PetType.iguana) // apply specific predicate to elements of a collection
    .or()
    .pets.any().name.contains("Fluffy");

```

#### Use generated repository to query or update a datasource
`@Criteria.Repository` instructs immutables to generate repository class with `find` / `insert` / `watch` operations.

```java

MongoCollection<Person> collection = ... // prepare collection with CodecRegistry
Backend backend = new MongoBackend(collection)l

// PersonRepository is automatically generated. You need to provide only backend instance 
PersonRepository repository = new PersonRepository(backend); 

repository.insert(ImmutablePerson.builder().id("aaa").fullName("John Smith").age(22).build());

// query repository
Publisher<Person> result = repository.find(PersonCriteria.person.fullName.contains("Smith")).fetch();
``` 

### Development 
`common` module contains runtime support. Remaining folders are backend implementation.

This folder contains classes specific to Criteria API and its runtime evaluation:

1. `common` shared classes by all modules
2. `elasticsearch` adapter for [Elastic Search](https://www.elastic.co/guide/en/elasticsearch/reference/current/query-dsl.html)
3. `mongo` adapter for [MongoDB](https://www.mongodb.com/) 
based on [reactive streams](https://mongodb.github.io/mongo-java-driver-reactivestreams/) driver.
4. `geode` adapter for [Apache Geode](https://geode.apache.org)
5. `inmemory` lightweight implementation of a backend based on existing Map

Criteria API requires Java 8 (or later)
