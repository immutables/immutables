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

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.SetMultimap;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;

public class CaseStructure {
  public final List<ValueType> implementationTypes;
  public final SetMultimap<String, ValueType> subtyping;
  public final Set<String> implementationTypeNames;
  public final SetMultimap<String, ValueType> subtypeUsages = HashMultimap.create();
  public final SetMultimap<String, ValueType> abstractUsages = HashMultimap.create();

  CaseStructure(ValueType discoveredValue) {
    this.implementationTypes = discoveredValue.nested;
    this.implementationTypeNames = buildImplementationType(implementationTypes);
    this.subtyping = buildSubtyping(implementationTypes);
  }

  private static Set<String> buildImplementationType(List<ValueType> implementationTypes) {
    ImmutableSet.Builder<String> builder = ImmutableSet.builder();
    for (ValueType discoveredValue : implementationTypes) {
      builder.add(discoveredValue.typeImmutable().toString());
    }
    return builder.build();
  }

  private static SetMultimap<String, ValueType> buildSubtyping(List<ValueType> implementationTypes) {
    ImmutableSetMultimap.Builder<String, ValueType> builder = ImmutableSetMultimap.builder();

    for (ValueType type : implementationTypes) {
      builder.put(type.element.getQualifiedName().toString(), type);

      for (String className : type.getExtendedClassesNames()) {
        builder.put(className, type);
      }
      for (String interfaceName : type.getImplementedInterfacesNames()) {
        builder.put(interfaceName, type);
      }
    }

    return builder.build();
  }

  public final Predicate<String> isImplementationType = new Predicate<String>() {
    @Override
    public boolean apply(String input) {
      return implementationTypeNames.contains(input);
    }
  };

  public final Function<String, Iterable<ValueType>> knownSubtypes =
      new Function<String, Iterable<ValueType>>() {
        @Override
        public Iterable<ValueType> apply(@Nullable String typeName) {
          Set<ValueType> subtypes = subtyping.get(typeName);
          subtypeUsages.putAll(typeName, subtypes);
          for (ValueType subtype : subtypes) {
            subtypeUsages.put(subtype.valueTypeName(), subtype);
          }
          return subtypes;
        }
      };
}
