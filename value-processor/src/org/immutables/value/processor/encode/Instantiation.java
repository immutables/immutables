package org.immutables.value.processor.encode;

import com.google.common.base.CaseFormat;
import com.google.common.base.Function;
import com.google.common.base.Functions;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import org.immutables.generator.Templates;
import org.immutables.generator.Templates.Invokable;
import org.immutables.generator.Templates.Invokation;
import org.immutables.value.processor.encode.Code.Binding;
import org.immutables.value.processor.encode.Code.Term;
import org.immutables.value.processor.encode.EncodedElement.Param;
import org.immutables.value.processor.encode.EncodedElement.TypeParam;
import org.immutables.value.processor.encode.Type.Defined;
import org.immutables.value.processor.encode.Type.Parameters;
import org.immutables.value.processor.encode.Type.Variable;
import org.immutables.value.processor.encode.Type.VariableResolver;
import org.immutables.value.processor.meta.Styles;

// The bulk of this could have possibly be coded in templates,
// but let it be for now as it is
public final class Instantiation {
  private final Map<String, String> generatedNames;

  final Type type;
  final EncodingInfo encoding;
  final EncodedElement expose;

  // these exist as functions that can be applied from a template
  final VariableResolver typer;
  final Code.Interpolator coder;
  final Function<String, String> namer;

  private final String name;

  Instantiation(
      EncodingInfo encoding,
      EncodedElement expose,
      Type exposedType,
      Styles.UsingName.AttributeNames names,
      VariableResolver resolver) {
    this.encoding = encoding;
    this.expose = expose;
    this.type = exposedType;
    this.typer = resolver;
    this.name = names.raw;

    this.generatedNames = new HashMap<>(encoding.element().size());
    for (EncodedElement e : encoding.element()) {
      this.generatedNames.put(e.name(), generateProperName(e, names));
    }

    for (Variable v : resolver.variables()) {
      this.generatedNames.put(v.name, resolver.apply(v).toString());
    }

    this.namer = Functions.forMap(generatedNames);
    this.coder = new Code.Interpolator(name, namer, NO_SUBSTITUTIONS);
  }

  public boolean supportsInternalImplConstructor() {
    return encoding.build().type().equals(encoding.impl().type());
  }

  public boolean supportsDefaultValue() {
    return !encoding.impl().code().isEmpty();
  }

  private String generateProperName(EncodedElement element, Styles.UsingName.AttributeNames names) {
    if (element.isExpose()) {
      return names.get;
    }
    if (element.naming().isIdentity()) {
      if (element.isCopy()) {
        return names.with;
      }
      if (element.isInit()) {
        return names.init;
      }
    }
    String raw = names.raw;
    if (element.isStaticField() && element.isFinal()) {
      raw = CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_UNDERSCORE, raw);
    }
    return element.naming().apply(raw);
  }

  final Templates.Invokable fragmentOf = new Templates.Invokable() {
    @Override
    public @Nullable Invokable invoke(Invokation invokation, Object... parameters) {
      final EncodedElement el = (EncodedElement) parameters[0];
      Code.Interpolator interpolator = coder;
      if (el.params().size() == 1 && parameters.length > 1) {
        final String name = el.params().get(0).name();
        final String param = parameters[1].toString();

        interpolator = new Code.Interpolator(name, namer, new Function<String, String>() {
          @Override
          public String apply(String input) {
            return input.equals(name) ? param : null;
          }
        });
      }

      if (!el.oneLiner().isEmpty()
          && !encoding.crossReferencedMembers().contains(el.name())) {
        for (Code.Term t : interpolator.apply(el.oneLiner())) {
          invokation.out(t);
        }
      } else {
        invokation.out(namer.apply(el.name())).out("(");
        boolean notFirst = false;
        for (Param p : el.params()) {
          if (notFirst) {
            invokation.out(", ");
          }
          notFirst = true;
          Binding binding = Code.Binding.newTop(p.name());
          invokation.out(interpolator.dereference(binding));
        }
        invokation.out(")");
      }
      return null;
    }
  };

  final Templates.Invokable codeOf = new Templates.Invokable() {
    @Override
    public @Nullable Invokable invoke(Invokation invokation, Object... parameters) {
      EncodedElement el = (EncodedElement) parameters[0];
      List<Term> code = el.code();
      if (parameters.length >= 2) {
        String returnReplacement = parameters[1].toString();
        code = Code.replaceReturn(code, returnReplacement);
      }
      printWithIndentation(invokation, coder.apply(code));
      return null;
    }

    private void printWithIndentation(Invokation invokation, List<Term> terms) {
      int indentLevel = 0;
      int indentWrap = 0;
      boolean nextNewline = false;

      for (Code.Term t : terms) {
        if (t.isWhitespace() && t.is('\n')) {
          nextNewline = true;
          continue;
        }
        // decrease indent level before writing a newline
        if (t.isDelimiter() && t.is('}')) {
          indentLevel--;
        }

        if (nextNewline) {
          nextNewline = false;
          invokation.ln();
          for (int i = 0; i < indentLevel + indentWrap; i++) {
            invokation.out("  ");
          }
          // auto-increase indent wrap unless semicolon will return it back
          indentWrap = 2;
        }

        if (t.isDelimiter() && (t.is(';') || t.is('}') || t.is('{'))) {
          indentWrap = 0;
        }
        // increase indent level after writing a newline
        if (t.isDelimiter() && t.is('{')) {
          indentLevel++;
        }
        // outputing actual token after any indents
        invokation.out(t);
      }
    }
  };

  final Function<EncodedElement, String> ownTypeParams =
      new Function<EncodedElement, String>() {
        @Override
        public String apply(EncodedElement input) {
          Parameters parameters = Type.Producer.emptyParameters();

          if (input.isFrom()) {
            // our from method have the same type parameters as
            // encoding in general, when instantiating it for specific
            // attribute, some may resolve to concrete types, some
            // may end up value-type specific type parameter
            // we need to write only type-specific type parameters omiting those
            // which resolves to specific type.
            // note that often 'from' method is inlined so this is not needed
            // then, it is only needed when non-inlined references are present.
            for (Variable v : typer.variables()) {
              Type t = typer.apply(v);
              if (t instanceof Type.Variable) {
                Type.Variable var = (Type.Variable) t;
                parameters = parameters.introduce(var.name, transformBounds(var.upperBounds));
              }
            }
          } else {
            for (TypeParam p : input.typeParams()) {
              parameters = parameters.introduce(p.name(), transformBounds(p.bounds()));
            }
          }

          if (parameters.names().isEmpty()) {
            return "";
          }
          return parameters + " ";
        }

        private ImmutableList<Defined> transformBounds(List<Defined> bounds) {
          return FluentIterable.from(bounds)
              .transform(typer)
              .filter(Defined.class)
              .toList();
        }
      };

  @Override
  public String toString() {
    return type + "(by " + encoding.name() + ")";
  }

  private static final Function<String, String> NO_SUBSTITUTIONS = new Function<String, String>() {
    @Override
    public String apply(String input) {
      return null;
    }
  };
}
