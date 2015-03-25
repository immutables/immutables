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

import org.immutables.generator.AnnotationMirrors;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import java.util.Map;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import org.immutables.generator.Generator;
import org.immutables.generator.Templates;

public final class Imports extends Introspection {
  private final TypeMirror importType;

  public Imports(ProcessingEnvironment environment) {
    super(environment);
    this.importType = elements.getTypeElement(Generator.Import.class.getCanonicalName()).asType();
  }

  public final ImmutableMap<String, TypeMirror> importsIn(TypeElement type) {
    Map<String, TypeMirror> collected = Maps.newHashMap();

    collectSimpleUsages(type, collected);
    collectBuiltins(collected);
    collectPackage(type, collected);
    collectImports(type, collected);
    collectTypedefs(type, collected);

    return ImmutableMap.copyOf(collected);
  }

  private void collectBuiltins(Map<String, TypeMirror> collected) {
    for (TypeKind kind : TypeKind.values()) {
      if (kind.isPrimitive()) {
        TypeElement boxedClass = types.boxedClass(types.getPrimitiveType(kind));
        collected.put(boxedClass.getSimpleName().toString(), boxedClass.asType());
      }
    }

    TypeElement typeElement = elements.getTypeElement(String.class.getCanonicalName());
    collected.put(typeElement.getSimpleName().toString(), typeElement.asType());

    typeElement = elements.getTypeElement(Templates.Invokable.class.getCanonicalName());
    collected.put(typeElement.getSimpleName().toString(), typeElement.asType());
  }

  private void collectPackage(TypeElement type, Map<String, TypeMirror> collected) {
    for (TypeElement samePackageType : ElementFilter.typesIn(elements.getPackageOf(type).getEnclosedElements())) {
      collectIfSimpleType(samePackageType.asType(), collected);
    }
  }

  private void collectTypedefs(TypeElement type, Map<String, TypeMirror> collected) {
    for (VariableElement field : ElementFilter.fieldsIn(elements.getAllMembers(type))) {
      if (field.getAnnotation(Generator.Typedef.class) != null) {
        collected.put(field.getSimpleName().toString(), field.asType());
      }
    }
  }

  private void collectSimpleUsages(TypeElement type, Map<String, TypeMirror> collected) {
    for (ExecutableElement method : ElementFilter.methodsIn(elements.getAllMembers(type))) {
      if (shouldConsideredAsTypeUsage(method)) {
        collectIfSimpleType(method.getReturnType(), collected);
        for (VariableElement parameter : method.getParameters()) {
          collectIfSimpleType(parameter.asType(), collected);
        }
      }
    }
  }

  private boolean shouldConsideredAsTypeUsage(ExecutableElement method) {
    return method.getTypeParameters().isEmpty()
        && !method.getModifiers().contains(Modifier.PRIVATE)
        && !method.getModifiers().contains(Modifier.STATIC);
  }

  private void collectIfSimpleType(TypeMirror type, Map<String, TypeMirror> collected) {
    if (type.getKind() == TypeKind.DECLARED) {
      DeclaredType declared = (DeclaredType) type;
      if (declared.getTypeArguments().isEmpty()) {
        collected.put(declared.asElement().getSimpleName().toString(), declared);
      }
    }
  }

  private void collectImports(TypeElement type, Map<String, TypeMirror> collected) {
    for (TypeMirror typeMirror : extractImports(type)) {
      collected.put(toSimpleName(typeMirror), typeMirror);
    }
  }

  private ImmutableList<TypeMirror> extractImports(TypeElement type) {
    ImmutableList.Builder<TypeMirror> importedTypes = ImmutableList.builder();

    for (TypeElement t = type; t != null; t = (TypeElement) types.asElement(t.getSuperclass())) {
      importedTypes.addAll(extractDeclaredImports(t).reverse());
      importedTypes.addAll(extractDeclaredImports(elements.getPackageOf(type)).reverse());
    }

    return importedTypes.build().reverse();
  }

  private ImmutableList<TypeMirror> extractDeclaredImports(Element element) {
    return AnnotationMirrors.getTypesFromMirrors(
        types,
        importType,
        "value",
        element.getAnnotationMirrors());
  }
}
