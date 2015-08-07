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
package org.immutables.mirror.processor;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import javax.annotation.Nullable;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.Name;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import javax.tools.Diagnostic;
import org.immutables.generator.AbstractTemplate;
import org.immutables.generator.Generator;
import org.immutables.mirror.Mirror;

@Generator.Template
@Generator.Import({Mirrors.MirrorModel.class, Mirrors.MirrorModel.AttributeModel.class})
class Mirrors extends AbstractTemplate {

  ImmutableList<MirrorModel> allMirrors() {
    ImmutableList.Builder<MirrorModel> builder = ImmutableList.builder();

    for (Element element : round().getElementsAnnotatedWith(Mirror.Annotation.class)) {
      @Nullable TypeElement typeElement = validated(element);
      if (typeElement == null) {
        continue;
      }
      builder.add(new MirrorModel(typeElement));
    }

    return builder.build();
  }

  class MirrorModel {
    final TypeElement element;
    final ImmutableList<AttributeModel> attributes;
    final String name;
    final String $$package;
    final String qualifiedName;

    MirrorModel(TypeElement element) {
      this.element = element;

      ImmutableList.Builder<AttributeModel> builder = ImmutableList.builder();
      for (ExecutableElement attribute : ElementFilter.methodsIn(element.getEnclosedElements())) {
        builder.add(new AttributeModel(attribute));
      }
      this.attributes = builder.build();
      this.name = element.getSimpleName().toString();

      PackageElement packageElement;
      for (Element e = element;; e = e.getEnclosingElement()) {
        if (e.getKind() == ElementKind.PACKAGE) {
          packageElement = (PackageElement) e;
          break;
        }
      }

      this.$$package = packageElement.getQualifiedName().toString();
      this.qualifiedName = element.getAnnotation(Mirror.Annotation.class).value();
    }

    String simpleName() {
      return Iterables.getLast(Splitter.on('.').splitToList(qualifiedName));
    }

    class AttributeModel {
      final ExecutableElement element;
      final String name;
      final boolean isArray;
      final TypeMirror type;
      final AttributeTypeKind kind;
      final String suffix;
      @Nullable
      final MirrorModel mirrorModel;

      AttributeModel(ExecutableElement element) {
        this.element = element;
        this.name = element.getSimpleName().toString();

        TypeMirror type = element.getReturnType();

        this.isArray = type.getKind() == TypeKind.ARRAY;
        this.type = this.isArray ? ((ArrayType) type).getComponentType() : type;
        this.kind = AttributeTypeKind.from(this.type);

        this.suffix = this.isArray ? "[]" : "";
        this.mirrorModel = getMirrorModelIfAnnotation();
      }

      @Nullable
      private MirrorModel getMirrorModelIfAnnotation() {
        if (this.kind == AttributeTypeKind.ANNOTATION) {
          TypeElement element = toElement(this.type);
          if (element.getAnnotation(Mirror.Annotation.class) != null) {
            return new MirrorModel(element);
          }
        }
        return null;
      }

      boolean isBoolean() {
        return type.getKind() == TypeKind.BOOLEAN;
      }

      boolean isFloat() {
        return type.getKind() == TypeKind.FLOAT;
      }

      boolean isDouble() {
        return type.getKind() == TypeKind.DOUBLE;
      }
    }
  }

  enum AttributeTypeKind {
    PRIMITIVE, STRING, ENUM, ANNOTATION, TYPE;

    boolean isPrimitive() {
      return this == PRIMITIVE;
    }

    boolean isString() {
      return this == STRING;
    }

    boolean isEnum() {
      return this == ENUM;
    }

    boolean isAnnotation() {
      return this == ANNOTATION;
    }

    boolean isType() {
      return this == TYPE;
    }

    static AttributeTypeKind from(TypeMirror type) {
      if (type.getKind() == TypeKind.DECLARED) {
        TypeElement typeElement = toElement(type);
        if (typeElement.getKind() == ElementKind.ENUM) {
          return ENUM;
        }
        if (typeElement.getKind() == ElementKind.ANNOTATION_TYPE) {
          return ANNOTATION;
        }
        Name qualifiedName = typeElement.getQualifiedName();
        if (qualifiedName.contentEquals(Class.class.getName())) {
          return TYPE;
        }
        if (qualifiedName.contentEquals(String.class.getName())) {
          return STRING;
        }
      } else if (type.getKind().isPrimitive()) {
        return PRIMITIVE;
      }
      throw new AssertionError();
    }
  }

  private static TypeElement toElement(TypeMirror type) {
    return (TypeElement) ((DeclaredType) type).asElement();
  }

  @Nullable
  private TypeElement validated(Element element) {
    Element enclosingElement = element.getEnclosingElement();

    if (element.getKind() == ElementKind.ANNOTATION_TYPE
        && element.getModifiers().contains(Modifier.PUBLIC)
        && enclosingElement != null
        && (enclosingElement.getKind() != ElementKind.PACKAGE
        || !((PackageElement) enclosingElement).isUnnamed())) {
      return (TypeElement) element;
    }

    processing().getMessager().printMessage(
        Diagnostic.Kind.ERROR,
        "Element annotated with @Mirror.Annotation annotation should public annotation type in a package",
        element);

    return null;
  }
}
