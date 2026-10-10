package org.immutables.fixture.jackson3.meta;

import org.immutables.value.Value;

/**
 * Carries a style meta-annotated with the Jackson 3 {@code @JsonSerialize}/{@code @JsonDeserialize}
 * on the type itself. Regression for #1649: the Jackson 3 form has to enable {@code @JsonProperty}
 * generation, exactly as the Jackson 2 form does.
 */
@Value.Immutable
@PlainStyle
public interface _TypeBound {
  int a();
  String b();
}
