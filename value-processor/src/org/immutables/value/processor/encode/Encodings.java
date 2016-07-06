package org.immutables.value.processor.encode;

import java.util.ArrayList;
import java.util.List;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.util.ElementFilter;
import org.immutables.generator.AbstractTemplate;
import org.immutables.generator.Generator;
import org.immutables.generator.Templates;
import org.immutables.value.processor.meta.Proto;
import org.immutables.value.processor.meta.Proto.DeclaringType;
import org.immutables.value.processor.meta.Reporter;

@Generator.Template
public abstract class Encodings extends AbstractTemplate {
  @Generator.Typedef
  Encoding Encoding;

  public abstract Templates.Invokable generate();

  final Reporter reporter = Reporter.from(processing());
  final List<Encoding> encodings = new ArrayList<>();

  Encodings() {
    // we just opportunistically reuse declaring type/package model to use some
    // of the infrastructure without needing much of the lazy detection code there.
    Proto.DeclaringFactory factory = new Proto.DeclaringFactory(processing());
    for (TypeElement a : annotations()) {
      for (TypeElement t : ElementFilter.typesIn(round().getElementsAnnotatedWith(a))) {
        encodings.add(new Encoding(factory.typeFor(t)));
      }
    }
  }

  class Encoding {
    private final DeclaringType type;
    private Object impl;

    Encoding(DeclaringType type) {
      this.type = type;
      if (type.element().getKind() != ElementKind.CLASS || !type.isTopLevel()) {
        type.report().error("Encoding type '%s' should be top-level class", type.simpleName());
      }
      if (type.sourceCode().length() == 0) {
        reporter.error("No source code can be extracted @Encoding. Probably, compilation mode is not supported");
      }

      for (Element e : type.element().getEnclosedElements()) {
        processMember(e);
      }
    }

    private void processMember(Element member) {
      if (member.getKind() == ElementKind.FIELD) {
        if (processField((VariableElement) member))
          return;
      }
      if (member.getKind() == ElementKind.METHOD) {
        if (processMethod((ExecutableElement) member))
          return;
      }
      if (member.getKind() == ElementKind.CLASS) {
        if (processClass((TypeElement) member))
          return;
      }

      reporter.withElement(member)
          .warning("Unrecognized element '%s' will be ignored", member.getSimpleName());
    }

    private boolean processMethod(ExecutableElement method) {
      return false;
    }

    private boolean processField(VariableElement field) {
      if (ImplMirror.isPresent(field)) {
        if (impl != null) {
          reporter.withElement(field).error(
              "@Encoding.Impl duplicate field '%s' cannot have more than one implementation field",
              field.getSimpleName());
          return true;
        }
        if (field.getModifiers().contains(Modifier.STATIC)) {
          reporter.withElement(field).error(
              "@Encoding.Impl field '%s' cannot be static",
              field.getSimpleName());
          return true;
        }
      }
      return false;
    }

    private boolean processClass(TypeElement type) {
      if (type.getSimpleName().contentEquals("Builder")) {

      }
      return false;
    }
  }

  class Impl {
    Impl() {

    }
  }
}
