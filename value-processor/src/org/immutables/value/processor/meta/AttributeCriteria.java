/*
 * Copyright 2019 Immutables Authors and Contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.immutables.value.processor.meta;

import com.google.common.base.Preconditions;
import org.immutables.value.processor.encode.Type;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Creates several matchers programmatically based on {@link ValueAttribute}.
 * {@code StringMatcher}, {@code WithMatcher}, {@code NotMatcher} etc.
 */
public class AttributeCriteria {

  private static final Iterable<Type.Defined> NO_BOUNDS = Collections.emptyList();

  private final ValueAttribute attribute;
  private final Type.Factory factory;
  private final Type.Parameters parameters;

  AttributeCriteria(ValueAttribute attribute) {
    this.attribute = Preconditions.checkNotNull(attribute, "attribute");
    this.factory = new Type.Producer();
    this.parameters = factory.parameters()
            .introduce("R", NO_BOUNDS)
            .introduce("S", NO_BOUNDS)
            .introduce("C", NO_BOUNDS)
            .introduce("V", NO_BOUNDS);
  }

  public MatcherDefinition matcher() {
    final Type type;

    // TODO probably need to use Transformer
    if (attribute.isStringType()) {
      // StringMatcher<R>
      type = parameterized("org.immutables.criteria.matcher.StringMatcher.Template", "R");
    } else if (Boolean.class.getName().equals(attribute.getWrapperType())) {
      type = parameterized("org.immutables.criteria.matcher.BooleanMatcher.Template", "R");
    } else if (attribute.isOptionalType()) {
      Type.Parameterized param = (Type.Parameterized) parameterized("org.immutables.criteria.matcher.OptionalMatcher", "R", "S");
      final Type.Defined def;
      if (attribute.hasCriteria()) {
        def = parameterized(attribute.getUnwrappedElementType() + "Criteria", "R");
      } else  {
        def = scalar();
      }

      final Type.VariableResolver resolver = Type.VariableResolver.empty()
              .bind(variable("S"), def);

      type = param.accept(resolver);
    } else if (attribute.isComparable()) {
      Type.Defined def = parameterized("org.immutables.criteria.matcher.ComparableMatcher.Template", "R", "V");
      final Type.VariableResolver resolver = Type.VariableResolver.empty()
              .bind(variable("V"), factory.reference(attribute.getWrappedElementType()));
      type = def.accept(resolver);
    } else if (attribute.isCollectionType()) {
      final Type.Defined param = parameterized("org.immutables.criteria.matcher.IterableMatcher", "R", "S", "V");
      final Type.Defined other;
      if (attribute.hasCriteria()) {
        other = parameterized(attribute.getUnwrappedElementType() + "Criteria", "R");
      } else {
        other = scalar();
      }

      final Type.VariableResolver resolver = Type.VariableResolver.empty()
              .bind(variable("S"), other)
              .bind(variable("V"), factory.reference(attribute.getWrappedElementType()));

      type = param.accept(resolver);
    } else if (attribute.hasCriteria()) {
      type = parameterized(attribute.getUnwrappedElementType() + "Criteria", "R");
    } else {
      final Type.VariableResolver resolver = Type.VariableResolver.empty()
              .bind(variable("V"), factory.reference(attribute.getWrappedElementType()));

      type = parameterized("org.immutables.criteria.matcher.ObjectMatcher.Template", "R", "V").accept(resolver);
    }

    return new MatcherDefinition(type);
  }


  public MatcherDefinition scalarMatcher() {
    return new MatcherDefinition(scalar());
  }

  private Type.Variable variable(String name) {
    return parameters.variable(name);
  }

  private Type.Defined scalar() {
    final Type.Defined type;
    if (Boolean.class.getName().equals(attribute.getWrappedElementType())) {
      type = parameterized("org.immutables.criteria.matcher.BooleanMatcher.Template", "R");
    } else if (String.class.getName().equals(attribute.getWrappedElementType())) {
      type = parameterized("org.immutables.criteria.matcher.StringMatcher.Template", "R");
    } else if (attribute.isMaybeComparableKey()) {
      type = parameterized("org.immutables.criteria.matcher.ComparableMatcher.Template", "R", "V");
    } else {
      type = parameterized("org.immutables.criteria.matcher.ObjectMatcher.Template", "R", "V");
    }

    final Type.VariableResolver resolver = Type.VariableResolver.empty()
            .bind(variable("V"), factory.reference(attribute.getWrappedElementType()));


    return (Type.Defined) type.accept(resolver);
  }

  public class MatcherDefinition {
    private final Type type;

    private MatcherDefinition(Type type) {
      this.type = type;
    }

    public MatcherDefinition toSelf() {
      return new MatcherDefinition(AttributeCriteria.this.toSelf(type));
    }

    public MatcherDefinition withoutParameters() {
      final Type.Transformer transformer = new Type.Transformer() {
        @Override
        public Type parameterized(Type.Parameterized parameterized) {
          return parameterized.reference;
        }
      };
      return new MatcherDefinition(type.accept(transformer));
    }

    /** Return upper level matcher: StringMatcher.Template -> StringMatcher. */
    public MatcherDefinition enclosing() {
      final String name = ((Type.Parameterized) type).reference.name;
      if (name.endsWith(".Self") || name.endsWith(".Template")) {
        final String newName = name.substring(0, name.lastIndexOf('.'));
        return new MatcherDefinition(factory.reference(newName));
      }

      return this;
    }

    public String toTemplate() {
      return type.toString();
    }

    @Override
    public String toString() {
      return toTemplate();
    }
  }

  private Type.Defined parameterized(String name, String ... params) {
    final Type.Reference reference = factory.reference(name);
    final List<Type.Variable> variables = new ArrayList<>();
    for (String param: params) {
      variables.add(parameters.variable(param));
    }

    return variables.isEmpty() ? reference : factory.parameterized(reference, variables);
  }

  private Type.Defined toSelf(Type type) {
    final Type.Transformer transformer = new Type.Transformer() {
      @Override
      public Type reference(Type.Reference reference) {
        // check if it is immutables matcher or attribute has criteria defined
        if (!(attribute.hasCriteria() || reference.name.startsWith("org.immutables.criteria.matcher."))) {
          return defaults(reference);
        }

        // is already transformed ?
        if (reference.name.contains(".Self")) {
          return defaults(reference);
        }

        // PetCriteria vs Pet vs IterableMatcher
        if (reference.name.endsWith(".Template")) {
          return factory.reference(reference.name.replace(".Template", ".Self"));
        } else {
          return factory.reference(reference.name + ".Self");
        }
      }

      @Override
      public Type parameterized(Type.Parameterized parameterized) {
        if (parameterized.reference.name.contains(".Self")) {
          return defaults(parameterized);
        }

        Type.Reference reference = (Type.Reference) parameterized.reference.accept(this);
        if (parameterized.arguments.size() <= 1) {
          return defaults(reference);
        }

        final List<Type.Nonprimitive> args = new ArrayList<>();
        for (Type.Nonprimitive arg: parameterized.arguments.subList(1, parameterized.arguments.size())) {
          if (arg instanceof Type.Parameterized) {
            args.add((Type.Nonprimitive) arg.accept(this));
          } else {
            args.add(arg);
          }
        }

        return factory.parameterized(reference, args);
      }
    };


    return (Type.Defined) type.accept(transformer);
  }

}
