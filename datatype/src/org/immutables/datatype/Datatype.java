package org.immutables.datatype;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;

public interface Datatype<T> {
  String name();

  Type type();

  boolean isInline();

  List<Feature<T, ?>> features();

  <F> F get(Feature<T, F> feature, T instance);

  boolean isInstantiable();

  default Set<Datatype<? extends T>> cases() {
    return Collections.emptySet();
  }

  Builder<T> builder();

  interface Builder<T> {
    List<Violation> verify();

    <F> void set(Feature<T, F> feature, F value);

    T build();
  }

  interface Feature<T, F> {
    int index();

    boolean nullable();

    String name();

    Type type();

    boolean supportsInput();

    boolean supportsOutput();

    boolean omittableOnInput();

    boolean ignorableOnOutput();

    static <T, F> Feature<T, F> of(
        int index,
        String owner,
        String name,
        Type type,
        boolean nullable,
        boolean setter,
        boolean getter,
        boolean omittable,
        boolean ignorable) {
      return new Feature<T, F>() {
        @Override
        public int index() {
          return index;
        }

        @Override
        public boolean nullable() {
          return nullable;
        }

        @Override
        public String name() {
          return name;
        }

        @Override
        public Type type() {
          return type;
        }

        @Override
        public boolean supportsInput() {
          return setter;
        }

        @Override
        public boolean supportsOutput() {
          return getter;
        }

        @Override
        public boolean omittableOnInput() {
          return omittable;
        }

        @Override
        public boolean ignorableOnOutput() {
          return ignorable;
        }

        @Override
        public String toString() {
          return String.format("%s.%s: %s%s(getter:%s,setter:%s,omittable:%s,ignorable:%s",
              owner,
              name,
              type,
              nullable ? "?" : "",
              getter,
              setter,
              omittable,
              ignorable);
        }
      };
    }
  }

  default Feature<T, ?> feature(String name) {
    for (Feature<T, ?> f : features()) {
      if (f.name().equals(name)) {
        return f;
      }
    }
    throw new NoSuchElementException(name);
  }

  @SuppressWarnings("unchecked") // runtime token check
  default <F> Feature<T, F> feature(String name, Type type) {
    for (Feature<T, ?> f : features()) {
      if (f.name().equals(name) && f.type().equals(type)) {
        return (Feature<T, F>) f;
      }
    }
    throw new NoSuchElementException(name + ": " + type);
  }

  interface Violation {
    Optional<Feature<?, ?>> feature();

    String rule();

    String message();

    static Violation of(String rule, String message, Feature<?, ?> setter) {
      return new Violation() {
        @Override
        public Optional<Feature<?, ?>> feature() {
          return Optional.of(setter);
        }

        @Override
        public String rule() {
          return rule;
        }

        @Override
        public String message() {
          return message;
        }

        @Override
        public String toString() {
          return String.format("'%s': %s (%s)", setter.name(), message, rule);
        }
      };
    }

    static Violation of(String rule, String message) {
      return new Violation() {
        @Override
        public Optional<Feature<?, ?>> feature() {
          return Optional.empty();
        }

        @Override
        public String rule() {
          return rule;
        }

        @Override
        public String message() {
          return message;
        }

        @Override
        public String toString() {
          return String.format("%s (%s)", message, rule);
        }
      };
    }
  }
}
