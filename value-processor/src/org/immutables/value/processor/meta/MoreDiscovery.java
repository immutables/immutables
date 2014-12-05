package org.immutables.value.processor.meta;

import javax.annotation.Nullable;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import org.immutables.value.Value;
import org.immutables.value.processor.meta.Proto.Protoclass;

@Deprecated
@Value.Immutable(builder = false)
abstract class MoreDiscovery {
  @Value.Parameter
  abstract Protoclass protoclass();

  @Value.Derived
  String typeMoreObjects() {
    String typeMoreObjects = UnshadeGuava.typeString("base.MoreObjects");
    String typeObjects = UnshadeGuava.typeString("base.Objects");
    @Nullable
    TypeElement typeElement =
        protoclass().processing().getElementUtils().getTypeElement(typeMoreObjects);

    return typeElement != null ? typeMoreObjects : typeObjects;
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

  @Deprecated
  public boolean validateAbstractValueType() {
    if (protoclass().declaringType().isPresent()) {
      protoclass().declaringType();
    }
    if (MoreDiscovery.isAbstractValueType(protoclass().sourceElement())) {
      return true;
    }
    protoclass().report().error(
        "Type '%s' annotated or included as value must be non-final class, interface or annotation type",
        protoclass().sourceElement().getQualifiedName());

    return false;
  }

  public ValueType createType() {
    return null;
  }
}
