package org.immutables.mirror.processor;

import com.google.common.collect.ImmutableList;
import javax.annotation.Nullable;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
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

    MirrorModel(TypeElement element) {
      this.element = element;

      ImmutableList.Builder<AttributeModel> builder = ImmutableList.builder();
      for (ExecutableElement attribute : ElementFilter.methodsIn(element.getEnclosedElements())) {
        builder.add(new AttributeModel(attribute));
      }
      this.attributes = builder.build();
      this.name = element.getSimpleName().toString();
      this.$$package = ((PackageElement) element.getEnclosingElement()).getQualifiedName().toString();
    }

    String annotationTypeCommas() {
      return element.getAnnotation(Mirror.Annotation.class).value().replace('.', ',');
    }

    class AttributeModel {
      final ExecutableElement element;
      final String name;
      final boolean isArray;
      final TypeMirror type;
      final AttributeTypeKind kind;
      final String suffix;
      final boolean isBoolean;

      AttributeModel(ExecutableElement element) {
        this.element = element;
        this.name = element.getSimpleName().toString();

        TypeMirror type = element.getReturnType();

        this.isArray = type.getKind() == TypeKind.ARRAY;
        this.isBoolean = type.getKind() == TypeKind.BOOLEAN;
        this.type = this.isArray ? ((ArrayType) type).getComponentType() : type;
        this.kind = AttributeTypeKind.from(this.type);

        this.suffix = this.isArray ? "[]" : "";
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
        if (typeElement.getQualifiedName().contentEquals(Class.class.getName())) {
          return TYPE;
        }
        if (typeElement.getQualifiedName().contentEquals(String.class.getName())) {
          return STRING;
        }
      } else if (type.getKind().isPrimitive()) {
        return PRIMITIVE;
      }
      throw new AssertionError();
    }

    private static TypeElement toElement(TypeMirror type) {
      return (TypeElement) ((DeclaredType) type).asElement();
    }
  }

  @Nullable
  private TypeElement validated(Element element) {
    Element enclosingElement = element.getEnclosingElement();

    if (element.getKind() == ElementKind.ANNOTATION_TYPE
        && element.getModifiers().contains(Modifier.PUBLIC)
        && enclosingElement != null
        && enclosingElement.getKind() == ElementKind.PACKAGE
        && !((PackageElement) enclosingElement).isUnnamed()) {
      return (TypeElement) element;
    }

    processing().getMessager().printMessage(
        Diagnostic.Kind.ERROR,
        "Element annotated with @Mirror.Annotation annotation should top-level annotation type in a package",
        element);

    return null;
  }
}
