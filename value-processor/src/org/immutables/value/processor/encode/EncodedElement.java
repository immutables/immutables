package org.immutables.value.processor.encode;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import java.util.List;
import java.util.Set;
import org.immutables.generator.Naming;
import org.immutables.value.Value;
import org.immutables.value.Value.Enclosing;
import org.immutables.value.Value.Immutable;
import org.immutables.value.processor.encode.Code.Term;

@Immutable
@Enclosing
public abstract class EncodedElement {
  enum Tag {
    IMPL, EXPOSE, BUILDER, STATIC, PRIVATE, FINAL, BUILD, INIT, HELPER, FIELD, TO_STRING, HASH_CODE, EQUALS, COPY
  }

  abstract String name();

  abstract Type type();

  abstract Naming naming();

  abstract List<Param> params();

  abstract List<Term> code();

  abstract List<Type> thrown();

  abstract Set<Tag> tags();

  abstract Type.Parameters typeParameters();

  abstract List<TypeParam> typeParams();

  public boolean has(Tag tag) {
    return tags().contains(tag);
  }

  public boolean not(Tag tag) {
    return !tags().contains(tag);
  }

  static class Builder extends ImmutableEncodedElement.Builder {}

  @Immutable
  abstract static class Param {
    @Value.Parameter
    abstract String name();

    @Value.Parameter
    abstract Type type();

    @Override
    public String toString() {
      return name() + ": " + type();
    }

    static Param of(String name, Type type) {
      return ImmutableEncodedElement.Param.of(name, type);
    }

    public static Param from(String input, Type.Parser parser) {
      List<String> parts = COLON_SPLITTER.splitToList(input);
      return of(parts.get(0), parser.parse(parts.get(1)));
    }
  }

  @Immutable
  abstract static class TypeParam {
    abstract String name();

    abstract List<Type.Defined> bounds();

    @Override
    public String toString() {
      return name() + ": " + Joiner.on(" & ").join(bounds());
    }

    static class Builder extends ImmutableEncodedElement.TypeParam.Builder {}

    static TypeParam from(String input, Type.Factory typeFactory, Type.Parameters typeParameters) {
      List<String> parts = COLON_SPLITTER.splitToList(input);

      Builder builder = new Builder()
          .name(parts.get(0));

      if (parts.size() == 1) {
        return builder.build();
      }

      Type.Parser parser = new Type.Parser(typeFactory, typeParameters);

      for (String bound : AMPER_SPLITTER.split(parts.get(1))) {
        builder.addBounds((Type.Defined) parser.parse(bound));
      }

      return builder.build();
    }
  }

  private static final Splitter COLON_SPLITTER = Splitter.on(':').trimResults();
  private static final Splitter AMPER_SPLITTER = Splitter.on('&').trimResults();
}
