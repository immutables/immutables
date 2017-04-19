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
package org.immutables.metainf.processor;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;
import org.immutables.generator.AbstractTemplate;
import org.immutables.generator.AnnotationMirrors;
import org.immutables.generator.Generator;
import org.immutables.generator.TypeHierarchyCollector;
import org.immutables.metainf.Metainf;

@Generator.Template
class Metaservices extends AbstractTemplate {

  ListMultimap<String, String> allMetaservices() {
    ImmutableListMultimap.Builder<String, String> builder = ImmutableListMultimap.builder();

    for (Element element : round().getElementsAnnotatedWith(Metainf.Service.class)) {
      @Nullable TypeElement typeElement = validated(element);
      if (typeElement == null) {
        continue;
      }
      Set<String> interfaceNames = extractServiceInterfaceNames(typeElement);
      String binaryName = processing().getElementUtils().getBinaryName(typeElement).toString();
      builder.putAll(binaryName, interfaceNames);
    }

    return builder.build();
  }

  private Set<String> extractServiceInterfaceNames(TypeElement typeElement) {
    ImmutableList<TypeMirror> typesMirrors =
        AnnotationMirrors.getTypesFromMirrors(
            Metainf.Service.class.getCanonicalName(), "value", typeElement.getAnnotationMirrors());

    if (typesMirrors.isEmpty()) {
      return useIntrospectedInterfacesForServices(typeElement);
    }

    return useProvidedTypesForServices(typeElement, typesMirrors);
  }

  private Set<String> useIntrospectedInterfacesForServices(TypeElement typeElement) {
    TypeHierarchyCollector typeHierarchyCollector = new TypeHierarchyCollector();
    typeHierarchyCollector.collectFrom(typeElement.asType());
    return typeHierarchyCollector.implementedInterfaceNames();
  }

  private Set<String> useProvidedTypesForServices(TypeElement typeElement, ImmutableList<TypeMirror> typesMirrors) {
    List<String> wrongTypes = Lists.newArrayList();
    List<String> types = Lists.newArrayList();
    for (TypeMirror typeMirror : typesMirrors) {
      if (typeMirror.getKind() != TypeKind.DECLARED
          || !processing().getTypeUtils().isAssignable(typeElement.asType(), typeMirror)) {
        wrongTypes.add(typeMirror.toString());
      } else {
        types.add(typeMirror.toString());
      }
    }

    if (!wrongTypes.isEmpty()) {
      processing().getMessager().printMessage(
          Diagnostic.Kind.ERROR,
          "@Metainf.Service(value = {...}) contains types that are not implemented by "
              + typeElement.getSimpleName()
              + ": " + wrongTypes,
          typeElement,
          AnnotationMirrors.findAnnotation(typeElement.getAnnotationMirrors(), Metainf.Service.class));
    }

    return FluentIterable.from(types).toSet();
  }

  @Nullable
  private TypeElement validated(Element element) {
    Element enclosingElement = element.getEnclosingElement();

    if (element.getKind() == ElementKind.CLASS
        && element.getModifiers().contains(Modifier.PUBLIC)
        && enclosingElement != null) {

      if (enclosingElement.getKind() == ElementKind.PACKAGE
          && !element.getModifiers().contains(Modifier.ABSTRACT)
          && !((PackageElement) enclosingElement).isUnnamed()) {
        return (TypeElement) element;
      }

      if (enclosingElement.getKind() == ElementKind.CLASS
         && element.getModifiers().contains(Modifier.STATIC)) {
          return (TypeElement) element;
      }
    }

    processing().getMessager()
        .printMessage(Diagnostic.Kind.ERROR,
            "Element annotated with @Metainf.Service annotation should be public top-level non-abstract or static class in a package",
            element);

    return null;
  }
}
