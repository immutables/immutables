package org.immutables.modeling;

import com.google.common.collect.ImmutableList;
import java.util.Map;
import javax.annotation.Nullable;
import javax.lang.model.type.TypeMirror;
import org.immutables.modeling.Accessors.Accessor;
import org.immutables.modeling.Accessors.BoundAccess;
import org.immutables.modeling.Accessors.BoundAccessor;
import org.immutables.modeling.Facets.FacetResolver;

public final class Binder {

  private final Accessors accessors;
  private final FacetResolver facets;

  public Binder(Accessors accessors, FacetResolver facets) {
    this.accessors = accessors;
    this.facets = facets;
  }

  public BoundAccessor bind(TypeMirror targetType, String attribute)
      throws UnresolvableAccessorException {
    @Nullable
    Accessor accessor = accessors.definedBy(targetType).get(attribute);

    if (accessor != null) {
      return accessor.bind(targetType);
    }

    for (TypeMirror facet : facets.resolveFor(targetType)) {
      accessor = accessors.definedBy(facet).get(attribute);
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
      return accessors.new LocalAccess(name, typeMirror);
    }
    return bind(type, name);
  }

  private ImmutableList<Accessor> collectAlternatives(TypeMirror targetType) {
    ImmutableList.Builder<Accessor> builder = ImmutableList.builder();

    builder.addAll(accessors.definedBy(targetType).values());

    for (TypeMirror facet : facets.resolveFor(targetType)) {
      builder.addAll(accessors.definedBy(facet).values());
    }

    return builder.build();
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
  }
}
