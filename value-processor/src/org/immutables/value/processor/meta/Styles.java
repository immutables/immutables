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

import com.google.common.base.CaseFormat;
import org.immutables.generator.Naming;

public final class Styles {
  private final StyleInfo style;
  private final Scheme scheme;
  private final Depluralizer depluralizer;

  Styles(StyleInfo style) {
    this.style = style;
    this.depluralizer = depluralizerFor(style);
    this.scheme = new Scheme();
  }

  private static Depluralizer depluralizerFor(StyleInfo style) {
    return style.depluralize()
        ? new Depluralizer.DictionaryAidedDepluralizer(style.depluralizeDictionary())
        : Depluralizer.NONE;
  }

  public ValueImmutableInfo defaults() {
    return style.defaults();
  }

  public StyleInfo style() {
    return style;
  }

  public UsingName.TypeNames forType(String name) {
    return new UsingName(name, scheme, depluralizer, "").new TypeNames();
  }

  public UsingName.AttributeNames forAccessorWithRaw(String name, String raw) {
    return new UsingName(name, scheme, depluralizer, raw).new AttributeNames();
  }

  public UsingName.AttributeNames forAccessor(String name) {
    return forAccessorWithRaw(name, "");
  }

  class Scheme {
    Naming[] typeAbstract = Naming.fromAll(style.typeAbstract());
    Naming typeImmutable = Naming.from(style.typeImmutable());
    Naming typeImmutableNested = Naming.from(style.typeImmutableNested());
    Naming typeImmutableEnclosing = Naming.from(style.typeImmutableEnclosing());
    Naming of = Naming.from(style.of());
    Naming copyOf = Naming.from(style.copyOf());
    Naming instance = Naming.from(style.instance());

    Naming typeBuilder = Naming.from(style.typeBuilder());
    Naming typeInnerBuilder = Naming.from(style.typeInnerBuilder());
    Naming from = Naming.from(style.from());
    Naming build = Naming.from(style.build());
    Naming buildOrThrow = Naming.from(style.buildOrThrow());

    Naming builder = Naming.from(style.builder());
    Naming newBuilder = Naming.from(style.newBuilder());

    Naming[] get = Naming.fromAll(style.get());
    Naming init = Naming.from(style.init());
    Naming with = Naming.from(style.with());

    Naming add = Naming.from(style.add());
    Naming addAll = Naming.from(style.addAll());
    Naming put = Naming.from(style.put());
    Naming putAll = Naming.from(style.putAll());

    Naming isInitialized = Naming.from(style.isInitialized());
    Naming isSet = Naming.from(style.isSet());
    Naming unset = Naming.from(style.unset());
    Naming set = Naming.from(style.set());
    Naming clear = Naming.from(style.clear());
    Naming create = Naming.from(style.create());
    Naming toImmutable = Naming.from(style.toImmutable());
    Naming typeModifiable = Naming.from(style.typeModifiable());
    Naming typeWith = Naming.from(style.typeWith());
  }

  public static class UsingName {
    private final String name;
    private final Scheme scheme;
    private final String forcedRaw;
    private final Depluralizer depluralizer;

    private UsingName(String name, Scheme scheme, Depluralizer depluralizer, String forcedRaw) {
      this.name = name;
      this.scheme = scheme;
      this.depluralizer = depluralizer;
      this.forcedRaw = forcedRaw;
    }

    private String detectRawFromGet() {
      return Keywords.safeIdentifier(detectRawFrom(name), name);
    }

    private String detectRawFrom(String name) {
      if (!forcedRaw.isEmpty()) {
        return forcedRaw;
      }
      for (Naming naming : scheme.get) {
        String raw = naming.detect(name);
        if (!raw.isEmpty()) {
          return raw;
        }
      }
      return name;
    }

    private String detectRawFromAbstract() {
      if (!forcedRaw.isEmpty()) {
        return forcedRaw;
      }
      return detectRawFromAbstract(name);
    }

    /** Forced raw will not work if using this method */
    String detectRawFromAbstract(String abstractName) {
      for (Naming naming : scheme.typeAbstract) {
        String raw = naming.detect(abstractName);
        if (!raw.isEmpty()) {
          // TBD is there a way to raise abstraction
          return CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_CAMEL, raw);
        }
      }
      return abstractName;
    }

    public class TypeNames {
      public final Scheme namings = scheme;
      public final String raw = detectRawFromAbstract();
      public final String typeAbstract = name;
      public final String typeImmutable = scheme.typeImmutable.apply(raw);
      public final String of = scheme.of.apply(raw);
      public final String instance = scheme.instance.apply(raw);
      // Builder template is being applied programatically in Constitution class
      // public final String typeBuilder = scheme.typeBuilder.apply(raw);
      public final String copyOf = scheme.copyOf.apply(raw);
      public final String from = scheme.from.apply(raw);
      public final String build = scheme.build.apply(raw);

      public final String typeImmutableEnclosing() {
        return scheme.typeImmutableEnclosing.apply(raw);
      }

      public final String typeImmutableNested() {
        return scheme.typeImmutableNested.apply(raw);
      }

      public final String typeWith() {
        return scheme.typeWith.apply(raw);
      }

      public final String builder() {
        return scheme.builder.apply(raw);
      }

      public final String buildOrThrow() {
        return scheme.buildOrThrow.apply(raw);
      }

      public final String isInitialized() {
        return scheme.isInitialized.apply(raw);
      }

      public final String create() {
        return scheme.create.apply(raw);
      }

      public final String clear() {
        return scheme.clear.apply(raw);
      }

      public final String toImmutable() {
        return scheme.toImmutable.apply(raw);
      }

      public final String typeModifiable() {
        return scheme.typeModifiable.apply(raw);
      }

      String rawFromAbstract(String abstractName) {
        return detectRawFromAbstract(abstractName);
      }
    }

    public class AttributeNames {
      public final String raw = detectRawFromGet();
      public final String get = name;
      public final String init = scheme.init.apply(raw);
      public final String with = scheme.with.apply(raw);
      private ForCollections coll = null;

      public String add() {
        return collectionSpecific().add;
      }

      public String put() {
        return collectionSpecific().put;
      }

      public String addAll() {
        return collectionSpecific().addAll;
      }

      public String putAll() {
        return collectionSpecific().putAll;
      }

      public String set() {
        return scheme.set.apply(raw);
      }

      public String isSet() {
        return scheme.isSet.apply(raw);
      }

      public String unset() {
        return scheme.unset.apply(raw);
      }

      private ForCollections collectionSpecific() {
        return coll == null ? coll = new ForCollections() : coll;
      }

      public class ForCollections {
        final String singular = depluralizer.depluralize(raw);
        final String add = scheme.add.apply(singular);
        final String put = scheme.put.apply(singular);
        final String addAll = scheme.addAll.apply(raw);
        final String putAll = scheme.putAll.apply(raw);

      }
    }
  }
}
