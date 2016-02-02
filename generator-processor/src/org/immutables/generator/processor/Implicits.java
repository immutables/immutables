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

import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.ListMultimap;
import java.util.List;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import org.immutables.generator.Implicit;
import static com.google.common.base.Preconditions.checkState;

public class Implicits extends Introspection {

  private final TypeMirror implicitTypeErasure;

  Implicits(ProcessingEnvironment environment) {
    super(environment);
    this.implicitTypeErasure = types.erasure(elements.getTypeElement(Implicit.class.getName()).asType());
  }

  public ImplicitResolver resolverFrom(final Iterable<? extends TypeMirror> imports) {
    return new ImplicitResolver() {
      ListMultimap<TypeMirror, TypeMirror> mappings = buildImplicitMappingFrom(imports);

      @Override
      public List<TypeMirror> resolveFor(TypeMirror typeMirror) {
        return mappings.get(typeMirror);
      }
    };
  }

  private ImmutableListMultimap<TypeMirror, TypeMirror> buildImplicitMappingFrom(Iterable<? extends TypeMirror> imports) {
    ImmutableListMultimap.Builder<TypeMirror, TypeMirror> builder = ImmutableListMultimap.builder();

    for (TypeMirror type : imports) {
      if (types.isSubtype(checkDeclaredType(type), implicitTypeErasure)) {
        for (TypeMirror superType : types.directSupertypes(type)) {
          if (types.isSubtype(superType, implicitTypeErasure)) {
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

  public interface ImplicitResolver {
    List<TypeMirror> resolveFor(TypeMirror typeMirror);
  }

  private DeclaredType checkDeclaredType(TypeMirror type) {
    checkState(type.getKind() == TypeKind.DECLARED, "'%s' should have been a declared type", type);
    return (DeclaredType) type;
  }
}
