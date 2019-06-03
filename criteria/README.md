This folder contains classes specific to Criteria API and its runtime evaluation:

1. `common` shared classes by all modules
2. `elasticsearch` adapter for [Elastic Search](https://www.elastic.co/guide/en/elasticsearch/reference/current/query-dsl.html)
3. `mongo` adapter for [MongoDB](https://www.mongodb.com/) 
based on [reactive streams](https://mongodb.github.io/mongo-java-driver-reactivestreams/) driver.
4. `geode` adapter for [Apache Geode](https://geode.apache.org)
5. `inmemory` simple evaluation of Criteria expression on a in-memory data structure 
(typically [Iterable](https://docs.oracle.com/javase/8/docs/api/java/lang/Iterable.html)) 

Criteria API requires Java 8 (or later)
