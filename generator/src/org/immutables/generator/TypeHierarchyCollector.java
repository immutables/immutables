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

import com.google.common.base.Function;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import java.util.List;
import java.util.Set;
import javax.annotation.concurrent.NotThreadSafe;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import static com.google.common.base.Preconditions.*;

@NotThreadSafe
public final class TypeHierarchyCollector {
  private final List<DeclaredType> extendedClasses = Lists.newArrayList();
  private final Set<DeclaredType> implementedInterfaces = Sets.newLinkedHashSet();

  public void collectFrom(TypeMirror typeMirror) {
    if (typeMirror.getKind() == TypeKind.DECLARED) {
      collectHierarchyMirrors(toDeclaredType(typeMirror), extendedClasses, implementedInterfaces);
    }
  }

  private DeclaredType toDeclaredType(TypeMirror typeMirror) {
    checkArgument(typeMirror.getKind() == TypeKind.DECLARED);
    return (DeclaredType) typeMirror;
  }

  public ImmutableList<DeclaredType> extendedClasses() {
    return ImmutableList.copyOf(extendedClasses);
  }

  public ImmutableSet<DeclaredType> implementedInterfaces() {
    return ImmutableSet.copyOf(implementedInterfaces);
  }

  public ImmutableList<String> extendedClassNames() {
    return FluentIterable.from(extendedClasses)
        .transform(ToNameOfTypeElement.FUNCTION)
        .toList();
  }

  public ImmutableSet<String> implementedInterfaceNames() {
    return FluentIterable.from(implementedInterfaces)
        .transform(ToNameOfTypeElement.FUNCTION)
        .toSet();
  }

  private void collectHierarchyMirrors(
      DeclaredType topClass,
      List<DeclaredType> extendedClasses,
      Set<DeclaredType> implementedInterfaces) {
    if (topClass.getKind() != TypeKind.DECLARED
        || topClass.toString().equals(Object.class.getName())) {
      return;
    }
    collectInterfacesMirrors(topClass, implementedInterfaces);

    TypeElement e = toTypeElement(topClass);
    TypeMirror superclassMirror = e.getSuperclass();
    if (superclassMirror.getKind() != TypeKind.NONE) {
      DeclaredType superclass = toDeclaredType(superclassMirror);
      extendedClasses.add(superclass);
      collectHierarchyMirrors(superclass, extendedClasses, implementedInterfaces);
    }

    for (TypeMirror typeMirror : e.getInterfaces()) {
      if (typeMirror.getKind() == TypeKind.DECLARED) {
        collectInterfacesMirrors(toDeclaredType(typeMirror), implementedInterfaces);
      }
    }
  }

  private void collectInterfacesMirrors(
      DeclaredType topClass,
      Set<DeclaredType> implementedInterfaces) {
    TypeElement e = toTypeElement(topClass);

    if (e.getKind().isInterface()) {
      implementedInterfaces.add(topClass);
      for (TypeMirror typeMirror : e.getInterfaces()) {
        if (typeMirror.getKind() == TypeKind.DECLARED) {
          collectInterfacesMirrors(toDeclaredType(typeMirror), implementedInterfaces);
        }
      }
    }
  }

  private TypeElement toTypeElement(DeclaredType input) {
    return ToDeclaredTypeElement.FUNCTION.apply(input);
  }

  private enum ToDeclaredTypeElement implements Function<DeclaredType, TypeElement> {
    FUNCTION;
    @Override
    public TypeElement apply(DeclaredType input) {
      return (TypeElement) input.asElement();
    }
  }

  private enum ToNameOfTypeElement implements Function<DeclaredType, String> {
    FUNCTION;
    @Override
    public String apply(DeclaredType input) {
      return ToDeclaredTypeElement.FUNCTION.apply(input).getQualifiedName().toString();
    }
  }
}
