```java
// Define abstract value type using interface, abstract class or annotation
@Value.Immutable
public interface ValueObject extend WithValueObject {
  // extend not-yet-generated WithValueObject to inherit `with*` method signatures
  String getName();
  List<Integer> getCounts();
  Optional<String> getDescription();

  class Builder extends ImmutableValueObject.Builder {}
  // ImmutableValueObject.Builder will be generated and
  // our builder will inherit and reexport methods as it's own
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
```

Read full documentation at http://immutables.org

[![Build Status](https://travis-ci.org/immutables/immutables.svg?branch=master)](https://travis-ci.org/immutables/immutables)

License
---------

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

Changelog
---------

### 2.2.5 (2016-06-09)
- Thanks to contributors and issue reporters!
- Bugfix and minor enhancement release
  + Numerous fixes for generics and interactions with other features
  + SkipNulls and Nullable elements in maps and collections if you really need them (#349)
  + `With*` interface generation to finely hide implementation classes while still having `with*` methods in API
  + Straw man support for custom collections (see [/META-INF/extensions/org.immutables.collect](https://github.com/immutables/immutables/blob/master/value-fixture/src/META-INF/extensions/org.immutables.collect))
- [Issues](https://github.com/immutables/immutables/issues?q=milestone%3A2.2.5)

### 2.2.2 (2016-06-03)
- Thanks to contributors and issue reporters!
- Bugfix release
  + Running in Eclipse Neon RC due to APT classloading changes
- [Issues](https://github.com/immutables/immutables/issues?q=milestone%3A2.2.2)

### 2.2.1 (2016-05-27)
- Thanks to contributors and issue reporters!
- Bugfixes and functionality refinements
  + Can disable generation of META-INF/services for Gson adapters `Gson.TypeAdapters.metainfServices = false` 
  + Gson Adapters may be configured to skip nulls in favor of default values. `Gson.TypeAdapters.nullAsDefault = true`
  + Numerous bug fixes
- [Issues](https://github.com/immutables/immutables/issues?q=milestone%3A2.2.1)

### 2.2 (2016-05-16)
- Thanks to contributors and issue reporters!
- Improvements
  + Generic parameters support for Immutable objects! Modifiables are supported (likely).
  + Jackson/Gson Adapters support for generics
  + Collections now can be nullable and/or also `@Value.Default`. Default attributes can be nullable, yet allowing arbitrary default values.
  + Depluralization dictionaries can be specified on multiple levels (type, package, meta-annotation). All applicable dictionary entries will be merged.
- Backward compatibility should be preserved.
- [Issues](https://github.com/immutables/immutables/issues?q=milestone%3A2.2)

### 2.1.19 (2016-05-04)
- Minor Hotfix, Thank you!
- [Issues](https://github.com/immutables/immutables/issues?q=milestone%3A2.1.19)

### 2.1.18 (2016-04-26)
- Minor Bugfixes, Thank you!
- [Issues](https://github.com/immutables/immutables/issues?q=milestone%3A2.1.18)

### 2.1.16 (2016-04-14)
- Thanks to contributors and issue reporters, important bugfixes were made!
- [Issues](https://github.com/immutables/immutables/issues?q=milestone%3A2.1.16)
- Minor enhancements
  + Javaslang Option support
  + More compact toString, omitting non-present Optionals and nulls
  + Cancel-out individual parameters when `allParameters = true`

### 2.1.14 (2016-03-12)
- Bugfix and minor improvement release.
  + Output package pattern can be configured using style
  + Ad-Hoc normalization using Value.Check
  + `Style(deepImmutablesDetection = true)` behavior refined
  + Refined `JsonSerialize` enclosing top-level type annotation handling
- Thanks to contributors and issue reporters!
- [Issues](https://github.com/immutables/immutables/issues?q=milestone%3A2.1.14)

### 2.1.12 (2016-02-29)
- Bugfix and minor changes release.
  + Findbugs' `SuppressFBWarnings` placed on immutable implementation if found in the classpath
  + Primitive wrapper types and strings have an `equals` comparison now in `with*` methods in short-circuiting check to `return this`.
  + Fixed some Eclipse specific compilation bugs. 
- Thanks to contributors and issue reporters!
- [Issues](https://github.com/immutables/immutables/issues?q=milestone%3A2.1.12)

### 2.1.11 (2016-02-18)
- Bugfix and Minor improvement release:
  + Depluralization for `add*` and `put*` methods enabled with `Style(depluralize, depluralizeDictionary)`
  + Fixed regression while using `java.util.Optional*` with `Builder.Factory` caused by auto-unboxing to nullable fields in 2.1.9
  + Improved handling for some internal problems in the processor
- Thanks to contributors and issue reporters!
- [Issues](https://github.com/immutables/immutables/issues?q=milestone%3A2.1.11)

### 2.1.10 (2016-02-02)
- Bugfix release:
  + License headers, added missing, plus some cleanup
  + Fixed override of final method in some cases
  + Javadoc linting to fix javadocs in generated code
- Thanks to contributors and issue reporters!  
- [Issues](https://github.com/immutables/immutables/issues?q=milestone%3A2.1.10)

### 2.1.9 (2016-01-26)
- Bugfix and Minor improvement release:
  + Automatic unboxing and boxing of java.util.Optional<A> to nullable A fields
- Thanks to contributors and issue reporters!  
- [Issues](https://github.com/immutables/immutables/issues?q=milestone%3A2.1.9)

### 2.1.8 (2016-01-15)
- Bugfix and Minor improvement release:
  + New 'func' module: Functions and predicate generator (for Guava, pre java 8)
  + `@Builder.Parameter` and `@Builder.Switch` are working on value attributes now
  + New 'android-stub' module, may be useful to compile android libraries using Immutables for API level < 19
- Thanks to contributors and issue reporters!  
- [Issues](https://github.com/immutables/immutables/issues?q=milestone%3A2.1.8)

### 2.1.5 (2016-01-02)
- Bugfix and Minor improvement release.
- Thanks to contributors and issue reporters!
- [Issues](https://github.com/immutables/immutables/issues?q=milestone%3A2.1.5)
  + Public constructors style

### 2.1.4 (2015-12-10)
- Bugfix and Minor improvement release.
- Thanks to contributors and issue reporters!
- [Issues](https://github.com/immutables/immutables/issues?q=milestone%3A2.1.4+is%3Aclosed)
  + Gson 2.5 support for SerializedName annotation including alternate names

### 2.1.1 (2015-12-03)
- Bugfix and Minor improvement release.
- Many thanks to all user and contributors for issue reports, PRs and suggestions!
  + Notable JSON fixes. Courtesy of @ldriscoll
  + Generated Javadocs corrections and proofreading. Courtesy of @io7m and @trask
  + Atlassian Fugue 2.x and 3.x `Option` support. Courtesy of @mzeijen
- [Issues](https://github.com/immutables/immutables/issues?q=milestone%3A2.1.1+is%3Aclosed)

### 2.1.0 (2015-10-23)
+ Added `Value.Modifiable` annotation to create modifiable companion classes, which may serve as uber-builder or instead of `buildPartial` in other builder toolkits.
+ Added number of minor styles and feature flags and refinements of existing functionality
+ Numerous bugfixes
+ [Issues](https://github.com/immutables/immutables/issues?q=milestone%3A2.1+is%3Aclosed)

### 2.0.18 (2015-08-13)
+ Bugfix and minor enhancement release [Issues](https://github.com/immutables/immutables/issues?q=milestone%3A2.0.18+is%3Aclosed)

### 2.0.17 (2015-08-06)
+ Bugfix and minor enhancement release [Issues](https://github.com/immutables/immutables/issues?q=milestone%3A2.0.17+is%3Aclosed)

### 2.0.16 (2015-07-09)
+ Bugfix release [Issues](https://github.com/immutables/immutables/issues?q=milestone%3A2.0.16+is%3Aclosed)

### 2.0.15 (2015-07-02)
+ Bugfixes and minor improvements [Issues](https://github.com/immutables/immutables/issues?q=is%3Aissue+is%3Aclosed+milestone%3A2.0.15)

### 2.0.14 (2015-06-18)
+ Bugfixes and minor improvements [Issues](https://github.com/immutables/immutables/issues?q=is%3Aissue+is%3Aclosed+milestone%3A2.0.14)

### 2.0.13 (2015-06-14)

+ Added new experimental [serialization module](http://immutables.github.io/immutable.html#serialization) with advanced structural binary serialization, which is based on the standard java binary serialization that allows for object evolution to some degree.
+ Bugfixes along with minor refinements of annotation handling. [Issues](https://github.com/immutables/immutables/issues?q=is%3Aissue+is%3Aclosed+milestone%3A2.0.13)

### 2.0.10 (2015-04-28)
Bugfix release along with other 2.0.X. [Issues](https://github.com/immutables/immutables/issues?q=is%3Aissue+is%3Aclosed+milestone%3A2.0.10)

### 2.0 (2015-03-24)
Many thanks to all contributors who helped to make it happen.
Thanks to the community for making feature requests, bug reports, questions and suggestions.

_Note versions 1.1.x are still supported, there's no rush to switch to 2.0 if you are not ready._

+ Thanks to @augustotravillio for implementing JDK-only code generation. Useful on Android or when Guava is not available.
+ Thanks to @ivysharev for a lot more precise imports post-processor.

#### Features
+ Support for java 8, including new `Optional*` classes, default methods. But type annotation support is rudimentary (works only in some cases). Java 7 is still required for compilation
+ `Multiset`, `Multimap`, `SetMultimap`, `ListMultimap` are now supported.
+ Full-featured Gson support with generated `TypeAdapter`s which use no reflection during serialization/deserialization.
+ Builder now can be generated as "strict" (Style#strictBuilder). Strict builders prevents initialization errors: addition only collection initializer and regular initializers could be called only once.
+ Now, there's no required dependencies, plain JDK will suffice. Guava still has first class support.
+ Processor now enjoy improved repackaging (using forked and patched `maven-shade-plugin`)
+ Added `@Builder.Switch` annotation
+ Numerous API and behavior refinements, resulting in lot less WTF.

#### Changes
+ Main annotation and processor artifact changed to be `org.immutables:value`. There's no confusing `value-standalone` or whatsoever.
+ `common` artifact was removed, all compile and runtime dependencies have been modularized. While annotation processor itself is pretty monolithic, now compile and optional runtime dependencies are externalized to dedicated artifacts. Some notable modules:
  * `gson` Gson support module
  * `mongo` MongoDB support module
  * `builder` Module with annotations for generating builder from static factory methods
  * `ordinal` Module to generate more exotic enum-like values and efficiently handle them, etc
+ JSON infrastructure underwent overhaul. See guide at http://immutables.org/json.html
+ JAX-RS support switched to Gson, for _Jackson_ integration there's no need to integrate anything, its own provider will fully work.
+ MongoDB repository generation was refined and adjusted following JSON changes. See guide at http://immutables.org/mongo.html
+ Temporarily removed JDBI integration. It may be resurrected later.
+ Direct inheritance of `@Value.Immutable` from another `@Value.Immutable` is discouraged.
+ Limited (and constrained to same level) inheritance of `@Value.Parameter` attributes.
+ Builder now has method to set/reset collection content (in non-strict mode)
+ Package style now also applies to all classes in sub-packages unless overridden
+ Constructor parameters for collections now accept more liberal input. `List<T>` parameter accepts `Iterable<? extends T>` etc.
+ Removed sample style annotation like `@BeanStyle.Accessors` in favor of documentation and samples
+ `@Value.Nested` was renamed to `@Value.Enclosing`
+ `@Value.Immutable#visibility` was moved to style `@Value.Style#visibility`
+ `@Value.Immutable.Include` was moved to `@Value.Include`
+ Moved `@Value.Builder` to `builder` module where it is called `@Builder.Factory`. Added `@Builder.Parameter` and `@Builder.Switch` to fine-tune generation of builders for factory methods.

### 1.1 (2014-12-27)

#### Features
+ [#53](https://github.com/immutables/immutables/issues/53) Implemented `SortedSet`/`NavigableSet`/`SortedMap`/`NavigableMap` attributes specifying `@Value.NaturalOrder` or `@Value.ReverseOrder` annotation. Idea contributed by Facebook Buck team. Thanks!
+ [#63](https://github.com/immutables/immutables/issues/63)  `@Value.Builder`: implemented standalone builder generations for static factory methods. This makes it possible to create builders for arbitrary factory methods, including google/AutoValue _create_ methods!
+ [#38](https://github.com/immutables/immutables/issues/38) `@Value.Immutable.Include`: Ability to include other classes even from different packages and libraries as abstract value types. Think of generating immutable implementation of annotations from different library!
+ [#33](https://github.com/immutables/immutables/issues/33) `@Value.Style`: insanely flexible style customization infrastructure now allows to tailor generated immutable types and builders to wide range of style and preferences!
  + `@BeanStyle.Accessors` is example of style annotations - allows accessors to be detected from with 'get' and 'is' prefixes, so prefix will be stripped on builder and in toString.
+ [#35](https://github.com/immutables/immutables/issues/35) `@Nullable` attributes. Support any annotation named `Nullable`. Thanks to John Wood for this and other valuable feature and bug reports!
+ [#44](https://github.com/immutables/immutables/issues/44) Ability to run generated classes on JDK6 (including runtime support library `common`). JDK7 is still required to run annotation processor. Credits to Trask Stalnaker for the contribution!
+ Improved code generation: more clean code, more useful javadocs, dozens of fixes to edge cases, more correctness for customized value types.
+ [#64](https://github.com/immutables/immutables/issues/64) `org.immutables.json-runtime` minimal JSON runtime jar, extracted from `common` with only necessary transitive Jackson dependencies.
+ [#54](https://github.com/immutables/immutables/issues/54) Support for including Jackson marshaled POJOs as attributes of `@Json.Marshaled` immutable objects. Together with `@Jackson.Mapped` this provides round-tripping from _Immutables'_ marshalers to Jackson and back.

#### Changes
* Dozens of fixes, including
  - [#61](https://github.com/immutables/immutables/issues/61) Partially fixed `@Value.Default` methods on Java 8 implemented with interface `default` methods. Known issue is with more complex interface inheritance [#67](https://github.com/immutables/immutables/issues/67)
  - [#48](https://github.com/immutables/immutables/issues/48) JDBI marshaling fixes
  - [#50](https://github.com/immutables/immutables/issues/50) Support for older versions of Guava, which did not have `MoreObjects` for example, detected from classpath. Checked with Guava v12, v16
  - Fixed resolution of accesors inherited from couple of interfaces. (Still do not take into account most specific covariant override)
* Deprecations
  - Deprecated `@Value.Immutable(nonpublic)` in favor of `@Value.Immutable(visibility)`, nonpublic not working now, but it should not break
  - Deprecated `@Value.Immutable(withers)` in favor of `@Value.Immutable(copy)`
  - Deprecated `@Value.Getters` in favor of using `@Value.Style`. May be undeprecated if found really useful
  - Removed underdeveloped annotations and processors to be reintroduced later (Transformer, Visitor, Parboiled)
* Incompatibilites
  - Upgrade to Jackson 2.4.4 for Jackson `ObjectMapper` cross-marshaling to work
  - Possible incompatibity: `@Json.Marshaled` now is required on each nested `@Value.Immutable`, marshaled annotation on `@Value.Nested` will not have effect
  - [#59](https://github.com/immutables/immutables/issues/59) `@Value.Default` on collection attributes now issues warning, that makes collection attribute generated as plain regular attributes without any special collection support in builder

### 1.0.1

#### Fixes
+ Improper unchecked suppressions in generated files [#36](https://github.com/immutables/immutables/issues/36)
+ fixed/refined underwriting of methods: hashCode, equals, toString [#37](https://github.com/immutables/immutables/issues/37)
+ Fixed duplication of instanceof checks in Transfromers
+ Fixed implementation of nDeprecationsonpublic=true (package private) immutable classes

#### Changes
+ Internal: using released 1.0 ‘value-standalone’ for self-compiling, rather than 'retrovalue' system/jar
+ Internal: made marshaling binding problems IOException instead of runtime

### 1.0
Release with all of what was developed, including reengineering of template engine, project/module restructuring and annotation API changes

#### Changes
* Immutable generation annotation now nested below umbrella annotation `@org.immutables.value.Value` which provided grouping and namespacing for the nested annotations.
  - `@GenerateImmutable` is now `@Value.Immutable`
  - `@GenerateConstructorParameter` is now `@Value.Parameter`
  - ... and so on, see website and API documentation for the details
* See other umbrella annotations in `org.immutables.value.*` package: `@Json`, `@Mongo`, `@Jackson`
* Main standalone artifact for the annotation processor is now `org.immutables:value-standalone:1.0`. There's is quick start module with transitive dependencies for integrations — to not pick dependencies one by one — `org.immutables:quickstart:1.0`
* Most notable generated API changes
  + Added `ImmutableValue.copyOf` methods
  + Added array attributes
  + Added `Builder.addAttribute(T...)` overload for collection attributes
  + Removed `ImmutableValue.Builder.copy` methods
