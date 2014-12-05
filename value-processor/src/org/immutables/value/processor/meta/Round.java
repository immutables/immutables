/*
    Copyright 2014 Ievgen Lukash

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

import org.immutables.value.processor.meta.Proto.DeclaringType;
import org.immutables.value.processor.meta.Proto.DeclaringPackage;
import org.immutables.value.processor.meta.Proto.Protoclass;
import com.google.common.collect.ImmutableList;
import java.util.Set;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import org.immutables.value.Value;
import org.immutables.value.processor.meta.Proto.Protoclass.Kind;

@Value.Immutable
@Value.Nested
public abstract class Round {
  public abstract ProcessingEnvironment processing();

  public abstract RoundEnvironment round();

  public abstract Set<TypeElement> annotations();

  @Value.Lazy
  public ImmutableList<Protoclass> allProtoclasses() {
    ProtoclassCollecter collecter = new ProtoclassCollecter();
    for (TypeElement annotation : annotations()) {
      for (final Element element : round().getElementsAnnotatedWith(annotation)) {
        collecter.collect(element);
      }
    }
    return collecter.builder.build();
  }

  private class ProtoclassCollecter {
    final ImmutableList.Builder<Protoclass> builder = ImmutableList.builder();

    void collect(Element element) {
      switch (element.getKind()) {
      case ANNOTATION_TYPE:
      case INTERFACE:
      case CLASS:
        collectIncludedAndDefinedBy((TypeElement) element);
        break;
      case PACKAGE:
        collectIncludedBy((PackageElement) element);
        break;
      default:
        Reporter.from(processing()).warning("Some unsuitable annotation will be skipped for annotation processing");
      }
    }

    void collectIncludedBy(PackageElement element) {
      final DeclaringPackage declaringPackage = ImmutableProto.DeclaringPackage.builder()
          .processing(processing())
          .element(element)
          .build();

      if (declaringPackage.hasInclude()) {
        for (TypeElement sourceElement : declaringPackage.includedTypes()) {
          builder.add(ImmutableProto.Protoclass.builder()
              .processing(processing())
              .packageOf(declaringPackage)
              .sourceElement(sourceElement)
              .kind(Kind.INCLUDED_IN_PACKAGE)
              .build());
        }
      }
    }

    void collectIncludedAndDefinedBy(TypeElement element) {
      DeclaringType declaringType = ImmutableProto.DeclaringType.builder()
          .processing(processing())
          .element(element)
          .build();

      if (declaringType.hasInclude()) {
        Kind kind = declaringType.isEnclosing()
            ? Kind.INCLUDED_IN_TYPE
            : Kind.INCLUDED_ON_TYPE;

        for (TypeElement sourceElement : declaringType.includedTypes()) {
          builder.add(ImmutableProto.Protoclass.builder()
              .processing(processing())
              .packageOf(declaringType.packageOf())
              .sourceElement(sourceElement)
              .declaringType(declaringType)
              .kind(kind)
              .build());
        }
      }

      if (declaringType.isImmutable() || declaringType.isEnclosing()) {
        builder.add(ImmutableProto.Protoclass.builder()
            .processing(processing())
            .packageOf(declaringType.packageOf())
            .sourceElement(element)
            .declaringType(declaringType)
            .kind(kindOfDefinedBy(declaringType))
            .build());
      }
    }

    private Kind kindOfDefinedBy(DeclaringType declaringType) {
      if (declaringType.isImmutable()) {
        if (declaringType.isEnclosing()) {
          return Kind.DEFINED_AND_ENCLOSING_TYPE;
        } else if (declaringType.enclosingOf().isPresent()) {
          return Kind.DEFINED_NESTED_TYPE;
        } else {
          return Kind.DEFINED_TYPE;
        }
      }
      assert declaringType.isEnclosing();
      return Kind.DEFINED_ENCLOSING_TYPE;
    }
  }
}
