package org.immutables.modeling;

import com.google.common.base.Optional;
import javax.annotation.Nullable;
import javax.lang.model.type.TypeMirror;
import org.immutables.modeling.Accessors.Accessor;
import org.immutables.modeling.Facets.FacetResolver;

public final class Binder {

  private final Accessors accessors;
  private final FacetResolver facets;

  public Binder(Accessors accessors, FacetResolver facets) {
    this.accessors = accessors;
    this.facets = facets;
  }

  public Optional<Binding> tryBind(TypeMirror targetType, String attribute) {
    @Nullable
    Accessor<?> accessor = accessors.definedBy(targetType).get(attribute);
    if (accessor != null) {
      return Optional.of(new Binding(targetType, accessor, null));
    }

    for (TypeMirror facet : facets.resolveFor(targetType)) {
      accessor = accessors.definedBy(facet).get(attribute);
      if (accessor != null) {
        return Optional.of(new Binding(targetType, accessor, facet));
      }
    }

    return Optional.absent();
  }

  public static class Binding {
    public final TypeMirror target;
    @Nullable
    public final TypeMirror facet;
    public final Accessor<?> accessor;

    public Binding(TypeMirror target, Accessor<?> accessor, @Nullable TypeMirror facet) {
      this.target = target;
      this.accessor = accessor;
      this.facet = facet;
    }
  }
}
