/*
    Copyright 2014 Immutables Authors and Contributors

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
package org.immutables.value.processor.meta;

import org.immutables.value.processor.meta.Proto.Protoclass;

import javax.annotation.Nullable;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;

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

    if (protoclass.kind().isFactory()) {

    } else {
      // This check is legacy, most such checks should have been done on a higher level?
      if (isAbstractValueType(type.element)) {
        if (protoclass.kind().isValue()) {
          new AccessorAttributesCollector(protoclass, type).collect();
        }
      } else {
        protoclass.report().error(
            "Type '%s' annotated or included as value must be non-final class, interface or annotation type",
            protoclass.sourceElement().getSimpleName());
        // Do nothing now. kind of way to less blow things up when it happens. actually need to
        // revise
      }
    }
    return type;
  }

  static boolean isAbstractValueType(Element element) {
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
