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

import com.google.common.base.CharMatcher;
import com.google.common.base.Joiner;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Multiset;
import java.util.Collection;
import java.util.List;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.Elements;
import org.immutables.value.processor.meta.Proto.Protoclass;

/**
 * It may grow later in some better abstraction, but as it stands now, currently it is
 * just a glue between new "protoclass" model and old discovery routines.
 */
public final class ValueTypeComposer {
  private static final CharMatcher ATTRIBUTE_NAME_CHARS =
      CharMatcher.is('_')
          .or(CharMatcher.inRange('a', 'z'))
          .or(CharMatcher.inRange('A', 'Z'))
          .or(CharMatcher.inRange('0', '9')).precomputed();

  ValueType compose(Protoclass protoclass) {
    ValueType type = new ValueType();
    type.element = protoclass.sourceElement();
    type.immutableFeatures = protoclass.features();
    type.constitution = protoclass.constitution();

    if (protoclass.kind().isFactory()) {
      new FactoryMethodAttributesCollector(protoclass, type).collect();
    } else if (protoclass.kind().isValue() || protoclass.kind().isModifiable()) {
      Collection<String> violations = Lists.newArrayList();
      // This check is legacy, most such checks should have been done on a higher level?
      if (checkAbstractValueType(type.element, violations)) {

        if (protoclass.kind().isValue()) {
          // essentially skip checks if only kind().isModifiable() and not kind().isValue()
          checkForMutableFields(protoclass, (TypeElement) type.element);
          checkForTypeHierarchy(protoclass, type);
        }

        new AccessorAttributesCollector(protoclass, type).collect();
      } else {
        protoclass.report()
            .error("Value type '%s' %s",
                protoclass.sourceElement().getSimpleName(),
                Joiner.on(", ").join(violations));
        // Do nothing now. kind of way to less blow things up when it happens.
      }

      type.detectSerialization();
    }

    checkAttributeNamesIllegalCharacters(type);
    checkAttributeNamesForDuplicates(type, protoclass);
    checkConstructability(type);
    checkStyleConflicts(type, protoclass);
    return type;
  }

  private void checkAttributeNamesIllegalCharacters(ValueType type) {
    for (ValueAttribute a : type.attributes) {
      if (!ATTRIBUTE_NAME_CHARS.matchesAllOf(a.name())) {
        a.report()
            .error("Name '%s' contains some unsupported or reserved characters, please use only A-Z, a-z, 0-9 and _ chars",
                a.name());
      }
    }
  }

  private void checkConstructability(ValueType type) {
    if (!type.isUseBuilder() || type.isUseConstructor()) {
      for (ValueAttribute a : type.getConstructorExcluded()) {
        if (a.isMandatory()) {
          a.report()
              .error("Attribute '%s' is mandatory and should be a constructor"
                  + " @Value.Parameter when builder is disabled or"
                  + " there are other constructor parameters",
                  a.name());
        }
      }
    }
    if (!type.isUseBuilder() && !type.isUseCopyMethods()) {
      for (ValueAttribute a : type.getConstructorExcluded()) {
        if (!a.isMandatory()) {
          a.report()
              .warning("There is no way to initialize '%s' attribute to non-default value."
                  + " Enable builder=true or copy=true or add it as a constructor @Value.Parameter",
                  a.name());
        }
      }
    }
    if (type.isUseSingleton() && !type.getMandatoryAttributes().isEmpty()) {
      for (ValueAttribute a : type.getMandatoryAttributes()) {
        if (a.isMandatory()) {
          a.report()
              .error("Attribute '%s' is mandatory and cannot be used with singleton enabled."
                  + " Singleton instance require all attributes to have default value, otherwise"
                  + " default instance could not be created",
                  a.name());
        }
      }
    }
  }

  private void checkStyleConflicts(ValueType type, Protoclass protoclass) {
    if (protoclass.features().prehash()
        && protoclass.styles().style().privateNoargConstructor()) {
      protoclass.report()
          .annotationNamed(ImmutableMirror.simpleName())
          .warning("'prehash' feature is automatically disabled when 'privateNoargConstructor' style is turned on");
    }
    if (type.isUseConstructor()
        && protoclass.constitution().factoryOf().isNew()) {
      if (type.isUseValidation()) {
        protoclass.report()
            .annotationNamed(ImmutableMirror.simpleName())
            .error("interning, singleton and validation will not work correctly with 'new' constructor configured in style");
      } else if (type.constitution.isImplementationHidden()
          && (type.kind().isEnclosing() || type.kind().isNested())) {
        protoclass.report()
            .annotationNamed(ImmutableMirror.simpleName())
            .error("Enclosing with hidden implementation do not mix with 'new' constructor configured in style");
      }
    }
  }

  private void checkForTypeHierarchy(Protoclass protoclass, ValueType type) {
    scanAndReportInvalidInheritance(protoclass, type.element, type.extendedClasses());
    scanAndReportInvalidInheritance(protoclass, type.element, type.implementedInterfaces());
  }

  private static void scanAndReportInvalidInheritance(
      Protoclass protoclass,
      Element element,
      Iterable<DeclaredType> supertypes) {
    for (TypeElement supertype : Iterables.transform(supertypes, Proto.DeclatedTypeToElement.FUNCTION)) {
      if (!CachingElements.equals(element, supertype) && ImmutableMirror.isPresent(supertype)) {
        protoclass.report()
            .error("Should not inherit %s which is a value type itself."
                + " Avoid extending from another abstract value type."
                + " Better to share common abstract class or interface which"
                + " are not carrying @%s annotation", supertype, ImmutableMirror.simpleName());
      }
    }
  }

  private void checkForMutableFields(Protoclass protoclass, TypeElement element) {
    Elements elementUtils = protoclass.environment().processing().getElementUtils();

    for (VariableElement field : ElementFilter.fieldsIn(
        elementUtils.getAllMembers(CachingElements.getDelegate(element)))) {
      if (!field.getModifiers().contains(Modifier.FINAL)) {
        Reporter report = protoclass.report();
        boolean ownField = CachingElements.equals(element, field.getEnclosingElement());
        if (ownField) {
          report.withElement(field).warning("Avoid introduction of fields (except constants) in abstract value types");
        } else {
          report.warning("Abstract value type inherits mutable fields");
        }
      }
    }
  }

  private void checkAttributeNamesForDuplicates(ValueType type, Protoclass protoclass) {
    if (!type.attributes.isEmpty()) {
      Multiset<String> attributeNames = HashMultiset.create(type.attributes.size());
      for (ValueAttribute attribute : type.attributes) {
        attributeNames.add(attribute.name());
      }

      List<String> duplicates = Lists.newArrayList();
      for (Multiset.Entry<String> entry : attributeNames.entrySet()) {
        if (entry.getCount() > 1) {
          duplicates.add(entry.getElement());
        }
      }

      if (!duplicates.isEmpty()) {
        protoclass.report()
            .error("Duplicate attribute names %s. You should check if correct @Value.Style applied",
                duplicates);
      }
    }
  }

  static boolean checkAbstractValueType(Element element, Collection<String> violations) {
    boolean ofSupportedKind = false
        || element.getKind() == ElementKind.INTERFACE
        || element.getKind() == ElementKind.ANNOTATION_TYPE
        || element.getKind() == ElementKind.CLASS;

    boolean staticOrTopLevel = false
        || element.getEnclosingElement().getKind() == ElementKind.PACKAGE
        || element.getModifiers().contains(Modifier.STATIC);

    boolean nonFinal = !element.getModifiers().contains(Modifier.FINAL);
//    boolean hasNoTypeParameters = ((TypeElement) element).getTypeParameters().isEmpty();

    boolean publicOrPackageVisible =
        !element.getModifiers().contains(Modifier.PRIVATE)
            && !element.getModifiers().contains(Modifier.PROTECTED);

    if (!ofSupportedKind) {
      violations.add("must be class or interface or annotation type");
    }

    if (!nonFinal) {
      violations.add("must be non-final");
    }

//    if (!hasNoTypeParameters) {
//      violations.add("should have no type parameters");
//    }

    if (!publicOrPackageVisible) {
      violations.add("should be public or package-visible");
    }

    if (!staticOrTopLevel) {
      violations.add("should be top-level or static inner class");
    }

    return violations.isEmpty();
  }
}
