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
import com.google.common.collect.*;
import org.immutables.generator.SourceTypes;
import org.immutables.value.processor.meta.LongBits.LongPositions;
import javax.lang.model.element.TypeElement;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public final class FromSupertypesModel {
  public final ImmutableList<FromSupertypesModel.FromSupertype> supertypes;
  public final ImmutableList<String> repeating;
  public final LongPositions positions;

  public final static class FromSupertype {
    public final String type;
    public final String wildcard;
    public final boolean hasWildcard;
    public final ImmutableList<ValueAttribute> attributes;

    FromSupertype(String type, Iterable<ValueAttribute> attribute) {
      this.type = type;
      this.hasWildcard = type.indexOf('<') > 0;
      this.wildcard = hasWildcard ? toRawWildcard(type) : type;
      this.attributes = ImmutableList.copyOf(attribute);
    }

    @Override
    public String toString() {
      return type + " -> " + attributes;
    }
  }

  FromSupertypesModel(
      String abstractTypeName,
      Collection<ValueAttribute> attributes,
      ImmutableListMultimap<String, TypeElement> accessorMapping) {

    SetMultimap<String, String> typesByAttribute = HashMultimap.create();

    for (ValueAttribute a : attributes) {
      String name = a.name();
      ImmutableList<TypeElement> elements = accessorMapping.get(a.names.get);
      for (TypeElement t : elements) {
        String type = isEligibleFromType(t)
            ? t.getQualifiedName().toString()
            : abstractTypeName;

        typesByAttribute.put(name, type);
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

    for (Entry<String, Collection<String>> e : attributeByType.asMap().entrySet()) {
      builder.add(new FromSupertype(e.getKey(), Iterables.transform(e.getValue(), getAttribute)));
    }

    // This abstract value type should be always present and be a from target,
    // even if it doesn't define any specific attributes (just inherits)
    if (!attributeByType.containsKey(abstractTypeName)) {
      builder.add(new FromSupertype(abstractTypeName, ImmutableList.<ValueAttribute>of()));
    }

    this.supertypes = builder.build();

    ImmutableList.Builder<String> repeatingBuilder = ImmutableList.builder();
    for (Entry<String, Collection<String>> e : typesByAttribute.asMap().entrySet()) {
      if (e.getValue().size() > 1) {
        repeatingBuilder.add(e.getKey());
      }
    }

    this.repeating = repeatingBuilder.build();
    this.positions = new LongBits().apply(repeating);
  }

  private boolean isEligibleFromType(TypeElement typeElement) {
    return typeElement.getTypeParameters().isEmpty();
  }

  private static String toRawWildcard(String type) {
    Entry<String, List<String>> withArgs = SourceTypes.extract(type);
    return SourceTypes.stringify(Maps.immutableEntry(withArgs.getKey(),
        Collections.nCopies(withArgs.getValue().size(), "?")));
  }

  public boolean hasManySupertypes() {
    return supertypes.size() > 1;
  }

  public boolean hasWildcards() {
    for (FromSupertype s : supertypes) {
      if (s.hasWildcard) {
        return true;
      }
    }
    return false;
  }
}
