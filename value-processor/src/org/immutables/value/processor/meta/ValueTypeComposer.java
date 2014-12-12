package org.immutables.value.processor.meta;

import javax.annotation.Nullable;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import org.immutables.value.processor.meta.Proto.Protoclass;

/**
 * It may grow later in some better abstraction, but as it stands now, currently it is
 * just a glue between new "protoclass" model and old discovery routines.
 */
public final class ValueTypeComposer {
  private final ProcessingEnvironment processing;
  private final String typeMoreObjects;

  ValueTypeComposer(ProcessingEnvironment processing) {
    this.processing = processing;
    this.typeMoreObjects = inferTypeMoreObjects();
  }

  String inferTypeMoreObjects() {
    String typeMoreObjects = UnshadeGuava.typeString("base.MoreObjects");
    String typeObjects = UnshadeGuava.typeString("base.Objects");
    @Nullable TypeElement typeElement =
        processing.getElementUtils().getTypeElement(typeMoreObjects);

    return typeElement != null ? typeMoreObjects : typeObjects;
  }

  ValueType compose(Protoclass protoclass) {
    ValueType type = new ValueType();
    type.typeMoreObjects = typeMoreObjects;
    type.element = protoclass.sourceElement();
    type.immutableFeatures = protoclass.features();
    type.constitution = ImmutableConstitution.builder()
        .protoclass(protoclass)
        .build();

    if (isAbstractValueType(type.element)) {
      if (protoclass.kind().isValue()) {
        new AttributesCollector(protoclass, type).collect();
      }
    } else {
      protoclass.report().error(
          "Type '%s' annotated or included as value must be non-final class, interface or annotation type",
          protoclass.sourceElement().getSimpleName());
      // Do nothing now. kind of way to less blow things up when it happens. actually need to revise
    }

    return type;
  }

  static boolean isAbstractValueType(TypeElement element) {
    boolean ofSupportedKind = false
        || element.getKind() == ElementKind.INTERFACE
        || element.getKind() == ElementKind.ANNOTATION_TYPE
        || element.getKind() == ElementKind.CLASS;

    boolean staticOrTopLevel = false
        || element.getEnclosingElement().getKind() == ElementKind.PACKAGE
        || element.getModifiers().contains(Modifier.STATIC);

    boolean nonFinal = !element.getModifiers().contains(Modifier.FINAL);

    return ofSupportedKind && staticOrTopLevel && nonFinal;
  }
}
