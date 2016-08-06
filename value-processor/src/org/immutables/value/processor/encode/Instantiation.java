package org.immutables.value.processor.encode;

import com.google.common.base.CaseFormat;
import com.google.common.base.Function;
import com.google.common.base.Functions;
import com.google.common.collect.FluentIterable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import org.immutables.generator.Templates;
import org.immutables.generator.Templates.Invokable;
import org.immutables.generator.Templates.Invokation;
import org.immutables.value.processor.encode.Code.Term;
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

    this.generatedNames = new HashMap<>(encoding.element().size());
    for (EncodedElement e : encoding.element()) {
      if (!e.isExpose()) {
        this.generatedNames.put(e.name(), generateProperName(e, names));
      }
    }
    this.generatedNames.put(expose.name(), names.get);

    for (Variable v : resolver.variables()) {
      this.generatedNames.put(v.name, resolver.apply(v).toString());
    }

    this.namer = Functions.forMap(generatedNames);
    this.coder = new Code.Interpolator(namer, NO_SUBSTITUTIONS);
  }

  public boolean supportsInternalImplConstructor() {
    return encoding.build().type().equals(encoding.impl().type());
  }

  private String generateProperName(EncodedElement element, Styles.UsingName.AttributeNames names) {
    if (!element.isInlinable() && element.isSynthetic()) {
      if (element.isCopy()) {
        return names.with;
      }
      if (element.isInit()) {
        return names.init;
      }
    }
    String raw = names.raw;
    if (element.isStatic() && element.isFinal()) {
      raw = CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_UNDERSCORE, raw);
    }
    return element.naming().apply(raw);
  }

  final Templates.Invokable fragmentOf = new Templates.Invokable() {
    @Override
    public @Nullable Invokable invoke(Invokation invokation, Object... parameters) {
      final EncodedElement el = (EncodedElement) parameters[0];
      final String param = parameters.length > 1 ? parameters[1].toString() : "";

      Code.Interpolator interpolator = coder;
      if (el.params().size() == 1) {
        final String name = el.params().get(0).name();
        interpolator = new Code.Interpolator(namer, new Function<String, String>() {
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
        invokation.out(namer.apply(el.name())).out("(").out(param).out(")");
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
      for (Code.Term t : coder.apply(code)) {
        invokation.out(t);
      }
      return null;
    }
  };

  final Function<EncodedElement, Type.Parameters> ownTypeParams =
      new Function<EncodedElement, Type.Parameters>() {
        @Override
        public Type.Parameters apply(EncodedElement input) {
          Parameters parameters = Type.Producer.emptyParameters();
          for (TypeParam p : input.typeParams()) {
            parameters = parameters.introduce(p.name(), FluentIterable.from(p.bounds())
                .transform(typer)
                .filter(Defined.class)
                .toList());
          }
          return parameters;
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
