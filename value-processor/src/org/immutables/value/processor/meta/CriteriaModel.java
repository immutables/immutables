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

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import org.immutables.value.processor.encode.Type;
import org.immutables.value.processor.encode.TypeExtractor;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Creates several matchers programmatically based on {@link ValueAttribute}.
 * {@code StringMatcher}, {@code WithMatcher}, {@code NotMatcher} etc.
 */
public class CriteriaModel {

  private static final Iterable<Type.Defined> NO_BOUNDS = Collections.emptyList();

  private final ValueAttribute attribute;
  private final Type.Factory factory;
  private final Elements elements;
  private final Types types;
  private final IntrospectedType introspectedType;

  CriteriaModel(ValueAttribute attribute) {
    this.attribute = Preconditions.checkNotNull(attribute, "attribute");
    this.factory = new Type.Producer();
    ProcessingEnvironment env = attribute.containingType.constitution.protoclass().environment().processing();
    this.elements = env.getElementUtils();
    this.types = env.getTypeUtils();
    this.introspectedType = new IntrospectedType(attribute.returnType, attribute.isNullable(), env.getTypeUtils(), env.getElementUtils());
  }

  private static class IntrospectedType {

    private final TypeMirror type;
    private final Types types;
    private final Elements elements;
    private final boolean nullable;

    // type erasure will be boxed for primitive types
    private final TypeMirror erasure;

    IntrospectedType(TypeMirror type, boolean nullable, Types types, Elements elements) {
      this.types = types;
      this.elements = elements;
      this.type = Preconditions.checkNotNull(type, "type");
      this.nullable = nullable;

      TypeMirror erasure = types.erasure(type);
      if (erasure.getKind().isPrimitive()) {
        erasure = types.boxedClass(MoreTypes.asPrimitiveType(erasure)).asType();
      }
      this.erasure = erasure;
    }

    public TypeMirror type() {
      return type;
    }

    public IntrospectedType withType(TypeMirror type) {
      return new IntrospectedType(type, false, types, elements);
    }

    private boolean isSubtypeOf(Class<?> maybeSuper) {
      Objects.requireNonNull(maybeSuper, "maybeSuper");
      return isSubtypeOf(elements.getTypeElement(maybeSuper.getCanonicalName()));
    }

    private boolean isSubtypeOf(Element element) {
      Objects.requireNonNull(element, "element");
      final TypeMirror maybeSuperType = element.asType();
      return types.isSubtype(erasure, types.erasure(maybeSuperType));
    }

    public boolean isBoolean() {
      return type.getKind() == TypeKind.BOOLEAN ||
              isSubtypeOf(Boolean.class);
    }

    public boolean isNumber() {
      return type.getKind().isPrimitive() && !isBoolean() || isSubtypeOf(Number.class);
    }

    public boolean isContainer() {
      return isIterable() || isOptional() || isArray() || isMap();
    }

    public boolean isScalar() {
      return !isContainer();
    }

    public boolean isEnum() {
      return types.asElement(type).getKind() == ElementKind.ENUM;
    }

    public boolean isIterable() {
      return isSubtypeOf(Iterable.class);
    }

    public boolean isArray() {
      return type.getKind() == TypeKind.ARRAY;
    }

    public boolean hasOptionalMatcher() {
      return isString() || isComparable() || isBoolean();
    }

    public boolean isComparable() {
      return isSubtypeOf(Comparable.class);
    }

    public boolean isString() {
      return isSubtypeOf(String.class);
    }

    public boolean isMap() {
      return isSubtypeOf(Map.class);
    }

    public TypeMirror box() {
      return type.getKind().isPrimitive() ? types.boxedClass(MoreTypes.asPrimitiveType(type)).asType() : type;
    }

    public boolean hasCriteria() {
      final Element element = types.asElement(type);
      return element != null && CriteriaMirror.find(element).isPresent();
    }

    private IntrospectedType optionalParameter() {
      if (isNullable()) {
        return withType(type);
      }

      final String typeName = erasure.toString();
      final TypeMirror newType;
      if ("java.util.OptionalInt".equals(typeName)) {
        newType = elements.getTypeElement(Integer.class.getName()).asType();
      } else if ("java.util.OptionalLong".equals(typeName)) {
        newType = elements.getTypeElement(Long.class.getName()).asType();
      } else if ("java.util.OptionalDouble".equals(typeName)) {
        newType = elements.getTypeElement(Double.class.getName()).asType();
      } else if ("java.util.Optional".equals(erasure.toString()) || "com.google.common.base.Optional".equals(erasure.toString())) {
        newType = MoreTypes.asDeclared(type).getTypeArguments().get(0);
      } else {
        throw new IllegalArgumentException(String.format("%s is not an optional type", type));
      }

      return withType(newType);
    }

    public boolean isNullable() {
      return nullable;
    }

    public boolean useOptional() {
      return isOptional() || isNullable();
    }

    public boolean isOptional() {
      final List<String> names = Arrays.asList("java.util.Optional", "java.util.OptionalInt",
              "java.util.OptionalDouble", "java.util.OptionalLong", Optional.class.getName());

      for (String name : names) {
        final Element element = elements.getTypeElement(name);
        if (element != null && isSubtypeOf(element)) {
          return true;
        }
      }

      return false;
    }
  }

  private Type toType(TypeMirror mirror) {
    if (mirror.getKind() == TypeKind.ARRAY) {
      return factory.array(toType(MoreTypes.asArray(mirror).getComponentType()));
    } else if (mirror.getKind().isPrimitive()) {
      final TypeElement boxed = types.boxedClass(MoreTypes.asPrimitiveType(mirror));
      return factory.reference(boxed.getQualifiedName().toString());
    }

    final Element element = types.asElement(mirror);
    if (element == null) {
      throw new IllegalArgumentException(String.format("Element for type %s not found (attribute %s %s)",
              mirror, attribute.name(), attribute.returnType));
    }

    final TypeExtractor extractor = new TypeExtractor(factory, MoreElements.asType(element));
    return extractor.get(mirror);
  }

  private Type.Parameterized matcherType(IntrospectedType introspected) {
    final TypeMirror type = introspected.type;
    final String name;

    if (introspected.useOptional()) {
      IntrospectedType param = introspected.optionalParameter();
      // use optional intersection-types ?
      if (param.isString()) {
        name = "org.immutables.criteria.matcher.OptionalStringMatcher.Template";
      } else if (param.isBoolean()) {
        name = "org.immutables.criteria.matcher.OptionalBooleanMatcher.Template";
      } else if (param.isComparable()) {
        name = "org.immutables.criteria.matcher.OptionalComparableMatcher.Template";
      } else {
        name = "org.immutables.criteria.matcher.OptionalMatcher";
      }
    } else if (introspected.hasCriteria()) {
      name = type.toString() + "CriteriaTemplate";
    } else if (introspected.isBoolean()) {
      name = "org.immutables.criteria.matcher.BooleanMatcher.Template";
    } else if (introspected.isString()) {
      name = "org.immutables.criteria.matcher.StringMatcher.Template";
    } else if (introspected.isIterable() || introspected.isArray()) {
      name = "org.immutables.criteria.matcher.IterableMatcher";
    } else if (introspected.isComparable()) {
      name = "org.immutables.criteria.matcher.ComparableMatcher.Template";
    } else {
      name = "org.immutables.criteria.matcher.ObjectMatcher.Template";
    }

    final Element element = elements.getTypeElement(name);
    final Type.Parameterized matcherType;
    if (element == null) {
      // means type not found in classpath. probably not yet generated criteria
      // create PersonCriteria<R> manually with Type.Parameterized
      final Type.Variable variable = factory.parameters().introduce("R", NO_BOUNDS).variable("R");
      matcherType = factory.parameterized(factory.reference(name), Collections.singleton(variable));
    } else {
      matcherType = (Type.Parameterized) toType(element.asType());
    }

    return matcherType;
  }

  public Type.Parameterized buildMatcher() {
    return buildMatcher(introspectedType);
  }

  private Type.Parameterized buildMatcher(IntrospectedType introspected) {
    Preconditions.checkNotNull(introspected, introspected);
    final TypeMirror type = introspected.type;
    final Type.Parameterized matcher = matcherType(introspected);
    if (matcher.arguments.size() > 1) {
      // replace second and maybe third argument
      // first type argument R unchanged
      Type.VariableResolver resolver = Type.VariableResolver.empty();

      final Type valueType;
      final Type.Variable arg1 = (Type.Variable) matcher.arguments.get(1);

      if (introspected.useOptional()) {
        final IntrospectedType newType = introspected.optionalParameter();
        valueType = toType(newType.type());
        if (newType.hasOptionalMatcher()) {
          // don't recurse if optional matcher is present like OptionalComparableMatcher
          resolver = resolver.bind(arg1, (Type.Nonprimitive) valueType);
        } else {
          resolver = resolver.bind(arg1, buildMatcher(newType));
        }
      } else if (introspected.isScalar()) {
        // this is leaf no need to recurse
        valueType = toType(introspected.box());
        resolver = resolver.bind(arg1, (Type.Nonprimitive) valueType);
      } else if (introspected.isArray()) {
        final TypeMirror mirror = MoreTypes.asArray(type).getComponentType();
        valueType = toType(mirror);
        resolver = resolver.bind(arg1, buildMatcher(introspected.withType(mirror)));
      } else {
        final TypeMirror mirror = MoreTypes.asDeclared(type).getTypeArguments().get(0);
        valueType = toType(mirror);
        resolver = resolver.bind(arg1, buildMatcher(introspected.withType(mirror)));
      }

      if (matcher.arguments.size() > 2) {
        // last parameter is usually value
        resolver = resolver.bind((Type.Variable) matcher.arguments.get(2), (Type.Nonprimitive) valueType);
      }

      return (Type.Parameterized) matcher.accept(resolver);
    }

    return matcher;
  }


  public MatcherDefinition matcher() {
    return new MatcherDefinition(attribute, buildMatcher());
  }

  public static class MatcherDefinition {
    private final ValueAttribute attribute;
    private final Type.Parameterized type;
    private final IntrospectedType introspectedType;

    private MatcherDefinition(ValueAttribute attribute, Type.Parameterized type) {
      this.attribute = attribute;
      this.type = Preconditions.checkNotNull(type, "type");
      ProcessingEnvironment env = attribute.containingType.constitution.protoclass().environment().processing();
      this.introspectedType = new IntrospectedType(attribute.returnType, false, env.getTypeUtils(), env.getElementUtils());
    }

    public Type.Parameterized matcherType() {
      return this.type;
    }

    // TODO the logic here is messy. Cleanup creator API and update this method
    public String creator() {
      final String withPath = String.format("withPath(%s.class, \"%s\")", attribute.containingType.typeDocument().toString(), attribute.name());

      final String firstCreator;
      final String name = type.reference.name;
      final boolean hasCriteria = attribute.hasCriteria() && !attribute.isContainerType();
      if (hasCriteria) {
        firstCreator = String.format("%s.creator().create(%%s)", attribute.returnType.toString() + "Criteria");
      } else {
        final String newName = name.endsWith(".Template") ? name.substring(0, name.lastIndexOf('.')) : name;
        firstCreator = String.format("%s.creator().create(%%s)", newName);
      }

      final String secondCreator = secondCreator();

      final String withCreators = String.format("withCreators(%s.creator(), %s)", attribute.containingType.name() + "Criteria", secondCreator);
      return String.format(firstCreator, new StringBuilder().append("context.").append(withPath).append(".").append(withCreators));
    }

    private String secondCreator() {
      String name = type.reference.name;
      if (name.endsWith(".Template")) {
        // scalar matcher
        return name.substring(0, name.lastIndexOf('.')) + ".creator()";
      }

      if (!(introspectedType.type().getKind() == TypeKind.DECLARED ||
              introspectedType.isOptional() || introspectedType.isIterable())) {
        return attribute.containingType.element.getSimpleName() + "Criteria.creator()";
      }

      final DeclaredType declaredType = MoreTypes.asDeclared(introspectedType.type());

      if (declaredType.getTypeArguments().isEmpty()) {
        // same as first
        return attribute.containingType.element.getSimpleName() + "Criteria.creator()";
      }

      Preconditions.checkArgument(declaredType.getTypeArguments().size() == 1, "Expected single arg for ", declaredType);
      IntrospectedType type2 = introspectedType.withType(declaredType.getTypeArguments().get(0));

      if (type2.hasCriteria()) {
        return type2.erasure + "Criteria.creator()";
      }

      Type.Parameterized param = (Type.Parameterized) type;
      if (name.endsWith("Matcher")) {
        return name + ".creator()";
      }

      throw new IllegalArgumentException("Can't detect second creator for " + introspectedType.type());
    }
  }
}
