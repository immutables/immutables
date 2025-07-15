/*
   Copyright 2015 Immutables Authors and Contributors

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

import com.google.common.base.Function;
import com.google.common.base.Functions;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimaps;
import com.google.common.collect.SetMultimap;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.annotation.Nullable;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;

import org.immutables.generator.SourceTypes;
import org.immutables.value.processor.encode.Type;
import org.immutables.value.processor.meta.LongBits.LongPositions;
import org.immutables.value.processor.meta.Reporter.About;

public final class FromSupertypesModel {
  private final ProcessingEnvironment processing;
  private static final AtomicBoolean typeParseExceptionReported = new AtomicBoolean();

  public final ImmutableList<FromSupertypesModel.FromSupertype> supertypes;
  public final ImmutableList<String> repeating;
  public final LongPositions positions;
  private final Reporter reporter;

  public final static class FromSupertype {
    public final String type;
    public final String wildcard;
    public final boolean hasGenerics;
    public final ImmutableList<ValueAttribute> attributes;
    public final String raw;

    FromSupertype(String type, Iterable<ValueAttribute> attribute) {
      this.type = type;
      this.hasGenerics = type.indexOf('<') > 0;
      Map.Entry<String, List<String>> withArgs = SourceTypes.extract(type);
      this.raw = withArgs.getKey();
      this.wildcard = hasGenerics
          ? SourceTypes.stringify(Maps.immutableEntry(withArgs.getKey(),
              Collections.nCopies(withArgs.getValue().size(), "?")))
          : type;
      this.attributes = ImmutableList.copyOf(attribute);
    }

    @Override
    public String toString() {
      return type + " -> " + attributes;
    }
  }

  FromSupertypesModel(
      Reporter reporter,
      String abstractTypeName,
      Collection<ValueAttribute> attributes,
      ImmutableListMultimap<String, TypeElement> accessorMapping,
      ProcessingEnvironment processing,
      List<TypeElement> extendedClasses,
      Set<TypeElement> implementedInterfaces) {

    this.reporter = reporter;
    this.processing = processing;
    SetMultimap<String, String> typesByAttribute = HashMultimap.create();

    for (ValueAttribute a : attributes) {
      String name = a.name();
      for (TypeElement t : Iterables.concat(implementedInterfaces, extendedClasses)) {
        if (isEligibleFromType(t, a)) {
          List<String> typeParamNames = new ArrayList<>(t.getTypeParameters().size());
          for (TypeParameterElement typeParameterElement : t.getTypeParameters()) {
            typeParamNames.add(typeParameterElement.toString());
          }
          typesByAttribute.put(name, SourceTypes.stringify(Maps.immutableEntry(t.getQualifiedName().toString(), typeParamNames)));
        } else {
          typesByAttribute.put(name, abstractTypeName);
        }
      }
    }

    SetMultimap<String, String> attributeByType =
        Multimaps.invertFrom(typesByAttribute, HashMultimap.<String, String>create());

    Map<String, ValueAttribute> attributeMap = Maps.newHashMapWithExpectedSize(attributes.size());

    for (ValueAttribute a : attributes) {
      attributeMap.put(a.name(), a);
    }

    Function<String, ValueAttribute> getAttribute = Functions.forMap(attributeMap);

    ImmutableList.Builder<FromSupertypesModel.FromSupertype> builder = ImmutableList.builder();

    for (Map.Entry<String, Collection<String>> e : attributeByType.asMap().entrySet()) {
      builder.add(new FromSupertype(e.getKey(), Iterables.transform(e.getValue(), getAttribute)));
    }

    // This abstract value type should be always present and be a from target,
    // even if it doesn't define any specific attributes (just inherits)
    if (!attributeByType.containsKey(abstractTypeName)) {
      builder.add(new FromSupertype(abstractTypeName, ImmutableList.<ValueAttribute>of()));
    }

    this.supertypes = builder.build();

    ImmutableList.Builder<String> repeatingBuilder = ImmutableList.builder();
    for (Map.Entry<String, Collection<String>> e : typesByAttribute.asMap().entrySet()) {
      if (e.getValue().size() > 1) {
        repeatingBuilder.add(e.getKey());
      }
    }

    this.repeating = repeatingBuilder.build();
    this.positions = new LongBits().apply(repeating);
  }

  private boolean isDirectAncestor(TypeElement parent, TypeElement child) {
    Types typeUtils = processing.getTypeUtils();
    TypeMirror erasedParent = typeUtils.erasure(parent.asType());

    // check for superclass
    if (typeUtils.isSameType(erasedParent, typeUtils.erasure(child.getSuperclass()))) {
      return true;
    }

    // check interfaces
    for (TypeMirror interfaceType : child.getInterfaces()) {
      if (typeUtils.isSameType(erasedParent, typeUtils.erasure(interfaceType))) {
        return true;
      }
    }

    return false;
  }

  private boolean boundsMatch(List<? extends TypeMirror> a, List<? extends TypeMirror> b) {
    if (a.size() != b.size()) {
      return false;
    }

    for (int i = 0; i < a.size(); i++) {
      if (!processing.getTypeUtils().isSameType(a.get(i), b.get(i))) {
        return false;
      }
    }

    return true;
  }

  private boolean isEligibleFromType(TypeElement typeElement, ValueAttribute attr) {
    @Nullable ExecutableElement accessor = findMethod(typeElement, attr.names.get);
    if (accessor == null) {
      // it can be now as we've changed upper loop
      return false;
    }
    if (!typeElement.getTypeParameters().isEmpty()) {
      TypeElement containingTypeElement = (TypeElement) attr.containingType.originalElement();

      // bail early if types don't both have 1 parameter
      if ((typeElement.getTypeParameters().size() != 1) || (containingTypeElement.getTypeParameters().size() != 1)) {
        return false;
      }

      // ensure this is a direct ancestor
      if (!isDirectAncestor(typeElement, containingTypeElement)) {
        return false;
      }

      // confirm bounds match
      for (int i = 0; i < typeElement.getTypeParameters().size(); i++) {
        if (!boundsMatch(typeElement.getTypeParameters().get(i).getBounds(), containingTypeElement.getTypeParameters().get(i).getBounds())) {
          return false;
        }
      }
    }
    try {
      String settledType = attr.returnType.toString();
      String supertypeMethod = accessor.getReturnType().toString();

      // This kind of parsing normalizes and ignores type annotations
      Type.Producer tf = new Type.Producer();
      Type.Parser parser = new Type.Parser(tf, tf.parameters());
      parser.nullableAnnotationSimpleName = attr.style().nullableAnnotation();
      Type parsedSupersType = parser.parse(supertypeMethod);
      boolean seenNullableAnnotation = parser.seenNullableTypeAnnotation;

      Type parsedSettledType = parser.parse(settledType);

      if (parsedSupersType.equals(parsedSettledType)) {
        if (seenNullableAnnotation) {
          attr.initTypeuseNullableSupertype();
        } else {
          attr.initNullabilitySupertype(accessor);
        }
        return true;
      }
    } catch (Exception typeParseException) {
      if (typeParseExceptionReported.compareAndSet(false, true)) {
        reporter.warning("Type parsing problem in FromSupertypesModel: %s", typeParseException + "\n\tat " + typeParseException.getStackTrace()[0]);
      }
    }

    reporter.warning(About.FROM,
        "Generated builder '.from' method will not copy from attribute '%s'"
        + " because it has some not-yet-generated type,"
        + " or different return type in supertype "
        + " (we cannot handle generic specialization or covariant overrides yet)."
        + " Sometimes it is possible to avoid this by providing abstract override method in this value object",
        attr.name());
    return false;
  }

  private @Nullable ExecutableElement findMethod(TypeElement typeElement, String getter) {
    for (ExecutableElement m : ElementFilter.methodsIn(processing.getElementUtils().getAllMembers(typeElement))) {
      if (m.getSimpleName().contentEquals(getter) && m.getParameters().isEmpty()) {
        return m;
      }
    }
    return null;
  }

  public boolean hasManySupertypes() {
    return supertypes.size() > 1;
  }

  public boolean hasWildcards() {
    for (FromSupertype s : supertypes) {
      if (s.hasGenerics) {
        return true;
      }
    }
    return false;
  }
}
