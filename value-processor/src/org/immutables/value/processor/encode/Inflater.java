package org.immutables.value.processor.encode;

import org.immutables.value.Value.Auxiliary;
import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import java.util.List;
import java.util.Set;
import org.immutables.generator.Naming;
import org.immutables.value.Value.Enclosing;
import org.immutables.value.Value.Immutable;
import org.immutables.value.Value.Lazy;
import org.immutables.value.processor.encode.EncodedElement.Tag;
import org.immutables.value.processor.encode.Mirrors.EncElement;
import org.immutables.value.processor.encode.Mirrors.EncMetadata;

@Enclosing
public final class Inflater implements Function<EncMetadata, Inflater.EncodingInfo> {
  private final Type.Factory typeFactory = new Type.Producer();

  public Instantiator instantiatorFor(Set<EncodingInfo> encodinds) {
    return new Instantiator(typeFactory, encodinds);
  }

  @Immutable
  public abstract static class EncodingInfo {
    abstract String name();

    @Auxiliary
    abstract Set<String> imports();

    @Auxiliary
    abstract Type.Parameters typeParameters();

    @Auxiliary
    abstract Type.Factory typeFactory();

    @Auxiliary
    abstract List<EncodedElement> element();

    static class Builder extends ImmutableInflater.EncodingInfo.Builder {}

    @Lazy
    public Iterable<EncodedElement> expose() {
      return Tagged.with(Tag.EXPOSE).filter(element());
    }

    @Lazy
    public Optional<EncodedElement> impl() {
      return Tagged.with(Tag.IMPL).find(element());
    }
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

  static class Tagged implements Predicate<EncodedElement> {
    private final Tag[] tags;
    private final boolean isAndOtherwiseOr;

    private Tagged(Tag[] tags, boolean isAndOtherwiseOr) {
      this.tags = tags;
      this.isAndOtherwiseOr = isAndOtherwiseOr;
    }

    static Tagged with(Tag tag) {
      return or(tag);
    }

    static Tagged and(Tag... tags) {
      return new Tagged(tags, true);
    }

    static Tagged or(Tag... tags) {
      return new Tagged(tags, false);
    }

    ImmutableList<EncodedElement> filter(Iterable<EncodedElement> elements) {
      return ImmutableList.copyOf(Iterables.filter(elements, this));
    }

    Optional<EncodedElement> find(Iterable<EncodedElement> elements) {
      return Iterables.tryFind(elements, this);
    }

    @Override
    public boolean apply(EncodedElement input) {
      if (isAndOtherwiseOr) {
        for (Tag t : tags) {
          if (!input.tags().contains(t)) {
            return false;
          }
        }
        return true;
      }
      for (Tag t : tags) {
        if (input.tags().contains(t)) {
          return true;
        }
      }
      return false;
    }
  }
}
