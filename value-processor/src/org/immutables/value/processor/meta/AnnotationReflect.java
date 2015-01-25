package org.immutables.value.processor.meta;

import com.google.common.base.Optional;
import java.util.Map;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.SimpleAnnotationValueVisitor7;

public abstract class AnnotationReflect<A extends AnnotationReflect<A>> {
  public final AnnotationTypeReflect<A> type;
  public final AnnotationMirror mirror;

  protected AnnotationReflect(AnnotationTypeReflect<A> type, AnnotationMirror mirror) {
    this.type = type;
    this.mirror = mirror;
  }

  public class ReflectAttribute {
    final String name;

    public ReflectAttribute(String name) {
      this.name = name;
    }

    AnnotationValue findAnnotationValue() {
      for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> e : mirror.getElementValues().entrySet()) {
        if (e.getKey().getSimpleName().contentEquals(name)) {
          return e.getValue();
        }
      }
      throw new AssertionError("Annotation reflect attribute is not compatible with actual annotation");
    }
  }

  public class StringAttribute extends ReflectAttribute {
    public final String value;

    protected StringAttribute(String name) {
      super(name);
      this.value = initValue();
    }

    private String initValue() {
      return String.valueOf(findAnnotationValue().getValue());
    }

    @Override
    public String toString() {
      return value;
    }
  }

  public class BooleanAttribute extends ReflectAttribute {
    public final boolean value;

    protected BooleanAttribute(String name) {
      super(name);
      this.value = initValue();
    }

    private boolean initValue() {
      findAnnotationValue().accept(new SimpleAnnotationValueVisitor7<Boolean, Void>(false) {
        @Override
        public Boolean visitBoolean(boolean b, Void p) {
          return b;
        }
      }, null);
      return false;
    }
  }

  public static abstract class AnnotationTypeReflect<A extends AnnotationReflect<A>> {
    public final String canonicalName;

    protected AnnotationTypeReflect(String canonicalName) {
      this.canonicalName = canonicalName;
    }

    protected abstract A create(AnnotationMirror mirror);

    public Optional<A> find(Iterable<? extends AnnotationMirror> mirrors) {
      for (AnnotationMirror annotationMirror : mirrors) {
        TypeElement element = (TypeElement) annotationMirror.getAnnotationType().asElement();
        if (element.getQualifiedName().contentEquals(canonicalName)) {
          return Optional.of(create(annotationMirror));
        }
      }
      return Optional.absent();
    }
  }
}
