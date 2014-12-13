package org.immutables.generator;

import javax.annotation.Nullable;
import java.lang.annotation.Annotation;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.SimpleAnnotationValueVisitor7;
import javax.lang.model.util.Types;

public final class AnnotationMirrors {
  private AnnotationMirrors() {}

  public static CharSequence toCharSequence(AnnotationMirror value) {
    PrintVisitor printer = new PrintVisitor();
    printer.visitAnnotation(value, null);
    return printer.builder;
  }

  public static CharSequence toCharSequence(AnnotationValue value) {
    PrintVisitor printer = new PrintVisitor();
    printer.visitValue(value);
    return printer.builder;
  }

  public static ImmutableList<TypeMirror> getTypesFromMirrors(
      String annotationQualifiedName,
      String annotationAttributeName,
      List<? extends AnnotationMirror> annotationMirrors) {
    ImmutableList.Builder<TypeMirror> builder = ImmutableList.builder();
    for (AnnotationMirror annotationMirror : annotationMirrors) {
      TypeElement element = (TypeElement) annotationMirror.getAnnotationType().asElement();
      if (element.getQualifiedName().contentEquals(annotationQualifiedName)) {
        collectTypesFromAnnotationAttribute(annotationAttributeName, builder, annotationMirror);
      }
    }
    return builder.build();
  }

  public static ImmutableList<TypeMirror> getTypesFromMirrors(
      Types types,
      TypeMirror annotationType,
      String annotationAttributeName,
      List<? extends AnnotationMirror> annotationMirrors) {
    ImmutableList.Builder<TypeMirror> builder = ImmutableList.builder();
    for (AnnotationMirror annotationMirror : annotationMirrors) {
      if (types.isSameType(annotationMirror.getAnnotationType(), annotationType)) {
        collectTypesFromAnnotationAttribute(annotationAttributeName, builder, annotationMirror);
      }
    }
    return builder.build();
  }

  private static void collectTypesFromAnnotationAttribute(
      String annotationValueName,
      final ImmutableCollection.Builder<TypeMirror> builder,
      AnnotationMirror annotationMirror) {

    for (Entry<? extends ExecutableElement, ? extends AnnotationValue> e : annotationMirror
        .getElementValues().entrySet()) {

      if (e.getKey().getSimpleName().contentEquals(annotationValueName)) {
        e.getValue().accept(new SimpleAnnotationValueVisitor7<Void, Void>() {
          @Override
          public Void visitArray(List<? extends AnnotationValue> vals, Void p) {
            for (AnnotationValue annotationValue : vals) {
              annotationValue.accept(this, p);
            }
            return null;
          }

          @Override
          public Void visitType(TypeMirror t, Void p) {
            builder.add(t);
            return null;
          }
        }, null);
      }
    }
  }

  @Nullable
  public static AnnotationMirror findAnnotation(
      List<? extends AnnotationMirror> annotationMirrors,
      Class<? extends Annotation> annotationType) {
    String annotationTypeName = annotationType.getCanonicalName();
    for (AnnotationMirror annotation : annotationMirrors) {
      if (((TypeElement) annotation.getAnnotationType().asElement())
          .getQualifiedName().contentEquals(annotationTypeName)) {
        return annotation;
      }
    }
    return null;
  }

  public static boolean isAnnotationPresent(
      List<? extends AnnotationMirror> annotationMirrors,
      Class<? extends Annotation> annotationType) {
    return findAnnotation(annotationMirrors, annotationType) != null;
  }

  private static final class PrintVisitor extends SimpleAnnotationValueVisitor7<Void, Void> {
    final StringBuilder builder = new StringBuilder();

    void visitValue(AnnotationValue value) {
      value.accept(this, null);
    }

    @Override
    protected Void defaultAction(Object o, Void p) {
      builder.append(o);
      return null;
    }

    @Override
    public Void visitString(String s, Void p) {
      builder.append(StringLiterals.toLiteral(s));
      return null;
    }

    @Override
    public Void visitType(TypeMirror t, Void p) {
      builder.append(t).append(".class");
      return null;
    }

    @Override
    public Void visitArray(List<? extends AnnotationValue> vals, Void p) {
      builder.append('{');
      boolean notFirst = false;
      for (AnnotationValue v : vals) {
        if (notFirst) {
          builder.append(',');
        }
        notFirst = true;
        visitValue(v);
      }
      builder.append('}');
      return null;
    }

    @Override
    public Void visitEnumConstant(VariableElement c, Void p) {
      builder.append(c.getEnclosingElement()).append('.').append(c.getSimpleName());
      return null;
    }

    @Override
    public Void visitAnnotation(AnnotationMirror a, Void p) {
      String annotationString = a.toString();

      int startOfParameters = annotationString.indexOf('(');
      if (startOfParameters > 0) {
        annotationString = annotationString.substring(0, startOfParameters);
      }

      builder.append(annotationString);

      Map<? extends ExecutableElement, ? extends AnnotationValue> values = a.getElementValues();
      if (!values.isEmpty()) {
        builder.append('(');
        boolean notFirst = false;
        for (Entry<? extends ExecutableElement, ? extends AnnotationValue> e : values.entrySet()) {
          if (notFirst) {
            builder.append(',');
          }
          notFirst = true;
          builder.append(e.getKey().getSimpleName()).append(" = ");
          visitValue(e.getValue());
        }
        builder.append(')');
      }
      return null;
    }
  }
}
