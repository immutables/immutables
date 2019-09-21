package org.immutables.fixture.builder.functional;

import java.util.List;
import java.util.Optional;
import javax.annotation.Nullable;
import org.immutables.fixture.builder.attribute_builders.FirstPartyImmutable;
import org.immutables.fixture.builder.attribute_builders.FirstPartyImmutableWithDifferentStyle;
import org.immutables.fixture.builder.attribute_builders.ThirdPartyImmutable;
import org.immutables.fixture.builder.attribute_builders.ThirdPartyImmutableWithPrimitive;

public interface AttributeBuilderValueI {
  FirstPartyImmutable firstPartyImmutable();

  FirstPartyImmutableWithDifferentStyle firstPartyImmutableWithDifferentStyle();

  List<FirstPartyImmutable> firstPartyImmutableList();

  ThirdPartyImmutable thirdPartyImmutable();

  List<ThirdPartyImmutable> thirdPartyImmutableList();

  ThirdPartyImmutableWithPrimitive thirdPartyImmutableWithPrimitive();

  Optional<FirstPartyImmutable> optionalFirstPartyImmutable();

  @Nullable
  FirstPartyImmutable nullableFirstPartyImmutable();
}
