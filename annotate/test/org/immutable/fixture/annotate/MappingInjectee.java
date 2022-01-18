package org.immutable.fixture.annotate;

import org.immutables.value.Value;

@Value.Immutable
@InjectPresentMapping
public interface MappingInjectee {
  @OriginalMapping(target = ImmutableMappingInjectee.class)
  int aab();

  @InjectMappingHere(target = ImmutableMappingInjectee.class)
  String bbc();
}
