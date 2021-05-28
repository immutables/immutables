/*
   Copyright 2016 Immutables Authors and Contributors

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */
package org.immutables.value.processor.encode;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import java.util.List;
import java.util.Set;
import org.immutables.generator.Naming;
import org.immutables.value.Value;
import org.immutables.value.Value.Default;
import org.immutables.value.Value.Derived;
import org.immutables.value.Value.Enclosing;
import org.immutables.value.Value.Immutable;
import org.immutables.value.processor.encode.Code.Term;

@Immutable
@Enclosing
public abstract class EncodedElement {
  enum Tag {
    IMPL,
    EXPOSE,
    BUILDER,
    STATIC,
    PRIVATE,
    FINAL,
    BUILD,
    INIT,
    FROM,
    HELPER,
    FIELD,
    TO_STRING,
    HASH_CODE,
    EQUALS,
    COPY,
    DEPLURALIZE,
    // syntethic element which is automatically inserted
    SYNTH,
    // applies to impl field
    VIRTUAL,
    IS_INIT
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

  abstract List<String> doc();

  abstract List<String> annotations();

  Param firstParam() {
    return params().get(0);
  }

  @Default
  StandardNaming standardNaming() {
    return StandardNaming.NONE;
  }

  boolean isVirtual() {
    return tags().contains(Tag.VIRTUAL);
  }

  @Derived
  Code.Binding asBinding() {
    return isField() || isImplField()
        ? Code.Binding.newField(name())
        : Code.Binding.newMethod(name());
  }

  @Derived
  boolean isToString() {
    return tags().contains(Tag.TO_STRING);
  }

  @Derived
  boolean isHashCode() {
    return tags().contains(Tag.HASH_CODE);
  }

  @Derived
  boolean isEquals() {
    return tags().contains(Tag.EQUALS);
  }

  @Derived
  boolean isFrom() {
    return tags().contains(Tag.FROM);
  }

  @Derived
  boolean isBuild() {
    return tags().contains(Tag.BUILD);
  }

  @Derived
  boolean isInit() {
    return tags().contains(Tag.INIT);
  }

  @Derived
  boolean isWasInit() {
    return tags().contains(Tag.IS_INIT);
  }

  @Derived
  boolean isCopy() {
    return tags().contains(Tag.COPY)
        && !inBuilder();
  }

  @Derived
  boolean isBuilderCopy() {
    return tags().contains(Tag.COPY)
        && inBuilder();
  }

  @Derived
  boolean isExpose() {
    return tags().contains(Tag.EXPOSE);
  }

  @Derived
  boolean inBuilder() {
    return tags().contains(Tag.BUILDER);
  }

  @Derived
  boolean isStatic() {
    return tags().contains(Tag.STATIC);
  }

  @Derived
  boolean isFinal() {
    return tags().contains(Tag.FINAL);
  }

  @Derived
  boolean isPrivate() {
    return tags().contains(Tag.PRIVATE);
  }

  @Derived
  boolean isSynthetic() {
    return tags().contains(Tag.SYNTH);
  }

  @Derived
  boolean isImplField() {
    return tags().contains(Tag.IMPL);
  }

  @Derived
  boolean isValueField() {
    return isField()
        && !tags().contains(Tag.IMPL)
        && !inBuilder()
        && !isStatic();
  }

  @Derived
  boolean isStaticField() {
    return isField()
        && !inBuilder()
        && isStatic();
  }

  @Derived
  boolean isField() {
    return tags().contains(Tag.FIELD);
  }

  @Derived
  boolean isBuilderField() {
    return isField()
        && inBuilder()
        && !isStatic();
  }

  @Derived
  boolean isStaticMethod() {
    return tags().contains(Tag.HELPER)
        && isStatic()
        && !inBuilder();
  }

  @Derived
  boolean isValueMethod() {
    return tags().contains(Tag.HELPER)
        && !isStatic()
        && !inBuilder();
  }

  @Derived
  boolean isBuilderMethod() {
    return tags().contains(Tag.HELPER)
        && inBuilder();
  }

  @Derived
  boolean isBuilderStaticField() {
    return isField()
        && inBuilder()
        && isStatic();
  }

  @Derived
  ImmutableList<Term> oneLiner() {
    return typeParams().isEmpty()
        ? ImmutableList.copyOf(Code.oneLiner(code()))
        : ImmutableList.<Term>of();
  }

  @Derived
  boolean usesThis() {
    return Code.usesThis(code());
  }

  @Derived
  boolean isInlinable() {
    return isEquals()
        || isToString()
        || isHashCode()
        || isFrom()
        || isCopy();
  }

  boolean depluralize() {
    return tags().contains(Tag.DEPLURALIZE);
  }

  String unitializedFieldValue() {
    Type type = type();
    if (type instanceof Type.Primitive) {
      return ((Type.Primitive) type).defaultValue;
    }
    return "null";
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
