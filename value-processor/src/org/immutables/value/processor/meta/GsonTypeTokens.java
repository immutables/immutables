package org.immutables.value.processor.meta;

import org.immutables.value.processor.encode.Type;
import org.immutables.value.processor.encode.Type.Array;
import org.immutables.value.processor.encode.Type.Nonprimitive;
import org.immutables.value.processor.encode.Type.Parameterized;
import org.immutables.value.processor.encode.Type.Primitive;
import org.immutables.value.processor.encode.Type.Reference;
import org.immutables.value.processor.encode.Type.Variable;
import org.immutables.value.processor.encode.Type.Wildcard.Extends;
import org.immutables.value.processor.encode.Type.Wildcard.Super;
import org.immutables.value.processor.encode.TypeExtractor;

public final class GsonTypeTokens {
  private final Generics generics;
  private final TypeExtractor typeExtractor;

  public GsonTypeTokens(Generics generics, TypeExtractor typeExtractor) {
    this.generics = generics;
    this.typeExtractor = typeExtractor;
  }

  public Type parseType(String typeUsage) {
    return typeExtractor.parser.parse(typeUsage);
  }

  public CharSequence sourceFor(String typeUsage) {
    return sourceFor(parseType(typeUsage));
  }

  public CharSequence sourceFor(final Type rootType) {
    final StringBuilder out = new StringBuilder();

    class Sourcer implements Type.Visitor<Void> {
      boolean wasUngenerifiedTypeUsed = false;

      StringBuilder openTokenMethod(String name) {
        return out.append("TypeToken.").append(name).append('(');
      }

      StringBuilder closeMethod() {
        return out.append(')');
      }

      StringBuilder closeMethodGetType() {
        return out.append(").getType()");
      }

      StringBuilder classLiteral() {
        return out.append(".class");
      }

      @Override
      public Void primitive(Primitive primitive) {
        out.append(primitive);
        classLiteral();
        return null;
      }

      @Override
      public Void reference(Reference reference) {
        out.append(reference);
        classLiteral();
        return null;
      }

      @Override
      public Void parameterized(Parameterized parameterized) {
        wasUngenerifiedTypeUsed = true;
        openTokenMethod("getParameterized");
        out.append(parameterized.reference);
        classLiteral();
        for (Nonprimitive argument : parameterized.arguments) {
          out.append(", ");
          argument.accept(this);
        }
        if (rootType == parameterized) {
          closeMethod();
        } else {
          closeMethodGetType();
        }
        return null;
      }

      @Override
      public Void variable(Variable variable) {
        if (rootType == variable) {
          openTokenMethod("get");
        }
        wasUngenerifiedTypeUsed = true;
        out.append("typeArguments[").append(generics.get(variable.name).index).append("]");
        if (rootType == variable) {
          closeMethod();
        }
        return null;
      }

      @Override
      public Void array(Array array) {
        if (array.element instanceof Parameterized) {
          wasUngenerifiedTypeUsed = true;
          openTokenMethod("getArray");
          array.element.accept(this);
          if (rootType == array) {
            closeMethod();
          } else {
            closeMethodGetType();
          }
        } else {
          if (rootType == array) {
            openTokenMethod("get");
          }
          out.append(array.element).append("[]");
          classLiteral();
          if (rootType == array) {
            closeMethod();
          }
        }
        return null;
      }

      @Override
      public Void superWildcard(Super wildcard) {
        return wildcard.lowerBound.accept(this);
      }

      @Override
      public Void extendsWildcard(Extends wildcard) {
        return wildcard.upperBound.accept(this);
      }
    }

    Sourcer sourcer = new Sourcer();
    rootType.accept(sourcer);

    if (sourcer.wasUngenerifiedTypeUsed) {
      StringBuilder castBuilder = new StringBuilder();
      castBuilder.append("(TypeToken<").append(rootType).append(">) ");
      out.insert(0, castBuilder);
    }
    return out;
  }
}
