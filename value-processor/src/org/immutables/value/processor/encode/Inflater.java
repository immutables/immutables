package org.immutables.value.processor.encode;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import java.util.Set;
import org.immutables.generator.Naming;
import org.immutables.value.Value.Enclosing;
import org.immutables.value.processor.encode.EncodedElement.Tag;
import org.immutables.value.processor.encode.Mirrors.EncElement;
import org.immutables.value.processor.encode.Mirrors.EncMetadata;

@Enclosing
public final class Inflater implements Function<EncMetadata, EncodingInfo> {
  private final Type.Factory typeFactory = new Type.Producer();

  public Instantiator instantiatorFor(Set<EncodingInfo> encodinds) {
    return new Instantiator(typeFactory, encodinds);
  }

  @Override
  public EncodingInfo apply(EncMetadata input) {
    Type.Parameters typeParameters = typeFactory.parameters();

    for (String t : input.typeParams()) {
      typeParameters = typeParameters.introduce(t, ImmutableList.<Type.Defined>of());
    }

    EncodingInfo.Builder builder = new EncodingInfo.Builder()
        .name(input.name())
        .addImports(input.imports())
        .typeFactory(typeFactory)
        .typeParameters(typeParameters);

    for (EncElement e : input.elements()) {
      builder.addElement(elementFor(e, typeParameters));
    }

    return builder.build();
  }

  private EncodedElement elementFor(EncElement e, Type.Parameters typeParameters) {
    EncodedElement.Builder builder = new EncodedElement.Builder();

    for (String input : e.typeParams()) {
      EncodedElement.TypeParam p = EncodedElement.TypeParam.from(input, typeFactory, typeParameters);
      typeParameters = typeParameters.introduce(input, p.bounds());
      builder.addTypeParams(p);
    }

    Type.Parser parser = new Type.Parser(typeFactory, typeParameters);

    builder.typeParameters(typeParameters)
        .name(e.name())
        .naming(Naming.from(e.naming()))
        .standardNaming(StandardNaming.valueOf(e.stdNaming()))
        .type(parser.parse(e.type()))
        .addAllCode(Code.termsFrom(e.code()));

    for (String input : e.params()) {
      builder.addParams(EncodedElement.Param.from(input, parser));
    }

    for (String input : e.tags()) {
      builder.addTags(Tag.valueOf(input));
    }

    return builder.build();
  }
}
