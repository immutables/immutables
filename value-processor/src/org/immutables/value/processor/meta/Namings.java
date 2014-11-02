package org.immutables.value.processor.meta;

import org.immutables.generator.Naming;
import org.immutables.value.Value.NamingStyle;

public final class Namings {
  public Namings() {}

  public static UsingStyle using(NamingStyle style) {
    return new UsingStyle(style);
  }

  public static class UsingStyle {
    final NamingStyle style;
    final Scheme scheme;

    UsingStyle(NamingStyle style) {
      this.style = style;
      this.scheme = new Scheme();
    }

    class Scheme {
      Naming immutableType = Naming.from(style.immutableType());
      Naming of = Naming.from(style.of());
      Naming copyOf = Naming.from(style.copyOf());

      Naming builderType = Naming.from(style.builderType());
      Naming builder = Naming.from(style.builder());
      Naming build = Naming.from(style.build());

      Naming modifiableType = Naming.from(style.modifiableType());
      Naming create = Naming.from(style.create());
      Naming toImmutable = Naming.from(style.toImmutable());

      Naming[] get = Naming.fromAll(style.get());
      Naming init = Naming.from(style.init());
      Naming with = Naming.from(style.with());

      Naming set = Naming.from(style.set());
      Naming hasSet = Naming.from(style.hasSet());
      Naming unset = Naming.from(style.unset());

      Naming add = Naming.from(style.add());
      Naming addAll = Naming.from(style.addAll());
    }

    public UsingName.TypeNames type(String name) {
      return new UsingName(name, scheme).new TypeNames();
    }

    public UsingName.AccessorNames accessor(String name) {
      return new UsingName(name, scheme).new AccessorNames();
    }
  }

  public static class UsingName {
    private final String name;
    private final UsingStyle.Scheme scheme;

    UsingName(String name, UsingStyle.Scheme scheme) {
      this.name = name;
      this.scheme = scheme;
    }

    public class TypeNames {
      public final String raw = name;
      public final String Immutable = scheme.immutableType.apply(raw);
    }

    public class AccessorNames {
      public final String raw = name;
    }
  }
}
