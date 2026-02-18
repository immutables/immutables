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
import javax.lang.model.element.*;
import javax.lang.model.type.*;
import static com.google.common.base.Verify.verify;

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

    TypevarContext(TypeElement element, String renderedTypeString) {
      List<? extends TypeParameterElement> typeParameters = element.getTypeParameters();
      if (!typeParameters.isEmpty()) {
        this.arguments = SourceTypes.extract(renderedTypeString).getValue();

        this.parameters = Lists.newArrayList();
        for (TypeParameterElement p : typeParameters) {
          parameters.add(p.getSimpleName().toString());
        }
        // we allow having no arguments in a string as raw type/unspecified argument scenario
        Verify.verify(arguments.isEmpty() || (parameters.size() == arguments.size()), parameters + " =/> " + arguments + "\n[\n" + renderedTypeString + "\n]");
      } else {
        this.parameters = Collections.emptyList();
        this.arguments = Collections.emptyList();
      }
    }
  }

  public void collectFrom(TypeMirror typeMirror) {
    if (typeMirror.getKind() == TypeKind.DECLARED) {
      collectHierarchyMirrors(typeMirror, stringify(typeMirror));
    }
  }

  static String stringify(TypeMirror mirror) {
    String string = mirror.toString();
    // -1 + -1 both not found, special symbols for type annotations
    // where @ is normal annotation sign, but : is synthetic insert in some
    // javac versions, accompanying weird Java type_use annotations
    if (string.indexOf('@') + string.indexOf(':') == -2) {
      return string;
    }

    StringBuilder builder = new StringBuilder();
    append(builder, mirror);
    return builder.toString();
  }

  static StringBuilder append(StringBuilder builder, TypeMirror mirror) {
    TypeKind kind = mirror.getKind();
    if (kind.isPrimitive()) {
      List<? extends AnnotationMirror> annotations = mirror.getAnnotationMirrors();
      if (!annotations.isEmpty()) {
        append(builder, annotations);
        builder.append(' ');
      }
      builder.append(kind.name().toLowerCase());
      return builder;
    }

    if (kind == TypeKind.DECLARED) {
      DeclaredType declared = (DeclaredType) mirror;
      TypeElement element = (TypeElement) declared.asElement();
      List<? extends AnnotationMirror> annotations = declared.getAnnotationMirrors();
      if (!annotations.isEmpty()) {
        String qualifiedName = element.getQualifiedName().toString();
        int qualifiedDotIndex = qualifiedName.lastIndexOf('.');

        if (qualifiedDotIndex > 0) {
          // special syntax for qualified name and type annotations:
          // org.package.name.@Annotation @Another SimpleName
          builder.append(qualifiedName, 0, qualifiedDotIndex + 1);
        }
        append(builder, annotations);
        // finishing with the type name itself
        builder.append(' ');
        if (qualifiedDotIndex > 0) {
          builder.append(qualifiedName, qualifiedDotIndex + 1, qualifiedName.length());
        } else {
          // this can be the case for unnamed package (right?)
          builder.append(qualifiedName);
        }
      } else {
        builder.append(element.getQualifiedName());
      }

      List<? extends TypeMirror> arguments = declared.getTypeArguments();
      if (!arguments.isEmpty()) {
        builder.append('<');
        int i = 0;
        for (TypeMirror a : arguments) {
          if (i++ > 0) builder.append(", ");
          // recursive call
          append(builder, a);
        }
        builder.append('>');
      }
      return builder;
    }

    return builder.append(mirror);
  }

  private static StringBuilder append(
      StringBuilder builder,
      List<? extends AnnotationMirror> annotations) {
    int i = 0;
    for (AnnotationMirror annotation : annotations) {
      if (i++ > 0) {
        // not the first annotation, so inserting space to separate
        builder.append(' ');
      }
      AnnotationMirrors.append(builder, annotation);
    }
    return builder;
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
    if (typeMirror.getKind() != TypeKind.DECLARED) return;

    DeclaredType declaredType = toDeclaredType(typeMirror);
    TypeElement e = toTypeElement(declaredType);

    if (e.getQualifiedName().contentEquals(Object.class.getName())) return;

    TypevarContext context = new TypevarContext(e, stringRepresentation);

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
      TypevarContext nestedContext = new TypevarContext(e, stringified);
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
