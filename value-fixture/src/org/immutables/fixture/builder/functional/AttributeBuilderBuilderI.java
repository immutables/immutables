package org.immutables.fixture.builder.functional;

import org.immutables.fixture.builder.attribute_builders.FirstPartyImmutable;
import org.immutables.fixture.builder.attribute_builders.FirstPartyImmutableWithDifferentStyle;
import org.immutables.fixture.builder.attribute_builders.ImmutableFirstPartyImmutable;
import org.immutables.fixture.builder.attribute_builders.ImmutableFirstPartyImmutableWithDifferentStyle;
import org.immutables.fixture.builder.attribute_builders.ThirdPartyImmutable;

public interface AttributeBuilderBuilderI<ValueT> {

  AttributeBuilderBuilderI firstPartyImmutable(FirstPartyImmutable firstPartyImmutable);

  ImmutableFirstPartyImmutable.Builder firstPartyImmutableBuilder();

  AttributeBuilderBuilderI firstPartyImmutableWithDifferentStyle(
      FirstPartyImmutableWithDifferentStyle firstPartyImmutableWithDifferentStyle);

  ImmutableFirstPartyImmutableWithDifferentStyle.Abonabon firstPartyImmutableWithDifferentStyleBuilder();

  AttributeBuilderBuilderI thirdPartyImmutable(ThirdPartyImmutable thirdPartyImmutable);

  ThirdPartyImmutable.Builder thirdPartyImmutableBuilder();

  AttributeBuilderBuilderI addFirstPartyImmutable(FirstPartyImmutable firstPartyImmutable);

  ImmutableFirstPartyImmutable.Builder addFirstPartyImmutableBuilder();

  AttributeBuilderBuilderI addThirdPartyImmutable(ThirdPartyImmutable thirdPartyImmutable);

  ThirdPartyImmutable.Builder addThirdPartyImmutableBuilder();

  ValueT build();
}
