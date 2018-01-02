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

import com.google.common.base.*;
import com.google.common.collect.*;
import java.util.*;
import javax.annotation.Nullable;
import org.immutables.generator.Templates;
import org.immutables.generator.Templates.Invokable;
import org.immutables.generator.Templates.Invokation;
import org.immutables.value.processor.encode.Code.Binding;
import org.immutables.value.processor.encode.Code.Term;
import org.immutables.value.processor.encode.EncodedElement.Param;
import org.immutables.value.processor.encode.EncodedElement.TypeParam;
import org.immutables.value.processor.encode.Type.*;
import org.immutables.value.processor.meta.Styles;
import org.immutables.value.processor.meta.Styles.UsingName.AttributeNames;
import org.immutables.value.processor.meta.ValueType;

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
  private final ValueType containingType;

  Instantiation(
      EncodingInfo encoding,
      EncodedElement expose,
      Type exposedType,
      Styles.UsingName.AttributeNames names,
      VariableResolver resolver,
      ValueType containingType) {
    this.encoding = encoding;
    this.expose = expose;
    this.type = exposedType;
    this.names = names;
    this.typer = resolver;
    this.containingType = containingType;

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

  public boolean hasTrivialFrom() {
    ImmutableList<Term> oneLiner = encoding.from().oneLiner();
    return oneLiner.size() == 1
        && oneLiner.get(0).equals(
            Binding.newTop(encoding.from().firstParam().name()));
  }

  public String getDecoratedImplFieldName() {
    return bindings.get(encoding.impl().asBinding()) + "$impl";
  }

  public boolean hasVirtualImpl() {
    return encoding.impl().isVirtual();
  }

  public boolean supportsInternalImplConstructor() {
    return encoding.build().type().equals(encoding.impl().type());
  }

  public boolean supportsDefaultValue() {
    return !encoding.impl().code().isEmpty();
  }

  private String generateProperName(EncodedElement element) {
    if (element.isImplField()) {
      return names.var;
    }

    if (element.isExpose()) {
      return names.get;
    }

    if (element.standardNaming() != StandardNaming.NONE) {
      switch (element.standardNaming()) {
      case GET:
        return names.get;
      case INIT:
        return names.init;
      case ADD:
        return names.add();
      case ADD_ALL:
        return names.addAll();
      case PUT:
        return names.put();
      case PUT_ALL:
        return names.putAll();
      case WITH:
        return names.with;
      default:
      }
    }

    if (isDefaultUnspecifiedValue(element)) {
      if (element.isCopy()) {
        return names.with;
      }
      if (element.isInit()) {
        return names.init;
      }
    }

    if (element.isStaticField() && element.isFinal()) {
      String base = CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_UNDERSCORE, rawName());
      return element.naming().apply(base);
    }

    return names.apply(element.naming(), element.depluralize());
  }

  private String rawName() {
    return names.raw;
  }

  final Predicate<EncodedElement> isSafeVarargs = new Predicate<EncodedElement>() {
    @Override
    public boolean apply(EncodedElement input) {
      return input.isFinal()
          && input.isSafeVarargs()
          && hasGenericVarargs(input);
    }
  };

  final Predicate<EncodedElement> isInlined = new Predicate<EncodedElement>() {
    @Override
    public boolean apply(EncodedElement input) {
      return isInlined(input);
    }
  };

  private boolean isInlined(EncodedElement el) {
    return el.isInlinable()
        && !el.oneLiner().isEmpty()
        && !encoding.crossReferencedMethods().contains(el.name())
        && !entangledBuildMethod(el);
  }

  protected boolean hasGenericVarargs(EncodedElement input) {
    List<Param> params = input.params();
    if (!params.isEmpty()) {
      Type t = Iterables.getLast(params).type();
      if (t instanceof Type.Array) {
        Type.Array a = (Type.Array) t;
        return a.varargs && typer.apply(a.element) instanceof Type.Variable;
      }
    }
    return false;
  }

  private boolean entangledBuildMethod(EncodedElement el) {
    return el.isBuild() && containingType.isGenerateBuilderConstructor();
  }

  private boolean isDefaultUnspecifiedValue(EncodedElement element) {
    return element.naming().isIdentity() && !element.depluralize();
  }

  final Templates.Invokable fragmentOf = new Templates.Invokable() {
    @Override
    public @Nullable Invokable invoke(Invokation invokation, Object... parameters) {
      final EncodedElement el = (EncodedElement) parameters[0];

      @Nullable Map<Binding, String> overrideBindings = null;
      if (el.params().size() == 1 && parameters.length > 1) {
        overrideBindings = ImmutableMap.of(
            Binding.newTop(el.firstParam().name()),
            parameters[1].toString());
      }

      Map<Binding, String> contextBindings = el.inBuilder() ? builderBindings : bindings;
      Code.Interpolator interpolator =
          new Code.Interpolator(rawName(), contextBindings, overrideBindings);

      if (isInlined(el)) {
        printWithIndentation(invokation, interpolator.apply(el.oneLiner()));
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

      Map<Binding, String> contextBindings = el.inBuilder() ? builderBindings : bindings;

      List<Term> code = el.code();

      if (parameters.length >= 2) {
        String param = parameters[1].toString();
        code = Code.replaceReturn(code, param);
      }

      Code.Interpolator interpolator =
          new Code.Interpolator(rawName(), contextBindings, null);

      printWithIndentation(invokation, interpolator.apply(code));
      return null;
    }
  };

  // for aux fields we can override impl field binding
  final Templates.Invokable codeOfAuxFieldVirtualDecorated = new Templates.Invokable() {
    @Override
    public @Nullable Invokable invoke(Invokation invokation, Object... parameters) {
      EncodedElement el = (EncodedElement) parameters[0];

      Map<Binding, String> contextBindings = el.inBuilder() ? builderBindings : bindings;
      Map<Binding, String> overrideBindings =
          ImmutableMap.of(Binding.newField(encoding.impl().name()), getDecoratedImplFieldName());

      Code.Interpolator interpolator =
          new Code.Interpolator(
              rawName(),
              contextBindings,
              overrideBindings);

      printWithIndentation(invokation, interpolator.apply(el.code()));
      return null;
    }
  };

  private static void printWithIndentation(Invokation invokation, List<Term> terms) {
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
      }

      if (t.isDelimiter() && (t.is(';') || t.is('}') || t.is('{'))) {
        indentWrap = 0;
      } else if (!t.isIgnorable()) {
        // auto-increase indent wrap unless semicolon will return it back
        indentWrap = 2;
      }

      // increase indent level after writing a newline
      if (t.isDelimiter() && t.is('{')) {
        indentLevel++;
      }

      // outputing actual token after any indents
      invokation.out(t);
    }
  }

  final Function<EncodedElement, String> ownTypeParams =
      new Function<EncodedElement, String>() {
        @Override
        public String apply(EncodedElement input) {
          Parameters parameters = Type.Producer.emptyParameters();

          // if our method have the same named type parameters as
          // encoding, when instantiating it for specific
          // attribute, some may resolve to concrete types, some
          // may end up value-type specific type parameter
          // we need to write only type-specific type parameters omiting those
          // which resolves to specific type.
          // note that some methods are to be inlined, so this is not needed
          // then, it is only needed when non-inlined references are present.

          if (input.isFrom()) {
            // from has implied type parameters, the same as encoding
            for (Variable v : typer.variables()) {
              parameters = introduceAsEncodingVar(parameters, v);
            }
          } else {
            for (TypeParam p : input.typeParams()) {
              @Nullable Variable encodingVar = typer.byName(p.name());
              if (encodingVar != null) {
                parameters = introduceAsEncodingVar(parameters, encodingVar);
              } else {
                parameters = parameters.introduce(p.name(), transformBounds(p.bounds()));
              }
            }
          }

          if (parameters.names().isEmpty()) {
            return "";
          }

          return parameters + " ";
        }

        private Parameters introduceAsEncodingVar(Parameters parameters, Variable encodingVar) {
          Type t = typer.apply(encodingVar);
          final Parameters[] pHolder = new Parameters[] {parameters};
          t.accept(new Type.Transformer() {
            @Override
            public Type variable(Variable v) {
              pHolder[0] = pHolder[0].introduce(v.name, transformBounds(v.upperBounds));
              return v;
            }
          });
          parameters = pHolder[0];
          return parameters;
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
