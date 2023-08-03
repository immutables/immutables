/*
   Copyright 2015 Immutables Authors and Contributors

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
package org.immutables.generator;

import com.google.common.base.Verify;
import com.google.common.collect.*;
import java.util.*;
import java.util.stream.Collectors;
import javax.annotation.concurrent.NotThreadSafe;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.type.*;
import static com.google.common.base.Verify.verify;

@NotThreadSafe
public class TypeHierarchyCollector {
  private final List<TypeElement> extendedClasses = Lists.newArrayList();
  private final Set<TypeElement> implementedInterfaces = Sets.newLinkedHashSet();
  private final Set<String> extendedClassNames = Sets.newLinkedHashSet();
  private final Set<String> implementedInterfaceNames = Sets.newLinkedHashSet();
  protected final Set<String> unresolvedYetArguments = Sets.newHashSet();

  /**
   * overridable stringify.
   * @param input the input
   * @param context the context
   * @return the string
   */
  protected String stringify(DeclaredType input, TypevarContext context) {
    return toTypeElement(input).getQualifiedName().toString();
  }

  public final class TypevarContext {
    public final List<String> parameters;
    public final List<String> arguments;

    TypevarContext(TypeElement element, List<? extends TypeMirror> typeArguments) {
      List<? extends TypeParameterElement> typeParameters = element.getTypeParameters();
      if (!typeParameters.isEmpty()) {

        this.arguments = typeArguments.stream()
            .map(TypeMirror::toString)
            .collect(Collectors.toList());

        this.parameters = Lists.newArrayList();
        for (TypeParameterElement p : typeParameters) {
          parameters.add(p.getSimpleName().toString());
        }
        // we allow having no arguments in a string as raw type/unspecified argument scenario
        Verify.verify(arguments.isEmpty() || (parameters.size() == arguments.size()), parameters + " =/> " + arguments);
      } else {
        this.parameters = Collections.emptyList();
        this.arguments = Collections.emptyList();
      }
    }
  }

  public void collectFrom(TypeMirror typeMirror) {
    if (typeMirror.getKind() == TypeKind.DECLARED) {
      collectHierarchyMirrors(typeMirror, typeMirror.toString());
    }
  }

  private DeclaredType toDeclaredType(TypeMirror typeMirror) {
    verify(typeMirror.getKind() == TypeKind.DECLARED || typeMirror.getKind() == TypeKind.ERROR);
    return (DeclaredType) typeMirror;
  }

  public ImmutableList<TypeElement> extendedClasses() {
    return ImmutableList.copyOf(extendedClasses);
  }

  public ImmutableSet<TypeElement> implementedInterfaces() {
    return ImmutableSet.copyOf(implementedInterfaces);
  }
  
  public ImmutableSet<String> unresolvedYetArguments() {
    return ImmutableSet.copyOf(unresolvedYetArguments);
  }

  public ImmutableSet<String> extendedClassNames() {
    return ImmutableSet.copyOf(extendedClassNames);
  }

  public ImmutableSet<String> implementedInterfaceNames() {
    return ImmutableSet.copyOf(implementedInterfaceNames);
  }

  private void collectHierarchyMirrors(TypeMirror typeMirror, String stringRepresentation) {
    if (typeMirror.getKind() != TypeKind.DECLARED
        || typeMirror.toString().equals(Object.class.getName())) {
      return;
    }

    DeclaredType declaredType = toDeclaredType(typeMirror);
    TypeElement e = toTypeElement(declaredType);
    TypevarContext context = new TypevarContext(e, declaredType.getTypeArguments());

    collectInterfacesMirrors(declaredType, context);

    TypeMirror superclassMirror = e.getSuperclass();
    if (superclassMirror.getKind() != TypeKind.NONE) {
      DeclaredType superclass = toDeclaredType(superclassMirror);
      String stringified = stringify(superclass, context);

      if (!stringified.equals(Object.class.getName())) {
        extendedClasses.add(toTypeElement(superclass));
        extendedClassNames.add(stringified);
        collectHierarchyMirrors(superclass, stringified);
      }
    }

    for (TypeMirror m : e.getInterfaces()) {
      collectUnresolvedInterface(m, context);
      collectInterfacesMirrors(m, context);
    }
  }

  private void collectUnresolvedInterface(TypeMirror typeMirror, TypevarContext context) {
    if (typeMirror.getKind() == TypeKind.ERROR) {
      DeclaredType declaredType = toDeclaredType(typeMirror);
      String stringified = stringify(declaredType, context);
      implementedInterfaceNames.add(stringified);
    }
  }

  private void collectInterfacesMirrors(TypeMirror typeMirror, TypevarContext context) {
    if (typeMirror.getKind() != TypeKind.DECLARED) {
      return;
    }
    DeclaredType declaredType = toDeclaredType(typeMirror);
    TypeElement e = toTypeElement(declaredType);

    if (e.getKind().isInterface()) {
      implementedInterfaces.add(e);
      String stringified = stringify(declaredType, context);
      TypevarContext nestedContext = new TypevarContext(e, declaredType.getTypeArguments());
      implementedInterfaceNames.add(stringified);
      for (TypeMirror m : e.getInterfaces()) {
        collectInterfacesMirrors(m, nestedContext);
      }
    }
  }

  private static TypeElement toTypeElement(DeclaredType input) {
    return (TypeElement) input.asElement();
  }
}
