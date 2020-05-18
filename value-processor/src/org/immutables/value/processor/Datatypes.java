package org.immutables.value.processor;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.immutables.generator.Generator;
import org.immutables.value.Value;
import org.immutables.value.processor.encode.Generator_Renderers;
import org.immutables.value.processor.encode.Renderers;
import org.immutables.value.processor.meta.Proto.AbstractDeclaring;
import org.immutables.value.processor.meta.Proto.DeclaringType;
import org.immutables.value.processor.meta.Proto.Protoclass;
import org.immutables.value.processor.meta.ValueAttribute;
import org.immutables.value.processor.meta.ValueType;

@Generator.Template
abstract class Datatypes extends ValuesTemplate {
  // renderers for encoding elements
  final Renderers rr = new Generator_Renderers();

  @Generator.Typedef
  DatatypeTypes Datatypes;

  @Value.Immutable
  abstract static class DatatypeTypes {
    abstract AbstractDeclaring definedBy();
    abstract String packageGenerated();
    abstract List<ValueType> types();
  }

  DatatypeTypes current;

  final Function<DatatypeTypes, Void> setCurrent = new Function<DatatypeTypes, Void>() {
    @Override
    public Void apply(DatatypeTypes input) {
      current = input;
      return null;
    }
  };

  ValueType currentType;
  private Map<ValueAttribute, Integer> attributeIndexMap;

  final Function<ValueType, Void> setCurrentType = new Function<ValueType, Void>() {
    @Override
    public Void apply(ValueType input) {
      currentType = input;
      attributeIndexMap  = new IdentityHashMap<>();
      int counter = 0;
      for (ValueAttribute a : input.getAllAccessibleAttributes()) {
        attributeIndexMap.put(a, counter++);
      }
      return null;
    }
  };

  final Function<ValueAttribute, Integer> attributeIndex = new Function<ValueAttribute, Integer>() {
    @Override
    public Integer apply(ValueAttribute input) {
      return attributeIndexMap.get(input);
    }
  };

  Iterable<DatatypeTypes> allDefinitions() {
    Multimap<AbstractDeclaring, ValueType> byDeclaring = ArrayListMultimap.create();
    for (ValueType value : values.values()) {
      Protoclass protoclass = value.constitution.protoclass();
      if (protoclass.kind().isValue() || protoclass.kind().isEnclosing()) {
        Optional<AbstractDeclaring> typeAdaptersProvider = protoclass.datatypeProvider();
        if (typeAdaptersProvider.isPresent()) {
          byDeclaring.put(typeAdaptersProvider.get(), value);
        } else if (protoclass.datatypeMarker().isPresent()
            && protoclass.declaringType().isPresent()) {
          DeclaringType topLevel = protoclass.declaringType().get().associatedTopLevel();
          byDeclaring.put(topLevel, value);
        }
      }
    }

    ImmutableList.Builder<DatatypeTypes> builder = ImmutableList.builder();
    for (Entry<AbstractDeclaring, Collection<ValueType>> entry : byDeclaring.asMap().entrySet()) {
      String pack = Iterables.get(entry.getValue(), 0).$$package();
      builder.add(ImmutableDatatypeTypes.builder()
          .definedBy(entry.getKey())
          .packageGenerated(pack)
          .addAllTypes(entry.getValue())
          .build());
    }

    return builder.build();
  }
}
