package org.immutables.criteria.elasticsearch;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.criteria.Criteria;
import org.immutables.value.Value;

import java.util.Optional;

@Value.Immutable
@Criteria
@JsonSerialize(as = ImmutableElasticModel.class)
@JsonDeserialize(as = ImmutableElasticModel.class)
public interface ElasticModel {

  String string();

  Optional<String> optionalString();

  boolean bool();

  int intNumber();

}