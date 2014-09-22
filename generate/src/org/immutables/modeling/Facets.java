package org.immutables.modeling;

import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.ListMultimap;
import java.util.List;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import static com.google.common.base.Preconditions.*;

public class Facets extends Introspection {

  private final TypeMirror facetTypeErasure;

  Facets(ProcessingEnvironment environment) {
    super(environment);
    this.facetTypeErasure = types.erasure(elements.getTypeElement(Facet.class.getName()).asType());
  }

  public FacetResolver resolverFrom(final Iterable<? extends TypeMirror> imports) {
    return new FacetResolver() {
      ListMultimap<TypeMirror, TypeMirror> mappings = buildFacetMappingFrom(imports);

      @Override
      public List<TypeMirror> resolveFor(TypeMirror typeMirror) {
        return mappings.get(typeMirror);
      }
    };
  }

  private ImmutableListMultimap<TypeMirror, TypeMirror> buildFacetMappingFrom(Iterable<? extends TypeMirror> imports) {
    ImmutableListMultimap.Builder<TypeMirror, TypeMirror> builder = ImmutableListMultimap.builder();

    for (TypeMirror type : imports) {
      if (types.isSubtype(checkDeclaredType(type), facetTypeErasure)) {
        for (TypeMirror superType : types.directSupertypes(type)) {
          if (types.isSubtype(superType, facetTypeErasure)) {
            TypeMirror targetType = getTargetTypeArgument(superType);
            builder.put(targetType, type);
          }
        }
      }
    }

    return builder.build();
  }

  private TypeMirror getTargetTypeArgument(TypeMirror facetTypeSupertype) {
    TypeMirror typeArgument = Iterables.getOnlyElement(checkDeclaredType(facetTypeSupertype).getTypeArguments());
    if (typeArgument instanceof TypeVariable) {
      return ((TypeVariable) typeArgument).getUpperBound();
    }
    return typeArgument;
  }

  public interface FacetResolver {
    List<TypeMirror> resolveFor(TypeMirror typeMirror);
  }

  private DeclaredType checkDeclaredType(TypeMirror type) {
    checkState(type instanceof DeclaredType, "'%s' should have been a declared type", type);
    return (DeclaredType) type;
  }
}
