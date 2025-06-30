package org.immutables.fixture.jackson;

import java.util.List;
import java.util.SortedSet;

import org.immutables.value.Value;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

@Value.Immutable
@JsonSerialize(as = ImmutableBug1553Example.class)
@JsonDeserialize(as = ImmutableBug1553Example.class)
public abstract class Bug1553Example implements WithBug1553Example {

  public abstract List<String> getAccounts();
}
