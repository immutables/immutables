/*
   Copyright 2014-2018 Immutables Authors and Contributors

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
import com.google.common.base.Functions;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.sun.tools.javac.code.Attribute;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import javax.annotation.Nullable;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Name;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.SimpleAnnotationValueVisitor7;
import javax.lang.model.util.Types;

public final class AnnotationMirrors {
  private AnnotationMirrors() {}

  // safe unchecked: known return type list or empty immutable list
  @SuppressWarnings("unchecked")
  public static List<? extends AnnotationMirror> from(TypeMirror type) {
    return (List<? extends AnnotationMirror>) GetTypeAnnotations.get(type);
  }

  private enum GetTypeAnnotations {
    ;
    @Nullable
    private static final Method METHOD;
    static {
      @Nullable Method method = null;
      try {
        method = TypeMirror.class.getMethod("getAnnotationMirrors");
      } catch (Exception ex) {
      }
      METHOD = method;
    }

    static Object get(Object input) {
      if (METHOD != null) {
        try {
          return METHOD.invoke(input);
        } catch (Exception ex) {
        }
      }
      return ImmutableList.of();
    }
  }

  public static StringBuilder append(StringBuilder builder, AnnotationMirror value) {
    PrintVisitor printer = new PrintVisitor(builder);
    printer.visitAnnotation(value, null);
    return builder;
  }

  public static CharSequence toCharSequence(AnnotationMirror value) {
    PrintVisitor printer = new PrintVisitor();
    printer.visitAnnotation(value, null);
    return printer.builder;
  }

  public static CharSequence toCharSequence(AnnotationValue value) {
    PrintVisitor printer = new PrintVisitor();
    printer.printValue(value);
    return printer.builder;
  }

  public static CharSequence toCharSequence(
      AnnotationMirror value,
      Function<String, String> unresovedImportsResolver) {
    PrintVisitor printer = new PrintVisitor(unresovedImportsResolver);
    printer.visitAnnotation(value, null);
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
        .getElementValues()
        .entrySet()) {

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
    return findAnnotation(annotationMirrors, annotationTypeName);
  }

  @Nullable
  public static AnnotationMirror findAnnotation(
      List<? extends AnnotationMirror> annotationMirrors,
      String annotationTypeName) {
    for (AnnotationMirror annotation : annotationMirrors) {
      if (((TypeElement) annotation.getAnnotationType().asElement())
          .getQualifiedName()
          .contentEquals(annotationTypeName)) {
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
    private static final String ATTRIBUTE_VALUE = "value";
    private static final String CONSTANT_MAX_VALUE = ".MAX_VALUE";
    private static final String CONSTANT_MIN_VALUE = ".MIN_VALUE";
    private static final String CONSTANT_NAN = ".NaN";
    private static final String CONSTANT_NEGATIVE_INFINITY = ".NEGATIVE_INFINITY";
    private static final String CONSTANT_POSITIVE_INFINITY = ".POSITIVE_INFINITY";

    final StringBuilder builder;
    final Function<String, String> unresovedImportsResolver;

    PrintVisitor() {
      this(new StringBuilder(), Functions.<String>identity());
    }

    PrintVisitor(Function<String, String> unresovedImportsResolver) {
      this(new StringBuilder(), unresovedImportsResolver);
    }

    private PrintVisitor(StringBuilder builder) {
      this(builder, Functions.<String>identity());
    }

    private PrintVisitor(StringBuilder builder, Function<String, String> unresovedImportsResolver) {
      this.unresovedImportsResolver = unresovedImportsResolver;
      this.builder = builder;
    }

    void visitValue(AnnotationValue value) {
      value.accept(this, null);
    }

    @Override
    public Void visitBoolean(boolean b, Void p) {
      builder.append(b);
      return null;
    }

    @Override
    public Void visitInt(int i, Void p) {
      appendLiteral(i);
      return null;
    }

    @Override
    public Void visitDouble(double d, Void p) {
      appendLiteral(d);
      return null;
    }

    @Override
    public Void visitFloat(float f, Void p) {
      appendLiteral(f);
      return null;
    }

    @Override
    public Void visitLong(long l, Void p) {
      appendLiteral(l);
      return null;
    }

    @Override
    public Void visitShort(short s, Void p) {
      appendLiteral(s);
      return null;
    }

    @Override
    public Void visitByte(byte b, Void p) {
      appendLiteral(b);
      return null;
    }

    @Override
    public Void visitChar(char c, Void p) {
      builder.append(StringLiterals.toLiteral(c));
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
      if (vals.size() == 1) {
        visitValue(vals.get(0));
        return null;
      }
      builder.append('{');
      boolean notFirst = false;
      for (AnnotationValue v : vals) {
        if (notFirst) {
          builder.append(", ");
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
      builder.append('@').append(a.getAnnotationType());

      Map<? extends ExecutableElement, ? extends AnnotationValue> values = a.getElementValues();
      if (!values.isEmpty()) {
        builder.append('(');
        boolean notFirst = false;
        for (Entry<? extends ExecutableElement, ? extends AnnotationValue> e : values.entrySet()) {
          if (notFirst) {
            builder.append(", ");
          }
          notFirst = true;
          Name name = e.getKey().getSimpleName();
          boolean onlyValue = values.size() == 1 && name.contentEquals(ATTRIBUTE_VALUE);
          if (!onlyValue) {
            builder.append(name).append(" = ");
          }
          printValue(e.getValue());
        }
        builder.append(')');
      }
      return null;
    }

    void printValue(AnnotationValue value) {
      // doing string comparison here because this class may not be available in Javac JDK7
      if (Compiler.JAVAC.isPresent()
          && JAVAC_UNRESOLVED_CLASS.equals(value.getClass().getCanonicalName())) {
        Attribute.UnresolvedClass unresolved = ((Attribute.UnresolvedClass) value);
        String typeString = ((Name) unresolved.classType.tsym.name).toString();
        builder.append(unresovedImportsResolver.apply(typeString)).append(".class");
      } else {
        visitValue(value);
      }
    }

    private void appendLiteral(float value) {
      if (value == Float.NEGATIVE_INFINITY) {
        appendConstant(Float.class, CONSTANT_NEGATIVE_INFINITY);
      } else if (value == Float.POSITIVE_INFINITY) {
        appendConstant(Float.class, CONSTANT_POSITIVE_INFINITY);
      } else if (value == Float.MAX_VALUE) {
        appendConstant(Float.class, CONSTANT_MAX_VALUE);
      } else if (value == Float.MIN_VALUE) {
        appendConstant(Float.class, CONSTANT_MIN_VALUE);
      } else if (Float.isNaN(value)) {
        appendConstant(Float.class, CONSTANT_NAN);
      } else {
        builder.append(value);
      }
    }

    private void appendConstant(Class<?> type, String accessor) {
      builder.append(type.getCanonicalName()).append(accessor);
    }

    private void appendLiteral(double value) {
      if (value == Double.NEGATIVE_INFINITY) {
        appendConstant(Double.class, CONSTANT_NEGATIVE_INFINITY);
      } else if (value == Double.POSITIVE_INFINITY) {
        appendConstant(Double.class, CONSTANT_POSITIVE_INFINITY);
      } else if (value == Double.MAX_VALUE) {
        appendConstant(Double.class, CONSTANT_MAX_VALUE);
      } else if (value == Double.MIN_VALUE) {
        appendConstant(Double.class, CONSTANT_MIN_VALUE);
      } else if (Double.isNaN(value)) {
        appendConstant(Double.class, CONSTANT_NAN);
      } else {
        builder.append(value);
      }
    }

    private void appendLiteral(long value) {
      if (value == Long.MAX_VALUE) {
        appendConstant(Long.class, CONSTANT_MAX_VALUE);
      } else if (value == Long.MIN_VALUE) {
        appendConstant(Long.class, CONSTANT_MIN_VALUE);
      } else {
        builder.append(value);
      }
    }

    private void appendLiteral(int value) {
      if (value == Integer.MAX_VALUE) {
        appendConstant(Integer.class, CONSTANT_MAX_VALUE);
      } else if (value == Integer.MIN_VALUE) {
        appendConstant(Integer.class, CONSTANT_MIN_VALUE);
      } else {
        builder.append(value);
      }
    }

    private void appendLiteral(short value) {
      if (value == Short.MAX_VALUE) {
        appendConstant(Short.class, CONSTANT_MAX_VALUE);
      } else if (value == Short.MIN_VALUE) {
        appendConstant(Short.class, CONSTANT_MIN_VALUE);
      } else {
        builder.append(value);
      }
    }

    private void appendLiteral(byte value) {
      if (value == Byte.MAX_VALUE) {
        appendConstant(Byte.class, CONSTANT_MAX_VALUE);
      } else if (value == Byte.MIN_VALUE) {
        appendConstant(Byte.class, CONSTANT_MIN_VALUE);
      } else {
        builder.append(value);
      }
    }

    private static final String JAVAC_UNRESOLVED_CLASS = "com.sun.tools.javac.code.Attribute.UnresolvedClass";
  }
}
