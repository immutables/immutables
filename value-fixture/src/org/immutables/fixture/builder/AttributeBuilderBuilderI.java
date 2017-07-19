package org.immutables.fixture.builder;

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
