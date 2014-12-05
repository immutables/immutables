package org.immutables.value.processor.meta;

import org.immutables.value.Value.Immutable;
import com.google.common.collect.Maps;
import java.util.Map;
import org.immutables.generator.Naming;
import org.immutables.value.Value.Style;

public final class Styles {
  // Let it be static non-thread-safe cache, it's ok for now
  private static final Map<Style, Styles> cache = Maps.newHashMap();

  @Style
  private static class DefaultStyle {
    static Style style = DefaultStyle.class.getAnnotation(Style.class);
  }

  public static Style defaultStyle() {
    return DefaultStyle.style;
  }

  public static Styles using(Style style) {
    Styles namings = cache.get(style);
    if (namings == null) {
      namings = new Styles(style);
      cache.put(style, namings);
    }
    return namings;
  }

  private final Style style;
  private final Scheme scheme;

  private Styles(Style style) {
    this.style = style;
    this.scheme = new Scheme();
  }

  public Immutable defaults() {
    return style.defaults();
  }

  public UsingName.TypeNames forType(String name) {
    return new UsingName(name, scheme, "").new TypeNames();
  }

  public UsingName.AttributeNames forAccessorWithRaw(String name, String raw) {
    return new UsingName(name, scheme, raw).new AttributeNames();
  }

  public UsingName.AttributeNames forAccessor(String name) {
    return forAccessorWithRaw(name, "");
  }

  private class Scheme {
    Naming[] typeAbstract = Naming.fromAll(style.typeAbstract());
    Naming typeImmutable = Naming.from(style.typeImmutable());
    Naming typeImmutableNested = Naming.from(style.typeImmutableNested());
    Naming typeImmutableEnclosing = Naming.from(style.typeImmutableEnclosing());
    Naming of = Naming.from(style.of());
    Naming copyOf = Naming.from(style.copyOf());
    Naming instance = Naming.from(style.instance());

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
//    Naming hasSet = Naming.from(style.hasSet());
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
    private final String forcedRaw;

    private UsingName(String name, Scheme scheme, String forcedRaw) {
      this.name = name;
      this.scheme = scheme;
      this.forcedRaw = forcedRaw;
    }

    private String detectRawFromGet() {
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
      for (Naming naming : scheme.typeAbstract) {
        String raw = naming.detect(name);
        if (!raw.isEmpty()) {
          return raw;
        }
      }
      return name;
    }

    public class TypeNames {
      public final String raw = detectRawFromAbstract();
      public final String typeAbstract = name;
      public final String typeImmutable = scheme.typeImmutable.apply(raw);
      public final String typeImmutableNesting = scheme.typeImmutableEnclosing.apply(raw);
      public final String typeImmutableNested = scheme.typeImmutableNested.apply(raw);
      public final String of = scheme.of.apply(raw);
      public final String copyOf = scheme.copyOf.apply(raw);
      public final String instance = scheme.instance.apply(raw);

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

      public final String set = scheme.set.apply(raw);
//      public final String hasSet = scheme.hasSet.apply(raw);
      public final String unset = scheme.unset.apply(raw);
      public final String clear = scheme.clear.apply(raw);
    }
  }
}
