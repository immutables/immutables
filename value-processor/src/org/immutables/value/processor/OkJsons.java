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
package org.immutables.value.processor;

import java.util.IdentityHashMap;
import com.google.common.collect.Iterables;
import com.google.common.base.Predicate;
import com.google.common.base.CaseFormat;
import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import javax.annotation.Nullable;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import org.immutables.generator.Generator;
import org.immutables.value.Value;
import org.immutables.value.processor.meta.OkNamedMirror;
import org.immutables.value.processor.meta.Proto.AbstractDeclaring;
import org.immutables.value.processor.meta.Proto.DeclaringType;
import org.immutables.value.processor.meta.Proto.Protoclass;
import org.immutables.value.processor.meta.ValueAttribute;
import org.immutables.value.processor.meta.ValueType;

//@Generator.Template
abstract class OkJsons extends ValuesTemplate {

  @Generator.Typedef
  OkTypeAdapterTypes Adapted;

  @Generator.Typedef
  AttributesByFirstLetter Mm;

  @Value.Immutable
  abstract static class OkTypeAdapterTypes {
    abstract AbstractDeclaring definedBy();

    abstract List<ValueType> types();

    @Value.Derived
    EnumAllDefinitions enums() {
      return new EnumAllDefinitions(types(), definedBy());
    }

    final Function<ValueAttribute, AdapterDecider> decider = new Function<ValueAttribute, AdapterDecider>() {
      private final IdentityHashMap<ValueAttribute, AdapterDecider> cache = Maps.newIdentityHashMap();

      @Override
      public AdapterDecider apply(ValueAttribute input) {
        AdapterDecider d = cache.get(input);
        if (d == null) {
          d = new AdapterDecider(input, enums());
          cache.put(input, d);
        }
        return d;
      }
    };

    final Predicate<ValueType> requireAdapters = new Predicate<ValueType>() {
      @Override
      public boolean apply(@Nullable ValueType input) {
        for (AdapterDecider d : Iterables.transform(input.allMarshalingAttributes(), decider)) {
          if (d.useAdapter()) {
            return true;
          }
        }
        return false;
      }
    };
  }

  @Generator.Typedef
  AdapterDecider Decider;

  static class AdapterDecider {
    final ValueAttribute attribute;
    private final EnumAllDefinitions enums;
    private final AdaptedUse adapterUse;
    private final LocalEnumUse enumUse;

    AdapterDecider(ValueAttribute attribute, EnumAllDefinitions enums) {
      this.attribute = attribute;
      this.enums = enums;
      this.adapterUse = inferAdapter();
      this.enumUse = inferEnum();
    }

    private enum AdaptedUse {
      NONE, FULL, FIRST, SECOND, BOTH;
    }

    private enum LocalEnumUse {
      NONE, FULL, FIRST, SECOND, BOTH;
    }

    private AdaptedUse inferAdapter() {
      if (!attribute.getJsonQualiferAnnotations().isEmpty()) {
        return AdaptedUse.FULL;
      }
      if (attribute.isOptionalType() || attribute.isCollectionType() || attribute.isArrayType()) {
        if (attribute.isRequiresMarshalingAdapter()
            && !enums.definitionsByQualifiedName.containsKey(attribute.firstTypeParameter())) {
          return AdaptedUse.FIRST;
        }
        return AdaptedUse.NONE;
      }
      if (attribute.isMapType()) {
        if (attribute.isRequiresMarshalingAdapter()
            && !enums.definitionsByQualifiedName.containsKey(attribute.firstTypeParameter())) {
          return AdaptedUse.FIRST;
        }
        if (attribute.isRequiresMarshalingSecondaryAdapter()
            && !enums.definitionsByQualifiedName.containsKey(attribute.secondTypeParameter())) {
          return AdaptedUse.SECOND;
        }
        return AdaptedUse.NONE;
      }
      if (attribute.isRequiresMarshalingAdapter()
          && !enums.definitionsByQualifiedName.containsKey(attribute.getType())) {
        return AdaptedUse.FULL;
      }
      return AdaptedUse.NONE;
    }

    private LocalEnumUse inferEnum() {
      if (adapterUse == AdaptedUse.FULL) {
        return LocalEnumUse.NONE;
      }

      if (adapterUse != AdaptedUse.FIRST && adapterUse != AdaptedUse.BOTH) {
        if (attribute.isOptionalType() || attribute.isCollectionType() || attribute.isArrayType()) {
          if (attribute.isRequiresMarshalingAdapter()
              && enums.definitionsByQualifiedName.containsKey(attribute.firstTypeParameter())) {
            return LocalEnumUse.FIRST;
          }
          return LocalEnumUse.NONE;
        }
      }
      if (adapterUse != AdaptedUse.BOTH) {
        if (attribute.isMapType()) {
          if (attribute.isRequiresMarshalingAdapter()
              && enums.definitionsByQualifiedName.containsKey(attribute.firstTypeParameter())) {

            if (adapterUse != AdaptedUse.SECOND) {
              if (attribute.isRequiresMarshalingSecondaryAdapter()
                  && enums.definitionsByQualifiedName.containsKey(attribute.secondTypeParameter())) {
                return LocalEnumUse.BOTH;
              }
            }

            return LocalEnumUse.FIRST;
          }

          if (adapterUse != AdaptedUse.SECOND) {
            if (attribute.isRequiresMarshalingSecondaryAdapter()
                && enums.definitionsByQualifiedName.containsKey(attribute.secondTypeParameter())) {
              return LocalEnumUse.SECOND;
            }
          }

          return LocalEnumUse.NONE;
        }
      }

      if (attribute.isRequiresMarshalingAdapter()
          && !enums.definitionsByQualifiedName.containsKey(attribute.getType())) {
        return LocalEnumUse.FULL;
      }

      return LocalEnumUse.NONE;
    }

    public boolean isSimple() {
      return adapterUse == AdaptedUse.FULL
          || enumUse == LocalEnumUse.FULL
          || attribute.typeKind().isRegular();
    }

    public boolean isComplex() {
      return !isSimple();
    }

    boolean useEnum() {
      return enumUse != LocalEnumUse.NONE;
    }

    @Nullable
    EnumDefinition enumFull() {
      if (enumUse == LocalEnumUse.FULL) {
        return enums.apply(attribute.getType());
      }
      return null;
    }

    @Nullable
    EnumDefinition enumFirst() {
      if (enumUse == LocalEnumUse.FIRST || enumUse == LocalEnumUse.BOTH) {
        return enums.apply(attribute.firstTypeParameter());
      }
      return null;
    }

    @Nullable
    EnumDefinition enumSecond() {
      if (enumUse == LocalEnumUse.SECOND || enumUse == LocalEnumUse.BOTH) {
        return enums.apply(attribute.secondTypeParameter());
      }
      return null;
    }

    boolean useAdapter() {
      return adapterUse != AdaptedUse.NONE;
    }

    @Nullable
    AdapterSpecifier adapterFull() {
      if (adapterUse == AdaptedUse.FULL) {
        if (!attribute.getJsonQualiferAnnotations().isEmpty()) {
          return new AdapterSpecifier(attribute.getType(), true);
        }
        return AdapterSpecifier.from(attribute.getType());
      }
      return null;
    }

    @Nullable
    AdapterSpecifier adapterFirst() {
      if (adapterUse == AdaptedUse.FIRST || adapterUse == AdaptedUse.BOTH) {
        return AdapterSpecifier.from(attribute.firstTypeParameter());
      }
      return null;
    }

    @Nullable
    AdapterSpecifier adapterSecond() {
      if (adapterUse == AdaptedUse.SECOND || adapterUse == AdaptedUse.BOTH) {
        return AdapterSpecifier.from(attribute.secondTypeParameter());
      }
      return null;
    }

    static class AdapterSpecifier {
      final String type;
      final boolean reflect;

      AdapterSpecifier(String type, boolean reflect) {
        this.type = type;
        this.reflect = reflect;
      }

      static AdapterSpecifier from(String type) {
        return new AdapterSpecifier(type, type.indexOf('<') > 0);
      }
    }
  }

  OkTypeAdapterTypes adapted;

  final Function<OkTypeAdapterTypes, Void> setCurrent = new Function<OkTypeAdapterTypes, Void>() {
    @Override
    public Void apply(OkTypeAdapterTypes input) {
      adapted = input;
      return null;
    }
  };

  Iterable<OkTypeAdapterTypes> typeAdapters() {
    Multimap<AbstractDeclaring, ValueType> byDeclaring = HashMultimap.create();
    for (ValueType value : values().values()) {
      Protoclass protoclass = value.constitution.protoclass();
      if (protoclass.kind().isValue()) {
        Optional<AbstractDeclaring> typeAdaptersProvider = protoclass.okTypeAdaptersProvider();
        if (typeAdaptersProvider.isPresent()) {
          byDeclaring.put(typeAdaptersProvider.get(), value);
        } else if (protoclass.okJsonTypeAdapters().isPresent()
            && protoclass.declaringType().isPresent()) {
          DeclaringType topLevel = protoclass.declaringType().get().associatedTopLevel();
          byDeclaring.put(topLevel, value);
        }
      }
    }

    ImmutableList.Builder<OkTypeAdapterTypes> builder = ImmutableList.builder();
    for (Entry<AbstractDeclaring, Collection<ValueType>> entry : byDeclaring.asMap().entrySet()) {
      builder.add(ImmutableOkTypeAdapterTypes.builder()
          .definedBy(entry.getKey())
          .addAllTypes(entry.getValue())
          .build());
    }

    return builder.build();
  }

  static class AttributesByFirstLetter {
    final Multimap<Character, ValueAttribute> byFirst;
    final Map<Character, Collection<ValueAttribute>> asMap;

    AttributesByFirstLetter(Iterable<ValueAttribute> attributes) {
      ImmutableMultimap.Builder<Character, ValueAttribute> builder = ImmutableMultimap.builder();

      for (ValueAttribute attribute : attributes) {
        String name = attribute.getMarshaledName();
        char firstChar = name.charAt(0);
        builder.put(firstChar, attribute);
      }

      byFirst = builder.build();
      asMap = byFirst.asMap();
    }

    Collection<ValueAttribute> values() {
      return byFirst.values();
    }

    boolean useFlatIfElse() {
      return byFirst.keySet().size() < 4
          || byFirst.values().size() < 4;
    }
  }

  final Function<Iterable<ValueAttribute>, AttributesByFirstLetter> byFirstCharacter =
      new Function<Iterable<ValueAttribute>, AttributesByFirstLetter>() {
        @Override
        public AttributesByFirstLetter apply(Iterable<ValueAttribute> attributes) {
          return new AttributesByFirstLetter(attributes);
        }
      };

  @Generator.Typedef
  EnumAllDefinitions EnumAllDefinitions;

  @Generator.Typedef
  EnumDefinition EnumDefinition;

  @Generator.Typedef
  EnumConstant EnumConstant;

  static class EnumAllDefinitions implements Function<String, EnumDefinition> {
    final Map<String, EnumDefinition> definitionsByQualifiedName = Maps.newHashMap();
    final Set<String> takenSimpleNames = Sets.newHashSet();

    EnumAllDefinitions(List<ValueType> types, AbstractDeclaring declaring) {
      for (ValueType t : types) {
        for (ValueAttribute v : t.getMarshaledAttributes()) {
          addEnumDefintions(declaring, v, ConvertionDirection.TO_STRING);
        }
        for (ValueAttribute v : t.getUnmarshaledAttributes()) {
          addEnumDefintions(declaring, v, ConvertionDirection.FROM_STRING);
        }
      }
    }

    private void addEnumDefintions(
        AbstractDeclaring declaring,
        ValueAttribute v,
        ConvertionDirection direction) {

      if (!v.getJsonQualiferAnnotations().isEmpty()) {
        // don't handle qualified enums as they should be
        // resolved using adapters etc
        return;
      }

      for (TypeElement e : v.getEnumElements()) {
        String qualified = e.getQualifiedName().toString();
        if (inDefinitionCoveredByAdapter(declaring, qualified)) {
          @Nullable EnumDefinition definition = definitionsByQualifiedName.get(qualified);
          if (definition == null) {
            String simple = unambigousSimpleName(takenSimpleNames, e);
            definition = new EnumDefinition(e, qualified, simple);
            definitionsByQualifiedName.put(qualified, definition);
          }
          definition.conversions.add(direction);
        }
      }
    }

    private boolean inDefinitionCoveredByAdapter(AbstractDeclaring declaring, String qualified) {
      return qualified.startsWith(declaring.asPrefix());
    }

    private String unambigousSimpleName(Set<String> simpleNames, TypeElement e) {
      String base = CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_CAMEL, e.getSimpleName().toString());
      String simple = base;
      for (int c = 0; !simpleNames.add(simple); c++) {
        simple = base + c;
      }
      return simple;
    }

    Collection<EnumDefinition> all() {
      return definitionsByQualifiedName.values();
    }

    @Override
    public EnumDefinition apply(String input) {
      return definitionsByQualifiedName.get(input);
    }
  }

  enum ConvertionDirection {
    TO_STRING, FROM_STRING
  }

  static class EnumConstant {
    final String name;
    final String json;

    EnumConstant(String name, String json) {
      this.name = name;
      this.json = json;
    }
  }

  static class EnumDefinition {
    final String qualified;
    final String simple;
    final Set<ConvertionDirection> conversions = EnumSet.noneOf(ConvertionDirection.class);
    final Multimap<Character, EnumConstant> byFirstLetter = HashMultimap.create();

    EnumDefinition(TypeElement element, String qualified, String simple) {
      this.qualified = qualified;
      this.simple = simple;

      for (Element e : element.getEnclosedElements()) {
        if (e.getKind() == ElementKind.ENUM_CONSTANT) {
          Optional<OkNamedMirror> nameAnnotation = OkNamedMirror.find(e);
          String name = e.getSimpleName().toString();
          String jsonName = name;

          if (nameAnnotation.isPresent()) {
            String s = nameAnnotation.get().name();
            // just ignore annotation with empty name
            if (!s.isEmpty()) {
              jsonName = s;
            }
          }
          byFirstLetter.put(
              jsonName.charAt(0),
              new EnumConstant(name, jsonName));
        }
      }
    }

    boolean useFlatIfElse() {
      return byFirstLetter.keySet().size() < 4
          || byFirstLetter.values().size() < 4;
    }

    Collection<EnumConstant> constants() {
      return byFirstLetter.values();
    }

    boolean useToString() {
      return conversions.contains(ConvertionDirection.TO_STRING);
    }

    boolean useFromString() {
      return conversions.contains(ConvertionDirection.FROM_STRING);
    }
  }
}
