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

[Changelog](https://github.com/immutables/immutables/blob/master/changelog.md)

[![Build Status](https://travis-ci.org/immutables/immutables.svg?branch=master)](https://travis-ci.org/immutables/immutables)


```
   Copyright 2013-2016 Immutables Authors and Contributors

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