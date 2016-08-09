package org.immutables.value.processor.encode;

import com.google.common.base.CaseFormat;
import com.google.common.base.Function;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
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
import org.immutables.value.processor.meta.Styles.UsingName.AttributeNames;

public final class Instantiation {
  private final Map<Binding, String> bindings;
  private final Map<Binding, String> builderBindings;

  final Type type;
  final EncodingInfo encoding;
  final EncodedElement expose;

  // these exist as functions that can be applied from a template
  final VariableResolver typer;
  final Function<EncodedElement, String> namer;

  private final AttributeNames names;

  Instantiation(
      EncodingInfo encoding,
      EncodedElement expose,
      Type exposedType,
      Styles.UsingName.AttributeNames names,
      VariableResolver resolver) {
    this.encoding = encoding;
    this.expose = expose;
    this.type = exposedType;
    this.names = names;
    this.typer = resolver;

    this.bindings = new HashMap<>(encoding.element().size());
    this.builderBindings = new HashMap<>(encoding.element().size());

    populateBindings(resolver);

    this.namer = new Function<EncodedElement, String>() {
      @Override
      public String apply(EncodedElement input) {
        return input.inBuilder()
            ? builderBindings.get(input.asBinding())
            : bindings.get(input.asBinding());
      }
    };
  }

  private void populateBindings(VariableResolver resolver) {
    for (EncodedElement e : encoding.element()) {
      if (e.isStatic()) {
        if (e.inBuilder()) {
          builderBindings.put(e.asBinding(), generateProperName(e));
        } else {
          // statics from value are visible in builder
          builderBindings.put(e.asBinding(), generateProperName(e));
          bindings.put(e.asBinding(), generateProperName(e));
        }
      }
    }

    for (EncodedElement e : encoding.element()) {
      if (!e.isStatic()) {
        if (e.inBuilder()) {
          builderBindings.put(e.asBinding(), generateProperName(e));
        } else {
          bindings.put(e.asBinding(), generateProperName(e));
        }
      }
    }

    for (Variable v : resolver.variables()) {
      Binding binding = Binding.newTop(v.name);
      String value = resolver.apply(v).toString();
      bindings.put(binding, value);
      builderBindings.put(binding, value);
    }
  }

  public boolean supportsInternalImplConstructor() {
    return encoding.build().type().equals(encoding.impl().type());
  }

  public boolean supportsDefaultValue() {
    return !encoding.impl().code().isEmpty();
  }

  private String generateProperName(EncodedElement element) {
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
    String raw = element.depluralize() ? names.singular() : names.raw;
    if (element.isStaticField() && element.isFinal()) {
      raw = CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_UNDERSCORE, raw);
    }
    return element.naming().apply(raw);
  }

  final Templates.Invokable fragmentOf = new Templates.Invokable() {
    @Override
    public @Nullable Invokable invoke(Invokation invokation, Object... parameters) {
      final EncodedElement el = (EncodedElement) parameters[0];

      @Nullable Map<Binding, String> overrideBindings = null;
      if (el.params().size() == 1 && parameters.length > 1) {
        overrideBindings = ImmutableMap.of(
            Binding.newTop(el.params().get(0).name()),
            parameters[1].toString());
      }

      Map<Binding, String> contextBindings = el.inBuilder() ? builderBindings : bindings;
      Code.Interpolator interpolator =
          new Code.Interpolator(names.raw, contextBindings, overrideBindings);

      if (!el.oneLiner().isEmpty()
          && !encoding.crossReferencedMethods().contains(el.name())) {
        for (Code.Term t : interpolator.apply(el.oneLiner())) {
          invokation.out(t);
        }
      } else {
        invokation.out(contextBindings.get(el.asBinding())).out("(");
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

      Map<Binding, String> contextBindings = el.inBuilder() ? builderBindings : bindings;

      Code.Interpolator interpolator =
          new Code.Interpolator(names.raw, contextBindings, null);

      printWithIndentation(invokation, interpolator.apply(code));
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
}
