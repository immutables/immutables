Read full documentation at http://immutables.org

![CI](https://github.com/immutables/immutables/workflows/CI/badge.svg)

## Modern usage style, aka "sandwich"
```java
// Define abstract value type using interface, abstract class or annotation
@Value.Immutable
public interface ValueObject extends WithValueObject {
  // WithValueObject is not yet generated, We extend With* to inherit `with*` method signatures
  String getName();
  List<Integer> getCounts();
  Optional<String> getDescription();

  class Builder extends ImmutableValueObject.Builder {}
  // ImmutableValueObject.Builder will be generated and
  // our builder will inherit and reexport methods as it's own.
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

//fetch values via standard getters
List<Integer> counts = v.getCounts();
Optional<String> description = v.getDescription();
```

ImmutableValueObject then would not be used outside of generated type. See about this and other generation [styles here](https://immutables.github.io/style.html) 

## License

```
   Copyright 2013-2023 Immutables Authors and Contributors

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
```

See [releases](https://github.com/immutables/immutables/releases) tab for release history. Archived [changelog](.archive/CHANGELOG.md) for earlier releases.
