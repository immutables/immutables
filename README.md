Read full documentation at http://immutables.org

![CI](https://github.com/immutables/immutables/workflows/CI/badge.svg)

## Modern usage style, aka "sandwich"
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
ValueObject v =
    new ValueObject.Builder()
        .name("Nameless")
        .description("present")
        .addCounts(1)
        .addCounts(2)
        .build();

v = v.withName("Doe");

//fetch values via accessors
List<Integer> counts = v.counts();
Optional<String> description = v.description();
```

ImmutableValueObject then would not be used outside generated type. See about this and other generation [styles here](https://immutables.github.io/style.html) 

See [releases](https://github.com/immutables/immutables/releases) tab for release history. Archived [changelog](.archive/CHANGELOG.md) for earlier releases.
