The goal of Criteria module is to provide fluent java API (DSL) to query heterogeneous data-sources.

#### Main Features

1. **Expressive and type-safe API** Compile-type validation of the query.
2. **Dynamic** Combine predicates at runtime based on some logic
3. **Data-source agnostic** Define criteria once and apply to different data-sources (Map, JDBC, Mongo, Elastic etc.)
4. **Blocking / asynchronous operations** Generated repositories allow you to query data in blocking, non-blocking and [reactive](https://www.reactive-streams.org/) fashion


This folder contains classes specific to Criteria API and its runtime evaluation:

1. `common` shared classes by all modules
2. `elasticsearch` adapter for [Elastic Search](https://www.elastic.co/guide/en/elasticsearch/reference/current/query-dsl.html)
3. `mongo` adapter for [MongoDB](https://www.mongodb.com/) 
based on [reactive streams](https://mongodb.github.io/mongo-java-driver-reactivestreams/) driver.
4. `geode` adapter for [Apache Geode](https://geode.apache.org)
5. `inmemory` lighweight implementation of a backend based on existing Map

Criteria API requires Java 8 (or later)
