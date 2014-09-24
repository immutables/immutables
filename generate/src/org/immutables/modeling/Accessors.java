package org.immutables.modeling;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
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
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;

public final class Accessors extends Introspection {

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
          .<String, ImmutableMap<String, Accessor>>build(
              new CacheLoader<String, ImmutableMap<String, Accessor>>() {
                @Override
                public ImmutableMap<String, Accessor> load(String key) throws Exception {
                  return extractFrom(elements.getTypeElement(key));
                }
              });

  ImmutableMap<String, Accessor> definedBy(TypeMirror type) {
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
      if (isAccessible(method) || isSimpleAccessor(method)) {
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
        && method.getTypeParameters().isEmpty();
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
  }

  public abstract class BoundAccess {
    public final TypeMirror type;
    @Nullable
    public final TypeMirror containedType;

    protected BoundAccess(TypeMirror type) {
      this.type = type;
      this.containedType = inferContainedType(type);
    }

    public boolean isContainer() {
      return containedType != null;
    }

    @Nullable
    private TypeMirror inferContainedType(TypeMirror type) {
      if (type instanceof DeclaredType) {
        if (types.isSubtype(types.erasure(type), iterableType)) {
          return Iterables.getOnlyElement(((DeclaredType) type).getTypeArguments());
        }
      }
      return null;
    }
  }

  public final class LocalAccess extends BoundAccess {
    public final String name;

    LocalAccess(String name, TypeMirror type) {
      super(type);
      this.name = name;
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
  }

  public LocalAccess local(String value, TypeMirror requiredVar) {
    return new LocalAccess(value, requiredVar);
  }
}
