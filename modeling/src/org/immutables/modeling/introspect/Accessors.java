package org.immutables.modeling.introspect;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
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
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import org.immutables.modeling.introspect.Facets.FacetResolver;

public final class Accessors extends Introspection {
  private static final String OPTIONAL_TYPE_SIMPLE_NAME = "Optional";

  public final TypeMirror iterableType;
  public final TypeElement iterableElement;

  Accessors(ProcessingEnvironment environment) {
    super(environment);
    this.iterableElement = elements.getTypeElement(Iterable.class.getName());
    this.iterableType = iterableElement.asType();
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
    collectAccesors(type, accesors);
    return ImmutableMap.<String, Accessor>builder()
        .putAll(accesors)
        .build();
  }

  private void collectAccesors(TypeElement type, Map<String, Accessor> accesors) {
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

    protected BoundAccess(TypeMirror type) {
      this.type = boxed(type);
      this.containedType = boxed(inferContainedType(type));
    }

    private TypeMirror boxed(TypeMirror type) {
      // types.boxedClass fails on some compiler implementations
      if (type == null || !(type instanceof PrimitiveType)) {
        return type;
      }
      Class<?> boxedClass;
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
      default:
        boxedClass = Void.class;
      }
      return elements.getTypeElement(boxedClass.getName()).asType();
    }

    public boolean isContainer() {
      return containedType != null;
    }

    @Nullable
    private TypeMirror inferContainedType(TypeMirror type) {
      if (type instanceof DeclaredType) {
        DeclaredType declaredType = (DeclaredType) type;
        if (isIterableType(type) || isOptionalType(declaredType)) {
          return Iterables.getOnlyElement(declaredType.getTypeArguments());
        }
      }
      if (type instanceof ArrayType) {
        return ((ArrayType) type).getComponentType();
      }
      return null;
    }

    private boolean isIterableType(TypeMirror type) {
      return types.isSubtype(types.erasure(type), iterableType);
    }

    private boolean isOptionalType(DeclaredType parametrizedType) {
      return parametrizedType.asElement().getSimpleName().contentEquals(OPTIONAL_TYPE_SIMPLE_NAME)
          && parametrizedType.getTypeArguments().size() == 1;
    }
  }

  public final class LocalAccess extends BoundAccess {
    public final String name;

    LocalAccess(String name, TypeMirror type) {
      super(type);
      this.name = name;
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
      super(type);
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

  public Binder binder(FacetResolver facets) {
    return new Binder(facets);
  }

  public final class Binder {
    private final FacetResolver facets;

    Binder(FacetResolver facets) {
      this.facets = facets;
    }

    public BoundAccessor bind(TypeMirror targetType, String attribute) {
      @Nullable
      Accessor accessor = definedBy(targetType).get(attribute);

      if (accessor != null) {
        return accessor.bind(targetType);
      }

      for (TypeMirror facet : facets.resolveFor(targetType)) {
        accessor = definedBy(facet).get(attribute);
        if (accessor != null) {
          return accessor.bind(facet);
        }
      }

      throw new UnresolvableAccessorException(
          targetType,
          attribute,
          collectAlternatives(targetType));
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

      for (TypeMirror facet : facets.resolveFor(targetType)) {
        builder.addAll(definedBy(facet).values());
      }

      return builder.build();
    }
  }

  public static class UnresolvableAccessorException extends RuntimeException {
    public final TypeMirror targetType;
    public final String attribute;
    public final ImmutableList<Accessor> alternatives;

    public UnresolvableAccessorException(
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
