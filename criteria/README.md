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
    List<Pet> pets();
}
```

#### Write query
Now you can write the following queries:
```java
PersonCriteria.person.id.isIn("id1", "id2", "id3");

person
    .fullName.startsWith("John") // basic string conditions
    .fullName.isEqualTo(3.1415D) // will not compile since fullName is String
    .nickName.isAbsent() // for Optional attribute
    .or() // disjunction
    .age.isGreaterThan(21)
    .nickName.value().startsWith("Adam")
    .or()
    .not(p -> p.nickName.value().hasSize(4)); // negation

// apply specific predicate to elements of a collection
person
    .pets.none().type.isEqualTo(Pet.PetType.iguana)  // no Iguanas
    .or()
    .pets.any().name.contains("fluffy"); // person has a pet which sounds like fluffy

```

You will notice that there are no `and` statements (conjunctions) that is because criteria uses 
[Disjunctive Normal Form](https://en.wikipedia.org/wiki/Disjunctive_normal_form) (in short DNF) by default. 

For more complex expressions, one can still combine criterias arbitrarily using `and`s / `or`s / `not`s. 
Statement like `A and (B or C)` can be written as follows:
```java
person.fullName.isEqualTo("John").and(person.age.isGreaterThan(22).or().nickName.isPresent())
```

#### Use generated repository to query or update a datasource
`@Criteria.Repository` instructs immutables to generate repository class with `find` / `insert` / `watch` operations.
You are required to provide a valid [backend](https://github.com/immutables/immutables/blob/master/criteria/common/src/org/immutables/criteria/adapter/Backend.java) 
instance (mongo, elastic, inmemory etc).

```java
MongoCollection<Person> collection = ... // prepare collection with DocumentClass / CodecRegistry
Backend backend = new MongoBackend(collection);

// PersonRepository is automatically generated. You need to provide only backend instance 
PersonRepository repository = new PersonRepository(backend); 

repository.insert(ImmutablePerson.builder().id("aaa").fullName("John Smith").age(22).build());

// query repository
Publisher<Person> result = repository.find(person.fullName.contains("Smith")).fetch();
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
