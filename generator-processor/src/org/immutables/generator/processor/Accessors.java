/*
   Copyright 2014 Immutables Authors and Contributors

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
package org.immutables.generator.processor;

import com.google.common.base.CaseFormat;
import com.google.common.base.MoreObjects;
import com.google.common.base.Optional;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.type.WildcardType;
import javax.lang.model.util.ElementFilter;
import org.immutables.generator.SourceOrdering;
import org.immutables.generator.SourceOrdering.AccessorProvider;
import org.immutables.generator.Templates;

public final class Accessors extends Introspection {
  private static final String OPTIONAL_TYPE_SIMPLE_NAME = Optional.class.getSimpleName();

  public final TypeMirror iterableTypeErasure;
  public final TypeElement iterableElement;
  public final TypeMirror invokableType;
  public final TypeMirror iterationType;
  public final TypeMirror objectType;

  Accessors(ProcessingEnvironment environment) {
    super(environment);
    this.iterableElement = elements.getTypeElement(Iterable.class.getName());
    this.iterableTypeErasure = types.erasure(iterableElement.asType());
    this.invokableType = elements.getTypeElement(Templates.Invokable.class.getCanonicalName()).asType();
    this.iterationType = elements.getTypeElement(Templates.Iteration.class.getCanonicalName()).asType();
    this.objectType = elements.getTypeElement(Object.class.getCanonicalName()).asType();
  }

  public TypeMirror wrapIterable(TypeMirror typeMirror) {
    return types.getDeclaredType(iterableElement, typeMirror);
  }

  private final Cache<String, ImmutableMap<String, Accessor>> accessorsDefined =
      new Cache<String, ImmutableMap<String, Accessor>>() {
        @Override
        public ImmutableMap<String, Accessor> load(String key) throws Exception {
          return extractFrom(elements.getTypeElement(key));
        }
      };

  ImmutableMap<String, Accessor> definedBy(TypeMirror type) {
    if (type.getKind() == TypeKind.DECLARED) {
      return accessorsDefined.get(toName(type));
    }
    return ImmutableMap.of();
  }

  private ImmutableMap<String, Accessor> extractFrom(@Nullable TypeElement type) {
    if (type == null) {
      return ImmutableMap.of();
    }
    Map<String, Accessor> accesors = Maps.newHashMap();
    collectAccessors(type, accesors);

    Optional<TypeElement> implementationSubclass = getImplementationSubclass(type);
    if (implementationSubclass.isPresent()) {
      collectAccessors(implementationSubclass.get(), accesors);
    }

    return ImmutableMap.copyOf(accesors);
  }

  private Optional<TypeElement> getImplementationSubclass(TypeElement type) {
    return Optional.fromNullable(
        elements.getTypeElement(
            GeneratedTypes.getQualifiedName(elements, type)));
  }

  private void collectAccessors(TypeElement type, Map<String, Accessor> accesors) {
    List<? extends Element> allMembers = elements.getAllMembers(type);
    for (VariableElement field : ElementFilter.fieldsIn(allMembers)) {
      if (isAccessible(field)) {
        Accessor accessor = new Accessor(field);
        accesors.put(accessor.name, accessor);
      }
    }
    // toString, hashCode from Object
    for (ExecutableElement method : ElementFilter.methodsIn(allMembers)) {
      TypeElement definingType = (TypeElement) method.getEnclosingElement();
      if (definingType.getQualifiedName().contentEquals(Object.class.getCanonicalName())
          || isSimpleAccessor(method) && isAccessible(method)) {
        Accessor accessor = new Accessor(method);
        accesors.put(accessor.name, accessor);
      }
    }

    // For other accessors we use shared utility
    AccessorProvider provider = SourceOrdering.getAllAccessorsProvider(elements, types, type);
    for (ExecutableElement method : provider.get()) {
      // this should be already checked, but we check for completeness
      if (isSimpleAccessor(method) && isAccessible(method)) {
        Accessor accessor = new Accessor(method);
        accesors.put(accessor.name, accessor);
      }
    }
  }

  private boolean isAccessible(Element element) {
    return !element.getModifiers().contains(Modifier.STATIC)
        && !element.getModifiers().contains(Modifier.PRIVATE);
  }

  private boolean isSimpleAccessor(ExecutableElement method) {
    return method.getParameters().isEmpty()
        && method.getThrownTypes().isEmpty()
        && method.getTypeParameters().isEmpty()
        && method.getReturnType().getKind() != TypeKind.VOID;
  }

  public final class Accessor {
    public final Element element;
    public final String name;
    public final boolean callable;

    Accessor(Element element) {
      this.element = element;
      this.name = element.getSimpleName().toString();
      this.callable = element.getKind() == ElementKind.METHOD;
    }

    final BoundAccessor bind(TypeMirror target) {
      // asMemberOf wrongly implemented in ECJ,
      // we use it for fields only in Javac
      // but we expect it is already resolved
      TypeMirror type = !inEclipseCompiler
          ? types.asMemberOf((DeclaredType) target, element)
          : element.asType();

      if (type instanceof ExecutableType) {
        type = ((ExecutableType) type).getReturnType();
      }
      return new BoundAccessor(this, target, type);
    }

    @Override
    public String toString() {
      return element.getEnclosingElement().getSimpleName() + "." + name + "" + (callable ? "()" : "");
    }
  }

  public abstract class BoundAccess {
    public final TypeMirror type;
    @Nullable
    public final TypeMirror containedType;
    public final String name;
    public final boolean invokable;
    public final boolean callable;
    public final boolean boxed;

    protected BoundAccess(TypeMirror type, String name, boolean callable) {
      this.name = name;
      this.callable = callable;
      this.type = boxed(type);
      this.boxed = this.type != type;
      this.containedType = boxed(inferContainedType(type));
      this.invokable = types.isAssignable(type, invokableType);
    }

    private TypeMirror boxed(TypeMirror type) {
      // types.boxedClass fails on some compiler implementations
      if (type == null) {
        return type;
      }
      Class<?> boxedClass = null;
      switch (type.getKind()) {
      case BOOLEAN:
        boxedClass = Boolean.class;
        break;
      case SHORT:
        boxedClass = Short.class;
        break;
      case INT:
        boxedClass = Integer.class;
        break;
      case LONG:
        boxedClass = Long.class;
        break;
      case FLOAT:
        boxedClass = Float.class;
        break;
      case DOUBLE:
        boxedClass = Double.class;
        break;
      case CHAR:
        boxedClass = Character.class;
        break;
      case BYTE:
        boxedClass = Byte.class;
        break;
      case VOID:
        boxedClass = Void.class;
        break;
      default:
      }
      return boxedClass == null
          ? type
          : elements.getTypeElement(boxedClass.getName()).asType();
    }

    public boolean isContainer() {
      return containedType != null;
    }

    @Nullable
    private TypeMirror inferContainedType(TypeMirror type) {
      if (type.getKind() == TypeKind.DECLARED) {
        DeclaredType declaredType = (DeclaredType) type;
        if (isIterableType(declaredType) || isOptionalType(declaredType)) {
          // TBD wrong logic to unpack, need to create super utility for introspecting type
          // hierarchy. Need to be fixed.
          List<? extends TypeMirror> typeArguments = declaredType.getTypeArguments();
          return typeArguments.size() == 1
              ? upperBound(typeArguments.get(0))
              : objectType;
        }
      }
      if (type instanceof ArrayType) {
        return upperBound(((ArrayType) type).getComponentType());
      }
      return null;
    }

    private TypeMirror upperBound(TypeMirror type) {
      switch (type.getKind()) {
      case WILDCARD:
        return MoreObjects.firstNonNull(((WildcardType) type).getExtendsBound(), objectType);
      case TYPEVAR:
        return ((TypeVariable) type).getUpperBound();
      default:
        return type;
      }
    }

    private boolean isIterableType(TypeMirror type) {
      return types.isSubtype(types.erasure(type), iterableTypeErasure);
    }

    private boolean isOptionalType(DeclaredType parametrizedType) {
      return parametrizedType.asElement().getSimpleName().contentEquals(OPTIONAL_TYPE_SIMPLE_NAME)
          && parametrizedType.getTypeArguments().size() == 1;
    }
  }

  public final class LocalAccess extends BoundAccess {
    LocalAccess(String name, TypeMirror type) {
      super(type, name, false);
    }

    @Override
    public String toString() {
      return "" + name + ": " + type;
    }
  }

  public final class BoundAccessor extends BoundAccess {
    public final Accessor accessor;
    public final TypeMirror target;

    BoundAccessor(Accessor accessor, TypeMirror target, TypeMirror type) {
      super(type, accessor.name, accessor.callable);
      this.target = target;
      this.accessor = accessor;
    }

    @Override
    public String toString() {
      return accessor + ": " + type;
    }
  }

  public LocalAccess local(String value, TypeMirror requiredVar) {
    return new LocalAccess(value, requiredVar);
  }

  public Binder binder(ImplicitResolver implicits) {
    return new Binder(implicits);
  }

  public final class Binder {
    private Binder() {}

    Binder(ImplicitResolver implicits) {
      this.implicits = implicits;
    }

    public BoundAccessor bind(TypeMirror targetType, String attribute) {
      @Nullable BoundAccessor accessor = resolveAccessorWithBeanAccessor(targetType, attribute);

      if (accessor != null) {
        return accessor;
      }

      throw new UnresolvedAccessorException(
          targetType,
          attribute,
          collectAlternatives(targetType));
    }

    @Nullable
    private BoundAccessor resolveAccessorWithBeanAccessor(TypeMirror targetType, String attribute) {
      @Nullable BoundAccessor accessor = resolveAccessor(targetType, attribute);
      if (accessor != null) {
        return accessor;
      }

      String capitalizedName = CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_CAMEL, attribute);

      accessor = resolveAccessor(targetType, "get" + capitalizedName);
      if (accessor != null) {
        return accessor;
      }

      accessor = resolveAccessor(targetType, "is" + capitalizedName);
      if (accessor != null) {
        return accessor;
      }

      accessor = resolveAccessor(targetType, "$$" + attribute);
      if (accessor != null) {
        return accessor;
      }

      return accessor;
    }

    @Nullable
    private BoundAccessor resolveAccessor(TypeMirror targetType, String attribute) {
      @Nullable Accessor accessor = definedBy(targetType).get(attribute);

      if (accessor != null) {
        return accessor.bind(targetType);
      }

      return null;
    }

    public BoundAccess bindLocalOrThis(TypeMirror type, String name, Map<String, TypeMirror> locals) {
      TypeMirror typeMirror = locals.get(name);
      if (typeMirror != null) {
        return new LocalAccess(name, typeMirror);
      }
      return bind(type, name);
    }

    private ImmutableList<Accessor> collectAlternatives(TypeMirror targetType) {
      ImmutableList.Builder<Accessor> builder = ImmutableList.builder();

      builder.addAll(definedBy(targetType).values());

      return builder.build();
    }
  }

  public static class UnresolvedAccessorException extends RuntimeException {
    public final TypeMirror targetType;
    public final String attribute;
    public final ImmutableList<Accessor> alternatives;

    public UnresolvedAccessorException(
        TypeMirror targetType,
        String attribute,
        ImmutableList<Accessor> alternatives) {
      this.targetType = targetType;
      this.attribute = attribute;
      this.alternatives = alternatives;
    }

    @Override
    public String getMessage() {
      return "Unresolvable: " + targetType + "." + attribute + "\n\tAlternatives: " + alternatives;
    }
  }

  // Do not use guava cache to slim down minimized jar
  @NotThreadSafe
  private static abstract class Cache<K, V> {
    private final Map<K, V> map = Maps.newHashMap();

    protected abstract V load(K key) throws Exception;

    final V get(K key) {
      @Nullable V value = map.get(key);
      if (value == null) {
        try {
          value = load(key);
        } catch (Exception ex) {
          throw Throwables.propagate(ex);
        }
        map.put(key, value);
      }
      return value;
    }
  }
}
