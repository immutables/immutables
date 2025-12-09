![CI](https://github.com/immutables/immutables/workflows/CI/badge.svg) [![Sonatype Central](https://maven-badges.sml.io/sonatype-central/com.softwaremill.sttp.ai/core_3/badge.svg)](https://maven-badges.sml.io/sonatype-central/com.softwaremill.sttp.ai/core_3/) 

![imcover.png](imcover.png)

Full documentation at [immutables.org](http://immutables.org)

## Record Builder

```java
@Value.Builder
record Person(String name, int age, String email) {}

// Use the generated builder
Person person = new PersonBuilder()
    .name("Alice")
    .age(30)
    .email("alice@example.com")
    .build();
```

More fancy example having copy-with methods generated, and style `withUnaryOperator="with*"`

```java
@Value.Builder
record Person(String name, int age) implements WithPerson {
  // Extend the generated PersonBuilder
  static class Builder extends PersonBuilder {}
}

// Use your custom builder
var person = new Person.Builder()
    .name("Bob")
    .age(18)
    .build();

person = person.withName("Bobby!")
    .withAge(age -> age + 3);
```

## Immutable class

Minimal, classical style

```java
@Value.Immutable
interface Book {
  String isbn();
  String title();
  List<String> authors();
}

ImmutableBook book = ImmutableBook.builder()
    .isbn("978-1-56619-909-4")
    .title("The Elements of Style")
    .addAuthors("William Strunk Jr.", "E.B. White.")
    .build();
```

"sandwich" style, with nested builder and extending `With*` interface

```java
// Define abstract value type using interface, abstract class or annotation
@Value.Immutable
public interface ValueObject extends WithValueObject {
  // WithValueObject is not yet generated, We extend With* to inherit `with*` method signatures
  String name();
  List<Integer> counts();
  Optional<String> description();

  class Builder extends ImmutableValueObject.Builder {}
  // ImmutableValueObject.Builder will be generated and
  // our builder will inherit and reexport methods as its own.
  // Static nested Builder will inherit all the public method
  // signatures of ImmutableValueObject.Builder
} 

// Use generated immutable implementation and builder
var value = new ValueObject.Builder()
    .name("Nameless")
    .description("present")
    .addCounts(1)
    .addCounts(2)
    .build();

value = value.withName("Doe");

//fetch values via accessors
List<Integer> counts = v.counts();
Optional<String> description = v.description();
```

## Changelog

See [releases](https://github.com/immutables/immutables/releases) tab for release history. Archived [changelog](.archive/CHANGELOG.md) for earlier releases.
