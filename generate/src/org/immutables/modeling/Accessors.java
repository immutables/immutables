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
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;

public final class Accessors extends Introspection {

  private final TypeMirror iterableType;

  Accessors(ProcessingEnvironment environment) {
    super(environment);
    this.iterableType = elements.getTypeElement(Iterable.class.getName()).asType();
  }

  private final LoadingCache<String, ImmutableMap<String, Accessor<?>>> accessorsDefined =
      CacheBuilder.newBuilder()
          .concurrencyLevel(1)
          .<String, ImmutableMap<String, Accessor<?>>>build(
              new CacheLoader<String, ImmutableMap<String, Accessor<?>>>() {
                @Override
                public ImmutableMap<String, Accessor<?>> load(String key) throws Exception {
                  return extractFrom(elements.getTypeElement(key));
                }
              });

  public ImmutableMap<String, Accessor<?>> definedBy(TypeMirror type) {
    return accessorsDefined.getUnchecked(toName(type));
  }

  private ImmutableMap<String, Accessor<?>> extractFrom(@Nullable TypeElement type) {
    if (type == null) {
      return ImmutableMap.of();
    }
    Map<String, Accessor<?>> accesors = Maps.newHashMap();
    collectAccesors(type, accesors);
    return ImmutableMap.<String, Accessor<?>>builder()
        .putAll(accesors)
        .build();
  }

  private void collectAccesors(TypeElement type, Map<String, Accessor<?>> accesors) {
    List<? extends Element> members = elements.getAllMembers(type);
    collectMethodAccesors(accesors, members);
    collectFieldAccessors(accesors, members);
  }

  private void collectFieldAccessors(Map<String, ? super ValueAccessor> accesors, List<? extends Element> allMembers) {
    for (VariableElement value : ElementFilter.fieldsIn(allMembers)) {
      if (isAccessible(value)) {
        ValueAccessor accessor = new ValueAccessor(value);
        accesors.put(accessor.getName(), accessor);
      }
    }
  }

  private void collectMethodAccesors(Map<String, ? super MethodAccessor> accesors, List<? extends Element> allMembers) {
    for (ExecutableElement method : ElementFilter.methodsIn(allMembers)) {
      if (isAccessible(method) || isSimpleAccessor(method)) {
        MethodAccessor accessor = new MethodAccessor(method);
        accesors.put(accessor.getName(), accessor);
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

  abstract class Accessor<E extends Element> {
    protected final E element;

    Accessor(E element) {
      this.element = element;
    }

    String getName() {
      return element.getSimpleName().toString();
    }

    abstract String accessSuffix();

    abstract TypeMirror getType();

    final boolean isContainerType() {
      return types.isSubtype(types.erasure(getType()), iterableType);
    }

    final TypeMirror getProvidingType() {
      return element.getEnclosingElement().asType();
    }

    final TypeMirror getContainedType() {
      return Iterables.getOnlyElement(((DeclaredType) getType()).getTypeArguments());
    }
  }

  final class ValueAccessor extends Accessor<VariableElement> {
    public ValueAccessor(VariableElement element) {
      super(element);
    }

    @Override
    String accessSuffix() {
      return "";
    }

    @Override
    public TypeMirror getType() {
      return element.asType();
    }
  }

  final class MethodAccessor extends Accessor<ExecutableElement> {
    public MethodAccessor(ExecutableElement element) {
      super(element);
    }

    @Override
    String accessSuffix() {
      return "()";
    }

    @Override
    public TypeMirror getType() {
      return element.getReturnType();
    }
  }
}
