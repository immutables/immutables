package org.immutables.generate.internal.processing;

import com.google.common.collect.ImmutableList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.SimpleAnnotationValueVisitor7;

final class AnnotationPrinter {
  static List<String> extractAnnotationLines(ExecutableElement element) {
    ImmutableList.Builder<String> annotationLines = ImmutableList.builder();

    for (AnnotationMirror annotation : element.getAnnotationMirrors()) {
      String annotationString0 = annotation.toString();
      if (annotationString0.startsWith("@org.immutables") || annotationString0.startsWith("@java.lang")) {
        continue;
      }
      final StringBuilder buffer = new StringBuilder();
      printAnnotation(buffer, annotation);

      annotationLines.add(buffer.toString());
    }

    return annotationLines.build();
  }

  private static void printAnnotation(StringBuilder p, AnnotationMirror annotation) {
    String annotationString = annotation.toString();
    // this gymnastics exists only to workaround annotation processors that do not fully print
    // annotation values (like Eclipse compiler)
    int startOfParameters = annotationString.indexOf('(');
    if (startOfParameters > 0) {
      annotationString = annotationString.substring(0, startOfParameters);
    }

    p.append(annotationString);

    Map<? extends ExecutableElement, ? extends AnnotationValue> values = annotation.getElementValues();
    if (!values.isEmpty()) {
      p.append('(');
      boolean notFirst = false;
      for (Entry<? extends ExecutableElement, ? extends AnnotationValue> e : values.entrySet()) {
        if (notFirst) {
          p.append(',');
        }
        notFirst = true;

        ExecutableElement key = e.getKey();
        p.append(key.getSimpleName()).append(" = ");
        printAnnotationValue(p, e.getValue());
      }
      p.append(')');
    }
  }

  private static void printAnnotationValue(final StringBuilder buffer, AnnotationValue value) {
    value.accept(new SimpleAnnotationValueVisitor7<Void, StringBuilder>() {
      @Override
      protected Void defaultAction(Object o, StringBuilder p) {
        p.append(o);
        return null;
      }

      @Override
      public Void visitString(String s, StringBuilder p) {
        p.append('"').append(StringEscapers.ESCAPER.escape(s)).append('"');
        return null;
      }

      @Override
      public Void visitType(TypeMirror t, StringBuilder p) {
        p.append(t.toString()).append(".class");
        return null;
      }

      @Override
      public Void visitArray(List<? extends AnnotationValue> vals, StringBuilder p) {
        p.append('{');
        boolean notFirst = false;
        for (AnnotationValue v : vals) {
          if (notFirst) {
            p.append(',');
          }
          notFirst = true;
          printAnnotationValue(buffer, v);
        }
        p.append('}');
        return null;
      }

      @Override
      public Void visitEnumConstant(VariableElement c, StringBuilder p) {
        p.append(c.getEnclosingElement()).append('.').append(c.getSimpleName());
        return null;
      }

      @Override
      public Void visitAnnotation(AnnotationMirror a, StringBuilder p) {
        printAnnotation(p, a);
        return null;
      }
    }, buffer);
  }

}
