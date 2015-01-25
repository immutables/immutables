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

@NotThreadSafe
public final class TypeHierarchyCollector {
  private final List<TypeMirror> extendedClasses = Lists.newArrayList();
  private final Set<TypeMirror> implementedInterfaces = Sets.newLinkedHashSet();

  public void collectFrom(TypeMirror typeMirror) {
    if (typeMirror.getKind() == TypeKind.DECLARED) {
      collectHierarchyMirrors(typeMirror, extendedClasses, implementedInterfaces);
    }
  }

  public ImmutableList<TypeMirror> extendedClasses() {
    return ImmutableList.copyOf(extendedClasses);
  }

  public ImmutableSet<TypeMirror> implementedInterfaces() {
    return ImmutableSet.copyOf(implementedInterfaces);
  }

  public ImmutableList<String> extendedClassNames() {
    return FluentIterable.from(extendedClasses)
        .filter(DeclaredType.class)
        .transform(ToNameOfTypeElement.FUNCTION)
        .toList();
  }

  public ImmutableSet<String> implementedInterfaceNames() {
    return FluentIterable.from(implementedInterfaces)
        .filter(DeclaredType.class)
        .transform(ToNameOfTypeElement.FUNCTION)
        .toSet();
  }

  private void collectHierarchyMirrors(
      TypeMirror topClass,
      List<TypeMirror> extendedClasses,
      Set<TypeMirror> implementedInterfaces) {
    if (topClass.getKind() != TypeKind.DECLARED || topClass.toString().equals(Object.class.getName())) {
      return;
    }
    collectInterfacesMirrors(topClass, implementedInterfaces);

    TypeElement e = toTypeElement(topClass);
    TypeMirror superClass = e.getSuperclass();

    extendedClasses.add(superClass);
    collectHierarchyMirrors(superClass, extendedClasses, implementedInterfaces);

    for (TypeMirror typeMirror : e.getInterfaces()) {
      collectInterfacesMirrors(typeMirror, implementedInterfaces);
    }
  }

  private void collectInterfacesMirrors(
      TypeMirror topClass,
      Set<TypeMirror> implementedInterfaces) {
    TypeElement e = toTypeElement(topClass);

    if (e.getKind().isInterface()) {
      implementedInterfaces.add(topClass);
      for (TypeMirror typeMirror : e.getInterfaces()) {
        collectInterfacesMirrors(typeMirror, implementedInterfaces);
      }
    }
  }

  private TypeElement toTypeElement(TypeMirror input) {
    return ToDeclaredTypeElement.FUNCTION.apply(input);
  }

  private enum ToDeclaredTypeElement implements Function<TypeMirror, TypeElement> {
    FUNCTION;
    @Override
    public TypeElement apply(TypeMirror input) {
      return (TypeElement) ((DeclaredType) input).asElement();
    }
  }

  private enum ToNameOfTypeElement implements Function<TypeMirror, String> {
    FUNCTION;
    @Override
    public String apply(TypeMirror input) {
      return ToDeclaredTypeElement.FUNCTION.apply(input).getQualifiedName().toString();
    }
  }
}
