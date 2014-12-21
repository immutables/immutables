Read documentation at http://immutables.org

[![Build Status](https://travis-ci.org/immutables/immutables.png?branch=master)](https://travis-ci.org/immutables/immutables)

Changelog
---------

### 1.1.0-rc1

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
  - [#61](https://github.com/immutables/immutables/issues/61) Fixed `@Value.Default` methods on Java 8 implemented with interface `default` methods
  - [#48](https://github.com/immutables/immutables/issues/48) JDBI marshaling fixes
  - [#50](https://github.com/immutables/immutables/issues/50) Support for older versions of Guava, which did not have `MoreObjects` for example, detected from classpath
  - Fixed resolution of accesors inherited from couple of interfaces. (Still do not take into account most specific covariant override)
* Deprecations
  - Deprecated `@Value.Immutable(nonpublic)` in favor of `@Value.Immutable(visibility)`, nonpublic not working now, but it should not break
  - Deprecated `@Value.Immutable(withers)` in favor of `@Value.Immutable(copy)`
  - Deprecated `@Value.Getters` in favor of using `@Value.Style`. May be undeprecated if found really useful
* Incompatibilites
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
Migration guide

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

