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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import javax.annotation.Nullable;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.ElementFilter;
import org.immutables.value.Value;
import org.immutables.value.processor.meta.Proto.DeclaringPackage;
import org.immutables.value.processor.meta.Proto.DeclaringType;
import org.immutables.value.processor.meta.Proto.Protoclass;
import org.immutables.value.processor.meta.Proto.Protoclass.Kind;

@Value.Immutable
public abstract class Round {
  public abstract ProcessingEnvironment processing();

  public abstract RoundEnvironment round();

  public abstract Set<TypeElement> annotations();

  public abstract Set<String> customImmutableAnnotations();

  private final Proto.Interning interners = new Proto.Interning();

  @Value.Derived
  ValueTypeComposer composer() {
    return new ValueTypeComposer();
  }

  @Value.Lazy
  public Proto.Environment environment() {
    return ImmutableProto.Environment.of(processing(), this);
  }

  public Multimap<DeclaringPackage, ValueType> collectValues() {
    ImmutableList<Protoclass> protoclasses = collectProtoclasses();
    Map<DeclaringType, ValueType> enclosingTypes = Maps.newHashMap();

    ImmutableMultimap.Builder<DeclaringPackage, ValueType> builder = ImmutableMultimap.builder();

    // Collect enclosing
    for (Protoclass protoclass : protoclasses) {
      if (protoclass.kind().isEnclosing()) {
        ValueType type = composeValue(protoclass);
        enclosingTypes.put(protoclass.declaringType().get(), type);
      }
    }
    // Collect remaining and attach if nested
    for (Protoclass protoclass : protoclasses) {
      @Nullable ValueType current = null;
      if (protoclass.kind().isNested()) {
        @Nullable ValueType enclosing = enclosingTypes.get(protoclass.enclosingOf().get());
        if (enclosing != null) {
          current = composeValue(protoclass);
          // Attach nested to enclosing
          enclosing.addNested(current);
        }
      }
      // getting the ValueType if it was alredy created and put into enclosingTypes
      if (current == null && protoclass.kind().isEnclosing()) {
        current = enclosingTypes.get(protoclass.declaringType().get());
      }
      // If none then we just create it
      if (current == null) {
        current = composeValue(protoclass);
      }
      // We put all enclosing and nested values by the package
      builder.put(protoclass.packageOf(), current);
    }

    return builder.build();
  }

  public ImmutableList<Protoclass> protoclassesFrom(Iterable<? extends Element> elements) {
    ProtoclassCollecter collecter = new ProtoclassCollecter();
    collecter.collect(elements);
    return collecter.builder.build();
  }

  public ImmutableList<Protoclass> collectProtoclasses() {
    ProtoclassCollecter collecter = new ProtoclassCollecter();
    collecter.collect(allAnnotatedElements());
    return collecter.builder.build();
  }

  private Set<Element> allAnnotatedElements() {
    Set<Element> elements = Sets.newLinkedHashSetWithExpectedSize(100);
    for (TypeElement annotation : annotations()) {
      Set<? extends Element> annotatedElements = round().getElementsAnnotatedWith(annotation);
      checkAnnotation(annotation, annotatedElements);
      elements.addAll(annotatedElements);
    }
    return elements;
  }

  private void checkAnnotation(TypeElement annotation, Set<? extends Element> annotatedElements) {
    if (annotation.getQualifiedName().contentEquals(ValueUmbrellaMirror.qualifiedName())) {
      for (Element element : annotatedElements) {
        Reporter.from(processing())
            .withElement(element)
            .annotationNamed(ValueUmbrellaMirror.simpleName())
            .warning("@Value annotation have no effect, use nested annotations instead, like @Value.Immutable");
      }
    }
  }

  TypeElement wrapElement(TypeElement element) {
    return CachingElements.asCaching(element);
  }

  ExecutableElement wrapElement(ExecutableElement element) {
    return CachingElements.asCaching(element);
  }

  PackageElement wrapElement(PackageElement element) {
    return CachingElements.asCaching(element);
  }

  DeclaringType inferDeclaringTypeFor(Element element) {
    return declaringTypeFrom(enclosingTypeOf(element));
  }

  private TypeElement enclosingTypeOf(Element element) {
    for (Element e = element; e != null;) {
      ElementKind kind = e.getKind();
      if (kind.isClass() || kind.isInterface()) {
        return (TypeElement) e;
      }
      e = e.getEnclosingElement();
    }
    throw new NoSuchElementException();
  }

  DeclaringType declaringTypeFrom(TypeElement element) {
    return interners.forType(
        ImmutableProto.DeclaringType.builder()
            .environment(environment())
            .interner(interners)
            .element(wrapElement(element))
            .build());
  }

  private final Map<Protoclass, ValueType> composedValues = new HashMap<>();

  ValueType composeValue(Protoclass protoclass) {
    @Nullable ValueType t = composedValues.get(protoclass);
    if (t == null) {
      t = new ValueType();
      composedValues.put(protoclass, t);
      composer().compose(t, protoclass);
    }
    return t;
  }

  private class ProtoclassCollecter {
    final ImmutableList.Builder<Protoclass> builder = ImmutableList.builder();

    void collect(Iterable<? extends Element> elements) {
      for (Element e : elements) {
        collect(e);
      }
    }

    void collect(Element element) {
      switch (element.getKind()) {
      case ANNOTATION_TYPE:
      case INTERFACE:
      case CLASS:
      case ENUM:
        collectIncludedAndDefinedBy((TypeElement) element);
        break;
      case METHOD:
      case CONSTRUCTOR:
        collectDefinedBy((ExecutableElement) element);
        break;
      case PACKAGE:
        collectIncludedBy((PackageElement) element);
        break;
      default:
        Reporter.from(processing())
            .withElement(element)
            .warning("Unmatched annotation will be skipped for annotation processing");
      }
    }

    void collectDefinedBy(ExecutableElement element) {
      DeclaringType declaringType = declaringTypeFrom((TypeElement) element.getEnclosingElement());

      if (FactoryMirror.isPresent(element)
          && declaringType.verifiedFactory(element)) {
        builder.add(interners.forProto(ImmutableProto.Protoclass.builder()
            .environment(environment())
            .packageOf(declaringType.packageOf())
            .sourceElement(wrapElement(element))
            .declaringType(declaringType)
            .kind(Kind.DEFINED_FACTORY)
            .build()));
      }

      if (FConstructorMirror.isPresent(element)
          && declaringType.verifiedConstructor(element)) {
        builder.add(interners.forProto(ImmutableProto.Protoclass.builder()
            .environment(environment())
            .packageOf(declaringType.packageOf())
            .sourceElement(wrapElement(element))
            .declaringType(declaringType)
            .kind(Kind.DEFINED_CONSTRUCTOR)
            .build()));
      }
    }

    void collectIncludedBy(PackageElement element) {
      final DeclaringPackage declaringPackage = interners.forPackage(ImmutableProto.DeclaringPackage.builder()
          .environment(environment())
          .interner(interners)
          .element(wrapElement(element))
          .build());

      if (declaringPackage.include().isPresent()) {
        for (TypeElement sourceElement : declaringPackage.includedTypes()) {
          builder.add(interners.forProto(
              ImmutableProto.Protoclass.builder()
                  .environment(environment())
                  .packageOf(declaringPackage)
                  .sourceElement(wrapElement(sourceElement))
                  .kind(Kind.INCLUDED_IN_PACKAGE)
                  .build()));
        }
      }

      if (declaringPackage.builderInclude().isPresent()) {
        for (TypeElement includedType : declaringPackage.builderIncludedTypes()) {
          for (ExecutableElement m : ElementFilter.methodsIn(includedType.getEnclosedElements())) {
            if (DeclaringType.suitableForBuilderFactory(m)) {
              builder.add(interners.forProto(
                  ImmutableProto.Protoclass.builder()
                      .environment(environment())
                      .packageOf(declaringPackage)
                      .sourceElement(wrapElement(m))
                      .kind(Kind.INCLUDED_FACTORY_IN_PACKAGE)
                      .build()));
            }
          }
          for (ExecutableElement c : ElementFilter.constructorsIn(includedType.getEnclosedElements())) {
            if (DeclaringType.suitableForBuilderConstructor(c)) {
              builder.add(interners.forProto(
                  ImmutableProto.Protoclass.builder()
                      .environment(environment())
                      .packageOf(declaringPackage)
                      .sourceElement(wrapElement(c))
                      .kind(Kind.INCLUDED_CONSTRUCTOR_IN_PACKAGE)
                      .build()));
              // stop on first suitable
              // don't have any good idea how to handle multiple
              // constructor. CBuilder, C2Builder, C3Builder for
              // class C seems even more crazy than stop on first suitable
              // but this is debatable
              break;
            }
          }
        }
      }
    }

    void collectIncludedAndDefinedBy(TypeElement element) {
      DeclaringType declaringType = declaringTypeFrom(element);

      if (declaringType.include().isPresent()) {
        Kind kind = declaringType.isEnclosing()
            ? Kind.INCLUDED_IN_TYPE
            : Kind.INCLUDED_ON_TYPE;

        for (TypeElement sourceElement : declaringType.includedTypes()) {
          builder.add(interners.forProto(ImmutableProto.Protoclass.builder()
              .environment(environment())
              .packageOf(declaringType.packageOf())
              .sourceElement(wrapElement(sourceElement))
              .declaringType(declaringType)
              .kind(kind)
              .build()));
        }
      }

      if (declaringType.builderInclude().isPresent()) {
        for (TypeElement includedType : declaringType.builderIncludedTypes()) {
          for (ExecutableElement m : ElementFilter.methodsIn(includedType.getEnclosedElements())) {
            if (DeclaringType.suitableForBuilderFactory(m)) {
              builder.add(interners.forProto(
                  ImmutableProto.Protoclass.builder()
                      .environment(environment())
                      .packageOf(declaringType.packageOf())
                      .declaringType(declaringType)
                      .sourceElement(wrapElement(m))
                      .kind(Kind.INCLUDED_FACTORY_ON_TYPE)
                      .build()));
            }
          }
          for (ExecutableElement c : ElementFilter.constructorsIn(includedType.getEnclosedElements())) {
            if (DeclaringType.suitableForBuilderConstructor(c)) {
              builder.add(interners.forProto(
                  ImmutableProto.Protoclass.builder()
                      .environment(environment())
                      .packageOf(declaringType.packageOf())
                      .declaringType(declaringType)
                      .sourceElement(wrapElement(c))
                      .kind(Kind.INCLUDED_CONSTRUCTOR_ON_TYPE)
                      .build()));
              // stop on first suitable
              // don't have any good idea how to handle multiple
              // constructor. CBuilder, C2Builder, C3Builder for
              // class C seems even more crazy than stop on first suitable
              // but this is debatable
              break;
            }
          }
        }
      }

      if (declaringType.isImmutable()
          || declaringType.isEnclosing()
          || declaringType.isModifiable()) {

        Kind kind = kindOfDefinedBy(declaringType);

        builder.add(interners.forProto(ImmutableProto.Protoclass.builder()
            .environment(environment())
            .packageOf(declaringType.packageOf())
            .sourceElement(wrapElement(element))
            .declaringType(declaringType)
            .kind(kind)
            .build()));
      } else if (declaringType.isTopLevel()) {
        for (TypeElement nested : ElementFilter.typesIn(declaringType.element().getEnclosedElements())) {
          collectIncludedAndDefinedBy(nested);
        }
      }
    }

    private Kind kindOfDefinedBy(DeclaringType declaringType) {
      if (declaringType.isImmutable()) {
        if (declaringType.isEnclosing()) {
          return Kind.DEFINED_AND_ENCLOSING_TYPE;
        } else if (declaringType.isEnclosed()) {
          return Kind.DEFINED_NESTED_TYPE;
        } else if (declaringType.isModifiable()) {
          return Kind.DEFINED_TYPE_AND_COMPANION;
        } else {
          return Kind.DEFINED_TYPE;
        }
      }
      if (declaringType.isModifiable()) {
        return Kind.DEFINED_COMPANION;
      }
      assert declaringType.isEnclosing();
      return Kind.DEFINED_ENCLOSING_TYPE;
    }
  }
}
