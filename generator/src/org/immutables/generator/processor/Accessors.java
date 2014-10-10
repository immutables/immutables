package org.immutables.generator.processor;

import com.google.common.base.CaseFormat;
import com.google.common.base.MoreObjects;
import com.google.common.base.Optional;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
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
import org.immutables.generator.Templates;
import org.immutables.generator.processor.Implicits.ImplicitResolver;

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

  private final LoadingCache<String, ImmutableMap<String, Accessor>> accessorsDefined =
      CacheBuilder.newBuilder()
          .concurrencyLevel(1)
          .build(new CacheLoader<String, ImmutableMap<String, Accessor>>() {
            @Override
            public ImmutableMap<String, Accessor> load(String key) throws Exception {
              return extractFrom(elements.getTypeElement(key));
            }
          });

  ImmutableMap<String, Accessor> definedBy(TypeMirror type) {
    if (!(type instanceof DeclaredType)) {
      return ImmutableMap.of();
    }
    return accessorsDefined.getUnchecked(toName(type));
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

    return ImmutableMap.<String, Accessor>builder()
        .putAll(accesors)
        .build();
  }

  private Optional<TypeElement> getImplementationSubclass(TypeElement type) {
    return Optional.fromNullable(
        elements.getTypeElement(
            GeneratedTypes.getQualifiedName(elements, type)));
  }

  private void collectAccessors(TypeElement type, Map<String, Accessor> accesors) {
    List<? extends Element> members = elements.getAllMembers(type);
    collectMethodAccesors(accesors, members);
    collectFieldAccessors(accesors, members);
  }

  private void collectFieldAccessors(Map<String, Accessor> accesors, List<? extends Element> allMembers) {
    for (VariableElement value : ElementFilter.fieldsIn(allMembers)) {
      if (isAccessible(value)) {
        Accessor accessor = new Accessor(value);
        accesors.put(accessor.name, accessor);
      }
    }
  }

  private void collectMethodAccesors(Map<String, Accessor> accesors, List<? extends Element> allMembers) {
    for (ExecutableElement method : ElementFilter.methodsIn(allMembers)) {
      if (isAccessible(method) && isSimpleAccessor(method)) {
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
      TypeMirror type = types.asMemberOf((DeclaredType) target, element);
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
      if (type instanceof DeclaredType) {
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
    private final ImplicitResolver implicits;

    Binder(ImplicitResolver implicits) {
      this.implicits = implicits;
    }

    public BoundAccessor bind(TypeMirror targetType, String attribute) {
      @Nullable
      BoundAccessor accessor = resolveAccessorWithBeanAccessor(targetType, attribute);

      if (accessor != null) {
        return accessor;
      }

      throw new UnresolvedAccessorException(
          targetType,
          attribute,
          collectAlternatives(targetType));
    }

    /** @deprecated To be removed after completion of migration of old templates */
    @Deprecated
    @Nullable
    private BoundAccessor resolveAccessorWithBeanAccessor(TypeMirror targetType, String attribute) {
      @Nullable
      BoundAccessor accessor = resolveAccessor(targetType, attribute);
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

      return accessor;
    }

    @Nullable
    private BoundAccessor resolveAccessor(TypeMirror targetType, String attribute) {
      @Nullable
      Accessor accessor = definedBy(targetType).get(attribute);

      if (accessor != null) {
        return accessor.bind(targetType);
      }

      for (TypeMirror implicit : implicits.resolveFor(targetType)) {
        accessor = definedBy(implicit).get(attribute);
        if (accessor != null) {
          return accessor.bind(implicit);
        }
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

      for (TypeMirror implicit : implicits.resolveFor(targetType)) {
        builder.addAll(definedBy(implicit).values());
      }

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
}
