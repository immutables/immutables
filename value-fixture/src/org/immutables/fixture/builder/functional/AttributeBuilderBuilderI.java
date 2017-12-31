package org.immutables.fixture.builder.functional;

import java.util.List;
import org.immutables.fixture.builder.attribute_builders.FirstPartyImmutable;
import org.immutables.fixture.builder.attribute_builders.FirstPartyImmutableWithDifferentStyle;
import org.immutables.fixture.builder.attribute_builders.ImmutableFirstPartyImmutable;
import org.immutables.fixture.builder.attribute_builders.ImmutableFirstPartyImmutable.Builder;
import org.immutables.fixture.builder.attribute_builders.ImmutableFirstPartyImmutableWithDifferentStyle;
import org.immutables.fixture.builder.attribute_builders.ThirdPartyImmutable;
import org.immutables.fixture.builder.attribute_builders.ThirdPartyImmutableWithPrimitive;

@SuppressWarnings("rawtypes")
public interface AttributeBuilderBuilderI<ValueT> {
  AttributeBuilderBuilderI firstPartyImmutable(FirstPartyImmutable firstPartyImmutable);

  ImmutableFirstPartyImmutable.Builder firstPartyImmutableBuilder();

  AttributeBuilderBuilderI firstPartyImmutableBuilder(ImmutableFirstPartyImmutable.Builder firstPartyImmutableBuilder);

  AttributeBuilderBuilderI firstPartyImmutableWithDifferentStyle(
      FirstPartyImmutableWithDifferentStyle firstPartyImmutableWithDifferentStyle);

  ImmutableFirstPartyImmutableWithDifferentStyle.Abonabon firstPartyImmutableWithDifferentStyleBuilder();

  AttributeBuilderBuilderI thirdPartyImmutable(ThirdPartyImmutable thirdPartyImmutable);

  ThirdPartyImmutable.Builder thirdPartyImmutableBuilder();

  AttributeBuilderBuilderI thirdPartyImmutableWithPrimitive(ThirdPartyImmutableWithPrimitive thirdPartyImmutableWithPrimitive);

  ThirdPartyImmutableWithPrimitive.Builder thirdPartyImmutableWithPrimitiveBuilder();

  AttributeBuilderBuilderI addFirstPartyImmutable(FirstPartyImmutable firstPartyImmutable);

  AttributeBuilderBuilderI addAllFirstPartyImmutableBuilders(ImmutableFirstPartyImmutable.Builder... elements);

  AttributeBuilderBuilderI addAllFirstPartyImmutableBuilders(Iterable<ImmutableFirstPartyImmutable.Builder> elements);

  List<Builder> firstPartyImmutableBuilders();

  ImmutableFirstPartyImmutable.Builder addFirstPartyImmutableBuilder();

  AttributeBuilderBuilderI addThirdPartyImmutable(ThirdPartyImmutable thirdPartyImmutable);

  ThirdPartyImmutable.Builder addThirdPartyImmutableBuilder();

  ValueT build();
}
