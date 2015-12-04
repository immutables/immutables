```java
// Define abstract value type using interface, abstract class or annotation
@Value.Immutable
public interface ValueObject {
  String getName();
  List<Integer> getCounts();
  Optional<String> getDescription();
}
// Use generated immutable implementation and builder
ValueObject valueObject =
    ImmutableValueObject.builder()
        .name("Nameless")
        .description("present")
        .addCounts(1)
        .addCounts(2)
        .build();
```

Read full documentation at http://immutables.org

[![Build Status](https://travis-ci.org/immutables/immutables.svg?branch=master)](https://travis-ci.org/immutables/immutables)
