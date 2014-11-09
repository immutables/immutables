package org.immutables.value.processor.meta;

import com.google.common.collect.Maps;
import org.immutables.generator.Naming;
import org.immutables.value.Value.NamingStyle;

import java.util.Map;

@NamingStyle
public final class NamingStyles {
  // Let it be static non-thread-safe cache, it's ok for now
  private static final Map<NamingStyle, NamingStyles> cache = Maps.newHashMap();

  private static class DefaultStyle {
    static NamingStyle style = NamingStyles.class.getAnnotation(NamingStyle.class);
  }

  public static NamingStyle defaultStyle() {
    return DefaultStyle.style;
  }

  public static NamingStyles using(NamingStyle style) {
    NamingStyles namings = cache.get(style);
    if (namings == null) {
      namings = new NamingStyles(style);
      cache.put(style, namings);
    }
    return namings;
  }

  public final NamingStyle style;
  final Scheme scheme;

  private NamingStyles(NamingStyle style) {
    this.style = style;
    this.scheme = new Scheme();
  }

  public UsingName.TypeNames forType(String name) {
    return new UsingName(name, scheme).new TypeNames();
  }

  public UsingName.AttributeNames forAccessor(String name) {
    return new UsingName(name, scheme).new AttributeNames();
  }

  private class Scheme {
    Naming typeImmutable = Naming.from(style.typeImmutable());
    Naming typeImmutableNested = Naming.from(style.typeImmutableNested());
    Naming typeImmutableNesting = Naming.from(style.typeImmutableNesting());
    Naming of = Naming.from(style.of());
    Naming copyOf = Naming.from(style.copyOf());

    Naming typeBuilder = Naming.from(style.typeBuilder());
    Naming builder = Naming.from(style.builder());
    Naming build = Naming.from(style.build());

    Naming typeModifiable = Naming.from(style.typeModifiable());
    Naming create = Naming.from(style.create());
    Naming toImmutable = Naming.from(style.toImmutable());

    Naming[] get = Naming.fromAll(style.get());
    Naming init = Naming.from(style.init());
    Naming with = Naming.from(style.with());

    Naming set = Naming.from(style.set());
    Naming hasSet = Naming.from(style.hasSet());
    Naming unset = Naming.from(style.unset());
    Naming clear = Naming.from(style.clear());

    Naming add = Naming.from(style.add());
    Naming addAll = Naming.from(style.addAll());
    Naming put = Naming.from(style.put());
    Naming putAll = Naming.from(style.putAll());
  }

  public static class UsingName {
    private final String name;
    private final Scheme scheme;

    UsingName(String name, Scheme scheme) {
      this.name = name;
      this.scheme = scheme;
    }

    String detectRawFromGet() {
      for (Naming naming : scheme.get) {
        String raw = naming.detect(name);
        if (!raw.isEmpty()) {
          return raw;
        }
      }
      return name;
    }

    public class TypeNames {
      public final String raw = name;
      public final String typeImmutable = scheme.typeImmutable.apply(raw);
      public final String typeImmutableNesting = scheme.typeImmutable.apply(raw);
      public final String typeImmutableNested = scheme.typeImmutable.apply(raw);
      public final String of = scheme.of.apply(raw);
      public final String copyOf = scheme.copyOf.apply(raw);

      public final String typeBuilder = scheme.typeBuilder.apply(raw);
      public final String builder = scheme.builder.apply(raw);
      public final String build = scheme.build.apply(raw);

      public final String typeModifiable = scheme.typeModifiable.apply(raw);
      public final String create = scheme.create.apply(raw);
      public final String toImmutable = scheme.toImmutable.apply(raw);
    }

    public class AttributeNames {
      public final String raw = detectRawFromGet();
      public final String get = name;
      public final String init = scheme.init.apply(raw);
      public final String with = scheme.with.apply(raw);
      public final String add = scheme.add.apply(raw);
      public final String addAll = scheme.addAll.apply(raw);
      public final String put = scheme.put.apply(raw);
      public final String putAll = scheme.putAll.apply(raw);

//    public final String set = scheme.set.apply(raw);
//    public final String hasSet = scheme.hasSet.apply(raw);
//    public final String unset = scheme.unset.apply(raw);
//    Naming clear = Naming.from(style.clear());
    }
  }
}
