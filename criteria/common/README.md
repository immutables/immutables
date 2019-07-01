Common functionality to Criteria API.

This module generates (and evaluates) Criteria classes based on immutable model. 

The functionality is work in progress and API is not stable yet (use at your own risk).

Generated classes allow type-safe query building which can be evaluated on destination data-source.

Conversion between criteria and native query is happening at runtime using visitor pattern.

### Missing functionality / Remaining Questions : 
1. Projections
2. Aggregations
