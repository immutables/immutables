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

import com.google.common.collect.HashMultimap;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.SetMultimap;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;

public class CaseStructure {
  public final List<ValueType> implementedTypes;
  public final SetMultimap<String, ValueType> subtypeUsages = HashMultimap.create();
  private final Set<String> implementedTypeNames;
  private final SetMultimap<String, ValueType> subtyping;

  CaseStructure(ValueType discoveredValue) {
    this.implementedTypes = discoveredValue.nested;
    this.implementedTypeNames = buildImplementedTypesSet(implementedTypes);
    this.subtyping = buildSubtyping(implementedTypes);
  }

  private static Set<String> buildImplementedTypesSet(List<ValueType> implementationTypes) {
    ImmutableSet.Builder<String> builder = ImmutableSet.builder();
    for (ValueType discoveredValue : implementationTypes) {
      builder.add(discoveredValue.typeValue().toString());
    }
    return builder.build();
  }

  private static SetMultimap<String, ValueType> buildSubtyping(List<ValueType> implementationTypes) {
    ImmutableSetMultimap.Builder<String, ValueType> builder = ImmutableSetMultimap.builder();

    for (ValueType type : implementationTypes) {
      String abstractValueTypeName = type.typeAbstract().toString();
      builder.put(abstractValueTypeName, type);

      for (String className : type.getExtendedClassesNames()) {
        if (!className.equals(abstractValueTypeName)) {
          builder.put(className, type);
        }
      }
      for (String interfaceName : type.getImplementedInterfacesNames()) {
        if (!interfaceName.equals(abstractValueTypeName)) {
          builder.put(interfaceName, type);
        }
      }
    }

    return builder.build();
  }

  public Set<ValueType> knownSubtypesOf(String typeName) {
    return subtyping.get(typeName);
  }

  public final Predicate<String> isImplementedType = new Predicate<String>() {
    @Override
    public boolean apply(String input) {
      return implementedTypeNames.contains(input);
    }
  };

  public final Function<String, Iterable<ValueType>> knownSubtypes =
      new Function<String, Iterable<ValueType>>() {
        @Override
        public Iterable<ValueType> apply(@Nullable String typeName) {
          Set<ValueType> subtypes = subtyping.get(typeName);
          subtypeUsages.putAll(typeName, subtypes);
          /*
          for (ValueType subtype : subtypes) {
            subtypeUsages.put(subtype.typeAbstract().toString(), subtype);
          }*/
          return subtypes;
        }
      };
}
