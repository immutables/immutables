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
    this.introspectedType = new IntrospectedType(attribute.returnType, env.getTypeUtils(), env.getElementUtils());
  }

  private static class IntrospectedType {

    private final TypeMirror type;
    private final Types types;
    private final Elements elements;

    // type erasure will be boxed for primitive types
    private final TypeMirror erasure;

    IntrospectedType(TypeMirror type, Types types, Elements elements) {
      this.types = types;
      this.elements = elements;
      this.type = Preconditions.checkNotNull(type, "type");

      TypeMirror erasure = types.erasure(type);
      if (erasure.getKind().isPrimitive()) {
        erasure = types.boxedClass(MoreTypes.asPrimitiveType(erasure)).asType();
      }
      this.erasure = erasure;
    }

    public IntrospectedType withType(TypeMirror type) {
      return new IntrospectedType(type, types, elements);
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

    private TypeMirror optionalParameter() {
      final String typeName = erasure.toString();
      if ("java.util.OptionalInt".equals(typeName)) {
        return elements.getTypeElement(Integer.class.getName()).asType();
      } else if ("java.util.OptionalLong".equals(typeName)) {
        return elements.getTypeElement(Long.class.getName()).asType();
      } else if ("java.util.OptionalDouble".equals(typeName)) {
        return elements.getTypeElement(Double.class.getName()).asType();
      } else if ("java.util.Optional".equals(erasure.toString()) || "com.google.common.base.Optional".equals(erasure.toString())) {
        return MoreTypes.asDeclared(type).getTypeArguments().get(0);
      }

      throw new IllegalArgumentException(String.format("%s is not an optional type", type));

    }

    public boolean isOptional() {
      final List<String> names = Arrays.asList("java.util.Optional", "java.util.OptionalInt",
              "java.util.OptionalDouble", "java.util.OptionalLong", Optional.class.getName());

      for (String name: names) {
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

  private Type.Parameterized matcherType(TypeMirror type) {
    final IntrospectedType introspected = this.introspectedType.withType(type);
    final String name;

    if (introspected.hasCriteria()) {
      name = type.toString() + "Criteria";
    } else if (introspected.isBoolean()) {
      name = "org.immutables.criteria.matcher.BooleanMatcher.Template";
    } else if (introspected.isString()) {
      name = "org.immutables.criteria.matcher.StringMatcher.Template";
    } else if (introspected.isOptional()) {
      name =  "org.immutables.criteria.matcher.OptionalMatcher";
    } else if (introspected.isIterable()) {
      name = "org.immutables.criteria.matcher.IterableMatcher";
    } else if (introspected.isArray()) {
      name = "org.immutables.criteria.matcher.ArrayMatcher";
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
    return buildMatcher(attribute.returnType);
  }

  private Type.Parameterized buildMatcher(TypeMirror type) {
    Preconditions.checkNotNull(type, "type");
    final IntrospectedType introspected = this.introspectedType.withType(type);
    final Type.Parameterized matcher = matcherType(type);
    if (matcher.arguments.size() > 1) {
      // replace second and maybe third argument
      // first type argument R unchanged
      Type.VariableResolver resolver = Type.VariableResolver.empty();

      final Type valueType;
      final Type.Variable arg1 = (Type.Variable) matcher.arguments.get(1);
      if (introspected.isScalar()) {
        // this is leaf no need to recurse
        valueType = toType(introspected.box());
        resolver = resolver.bind(arg1, (Type.Nonprimitive) valueType);
      } else if (introspected.isOptional()) {
        final TypeMirror mirror = introspected.optionalParameter();
        valueType = toType(mirror);
        resolver = resolver.bind(arg1, buildMatcher(mirror));
      } else if (introspected.isArray()) {
        final TypeMirror mirror = MoreTypes.asArray(type).getComponentType();
        valueType = toType(mirror);
        resolver = resolver.bind(arg1, buildMatcher(mirror));
      } else {
        final TypeMirror mirror = MoreTypes.asDeclared(type).getTypeArguments().get(0);
        valueType = toType(mirror);
        resolver = resolver.bind(arg1, buildMatcher(mirror));
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
    return new MatcherDefinition(buildMatcher());
  }

  public class MatcherDefinition {
    private final Type.Parameterized type;

    private MatcherDefinition(Type.Parameterized type) {
      this.type = Preconditions.checkNotNull(type, "type");
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
        firstCreator = String.format("%s.create(%%s)", name);
      } else {
        final String newName = name.endsWith(".Template") ? name.substring(0, name.lastIndexOf('.')) : name;
        firstCreator = String.format("%s.creator().create(%%s)", newName);
      }

      final String secondCreator;
      if (hasCriteria || type.arguments.size() <= 1) {
        secondCreator = creator(type);
      } else {
        secondCreator = creator(type.arguments.get(1));
      }

      final String withCreators = String.format("withCreators(ctx -> %s.create(ctx), %s)", attribute.containingType.name() + "Criteria", secondCreator);
      return String.format(firstCreator, new StringBuilder().append("context.").append(withPath).append(".").append(withCreators));
    }

    private String creator(Type type) {
      if (!(type instanceof Type.Parameterized)) {
        return "org.immutables.criteria.matcher.ObjectMatcher.creator()";
      }

      Type.Parameterized param = (Type.Parameterized) type;
      String name = param.reference.name;
      if (name.endsWith(".Template")) {
        final String newName = name.endsWith(".Template") ? name.substring(0, name.lastIndexOf('.')) : name;
        return newName + ".creator()";
      } else if (name.endsWith("Matcher")) {
        return name + ".creator()";
      } else {
        // criteria
        return "ctx -> " + name + ".create(ctx)";
      }
    }
  }
}
