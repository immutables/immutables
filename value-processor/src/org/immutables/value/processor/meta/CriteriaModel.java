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
import java.math.BigDecimal;
import java.math.BigInteger;
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


  private static final String MATCHER = "org.immutables.criteria.matcher.Matcher";

  private static final Iterable<Type.Defined> NO_BOUNDS = Collections.emptyList();

  private final ValueAttribute attribute;
  private final Type.Factory factory;
  private final Elements elements;
  private final Types types;
  private final IntrospectedType introspectedType;
  private final MatcherDefinition matcherDefinition;

  CriteriaModel(ValueAttribute attribute) {
    this.attribute = Preconditions.checkNotNull(attribute, "attribute");
    this.factory = new Type.Producer();
    ProcessingEnvironment env = attribute.containingType.constitution.protoclass().environment().processing();
    this.elements = env.getElementUtils();
    this.types = env.getTypeUtils();
    this.introspectedType = new IntrospectedType(attribute.returnType, attribute.isNullable(), env.getTypeUtils(), env.getElementUtils());
    this.matcherDefinition = new MatcherDefinition(attribute, buildMatcher());

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
      return type.getKind().isPrimitive() && !isBoolean() && type.getKind() != TypeKind.CHAR || isSubtypeOf(Number.class);
    }

    public boolean isInteger() {
      return type.getKind() == TypeKind.INT || isSubtypeOf(Integer.class);
    }

    public boolean isLong() {
      return type.getKind() == TypeKind.LONG || isSubtypeOf(Long.class);
    }

    public boolean isDouble() {
      return type.getKind() == TypeKind.DOUBLE || isSubtypeOf(Double.class);
    }

    public boolean isBigInteger() {
      return isSubtypeOf(BigInteger.class);
    }

    public boolean isBigDecimal() {
      return isSubtypeOf(BigDecimal.class);
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
      return isString() || isComparable() || isBoolean() || isNumber();
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

    public boolean isMatcher() {
      return isSubtypeOf(elements.getTypeElement("org.immutables.criteria.matcher.Matcher"));
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

  /**
   * Criteria templates are always generated as top-level class (separate file).
   * Construct criteria name from {@linkplain TypeMirror}
   *
   * @return fully qualified criteria (template) class name
   */
  private static String topLevelCriteriaClassName(TypeMirror type) {
    DeclaredType declaredType = MoreTypes.asDeclared(type);
    Element element = declaredType.asElement();
    do {
      element = element.getEnclosingElement();
    } while (element.getKind() != ElementKind.PACKAGE);

    String packagePrefix = "";
    if (!element.getSimpleName().contentEquals("")) {
      packagePrefix = MoreElements.asPackage(element).getQualifiedName() + ".";
    }

    // package name + type name + "CriteriaTemplate"
    return packagePrefix + declaredType.asElement().getSimpleName().toString() + "CriteriaTemplate";
  }

  private Type.Parameterized matcherType(IntrospectedType introspected) {
    final TypeMirror type = introspected.type;
    String name;

    if (introspected.useOptional()) {
      IntrospectedType param = introspected.optionalParameter();
      // use optional intersection-types ?
      if (param.isString()) {
        name = "org.immutables.criteria.matcher.OptionalStringMatcher.Template";
      } else if (param.isBoolean()) {
        name = "org.immutables.criteria.matcher.OptionalBooleanMatcher.Template";
      } else if (param.isNumber()) {
        if (param.isInteger()) {
          name = "org.immutables.criteria.matcher.OptionalIntegerMatcher.Template";
        } else if (param.isLong()) {
          name = "org.immutables.criteria.matcher.OptionalLongMatcher.Template";
        } else if (param.isDouble()) {
          name = "org.immutables.criteria.matcher.OptionalDoubleMatcher.Template";
        } else if (param.isBigInteger()) {
          name = "org.immutables.criteria.matcher.OptionalBigIntegerMatcher.Template";
        } else if (param.isBigDecimal()) {
          name = "org.immutables.criteria.matcher.OptionalBigDecimalMatcher.Template";
        } else {
          // generic number
          name = "org.immutables.criteria.matcher.OptionalNumberMatcher.Template";
        }
      } else if (param.isComparable()) {
        name = "org.immutables.criteria.matcher.OptionalComparableMatcher.Template";
      } else {
        name = "org.immutables.criteria.matcher.OptionalMatcher.Template";
      }
    } else if (introspected.hasCriteria()) {
      name = topLevelCriteriaClassName(type);
    } else if (introspected.isBoolean()) {
      name = "org.immutables.criteria.matcher.BooleanMatcher.Template";
    } else if (introspected.isNumber()) {
      if (introspected.isInteger()) {
        name = "org.immutables.criteria.matcher.IntegerMatcher.Template";
      } else if (introspected.isLong()) {
        name = "org.immutables.criteria.matcher.LongMatcher.Template";
      } else if (introspected.isDouble()) {
        name = "org.immutables.criteria.matcher.DoubleMatcher.Template";
      } else if (introspected.isBigInteger()) {
        name = "org.immutables.criteria.matcher.BigIntegerMatcher.Template";
      } else if (introspected.isBigDecimal()) {
        name = "org.immutables.criteria.matcher.BigDecimalMatcher.Template";
      } else {
        // generic number
        name = "org.immutables.criteria.matcher.NumberMatcher.Template";
      }
    } else if (introspected.isString()) {
      name = "org.immutables.criteria.matcher.StringMatcher.Template";
    } else if (introspected.isIterable() || introspected.isArray()) {
      name = "org.immutables.criteria.matcher.IterableMatcher.Template";
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


  private static class CreatorVisitor implements Type.Visitor<MatcherDef> {
    private final Types types;
    private final Elements elements;
    private final ValueAttribute attribute;

    private CreatorVisitor(Types types, Elements elements, ValueAttribute attribute) {
      this.types = types;
      this.elements = elements;
      this.attribute = attribute;
    }

    @Override
    public MatcherDef primitive(Type.Primitive primitive) {
      throw new AssertionError();
    }

    @Override
    public MatcherDef reference(Type.Reference reference) {
      return new MatcherDef(reference, creator(reference));
    }

    private String creator(Type.Reference reference) {
      if (isMatcher(reference)) {
        String name = reference.name;
        if (name.endsWith(".Template")) {
          name = name.substring(0, name.lastIndexOf(".Template"));
        }

        if (attribute.hasCriteria() && name.endsWith("Template")) {
          name = name.substring(0, name.lastIndexOf("Template"));
        }

        return name + ".creator()";
      }

      // default (and generic) object matcher
      return "org.immutables.criteria.matcher.ObjectMatcher.creator()";
    }

    @Override
    public MatcherDef parameterized(Type.Parameterized parameterized) {
      for (Type.Nonprimitive arg: parameterized.arguments) {
        if (arg instanceof Type.Parameterized) {
          Type.Parameterized param = (Type.Parameterized) arg;
          if (isMatcher(param.reference)) {
            // single match expected for now
            return new ContainerDef(parameterized.reference, param.accept(this), creator(parameterized.reference));
          }
        }
      }

      return reference(parameterized.reference);
    }

    private boolean isMatcher(Type.Reference reference) {
      // is matcher
      Element element = elements.getTypeElement(reference.name);
      if (element != null && types.isSubtype(element.asType(), elements.getTypeElement(MATCHER).asType())) {
        return true;
      }

      return attribute.hasCriteria();
    }

    @Override
    public MatcherDef variable(Type.Variable variable) {
      throw new AssertionError();
    }

    @Override
    public MatcherDef array(Type.Array array) {
      throw new AssertionError();
    }

    @Override
    public MatcherDef superWildcard(Type.Wildcard.Super wildcard) {
      throw new AssertionError();
    }

    @Override
    public MatcherDef extendsWildcard(Type.Wildcard.Extends wildcard) {
      throw new AssertionError();
    }
  }

  /**
   * Composite matcher for containers like Iterable / Map / Optional / @Nullable
   */
  private static class ContainerDef extends MatcherDef {

    public final MatcherDef element;

    private ContainerDef(Type.Defined type, MatcherDef element, String creator) {
      super(type, creator);
      this.element = Objects.requireNonNull(element, "element");
    }
  }

  private static class MatcherDef {
    public final Type.Defined type;
    public final String creator;

    private MatcherDef(Type.Defined type, String creator) {
      this.type = Objects.requireNonNull(type, "type");
      this.creator = creator;
    }
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
      // resolve P (which is projection type) for Optional
      for (Type.Nonprimitive arg: matcher.arguments) {
        if (arg instanceof Type.Variable && ((Type.Variable) arg).name.equals("P")) {
          // resolve projection type (P) for .Template<R, P>.
          // projection type is identical to attribute type
          // Example .Template<R, Optional<Boolean>>
          resolver = resolver.bind((Type.Variable) arg, factory.reference(type.toString()));
        }
      }

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

      if (matcher.arguments.size() > 2 && ((Type.Variable) matcher.arguments.get(2)).name.equals("V")) {
        // parameter called V is usually value type
        resolver = resolver.bind((Type.Variable) matcher.arguments.get(2), (Type.Nonprimitive) valueType);
      }

      // resolve P (which is projection type) for Array / Iterable
      if (introspected.isArray() || introspected.isIterable()) {
        for (Type.Nonprimitive arg: matcher.arguments) {
          if (arg instanceof Type.Variable && ((Type.Variable) arg).name.equals("P")) {
            // resolve projection type (P) for .Template<R, P>.
            // projection type is identical to attribute type
            // Example .Template<R, Optional<Boolean>>
            resolver = resolver.bind((Type.Variable) arg, factory.reference(type.toString()));
          }
        }
      }

      return (Type.Parameterized) matcher.accept(resolver);
    }

    return matcher;
  }


  public MatcherDefinition matcher() {
    return matcherDefinition;
  }
  
  public static class MatcherDefinition {
    private final ValueAttribute attribute;
    private final Type.Parameterized matcherType;
    private final MatcherDef def;

    private MatcherDefinition(ValueAttribute attribute, Type.Parameterized matcherType) {
      this.attribute = attribute;
      this.matcherType = Preconditions.checkNotNull(matcherType, "type");
      ProcessingEnvironment env = attribute.containingType.constitution.protoclass().environment().processing();
      this.def = matcherType.accept(new CreatorVisitor(env.getTypeUtils(), env.getElementUtils(), attribute));
    }

    public Type.Parameterized matcherType() {
      return this.matcherType;
    }

    public String creator() {

      final String first = def.creator;
      final String second = def instanceof ContainerDef ? ((ContainerDef) def).element.creator : first;

      final String withPath = String.format("context.newChild(%s.class, \"%s\", %s)", attribute.containingType.typeDocument().toString(),
              attribute.originalElement().getSimpleName().toString(), second);

      return String.format("%s.create(%s)", first, withPath);
    }

  }
}
