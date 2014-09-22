package org.immutables.modeling;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import java.util.Map;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;

public final class Imports extends Introspection {
  private final TypeMirror importType;

  Imports(ProcessingEnvironment environment) {
    super(environment);
    this.importType = elements.getTypeElement(Template.Import.class.getCanonicalName()).asType();
  }

  public final ImmutableMap<String, TypeMirror> importsIn(TypeElement type) {
    Map<String, TypeMirror> collected = Maps.newHashMap();

    collectBuiltins(collected);
    collectImports(type, collected);
    collectTypedefs(type, collected);

    return ImmutableMap.copyOf(collected);
  }

  private void collectBuiltins(Map<String, TypeMirror> collected) {
    TypeKind[] primitiveKinds = {
        TypeKind.BOOLEAN,
        TypeKind.BYTE,
        TypeKind.CHAR,
        TypeKind.DOUBLE,
        TypeKind.FLOAT,
        TypeKind.INT,
        TypeKind.LONG,
        TypeKind.SHORT };

    for (TypeKind kind : primitiveKinds) {
      TypeElement boxedClass = types.boxedClass(types.getPrimitiveType(kind));
      collected.put(boxedClass.getSimpleName().toString(), boxedClass.asType());
    }

    TypeElement typeElement = elements.getTypeElement(String.class.getName());
    collected.put(typeElement.getSimpleName().toString(), typeElement.asType());
  }

  private void collectTypedefs(TypeElement type, Map<String, TypeMirror> collected) {
    for (VariableElement field : ElementFilter.fieldsIn(elements.getAllMembers(type))) {
      if (field.getAnnotation(Template.Typedef.class) != null) {
        collected.put(field.getSimpleName().toString(), field.asType());
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
    return extractTypesFromAnnotations(
        importType,
        "value",
        element.getAnnotationMirrors());
  }
}
