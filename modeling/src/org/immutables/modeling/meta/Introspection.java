package org.immutables.modeling.meta;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import java.util.Map;
import javax.annotation.Nullable;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;

public final class Introspection {
  private final Elements elements;
  private final Types types;
  private final TypeMirror iterableType;

  Introspection(ProcessingEnvironment environment) {
    this.elements = environment.getElementUtils();
    this.types = environment.getTypeUtils();
    this.iterableType = elements.getTypeElement(Iterable.class.getName()).asType();
  }

  private final Cache<String, ImmutableMap<String, Accessor>> accessorsDefined =
      CacheBuilder.newBuilder()
          .concurrencyLevel(1)
          .<String, ImmutableMap<String, Accessor>>build(
              new CacheLoader<String, ImmutableMap<String, Accessor>>() {
                @Override
                public ImmutableMap<String, Accessor> load(String key) throws Exception {
                  return accessorsOf(elements.getTypeElement(key));
                }
              });

  private ImmutableMap<String, Accessor> accessorsOf(@Nullable TypeElement type) {
    if (type == null) {
      return ImmutableMap.of();
    }

    Map<String, Accessor> accesors = Maps.<String, Accessor>newHashMap();
    collectAccesors(type, accesors);

    return ImmutableMap.<String, Accessor>builder()
        .putAll(accesors)
        .build();
  }

  private void collectAccesors(TypeElement type, Map<String, Accessor> accesors) {
    for (Element member : elements.getAllMembers(type)) {
      if (member.getKind() == ElementKind.METHOD) {
        ExecutableElement method = (ExecutableElement) member;
        if (isAccessor(method)) {
          Accessor accessor = new Accessor(method);
          accesors.put(accessor.getName(), accessor);
        }
      }
    }
  }

  private boolean isAccessor(ExecutableElement method) {
    return !method.getModifiers().contains(Modifier.STATIC)
        && method.getModifiers().contains(Modifier.PUBLIC)
        && method.getParameters().isEmpty()
        && method.getThrownTypes().isEmpty()
        && method.getTypeParameters().isEmpty();
  }

  public class Accessor {
    private final ExecutableElement accessor;

    public Accessor(ExecutableElement accessor) {
      this.accessor = accessor;
    }

    public String getName() {
      return accessor.getSimpleName().toString();
    }

    public boolean isContainerType() {
      TypeMirror returnType = accessor.getReturnType();
      return types.isSubtype(types.erasure(returnType), iterableType);
    }

    public TypeMirror getProvidingType() {
      return accessor.getEnclosingElement().asType();
    }

    public TypeMirror getType() {
      return accessor.getReturnType();
    }

    public TypeMirror getContainedType() {
      return Iterables.getOnlyElement(((DeclaredType) accessor.getReturnType()).getTypeArguments());
    }
  }
}
