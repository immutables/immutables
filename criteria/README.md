This module generates Criteria classes based on immutable model.

Generated classes allow type-safe query building which can be evaluated on destination data-source.

Conversion between criteria and native query is happening at runtime using visitor pattern.

### Missing functionality / Remaining Questions : 
1. (sub-)Criteria on Iterables. Example 
    ```java
    user.friends.anyMatch(FriendCriteria.create().age().greaterThan(22));
    user.friends.noneMatch(FriendCriteria.create().age().greaterThan(22))
    user.friends.allMatch(FriendCriteria.create().age().greaterThan(22));
   
    // something similar for optionals
    ```
2. Combining criterias (using `AND`s / `OR`s)
3. Nested criterias
4. Projections
5. Aggregations
6. DSL is not restrictive enough.
   ```java
   // one can write something like this
   criteria.or().or().or();
   criteria.age().greaterThan(111).or().or(); // or this
   ```
7. How are we better than [QueryDSL](http://www.querydsl.com/) ?
