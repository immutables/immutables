Read documentation at http://immutables.org

[![Build Status](https://travis-ci.org/immutables/immutables.png?branch=master)](https://travis-ci.org/immutables/immutables)

Changelog
---------

### 1.0.1

#### Fixes
+ Improper unchecked suppressions in generated files [#36](https://github.com/immutables/immutables/issues/36)
+ fixed/refined underwriting of methods: hashCode, equals, toString [#37](https://github.com/immutables/immutables/issues/37)
+ Fixed duplication of instanceof checks in Transfromers
+ Completed implementation of nonpublic=true (package private) immutable classes
+ Internal: using released 1.0 ‘value-standalone’ for self-compiling, rather than 'retrovalue' system/jar
+ Internal: made marshaling binding problems IOException instead of runtime

### 1.0
Initial release with all of what was developed, including reengineering of template engine, project/module restructuring and annotation API changes
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

